package com.babyneeds;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A pig feeding itself the way a pig actually does: nose down, turning the soil over for roots and
 * grubs. This is what makes pigs playable in a wallow pen, where by definition no grass grows - they
 * were never meant to live off pasture like a cow.
 *
 * It is not free food. A patch gives less every time it is worked over and is eventually exhausted
 * until it has had time to recover, so a pig pen supports fewer animals than a pasture of the same
 * size unless the farmer brings crops. Mud is the exception: it is rich, and a wallow feeds better
 * than dry ground, which is the second reason to build one.
 *
 * Rooting also destroys pasture outright - grass turned over by a pig comes up as bare dirt. That is
 * true to life and it means a pig pen should not be your good field.
 */
public class RootGoal extends BlockConsumingGoal {

    public RootGoal(Animal animal, Species species) {
        super(animal, species);
    }

    @Override
    protected boolean isTarget(BlockState state) {
        return state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.DIRT)
                || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.ROOTED_DIRT)
                || state.is(Blocks.PODZOL)
                || MuddyWater.isMud(state);
    }

    @Override
    protected int chewDuration() {
        return Config.ROOT_TICKS.get();
    }

    @Override
    protected void consume(BlockPos pos, BlockState state) {
        if (!(animal.level() instanceof ServerLevel server)) return;

        boolean rich = MuddyWater.isMud(state);
        int timesRooted = PastureData.get(server).root(pos, server.getGameTime(),
                Config.ROOT_RECOVERY_TICKS.get());
        double food = RootingCalculator.yield(
                Config.ROOT_FOOD_VALUE.get(),
                Config.MUD_ROOT_MULTIPLIER.get(),
                rich,
                timesRooted,
                Config.MAX_ROOTS_PER_BLOCK.get());

        // Rooted-over pasture comes up as bare dirt - a pig ploughs a field it feeds on.
        stripTurf(pos);
        server.levelEvent(2001, pos, net.minecraft.world.level.block.Block.getId(state));

        if (food > 0.0) {
            feed(food);
        }
    }
}
