package com.babyneeds;

/**
 * Nutrition bookkeeping for an animal that eats solids. Nutrition drains constantly, refills by
 * grazing or being hand-fed, and gates everything downstream: a mother below the lactation
 * threshold makes no milk, and an animal at zero eventually starves. This is what makes a packed
 * pen lethal - the pen runs out of grass long before the animals run out of appetite.
 */
public final class HungerCalculator {

    public static final double MAX_NUTRITION = 100.0;
    private static final double TICKS_PER_MINUTE = 1200.0;

    private HungerCalculator() {
    }

    /**
     * @param inMud pigs wallowing in mud thermoregulate instead of burning fat, so they get a
     *              discount on hunger decay - the mud puddle is a real husbandry bonus.
     */
    public static double decayPerTick(double decayPerMinute, double speciesRate,
                                      boolean inMud, double mudReduction) {
        double perTick = Math.max(0.0, decayPerMinute) * Math.max(0.0, speciesRate) / TICKS_PER_MINUTE;
        if (inMud) {
            perTick *= Math.max(0.0, 1.0 - clamp01(mudReduction));
        }
        return perTick;
    }

    public static double drain(double nutrition, double amount) {
        return Math.max(0.0, nutrition - Math.max(0.0, amount));
    }

    public static double feed(double nutrition, double food) {
        return Math.min(MAX_NUTRITION, nutrition + Math.max(0.0, food));
    }

    /** Below this the animal actively looks for grass instead of wandering. */
    public static boolean wantsToGraze(double nutrition, double grazeThreshold) {
        return nutrition < grazeThreshold;
    }

    /**
     * Starvation damage only starts after a grace period at zero, and then ticks on an interval,
     * so a bad afternoon is survivable but a permanently overstocked pen is not.
     */
    public static boolean shouldTakeStarvationDamage(double nutrition, int starvingTicks,
                                                     int graceTicks, int intervalTicks) {
        if (nutrition > 0.0) return false;
        if (starvingTicks < graceTicks) return false;
        int safeInterval = Math.max(1, intervalTicks);
        return (starvingTicks - graceTicks) % safeInterval == 0;
    }

    private static double clamp01(double value) {
        if (value < 0.0) return 0.0;
        return Math.min(value, 1.0);
    }
}
