package com.babyneeds;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = BabyNeeds.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class BabyNeedsClient {
    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        BabyNeeds.LOGGER.info("BabyNeeds client setup complete.");
    }
}
