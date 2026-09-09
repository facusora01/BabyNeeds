package com.babyneeds;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

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
            // A sow nursing her litter. There is no vanilla lying pose for pigs, so she is tipped
            // onto her side and lifted clear of the ground.
            pose.translate(0.0, 0.35, 0.0);
            pose.mulPose(Axis.ZP.rotationDegrees(90.0f));
        }
    }

    @SubscribeEvent
    static void onRenderPost(RenderLivingEvent.Post<?, ?> event) {
        if (ClientStageCache.get(event.getEntity().getId()) == null) return;
        event.getPoseStack().popPose();
    }
}
