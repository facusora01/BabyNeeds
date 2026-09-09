package com.babyneeds;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client-side mirror of what the server told us about each animal, plus the animation state that
 * belongs to the client alone.
 *
 * The server only ever sends a target pose. Everything between the poses - a sow folding down onto
 * her side over a second, a pig easing back onto its haunches, the rocking of a wallow - is
 * computed here from elapsed real time, so a full animation costs exactly one packet and looks the
 * same regardless of tick rate or latency.
 */
@Mod.EventBusSubscriber(modid = BabyNeeds.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientStageCache {

    /** Seconds a full sit-down takes. Slow enough to read as an animal deciding to. */
    private static final float SIT_DOWN_SECONDS = 0.7f;
    /** A neck moves far faster than a whole body: this is a dip, not a fold. */
    private static final float HEAD_DOWN_SECONDS = 0.35f;

    public static class Entry {
        private GrowthStage stage;
        private PigPose pose;
        private float muddiness;

        /** 0 upright, 1 flat on the flank. Eased towards whatever the pose calls for. */
        private float sideProgress;
        /** 0 upright, 1 sat back on the haunches. */
        private float sitProgress;
        /** 0 head level, 1 nose to the ground. */
        private float headDownProgress;
        /** Seconds spent in the current pose, which drives the rocking of a wallow. */
        private float poseSeconds;
        private long lastFrameNanos;

        Entry(GrowthStage stage, PigPose pose, float muddiness) {
            this.stage = stage;
            this.pose = pose;
            this.muddiness = muddiness;
            this.lastFrameNanos = System.nanoTime();
            // Whatever pose the animal is in when it first comes into view is where it already is:
            // animals do not fold themselves up the moment a player walks round the corner.
            this.sideProgress = pose.isOnSide() ? 1.0f : 0.0f;
            this.sitProgress = pose == PigPose.SITTING ? 1.0f : 0.0f;
            this.headDownProgress = pose == PigPose.HEAD_DOWN ? 1.0f : 0.0f;
        }

        void retarget(GrowthStage stage, PigPose pose, float muddiness) {
            this.stage = stage;
            this.muddiness = muddiness;
            if (this.pose != pose) {
                this.pose = pose;
                this.poseSeconds = 0.0f;
            }
        }

        /** Advances the animation to now. Called once per rendered frame, per animal. */
        void advance() {
            long now = System.nanoTime();
            float delta = (now - lastFrameNanos) / 1_000_000_000.0f;
            lastFrameNanos = now;
            // A paused game or a stalled frame must not teleport the animation.
            delta = Math.min(Math.max(delta, 0.0f), 0.1f);
            poseSeconds += delta;

            float lieSeconds = Math.max(0.05f, Config.LIE_SECONDS.get().floatValue());
            sideProgress = approach(sideProgress, pose.isOnSide() ? 1.0f : 0.0f, delta / lieSeconds);
            sitProgress = approach(sitProgress, pose == PigPose.SITTING ? 1.0f : 0.0f, delta / SIT_DOWN_SECONDS);
            headDownProgress = approach(headDownProgress, pose == PigPose.HEAD_DOWN ? 1.0f : 0.0f,
                    delta / HEAD_DOWN_SECONDS);
        }

        private static float approach(float current, float target, float step) {
            if (current < target) return Math.min(target, current + step);
            return Math.max(target, current - step);
        }

        public GrowthStage stage() {
            return stage;
        }

        public PigPose pose() {
            return pose;
        }

        public float muddiness() {
            return muddiness;
        }

        /** Eased 0..1, so the body settles rather than snapping over. */
        public float sideProgress() {
            return smooth(sideProgress);
        }

        public float sitProgress() {
            return smooth(sitProgress);
        }

        public float headDownProgress() {
            return smooth(headDownProgress);
        }

        public float poseSeconds() {
            return poseSeconds;
        }

        /**
         * Smootherstep rather than smoothstep. The plain curve still leaves a visible corner at each
         * end, which is what makes a sow folding down read as rigid: this one starts and finishes
         * with zero acceleration as well as zero speed, so the movement has weight to it.
         */
        private static float smooth(float t) {
            return t * t * t * (t * (t * 6.0f - 15.0f) + 10.0f);
        }
    }

    private static final Map<Integer, Entry> ENTRIES = new ConcurrentHashMap<>();

    private ClientStageCache() {
    }

    public static void accept(StageSyncPayload payload) {
        Entry entry = ENTRIES.get(payload.entityId());
        if (entry == null) {
            ENTRIES.put(payload.entityId(),
                    new Entry(payload.stage(), payload.pose(), payload.muddiness()));
        } else {
            entry.retarget(payload.stage(), payload.pose(), payload.muddiness());
        }
    }

    /** Returns the entry with its animation advanced to this frame, or null if unknown. */
    public static Entry get(int entityId) {
        Entry entry = ENTRIES.get(entityId);
        if (entry != null) {
            entry.advance();
        }
        return entry;
    }

    /** Read without advancing time - for a second look at the same entity in the same frame. */
    public static Entry peek(int entityId) {
        return ENTRIES.get(entityId);
    }

    @SubscribeEvent
    static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            ENTRIES.clear();
        }
    }
}
