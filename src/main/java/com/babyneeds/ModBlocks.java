package com.babyneeds;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, BabyNeeds.MODID);

    /** The in-world form of the wallow. Copies water so it behaves like water in every other way. */
    public static final RegistryObject<LiquidBlock> MUDDY_WATER_BLOCK = BLOCKS.register("muddy_water",
            () -> new LiquidBlock(ModFluids.MUDDY_WATER, BlockBehaviour.Properties.copy(Blocks.WATER)));

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
    }
}
