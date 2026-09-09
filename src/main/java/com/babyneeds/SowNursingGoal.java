package com.babyneeds;

import java.util.EnumSet;
import java.util.List;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;

/**
 * A sow settling down for a nursing bout. Pigs cannot nurse standing up, so unlike a cow - whose
 * calf can drink whenever it feels like it - a litter only feeds when the mother decides to lie
 * down, and she will not do it if she has no milk to give.
 */
public class SowNursingGoal extends Goal {

    private static final double LITTER_RADIUS = 8.0;

    private final Animal sow;
    private int lyingTicks;

    public SowNursingGoal(Animal sow) {
        this.sow = sow;
        setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (AnimalState.lieCooldown(sow) > 0) return false;
        if (AnimalState.pose(sow).isResting()) return false;
        if (!AnimalState.isLactating(sow) || AnimalState.milk(sow) <= 0.0) return false;
        if (sow.isInWater() || !sow.onGround()) return false;
        return !hungryLitter().isEmpty();
    }

    @Override
    public boolean canContinueToUse() {
        if (lyingTicks >= Config.SOW_LIE_DOWN_TICKS.get()) return false;
        if (AnimalState.pose(sow) != PigPose.LYING) return false;
        if (AnimalState.milk(sow) <= 0.0) return false;
        return !hungryLitter().isEmpty();
    }

    @Override
    public void start() {
        lyingTicks = 0;
        sow.getNavigation().stop();
        AnimalEvents.setPose(sow, PigPose.LYING);
    }

    @Override
    public void stop() {
        if (AnimalState.pose(sow) == PigPose.LYING) {
            AnimalEvents.setPose(sow, PigPose.STANDING);
        }
        AnimalState.setLieCooldown(sow, Config.SOW_LIE_COOLDOWN_TICKS.get());
    }

    @Override
    public void tick() {
        lyingTicks++;
        sow.getNavigation().stop();
        sow.setDeltaMovement(0.0, sow.getDeltaMovement().y, 0.0);
    }

    /** Only her own babies count; a neighbour's piglets will not make her lie down. */
    private List<Animal> hungryLitter() {
        return sow.level().getEntitiesOfClass(Animal.class, sow.getBoundingBox().inflate(LITTER_RADIUS),
                other -> other != sow
                        && sow.getUUID().equals(AnimalState.mother(other))
                        && GrowthCalculator.acceptsMilk(AnimalState.stage(other)));
    }
}
