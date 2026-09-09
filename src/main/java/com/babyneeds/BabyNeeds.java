package com.babyneeds;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(BabyNeeds.MODID)
public class BabyNeeds {
    public static final String MODID = "babyneeds";
    public static final Logger LOGGER = LogUtils.getLogger();

    public BabyNeeds() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        try {
            ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        } catch (Exception e) {
            LOGGER.error("Failed to register config", e);
        }

        modEventBus.addListener(this::onCommonSetup);
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        ModMessages.register();
        LOGGER.info("BabyNeeds setup complete.");
    }
}
