package com.babyneeds;

/**
 * A mother's udder. Milk is manufactured out of her own nutrition, which is what enforces the
 * chain the whole mod is built around: grass feeds the mother, the mother feeds the calf. No
 * grass means no nutrition, no nutrition means no milk, no milk means the calf never grows and
 * eventually dies.
 *
 * The udder is one shared pool, so everything here is written around the case that actually breaks
 * naive sharing: a sow with a litter of four. Her yield and her capacity scale with how many babies
 * depend on her, and no single baby may drink more than its share of what is in there, so the
 * fourth piglet to reach her never finds it empty.
 */
public final class LactationCalculator {

    /** Capacity and yield for one baby; a litter multiplies both. */
    public static final double MAX_MILK = 100.0;
    private static final double TICKS_PER_MINUTE = 1200.0;

    private LactationCalculator() {
    }

    /** A mother running on empty keeps herself alive first; lactation stops. */
    public static boolean canProduce(double nutrition, double minNutrition) {
        return nutrition >= minNutrition;
    }

    /** Udder capacity. A sow nursing four banks four times what a cow with one calf does. */
    public static double capacity(int dependents) {
        return MAX_MILK * mouths(dependents);
    }

    /**
     * Yield scales with the number of mouths, so a whole litter is fed as well as a single calf is.
     * It is not free: the nutrition cost scales with it too, which is why a sow with four piglets
     * has to graze almost constantly.
     */
    public static double productionPerTick(double milkPerMinute, double speciesRate, int dependents) {
        return Math.max(0.0, milkPerMinute) * Math.max(0.0, speciesRate) * mouths(dependents)
                / TICKS_PER_MINUTE;
    }

    /** Milk after one tick of production, capped at udder capacity. */
    public static double produce(double milk, double amount, double capacity) {
        return Math.min(capacity, milk + Math.max(0.0, amount));
    }

    /** Nutrition burnt to make that milk. Producing is expensive; a nursing mother eats more. */
    public static double nutritionCost(double milkProduced, double costRatio) {
        return Math.max(0.0, milkProduced) * Math.max(0.0, costRatio);
    }

    /**
     * How much one baby may take from the shared udder in a single bout.
     *
     * Two rules, and between them a litter feeds without anyone going hungry: a baby never drinks
     * more than its even share of what is currently there, so it cannot empty the pool ahead of its
     * siblings; and while there is any milk at all it always gets at least {@code minRation}, which
     * is sized to cover survival, so even a nearly dry sow keeps all four piglets alive rather than
     * feeding two and losing two.
     *
     * @param dependents babies currently nursing from this mother
     * @param requested  what the baby would like to drink (the configured bout size)
     * @param minRation  guaranteed floor per bout while the udder is not empty
     */
    public static double ration(double milk, int dependents, double requested, double minRation) {
        if (milk <= 0.0 || requested <= 0.0) return 0.0;
        double share = Math.max(Math.max(0.0, minRation), milk / mouths(dependents));
        return Math.min(Math.min(requested, share), milk);
    }

    /**
     * True when a player can take a bucket without eating into what the babies still need. The
     * reserve is per baby, so a sow with a litter is protected four times over.
     */
    public static boolean canBeMilkedByPlayer(double milk, double bucketCost, int dependents,
                                              double reservePerChild) {
        double reserve = dependents <= 0 ? 0.0 : Math.max(0.0, reservePerChild) * dependents;
        return milk - reserve >= bucketCost;
    }

    private static int mouths(int dependents) {
        return Math.max(1, dependents);
    }
}
