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
 * Draws the four stages. Vanilla gives us two model sizes (baby and adult); scaling each of them
 * down once more turns that into a four-step ramp, so a calf visibly grows instead of popping from
 * baby to adult. Hitboxes stay vanilla - this is presentation only.
 */
@Mod.EventBusSubscriber(modid = BabyNeeds.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientRenderEvents {

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

        if (entry.lying()) {
            tipOntoSide(pose, entity, event.getPartialTick());
        }
    }

    /**
     * A sow settled down for a nursing bout. There is no vanilla lying pose for pigs, so she is
     * rolled onto her side by hand.
     *
     * The roll has to happen about the axis she is facing along, not about a fixed world axis:
     * this event fires before vanilla applies her body rotation, so a plain Z-axis roll tips every
     * pig the same way in world space and any pig not facing due north or south ends up lying
     * diagonally. Rolling about her own forward vector lands her flat on her flank whichever way
     * she happens to be pointing.
     */
    private static void tipOntoSide(PoseStack pose, LivingEntity entity, float partialTick) {
        float bodyYaw = Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot);
        double radians = Math.toRadians(bodyYaw);
        float forwardX = -(float) Math.sin(radians);
        float forwardZ = (float) Math.cos(radians);

        // Lift the pivot to roughly half her standing height first, so rolling her does not bury
        // half the body in the ground.
        pose.translate(0.0, 0.45, 0.0);
        pose.mulPose(new Quaternionf().rotateAxis((float) Math.toRadians(90.0), forwardX, 0.0f, forwardZ));
    }

    @SubscribeEvent
    static void onRenderPost(RenderLivingEvent.Post<?, ?> event) {
        if (ClientStageCache.get(event.getEntity().getId()) == null) return;
        event.getPoseStack().popPose();
    }
}
