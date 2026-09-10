package com.babyneeds;

import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Quaternionf;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Draws the four growth stages and the body poses. Vanilla gives us two model sizes (baby and
 * adult); scaling each of them down once more turns that into a four-step ramp, so a calf visibly
 * grows instead of popping from baby to adult.
 *
 * The poses are built out of transforms rather than model changes, because vanilla has no sitting or
 * lying pose for a farm animal. Two rules keep that from looking like a glitch: everything is eased
 * over about a second rather than switched, and <em>eating never rotates the body</em> - lowering the
 * head is done by driving the animal's own head pitch, which is the difference between an animal
 * grazing and an animal falling over. Hitboxes stay vanilla throughout: this is presentation only.
 */
@Mod.EventBusSubscriber(modid = BabyNeeds.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientRenderEvents {

    /** Nose to the ground. Vanilla clamps a mob's own look to 40 degrees, which is not enough. */
    private static final float HEAD_DOWN_ANGLE = 62.0f;

    @SubscribeEvent
    static void onRenderPre(RenderLivingEvent.Pre<?, ?> event) {
        LivingEntity entity = event.getEntity();
        ClientStageCache.Entry entry = ClientStageCache.get(entity.getId());
        if (entry == null) return;

        applyHeadPitch(entity, entry);

        PoseStack pose = event.getPoseStack();
        pose.pushPose();

        float scale = entry.stage().renderScale();
        if (scale != 1.0f) {
            pose.scale(scale, scale, scale);
        }

        applyBodyPose(pose, entity, entry, event.getPartialTick());
    }

    @SubscribeEvent
    static void onRenderPost(RenderLivingEvent.Post<?, ?> event) {
        if (ClientStageCache.peek(event.getEntity().getId()) == null) return;
        event.getPoseStack().popPose();
    }

    /**
     * Lowers the head, and only the head. The renderer reads the entity's pitch a few lines after
     * this event fires and hands it to the model as the head rotation, so writing it here bends the
     * neck through vanilla's own rig - no model surgery, no body rotation, and it eases in and out
     * because the value it is driven by does.
     *
     * Sheep are left alone: they have a real eating animation in vanilla, and the goal fires it.
     */
    private static void applyHeadPitch(LivingEntity entity, ClientStageCache.Entry entry) {
        if (entity instanceof Sheep) return;

        float headDown = entry.headDownProgress();
        if (headDown <= 0.0f && entry.pose() != PigPose.HEAD_DOWN) return;

        float pitch = HEAD_DOWN_ANGLE * headDown;
        // Both the current and previous pitch, so the renderer's own interpolation has nothing to
        // fight with: the easing is already in headDown.
        entity.xRotO = pitch;
        entity.setXRot(pitch);
    }

    /**
     * Rolls and tips the body according to how far into its pose the animal is.
     *
     * The roll has to happen about the axis the animal is facing along, not about a fixed world
     * axis: this event fires before vanilla applies the body rotation, so a plain Z-axis roll tips
     * every pig the same way in world space and any pig not facing due north or south ends up lying
     * diagonally.
     */
    private static void applyBodyPose(PoseStack pose, LivingEntity entity,
                                      ClientStageCache.Entry entry, float partialTick) {
        // Sitting is not here: it is posed part by part inside SittingPigModel, because tipping a
        // whole model back is what buried the hind legs and pointed the head at the sky. Only the
        // poses that really are a whole-body roll - lying and wallowing - are transforms.
        float side = entry.sideProgress();
        if (side <= 0.0f) return;

        float bodyYaw = Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot);
        double radians = Math.toRadians(bodyYaw);
        float forwardX = -(float) Math.sin(radians);
        float forwardZ = (float) Math.cos(radians);

        if (side <= 0.0f) return;

        boolean rolling = entry.pose() == PigPose.WALLOWING;
        float roll;
        float axisHeight;

        if (rolling) {
            // Worked out from the game time the roll began rather than from a local stopwatch,
            // because the server is moving the animal sideways off the very same function. Any
            // other clock and the pig would slide instead of roll.
            double phase = WallowRoll.phase(entity.level().getGameTime(), partialTick,
                    entry.poseStart(), Config.WALLOW_ROLL_SPEED.get());
            roll = (float) Math.toDegrees(WallowRoll.angle(phase, Config.WALLOW_ROLL_ANGLE.get())) * side;

            // A rolling body turns about the axis running through its middle, a radius off the
            // ground - not about its feet. Turning it about its feet is a pendulum: it swings rather
            // than rolls, and anything past a quarter turn swings the body underground, which is
            // why a wallowing pig disappeared into the dirt.
            axisHeight = Config.WALLOW_ROLL_HEIGHT.get().floatValue();
            pose.translate(0.0, -Config.WALLOW_SINK.get().floatValue() * side, 0.0);
        } else {
            // Lying down is not rolling: a sow settling onto her flank really does pivot about the
            // ground she is lying on, and the lift is what keeps her flank from sinking through it.
            // Signed per animal, off the same tick the model reads, so the body goes down on the
            // flank whose legs are being posed. Every pig lying the same way looked mechanical.
            roll = LyingSide.roll(Config.LIE_ANGLE.get().floatValue() * side, entry.poseStart());
            axisHeight = 0.0f;
            pose.translate(0.0, Config.LIE_LIFT.get().floatValue() * side * side, 0.0);
        }

        Quaternionf rotation = new Quaternionf()
                .rotateAxis((float) Math.toRadians(roll), forwardX, 0.0f, forwardZ);

        if (axisHeight == 0.0f) {
            pose.mulPose(rotation);
            return;
        }
        pose.translate(0.0, axisHeight, 0.0);
        pose.mulPose(rotation);
        pose.translate(0.0, -axisHeight, 0.0);
    }
}
