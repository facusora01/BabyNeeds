package com.babyneeds;

/**
 * Turns food into growth. An animal advances a stage only after it has eaten the intake that
 * stage requires AND spent a minimum time in it, so a player cannot bottle-feed a calf to
 * adulthood in ten seconds, and a starving one simply never grows.
 */
public final class GrowthCalculator {

    private GrowthCalculator() {
    }

    /** Milk is only digestible while the animal still nurses. */
    public static boolean acceptsMilk(GrowthStage stage) {
        return stage == GrowthStage.BABY || stage == GrowthStage.WEANLING;
    }

    /** A newborn cannot handle grass; everything past BABY can graze. */
    public static boolean acceptsSolids(GrowthStage stage) {
        return stage != GrowthStage.BABY;
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
