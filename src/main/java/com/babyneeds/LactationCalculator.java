package com.babyneeds;

/**
 * A mother's udder. Milk is manufactured out of her own nutrition, which is what enforces the
 * chain the whole mod is built around: grass feeds the mother, the mother feeds the calf. No
 * grass means no nutrition, no nutrition means no milk, no milk means the calf never grows and
 * eventually dies.
 */
public final class LactationCalculator {

    public static final double MAX_MILK = 100.0;
    private static final double TICKS_PER_MINUTE = 1200.0;

    private LactationCalculator() {
    }

    /** A mother running on empty keeps herself alive first; lactation stops. */
    public static boolean canProduce(double nutrition, double minNutrition) {
        return nutrition >= minNutrition;
    }

    public static double productionPerTick(double milkPerMinute, double speciesRate) {
        return Math.max(0.0, milkPerMinute) * Math.max(0.0, speciesRate) / TICKS_PER_MINUTE;
    }

    /** Milk after one tick of production, capped at udder capacity. */
    public static double produce(double milk, double amount) {
        return Math.min(MAX_MILK, milk + Math.max(0.0, amount));
    }

    /** Nutrition burnt to make that milk. Producing is expensive; a nursing mother eats more. */
    public static double nutritionCost(double milkProduced, double costRatio) {
        return Math.max(0.0, milkProduced) * Math.max(0.0, costRatio);
    }

    /**
     * How much of a requested feed the udder can actually deliver. Several babies on one mother
     * split what is there, which is exactly the failure mode of a big litter on a thin sow.
     */
    public static double drawMilk(double milk, double requested) {
        return Math.max(0.0, Math.min(milk, requested));
    }

    /** True when a player can take a bucket without emptying what a nursing baby still needs. */
    public static boolean canBeMilkedByPlayer(double milk, double bucketCost, boolean hasNursingChild,
                                              double reserveForChild) {
        double reserve = hasNursingChild ? Math.max(0.0, reserveForChild) : 0.0;
        return milk - reserve >= bucketCost;
    }
}
