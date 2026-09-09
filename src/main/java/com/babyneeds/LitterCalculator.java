package com.babyneeds;

/**
 * Litter size for pigs. A sow with access to a mud wallow occasionally throws more than one
 * piglet; the bigger the litter, the rarer it is, so four at once is a genuinely lucky farrowing
 * and not something a player can count on.
 */
public final class LitterCalculator {

    private LitterCalculator() {
    }

    /**
     * @param litterRoll     roll deciding whether a litter happens at all (0..1)
     * @param litterChance   chance of a litter, e.g. 0.08 for 8%
     * @param sizeRoll       roll deciding how big it is (0..1)
     * @param maxLitterSize  total babies at the very best roll, the vanilla one included
     * @param rarityExponent higher = big litters get rarer. At 3.0 and a max of 4, a litter that
     *                       happens is about 69% twins, 18% triplets and 13% quadruplets.
     * @return extra babies to spawn on top of the one vanilla already made (0 when no litter)
     */
    public static int extraBabies(double litterRoll, double litterChance,
                                  double sizeRoll, int maxLitterSize, double rarityExponent) {
        if (maxLitterSize <= 1) return 0;
        if (litterRoll >= litterChance) return 0;

        double exponent = rarityExponent <= 0 ? 1.0 : rarityExponent;
        double curved = Math.pow(clamp01(sizeRoll), exponent);
        int extra = 1 + (int) Math.floor(curved * (maxLitterSize - 1));
        return Math.min(extra, maxLitterSize - 1);
    }

    private static double clamp01(double value) {
        if (value < 0.0) return 0.0;
        return Math.min(value, 1.0);
    }
}
