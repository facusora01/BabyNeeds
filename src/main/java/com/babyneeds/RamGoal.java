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
 * harder melee swing: she plants herself, drops her head, and then runs the whole distance in a
 * straight line and hits whatever is at the end of it.
 *
 * Vanilla builds the goat's version out of brain behaviours, which cows do not have, so this is the
 * same shape rebuilt as a goal: a wind-up you can see coming, a committed run that does not steer,
 * and an impact that throws the target clear. Committing to the line matters - a charge that homes
 * in on a dodging player is not a charge, it is a magnet.
 */
public class RamGoal extends Goal {

    private static final int WIND_UP_TICKS = 14;
    private static final int MAX_CHARGE_TICKS = 30;
    private static final double MIN_RANGE = 3.0;
    private static final double MAX_RANGE = 12.0;
    private static final double IMPACT_RANGE_SQ = 4.0;

    private final Animal animal;
    private LivingEntity target;
    private Vec3 chargeDirection;
    private int windUpTicks;
    private int chargeTicks;
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
        if (distanceSq < MIN_RANGE * MIN_RANGE || distanceSq > MAX_RANGE * MAX_RANGE) return false;
        if (!animal.getSensing().hasLineOfSight(current)) return false;

        // Rolled once per opportunity rather than once per tick, so the odds mean what they say
        // instead of becoming a certainty within half a second. The further off the target is, the
        // likelier she is to take the run at them.
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
        if (chargeTicks > MAX_CHARGE_TICKS) return false;
        // Hit a fence or a wall: the charge is spent, exactly as a goat's is.
        return !(chargeTicks > 0 && animal.horizontalCollision);
    }

    @Override
    public void start() {
        windUpTicks = WIND_UP_TICKS;
        chargeTicks = 0;
        chargeDirection = null;
        animal.getNavigation().stop();

        // Head down: the tell that gives the player a moment to move.
        AnimalEvents.setPose(animal, PigPose.HEAD_DOWN);
        AnimalState.setHeadDownTicks(animal, WIND_UP_TICKS + MAX_CHARGE_TICKS);
        animal.playSound(SoundEvents.GOAT_PREPARE_RAM, 1.0f, 0.8f);
    }

    @Override
    public void stop() {
        target = null;
        chargeDirection = null;
        windUpTicks = 0;
        chargeTicks = 0;
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
        animal.getLookControl().setLookAt(target, 30.0f, 30.0f);

        if (windUpTicks > 0) {
            windUpTicks--;
            // Braced, not walking: the pause is what makes the run afterwards read as a charge.
            animal.setDeltaMovement(0.0, animal.getDeltaMovement().y, 0.0);
            if (windUpTicks == 0) {
                chargeDirection = target.position().subtract(animal.position());
                chargeDirection = new Vec3(chargeDirection.x, 0.0, chargeDirection.z).normalize();
            }
            return;
        }

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
