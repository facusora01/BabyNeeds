package com.babyneeds;

import java.util.EnumSet;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.block.Blocks;

/**
 * An animal settling down because it feels like it. Nothing commands this and no player triggers it:
 * a fed, unbothered pig will sit on its haunches for a while, and one standing in a wallow will lie
 * over and start rocking, which is what actually works mud into its hide.
 *
 * Both are the same behaviour with a different pose, because the decision is the same one - stop
 * walking, settle, get up again later - and only the reason and the payoff differ.
 */
public class RestGoal extends Goal {

    private final Animal animal;
    private PigPose chosenPose;
    private int restingTicks;
    private int duration;

    public RestGoal(Animal animal) {
        this.animal = animal;
        setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        if (AnimalState.pose(animal).isResting()) return false;
        if (!animal.onGround() || animal.isInWater() && !inWallow()) return false;
        // Anything more pressing - hunger, a litter waiting to nurse, being chased - wins, because
        // those goals sit at a higher priority and this one simply never gets the chance to run.
        if (animal.getLastHurtByMob() != null) return false;

        if (inWallow() && AnimalState.muddiness(animal) < 1.0
                && animal.getRandom().nextInt(Config.WALLOW_CHANCE.get()) == 0) {
            chosenPose = PigPose.WALLOWING;
            duration = Config.WALLOW_TICKS.get();
            return true;
        }

        if (AnimalState.nutrition(animal) >= Config.GRAZE_THRESHOLD.get()
                && animal.getRandom().nextInt(Config.SIT_CHANCE.get()) == 0) {
            chosenPose = PigPose.SITTING;
            duration = Config.SIT_TICKS.get() / 2 + animal.getRandom().nextInt(Config.SIT_TICKS.get());
            return true;
        }
        return false;
    }

    @Override
    public boolean canContinueToUse() {
        if (restingTicks >= duration) return false;
        if (animal.getLastHurtByMob() != null) return false;
        if (chosenPose == PigPose.WALLOWING && !inWallow()) return false;
        return AnimalState.pose(animal) == chosenPose;
    }

    @Override
    public void start() {
        restingTicks = 0;
        animal.getNavigation().stop();
        AnimalEvents.setPose(animal, chosenPose);
    }

    @Override
    public void stop() {
        restingTicks = 0;
        if (AnimalState.pose(animal) == chosenPose) {
            AnimalEvents.setPose(animal, PigPose.STANDING);
        }
    }

    @Override
    public void tick() {
        restingTicks++;
        animal.getNavigation().stop();
        animal.setDeltaMovement(0.0, animal.getDeltaMovement().y, 0.0);

        if (chosenPose == PigPose.WALLOWING) {
            wallow();
        }
    }

    /** Rocking in the mud: it coats the animal, and it throws mud around while it happens. */
    private void wallow() {
        AnimalState.setMuddiness(animal,
                Math.min(1.0, AnimalState.muddiness(animal) + Config.WALLOW_MUD_PER_TICK.get()));

        if (!(animal.level() instanceof ServerLevel server)) return;
        if (restingTicks % 6 == 0) {
            server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.MUD.defaultBlockState()),
                    animal.getX(), animal.getY(0.3), animal.getZ(), 6, 0.35, 0.1, 0.35, 0.02);
        }
        if (restingTicks % 25 == 0) {
            animal.playSound(SoundEvents.MUD_STEP, 0.7f, 0.8f + animal.getRandom().nextFloat() * 0.3f);
        }
    }

    private boolean inWallow() {
        return AnimalState.isInMud(animal);
    }
}
