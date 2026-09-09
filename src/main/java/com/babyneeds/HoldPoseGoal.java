package com.babyneeds;

import java.util.EnumSet;

import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.phys.Vec3;

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
    /** The line it rolls along, fixed when it settles so the roll does not wander off course. */
    private float facing;

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
        facing = animal.yBodyRot;
    }

    @Override
    public void tick() {
        animal.getNavigation().stop();
        // Downward motion is left alone so a settled animal still falls if the ground goes away.
        animal.setDeltaMovement(0.0, Math.min(0.0, animal.getDeltaMovement().y), 0.0);

        if (AnimalState.pose(animal) == PigPose.WALLOWING) {
            holdFacing();
            rollSideways();
        }
    }

    /**
     * A rolling animal is not steering, and the direction it faces is the line it rolls along, so
     * letting the usual drift turn it would send it wandering off across the field one nudge at a
     * time.
     */
    private void holdFacing() {
        animal.setYRot(facing);
        animal.yBodyRot = facing;
        animal.yBodyRotO = facing;
    }

    /**
     * Carries the animal sideways as it turns. This lives with the pose rather than with the goal
     * that chose the pose, for the same reason the picture does: whatever put the animal into a
     * roll - its own idea, an operator command, anything added later - it should roll the same, and
     * the travel has to come from the same function the renderer is drawing or the pig slides.
     *
     * The distance is not chosen: a body rolling on the ground covers its own radius for every
     * radian it turns. And it moves through {@code move}, so a fence stops the roll where a real
     * one would.
     */
    private void rollSideways() {
        long gameTime = animal.level().getGameTime();
        long start = AnimalState.poseStart(animal);
        double speed = Config.WALLOW_ROLL_SPEED.get();
        double amplitude = Config.WALLOW_ROLL_ANGLE.get();

        double now = WallowRoll.angle(WallowRoll.phase(gameTime, 0.0, start, speed), amplitude);
        double before = WallowRoll.angle(WallowRoll.phase(gameTime - 1, 0.0, start, speed), amplitude);
        double step = WallowRoll.travel(now - before, Config.WALLOW_ROLL_RADIUS.get());
        if (Math.abs(step) < 1.0E-5) return;

        // Sideways to the animal, not to the world: it rolls across its own flank.
        double yaw = Math.toRadians(facing);
        animal.move(MoverType.SELF, new Vec3(Math.cos(yaw) * step, 0.0, Math.sin(yaw) * step));
    }
}
