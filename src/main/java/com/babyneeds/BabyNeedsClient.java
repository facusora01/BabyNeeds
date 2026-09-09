package com.babyneeds;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = BabyNeeds.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class BabyNeedsClient {

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        BabyNeeds.LOGGER.info("BabyNeeds client setup complete.");
    }

    /** Bolts the mud coating onto vanilla's pig renderer rather than replacing the renderer. */
    @SubscribeEvent
    static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        LivingEntityRenderer<Pig, EntityModel<Pig>> renderer = event.getRenderer(EntityType.PIG);
        if (renderer == null) return;
        renderer.addLayer(new MudLayer<>(renderer));
    }
}
