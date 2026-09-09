package com.babyneeds;

import java.util.EnumSet;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;

/**
 * Keeps a settled animal settled. An animal that is sitting, lying or wallowing does not walk, and
 * this is the thing that makes that true no matter what put it in that pose - a goal of its own, an
 * operator command, or anything added later.
 *
 * It has to be a goal rather than a tick handler. Movement is decided inside the AI step, so
 * stopping navigation from an entity tick event happens before the AI runs and gets undone
 * immediately afterwards - which is exactly how a pig ended up strolling around in a sitting pose.
 * Holding the MOVE and JUMP flags at the top priority takes the decision away from every goal below
 * it instead of arguing with them after the fact.
 */
public class HoldPoseGoal extends Goal {

    private final Animal animal;

    public HoldPoseGoal(Animal animal) {
        this.animal = animal;
        setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        return AnimalState.pose(animal).isResting();
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        animal.getNavigation().stop();
    }

    @Override
    public void tick() {
        animal.getNavigation().stop();
        // Downward motion is left alone so a settled animal still falls if the ground goes away.
        animal.setDeltaMovement(0.0, Math.min(0.0, animal.getDeltaMovement().y), 0.0);
    }
}
