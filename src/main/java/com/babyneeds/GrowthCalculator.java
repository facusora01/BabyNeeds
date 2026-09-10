package com.babyneeds;

/**
 * Turns food into growth. An animal advances a stage when it has eaten what that stage costs, and
 * a starving one simply never grows.
 *
 * The diet rules take the species as well as the stage, because "newborns drink milk" is a mammal
 * rule and this mod has a bird in it. A chick has no milk to drink and does not need any: it pecks
 * from the day it hatches.
 */
public final class GrowthCalculator {

    private GrowthCalculator() {
    }

    /** Milk is only digestible while the animal still nurses, and only if its kind makes any. */
    public static boolean acceptsMilk(GrowthStage stage, Species species) {
        if (species == null || !species.lactates()) return false;
        return stage == GrowthStage.BABY || stage == GrowthStage.WEANLING;
    }

    /**
     * A newborn mammal cannot handle solid food - a calf has no rumen yet and grass would kill it,
     * which is precisely why losing its mother is fatal. A chick is born able to feed itself.
     */
    public static boolean acceptsSolids(GrowthStage stage, Species species) {
        if (species != null && species.babyEatsSolids()) return true;
        return stage != GrowthStage.BABY;
    }

    /**
     * Whether a bucket of milk from a player still does anything. Narrower than {@link #acceptsMilk}
     * on purpose: nursing is what a weanling does at its mother's side while it learns to eat, and
     * hand-feeding it milk out of a bucket would let a player skip that stage entirely. Only a
     * newborn, which has nothing else it can swallow, can be rescued this way.
     */
    public static boolean acceptsBottle(GrowthStage stage, Species species) {
        return stage == GrowthStage.BABY && species != null && species.lactates();
    }

    /**
     * Intake units needed to leave this stage. Later stages need more because the animal is
     * bigger; the config multiplier scales the whole ramp for players who want it faster.
     */
    public static double requiredIntake(GrowthStage stage, double baseIntake, double speedMultiplier) {
        double safeSpeed = speedMultiplier <= 0 ? 1.0 : speedMultiplier;
        double factor = switch (stage) {
            case BABY -> 1.0;
            case WEANLING -> 1.6;
            case JUVENILE -> 2.2;
            case ADULT -> 0.0;
        };
        return Math.max(0.0, baseIntake * factor / safeSpeed);
    }

    public static double addIntake(double progress, double amount, double required) {
        double result = progress + Math.max(0.0, amount);
        return Math.min(result, required);
    }

    /**
     * Whether the animal has earned the next stage. Food is the whole of it.
     *
     * There used to be a minimum time in the stage as well, meant to stop a player bucket-feeding a
     * newborn straight to adult. It did stop that, and it also broke the promise the mod is built
     * on: a calf that had drunk everything its stage asked for sat there fully fed and did not
     * grow, waiting on a clock - which is the vanilla behaviour this replaces. What limits growth
     * is how fast an animal can actually be fed, and that is already limited by the udder, by the
     * nursing cooldown and by how fast grass grows back.
     */
    public static boolean canAdvance(GrowthStage stage, double progress, double required) {
        if (stage == GrowthStage.ADULT) return false;
        return progress >= required;
    }
}
