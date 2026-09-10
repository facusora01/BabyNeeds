package com.babyneeds;

import java.util.EnumSet;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;

/**
 * The baby does the work, not the mother: it walks to her, latches on, and only then gets fed.
 * It nurses from its own mother and no one else, so separating a calf from its mother is a
 * decision with consequences, and a sow's litter has to wait until she lies down.
 */
public class NurseGoal extends Goal {

    private static final double LATCH_DISTANCE_SQ = 4.0;

    private final Animal baby;
    private Animal mother;
    private int latchTicks;
    private int repathTicks;

    public NurseGoal(Animal baby) {
        this.baby = baby;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        GrowthStage stage = AnimalState.stage(baby);
        if (!GrowthCalculator.acceptsMilk(stage)) return false;
        if (AnimalState.nurseCooldown(baby) > 0) return false;
        if (!isHungryForMilk(stage)) return false;

        Animal found = AnimalState.findMother(baby);
        if (found == null) return false;
        if (!AnimalState.isLactating(found) || AnimalState.milk(found) <= 0.0) return false;

        this.mother = found;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (mother == null || !mother.isAlive()) return false;
        if (AnimalState.milk(mother) <= 0.0) return false;
        if (baby.distanceToSqr(mother) > 32.0 * 32.0) return false;
        return AnimalState.nurseCooldown(baby) <= 0;
    }

    @Override
    public void start() {
        latchTicks = 0;
        repathTicks = 0;
        baby.getNavigation().moveTo(mother, 1.15);
    }

    @Override
    public void stop() {
        mother = null;
        latchTicks = 0;
        baby.getNavigation().stop();
    }

    @Override
    public void tick() {
        baby.getLookControl().setLookAt(mother, 30.0f, 30.0f);

        if (baby.distanceToSqr(mother) > LATCH_DISTANCE_SQ) {
            latchTicks = 0;
            if (--repathTicks <= 0) {
                repathTicks = 10;
                baby.getNavigation().moveTo(mother, 1.15);
            }
            return;
        }

        baby.getNavigation().stop();

        // A piglet cannot reach the teats of a standing sow, so it asks her to lie down and waits
        // by her side for an answer. She is allowed to refuse, and a refusal is not the end of it -
        // the ask cooldown runs down and it tries again.
        Species motherSpecies = AnimalState.speciesOf(mother);
        if (motherSpecies != null && motherSpecies.nursesLyingDown()
                && !AnimalState.pose(mother).isOnSide()) {
            latchTicks = 0;
            NursingRequest.ask(baby, mother);
            return;
        }

        if (++latchTicks < Config.SUCKLE_DURATION_TICKS.get()) return;
        suckle();
    }

    private void suckle() {
        // Its fair share of what is in the udder, never the whole pool: with a litter of four, the
        // first piglet to latch cannot drink the other three dry. The floor in ration() means a
        // late sibling still gets enough to live on even when the sow is nearly empty.
        double drawn = LactationCalculator.ration(
                AnimalState.milk(mother),
                AnimalState.dependents(mother),
                Config.SUCKLE_AMOUNT.get(),
                Config.MIN_RATION.get());
        latchTicks = 0;
        if (drawn <= 0.0) return;

        AnimalState.setMilk(mother, AnimalState.milk(mother) - drawn);
        // Suckling is the demand signal that keeps her producing.
        AnimalState.refreshLactation(mother);
        AnimalEvents.feedMilk(baby, drawn, AnimalState.stage(baby));

        baby.playSound(SoundEvents.GENERIC_DRINK, 0.6f, 1.5f);
        if (baby.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    baby.getX(), baby.getY(0.6), baby.getZ(), 4, 0.2, 0.2, 0.2, 0.0);
        }
    }

    /** Nurses to grow, and also whenever it is simply hungry. */
    private boolean isHungryForMilk(GrowthStage stage) {
        double required = GrowthCalculator.requiredIntake(stage, Config.BASE_STAGE_INTAKE.get(),
                Config.GROWTH_SPEED.get());
        if (AnimalState.progress(baby) < required) return true;
        return AnimalState.nutrition(baby) < Config.GRAZE_THRESHOLD.get();
    }
}
