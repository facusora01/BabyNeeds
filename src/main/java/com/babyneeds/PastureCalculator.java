package com.babyneeds;

/**
 * Pasture wear. Every grazed block turns to bare dirt; when the surrounding pasture is already
 * chewed down, it turns to coarse dirt instead, which vanilla grass never spreads back onto until
 * a player hoes it. That is the rotation pressure: a small pen with many animals degrades to
 * coarse dirt and stops feeding them, while a big or rotated pasture recovers on its own.
 */
public final class PastureCalculator {

    public enum Outcome {
        /** Grass eaten, soil still healthy - vanilla grass will creep back. */
        DIRT,
        /** Overgrazed: soil compacted to coarse dirt, needs a hoe to come back. */
        COARSE_DIRT
    }

    private PastureCalculator() {
    }

    /**
     * @param grassRatio share of the blocks around the grazed spot that are still grass (0..1)
     * @return chance the spot degrades to coarse dirt; rises as the pasture is stripped
     */
    public static double coarseChance(double grassRatio, double baseChance, double maxChance) {
        double ratio = clamp01(grassRatio);
        double base = Math.max(0.0, baseChance);
        double max = Math.max(base, maxChance);
        // Linear from max (bare surroundings) down to base (lush surroundings).
        return base + (max - base) * (1.0 - ratio);
    }

    public static Outcome graze(double coarseChance, double roll) {
        return roll < coarseChance ? Outcome.COARSE_DIRT : Outcome.DIRT;
    }

    /** Food gained from one grazed block; a stripped pasture yields thinner mouthfuls. */
    public static double foodValue(double grassRatio, double fullValue) {
        double ratio = clamp01(grassRatio);
        return fullValue * (0.5 + 0.5 * ratio);
    }

    private static double clamp01(double value) {
        if (value < 0.0) return 0.0;
        return Math.min(value, 1.0);
    }
}
