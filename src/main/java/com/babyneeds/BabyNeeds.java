package com.babyneeds;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
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

        ModBlocks.register(modEventBus);

        modEventBus.addListener(this::onCommonSetup);
        modEventBus.addListener(this::onAttributeModify);
    }

    /**
     * Farm animals have no attack damage in vanilla, because they never attack. A mother defending
     * her baby needs some, or her charge is a shove with no consequence at all.
     */
    private void onAttributeModify(EntityAttributeModificationEvent event) {
        for (EntityType<? extends LivingEntity> type : event.getTypes()) {
            if (!event.has(type, Attributes.ATTACK_DAMAGE)) {
                event.add(type, Attributes.ATTACK_DAMAGE, 2.0);
            }
        }
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        ModMessages.register();
        LOGGER.info("BabyNeeds setup complete.");
    }
}
