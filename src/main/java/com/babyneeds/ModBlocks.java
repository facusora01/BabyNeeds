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

    /** The in-world form of the wallow. Copies water, then adds the drag of thick mud. */
    public static final RegistryObject<LiquidBlock> MUDDY_WATER_BLOCK = BLOCKS.register("muddy_water",
            () -> new MuddyWaterBlock(ModFluids.MUDDY_WATER, BlockBehaviour.Properties.copy(Blocks.WATER)));

    /**
     * Ground the herd has eaten the turf off. It is dirt in every way a player can tell - same
     * texture, same sound, same shovel, and it drops dirt - and differs in exactly one: it is not
     * {@code Blocks.DIRT}, and vanilla decides where to creep grass with a hard identity check
     * against that block rather than with a tag.
     *
     * That one difference is the whole pasture system. Grass otherwise floods back over grazed
     * ground within seconds, which resets the trampling record long before soil could ever be worn
     * out - and the previous fix, taking the turf off again after vanilla put it there, made the
     * field visibly flicker between grass and dirt. Ground that is simply never a valid target does
     * not flicker: it sits bare until its recovery window is up, and then it is turned back into
     * ordinary dirt and vanilla is welcome to it.
     */
    public static final RegistryObject<Block> GRAZED_DIRT = BLOCKS.register("grazed_dirt",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.DIRT)));

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
    }
}
