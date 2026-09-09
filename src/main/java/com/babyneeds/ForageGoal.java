package com.babyneeds;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A pig in the vegetable patch. Ripe crops it can reach are fair game, which is both the best food
 * available to it and a standing warning about pen doors: the farmer who feeds pigs on potatoes and
 * the farmer whose potatoes get eaten are the same person.
 *
 * Only mature crops count, so a pig will not trample a field back to seedlings, and it still has to
 * path to them - a fenced garden is safe.
 */
public class ForageGoal extends BlockConsumingGoal {

    public ForageGoal(Animal animal, Species species) {
        super(animal, species);
    }

    @Override
    protected boolean isTarget(BlockState state) {
        if (!Config.PIGS_EAT_CROPS.get()) return false;
        return state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state);
    }

    @Override
    protected int chewDuration() {
        return Config.ROOT_TICKS.get();
    }

    /** Worth crossing a pen for, so hunger is judged more loosely than for a mouthful of dirt. */
    @Override
    protected boolean wantsToEat() {
        return AnimalState.nutrition(animal) < HungerCalculator.MAX_NUTRITION;
    }

    @Override
    protected void consume(BlockPos pos, BlockState state) {
        // Eaten, not harvested: nothing drops for the player to pick up afterwards.
        animal.level().destroyBlock(pos, false);
        feed(Config.CROP_FOOD_VALUE.get());
    }
}
