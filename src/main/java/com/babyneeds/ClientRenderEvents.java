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

        if (side > 0.0f) {
            // Going down is not the same shape as being down: the body drops as it rolls, and the
            // lift only reaches its full height once it is actually on its flank. Rolling about a
            // fixed high pivot is what left a lying sow hanging in the air.
            float lift = Config.LIE_LIFT.get().floatValue() * side * side;
            if (entry.pose() == PigPose.WALLOWING) {
                lift -= Config.WALLOW_SINK.get().floatValue() * side;
            }
            pose.translate(0.0, lift, 0.0);

            float roll = Config.LIE_ANGLE.get().floatValue() * side;
            if (entry.pose() == PigPose.WALLOWING) {
                // Rocking from flank to flank - the actual motion that works mud into a pig's hide.
                roll += Mth.sin(entry.poseSeconds() * Config.WALLOW_ROCK_SPEED.get().floatValue())
                        * Config.WALLOW_ROCK_ANGLE.get().floatValue() * side;
            }
            pose.mulPose(new Quaternionf().rotateAxis(
                    (float) Math.toRadians(roll), forwardX, 0.0f, forwardZ));
        }
    }
}
