package com.babyneeds;

import java.util.EnumSet;
import java.util.List;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.phys.Vec3;

/**
 * A sow settling down for a nursing bout. Pigs cannot nurse standing up, so unlike a cow - whose
 * calf can drink whenever it feels like it - a litter only feeds when the mother goes down for them.
 *
 * She does not decide that on her own. A hungry piglet asks (see {@link NursingRequest}), she says
 * yes or no, and this goal is only what carries out a yes. All it looks for is a granted request.
 *
 * It deliberately holds no MOVE or JUMP flag. {@link HoldPoseGoal} sits at priority 0 and claims
 * both the moment anything takes a resting pose, so a goal down here that held them too would be
 * evicted by it on the very next tick - and being evicted runs {@link #stop()}, which stands her
 * back up and starts the cooldown. That is not a hypothetical: it is why she used to lie down for a
 * fraction of a tick, never visibly, and then sit out thirty seconds of cooldown having fed nobody.
 * Keeping still is HoldPoseGoal's job; this one only decides how long the bout lasts.
 */
public class SowNursingGoal extends Goal {

    private static final double LITTER_RADIUS = 8.0;
    /** Comfortably longer than the gap between two ticks of this goal, and no longer. */
    private static final int POSE_REFRESH_TICKS = 40;

    private final Animal sow;
    private int lyingTicks;
    private Vec3 restingSpot;

    public SowNursingGoal(Animal sow) {
        this.sow = sow;
        setFlags(EnumSet.of(Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        // The one thing that starts a bout: a piglet asked and she agreed.
        if (AnimalState.lieRequest(sow) <= 0) return false;
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
        restingSpot = sow.position();
        // The request is spent the moment she acts on it; the next bout needs a fresh one.
        AnimalState.setLieRequest(sow, 0);
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
        AnimalState.setPoseTicks(sow, POSE_REFRESH_TICKS);
        AnimalEvents.holdStill(sow, restingSpot);
    }

    /** Only her own babies count; a neighbour's piglets will not make her lie down. */
    private List<Animal> hungryLitter() {
        return sow.level().getEntitiesOfClass(Animal.class, sow.getBoundingBox().inflate(LITTER_RADIUS),
                other -> other != sow
                        && sow.getUUID().equals(AnimalState.mother(other))
                        && GrowthCalculator.acceptsMilk(AnimalState.stage(other),
                                AnimalState.speciesOf(other)));
    }
}
