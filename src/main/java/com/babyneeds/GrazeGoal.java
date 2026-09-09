package com.babyneeds;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Cattle, sheep and goats feeding themselves, and wearing the pasture down while they do it. Every
 * mouthful - loose grass or the turf itself - strips the sward off the block underneath, leaving
 * bare dirt and starting that block's recovery clock. No soil is lost here: dirt grows back on its
 * own. It is only lost later, and only if the herd keeps standing on the same bare ground long
 * after grass should have returned.
 *
 * Pigs do not use this. They root instead - see {@link RootGoal}.
 */
public class GrazeGoal extends BlockConsumingGoal {

    public GrazeGoal(Animal animal, Species species) {
        super(animal, species);
    }

    @Override
    protected boolean isTarget(BlockState state) {
        return PastureBlocks.isEdible(state);
    }

    @Override
    protected int chewDuration() {
        return Config.GRAZE_CHEW_TICKS.get();
    }

    @Override
    protected void consume(BlockPos pos, BlockState state) {
        // Whether the mouthful is loose grass or the turf itself, the block that pays for it is the
        // soil under it: a grazing animal crops the sward down to the roots. Without this the herd
        // would nibble the decorative grass off a plains pen forever and the ground would never
        // visibly wear, which is the signal the whole rotation mechanic depends on.
        BlockPos turf = state.is(Blocks.GRASS_BLOCK) ? pos : pos.below();
        double grassRatio = PastureBlocks.grassRatioAround(
                animal.level(), turf, Config.PASTURE_SCAN_RADIUS.get());

        double food;
        if (state.is(Blocks.GRASS_BLOCK)) {
            food = PastureCalculator.foodValue(grassRatio, species.grassFoodValue());
        } else {
            // Loose grass is a fuller mouthful than bare turf, but it takes the sward with it.
            food = species.grassFoodValue();
            animal.level().destroyBlock(pos, false);
        }

        stripTurf(turf);
        feed(food);
    }
}
