package com.babyneeds;

import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Quaternionf;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Draws the four growth stages and the body poses. Vanilla gives us two model sizes (baby and
 * adult); scaling each of them down once more turns that into a four-step ramp, so a calf visibly
 * grows instead of popping from baby to adult.
 *
 * The poses are built the same way, out of transforms rather than model changes. There is no sitting
 * or lying pig in vanilla, so a pig sits by tipping back onto its haunches and lies by rolling onto
 * its flank - and every one of those is driven by an eased progress value, so the animal folds down
 * over about a second instead of snapping between states. Hitboxes stay vanilla throughout: this is
 * presentation only.
 */
@Mod.EventBusSubscriber(modid = BabyNeeds.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientRenderEvents {

    /** How far back a sitting animal tips onto its haunches. */
    private static final float SIT_ANGLE = 24.0f;
    /** Flat on the flank. */
    private static final float SIDE_ANGLE = 90.0f;
    /** How far a wallowing animal rocks either side of flat, and how fast. */
    private static final float WALLOW_ROCK_ANGLE = 55.0f;
    private static final float WALLOW_ROCK_SPEED = 1.9f;

    @SubscribeEvent
    static void onRenderPre(RenderLivingEvent.Pre<?, ?> event) {
        LivingEntity entity = event.getEntity();
        ClientStageCache.Entry entry = ClientStageCache.get(entity.getId());
        if (entry == null) return;

        PoseStack pose = event.getPoseStack();
        pose.pushPose();

        float scale = entry.stage().renderScale();
        if (scale != 1.0f) {
            pose.scale(scale, scale, scale);
        }

        applyPose(pose, entity, entry, event.getPartialTick());
    }

    @SubscribeEvent
    static void onRenderPost(RenderLivingEvent.Post<?, ?> event) {
        if (ClientStageCache.peek(event.getEntity().getId()) == null) return;
        event.getPoseStack().popPose();
    }

    /**
     * Rolls and tips the body according to how far into its pose the animal is.
     *
     * The roll has to happen about the axis the animal is facing along, not about a fixed world
     * axis: this event fires before vanilla applies the body rotation, so a plain Z-axis roll tips
     * every pig the same way in world space and any pig not facing due north or south ends up lying
     * diagonally.
     */
    private static void applyPose(PoseStack pose, LivingEntity entity,
                                  ClientStageCache.Entry entry, float partialTick) {
        float side = entry.sideProgress();
        float sit = entry.sitProgress();
        if (side <= 0.0f && sit <= 0.0f) return;

        float bodyYaw = Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot);
        double radians = Math.toRadians(bodyYaw);
        float forwardX = -(float) Math.sin(radians);
        float forwardZ = (float) Math.cos(radians);
        // The animal's own left-right axis, perpendicular to the way it is facing.
        float rightX = forwardZ;
        float rightZ = -forwardX;

        if (sit > 0.0f) {
            // Sitting: the hindquarters go down and the chest comes up, pivoting about the rump.
            pose.translate(0.0, -0.12 * sit, 0.0);
            pose.mulPose(new Quaternionf().rotateAxis(
                    (float) Math.toRadians(SIT_ANGLE * sit), rightX, 0.0f, rightZ));
        }

        if (side > 0.0f) {
            // Lift the pivot as it goes over, so rolling the body does not bury half of it in the
            // ground on the way down.
            pose.translate(0.0, 0.45 * side, 0.0);

            float roll = SIDE_ANGLE * side;
            if (entry.pose() == PigPose.WALLOWING) {
                // Rocking from flank to flank - the actual motion that works mud into a pig's hide.
                roll += Mth.sin(entry.poseSeconds() * WALLOW_ROCK_SPEED) * WALLOW_ROCK_ANGLE * side;
            }
            pose.mulPose(new Quaternionf().rotateAxis(
                    (float) Math.toRadians(roll), forwardX, 0.0f, forwardZ));
        }
    }
}
