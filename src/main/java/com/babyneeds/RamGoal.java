package com.babyneeds;

import java.util.EnumSet;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.phys.Vec3;

/**
 * The charge a goat does, given to a cow defending her calf. It is a move of its own rather than a
 * harder swing: she backs off to make room, plants herself, drops her head, and then runs the whole
 * distance in a straight line and hits whatever is at the end of it.
 *
 * The backing off is not decoration. Chasing brings her nose to nose with whoever she is angry at,
 * and a charge needs a run-up, so without it she simply never has the room and the move never
 * happens - which is exactly how it behaved before: a cow that trots up to you and then stands
 * there. Now being too close is a reason to make space rather than a reason not to charge.
 *
 * Vanilla builds the goat's version out of brain behaviours, which cows do not have, so this is the
 * same shape rebuilt as a goal.
 */
public class RamGoal extends Goal {

    private static final int WIND_UP_TICKS = 14;
    private static final int MAX_CHARGE_TICKS = 30;
    private static final int MAX_BACKING_TICKS = 50;
    /** Below this there is no room to build up speed, so she makes some. */
    private static final double MIN_RANGE = 3.0;
    private static final double MAX_RANGE = 12.0;
    /** How far past the minimum she backs off, so a step forward does not cancel the charge. */
    private static final double BACKING_MARGIN = 2.0;
    private static final double IMPACT_RANGE_SQ = 4.0;

    private enum Phase { BACKING, WINDING, CHARGING }

    private final Animal animal;
    private LivingEntity target;
    private Phase phase;
    private Vec3 chargeDirection;
    private int windUpTicks;
    private int chargeTicks;
    private int backingTicks;
    private int cooldown;

    public RamGoal(Animal animal) {
        this.animal = animal;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        if (cooldown-- > 0) return false;
        if (AnimalState.pose(animal).isResting()) return false;
        if (!animal.onGround() || animal.isInWater()) return false;

        LivingEntity current = animal.getTarget();
        if (current == null || !current.isAlive()) return false;

        double distanceSq = animal.distanceToSqr(current);
        if (distanceSq > MAX_RANGE * MAX_RANGE) return false;
        if (!animal.getSensing().hasLineOfSight(current)) return false;

        // Rolled once per opportunity rather than once per tick, so the odds mean what they say
        // instead of becoming a certainty within half a second. The further off the target is, the
        // likelier she is to take the run at them - and from close up she has to make the room
        // first, which is a decision worth making less often.
        cooldown = Config.RAM_COOLDOWN_TICKS.get();
        double chance = RamCalculator.chance(Math.sqrt(distanceSq), MIN_RANGE, MAX_RANGE,
                Config.RAM_CHANCE_NEAR.get(), Config.RAM_CHANCE_FAR.get());
        if (animal.getRandom().nextDouble() >= chance) return false;

        this.target = current;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (target == null || !target.isAlive()) return false;
        if (phase == Phase.BACKING && backingTicks > MAX_BACKING_TICKS) return false;
        if (chargeTicks > MAX_CHARGE_TICKS) return false;
        // Hit a fence or a wall mid-charge: the charge is spent, exactly as a goat's is.
        return !(phase == Phase.CHARGING && chargeTicks > 0 && animal.horizontalCollision);
    }

    @Override
    public void start() {
        windUpTicks = WIND_UP_TICKS;
        chargeTicks = 0;
        backingTicks = 0;
        chargeDirection = null;

        boolean roomToRun = animal.distanceToSqr(target) >= MIN_RANGE * MIN_RANGE;
        phase = roomToRun ? Phase.WINDING : Phase.BACKING;
        animal.getNavigation().stop();

        if (phase == Phase.WINDING) {
            brace();
        }
    }

    @Override
    public void stop() {
        target = null;
        phase = null;
        chargeDirection = null;
        windUpTicks = 0;
        chargeTicks = 0;
        backingTicks = 0;
        cooldown = Config.RAM_COOLDOWN_TICKS.get();
        AnimalState.setHeadDownTicks(animal, 0);
        AnimalEvents.setPose(animal, PigPose.STANDING);
        animal.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        // Eyes on the target throughout, including while retreating - backing away while staring at
        // someone is the part that reads as a threat rather than as running off.
        animal.getLookControl().setLookAt(target, 30.0f, 30.0f);

        switch (phase) {
            case BACKING -> backOff();
            case WINDING -> windUp();
            case CHARGING -> charge();
        }
    }

    /** Making room for the run-up, walking backwards away from whoever she is squaring up to. */
    private void backOff() {
        backingTicks++;

        if (animal.distanceToSqr(target) >= (MIN_RANGE + BACKING_MARGIN) * (MIN_RANGE + BACKING_MARGIN)) {
            phase = Phase.WINDING;
            windUpTicks = WIND_UP_TICKS;
            animal.getNavigation().stop();
            brace();
            return;
        }

        if (backingTicks % 10 == 1) {
            Vec3 away = animal.position().subtract(target.position());
            if (away.horizontalDistanceSqr() < 1.0E-4) {
                away = animal.getLookAngle().reverse();
            }
            Vec3 spot = animal.position().add(away.normalize().scale(MIN_RANGE + BACKING_MARGIN));
            animal.getNavigation().moveTo(spot.x, spot.y, spot.z, Config.DEFEND_SPEED.get());
        }
    }

    /** Head down, feet planted: the tell that gives whoever it is a moment to move. */
    private void brace() {
        AnimalEvents.setPose(animal, PigPose.HEAD_DOWN);
        AnimalState.setHeadDownTicks(animal, WIND_UP_TICKS + MAX_CHARGE_TICKS);
        animal.playSound(SoundEvents.GOAT_PREPARE_RAM, 1.0f, 0.8f);
    }

    private void windUp() {
        windUpTicks--;
        animal.setDeltaMovement(0.0, animal.getDeltaMovement().y, 0.0);
        if (windUpTicks > 0) return;

        chargeDirection = target.position().subtract(animal.position());
        chargeDirection = new Vec3(chargeDirection.x, 0.0, chargeDirection.z).normalize();
        phase = Phase.CHARGING;
    }

    private void charge() {
        chargeTicks++;
        if (chargeDirection == null) return;

        double speed = Config.RAM_SPEED.get();
        animal.setDeltaMovement(chargeDirection.x * speed,
                animal.getDeltaMovement().y,
                chargeDirection.z * speed);

        if (animal.distanceToSqr(target) <= IMPACT_RANGE_SQ) {
            impact();
        }
    }

    private void impact() {
        target.hurt(animal.damageSources().mobAttack(animal), Config.RAM_DAMAGE.get().floatValue());

        // Knockback is measured from her to the target, so it throws them along the line she ran.
        target.knockback(Config.RAM_KNOCKBACK.get(), -chargeDirection.x, -chargeDirection.z);
        target.push(0.0, 0.42, 0.0);
        target.hurtMarked = true;

        animal.playSound(SoundEvents.GOAT_RAM_IMPACT, 1.0f, 0.9f);
        if (animal.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.SWEEP_ATTACK,
                    target.getX(), target.getY(0.6), target.getZ(), 3, 0.2, 0.2, 0.2, 0.0);
        }
        chargeTicks = MAX_CHARGE_TICKS + 1;
    }
}
