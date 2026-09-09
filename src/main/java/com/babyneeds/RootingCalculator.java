package com.babyneeds;

/**
 * What a pig gets out of turning over a patch of ground. A fresh patch pays well, a patch already
 * worked over pays less, and an exhausted one pays nothing until it has had time to recover - which
 * is what stops a single square of dirt from feeding a herd of pigs forever.
 */
public final class RootingCalculator {

    private RootingCalculator() {
    }

    /**
     * @param baseValue      nutrition from a fresh patch of dry ground
     * @param mudMultiplier  how much richer mud is than dry ground
     * @param inMud          whether this patch is mud - the reason a wallow is worth building
     * @param timesRooted    how often this patch has already been worked since it last recovered
     * @param maxRoots       roots a patch yields before it is exhausted
     */
    public static double yield(double baseValue, double mudMultiplier, boolean inMud,
                               int timesRooted, int maxRoots) {
        if (maxRoots <= 0) return 0.0;
        if (timesRooted >= maxRoots) return 0.0;

        double richness = inMud ? Math.max(0.0, mudMultiplier) : 1.0;
        // Straight-line depletion: the last root before exhaustion still pays something.
        double remaining = 1.0 - (double) Math.max(0, timesRooted) / maxRoots;
        return Math.max(0.0, baseValue) * richness * remaining;
    }
}
