package com.babyneeds;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Which blocks count as pasture, and how healthy the pasture around a spot is. */
public final class PastureBlocks {

    private PastureBlocks() {
    }

    public static boolean isEdible(BlockState state) {
        return state.is(Blocks.GRASS) || state.is(Blocks.TALL_GRASS) || state.is(Blocks.FERN)
                || state.is(Blocks.GRASS_BLOCK);
    }


    /**
     * Ground with the turf off it, whichever way it got that way: eaten by the herd (which leaves
     * the mod's own grazed dirt) or already bare when the animals arrived.
     */
    public static boolean isBareSoil(BlockState state) {
        return state.is(ModBlocks.GRAZED_DIRT.get()) || state.is(Blocks.DIRT);
    }

    /**
     * Share of the soil around a spot that is still grass. Only soil is counted - walls, water and
     * paths are not pasture either way, so a pen against a cliff is not judged as overgrazed.
     */
    public static double grassRatioAround(BlockGetter level, BlockPos center, int radius) {
        int grass = 0;
        int total = 0;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                BlockState state = level.getBlockState(center.offset(dx, 0, dz));
                if (state.is(Blocks.GRASS_BLOCK) || isBareSoil(state) || state.is(Blocks.COARSE_DIRT)) {
                    total++;
                    if (state.is(Blocks.GRASS_BLOCK)) grass++;
                }
            }
        }
        return total == 0 ? 1.0 : (double) grass / total;
    }
}
