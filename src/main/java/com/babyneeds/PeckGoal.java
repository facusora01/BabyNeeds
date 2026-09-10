package com.babyneeds;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A chicken feeding itself: head down, working over the same ground a pig would, after seeds and
 * insects rather than roots.
 *
 * Mechanically this is the pig's rooting with two deliberate differences, and both of them are the
 * whole character of the bird. It takes far less out of a block, because a chicken is small and
 * lives off what it can find rather than off what it can dig up. And it <em>leaves the turf</em>: a
 * chicken scratches at grass, it does not plough it. A flock is therefore something a player can
 * keep on a good field without ruining it, which is exactly what a pig pen is not.
 *
 * What it does share is exhaustion. A patch worked over enough times has nothing left in it until
 * it has had time to come back, so a hen run still needs room - the birds have to keep moving even
 * though they leave no mark. Without that, one grass block would feed a flock forever.
 */
public class PeckGoal extends BlockConsumingGoal {

    public PeckGoal(Animal animal, Species species) {
        super(animal, species);
    }

    @Override
    protected boolean isTarget(BlockState state) {
        return state.is(Blocks.GRASS_BLOCK)
                || state.is(ModBlocks.GRAZED_DIRT.get())
                || state.is(Blocks.DIRT)
                || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.ROOTED_DIRT)
                || state.is(Blocks.PODZOL);
    }

    /** Ground already picked clean still looks like ground, so it has to be ruled out by its record. */
    @Override
    protected boolean isTargetAt(BlockPos pos, BlockState state) {
        if (!isTarget(state)) return false;
        if (!(animal.level() instanceof ServerLevel server)) return true;

        int timesWorked = PastureData.get(server).rootsAt(pos, server.getGameTime(),
                Config.ROOT_RECOVERY_TICKS.get());
        return timesWorked < Config.MAX_ROOTS_PER_BLOCK.get();
    }

    @Override
    protected int chewDuration() {
        return Config.PECK_TICKS.get();
    }

    @Override
    protected void consume(BlockPos pos, BlockState state) {
        if (!(animal.level() instanceof ServerLevel server)) return;

        int timesWorked = PastureData.get(server).root(pos, server.getGameTime(),
                Config.ROOT_RECOVERY_TICKS.get());
        double food = RootingCalculator.yield(
                Config.PECK_FOOD_VALUE.get(),
                1.0,
                false,
                timesWorked,
                Config.MAX_ROOTS_PER_BLOCK.get());

        // No stripTurf, and that omission is the point: the grass stays exactly where it was.
        server.levelEvent(2001, pos, net.minecraft.world.level.block.Block.getId(state));

        if (food > 0.0) {
            feed(food);
        }
    }
}
