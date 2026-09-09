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

    /** Minimum ticks in a stage, so growth stays a process even with unlimited food. */
    public static int requiredTicks(GrowthStage stage, int baseTicks, double speedMultiplier) {
        double safeSpeed = speedMultiplier <= 0 ? 1.0 : speedMultiplier;
        double factor = switch (stage) {
            case BABY -> 1.0;
            case WEANLING -> 1.5;
            case JUVENILE -> 2.0;
            case ADULT -> 0.0;
        };
        return (int) Math.max(0, Math.round(baseTicks * factor / safeSpeed));
    }

    public static double addIntake(double progress, double amount, double required) {
        double result = progress + Math.max(0.0, amount);
        return Math.min(result, required);
    }

    public static boolean canAdvance(GrowthStage stage, double progress, double required,
                                     int ticksInStage, int requiredTicks) {
        if (stage == GrowthStage.ADULT) return false;
        return progress >= required && ticksInStage >= requiredTicks;
    }
}
