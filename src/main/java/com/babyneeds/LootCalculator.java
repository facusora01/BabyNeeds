package com.babyneeds;

/**
 * Body condition at death, applied to what the carcass yields. An animal that starved to death is
 * skin and bone: it cannot give the same meat and hide as one that died well fed. The same scale
 * runs on animals a player butchers while they are underfed, so letting a herd go hungry costs the
 * player something real even when they slaughter before anything actually dies.
 */
public final class LootCalculator {

    private LootCalculator() {
    }

    /**
     * @param nutrition     the animal's nutrition at the moment of death
     * @param starved       true when starvation is what killed it - always the worst carcass
     * @param threshold     nutrition at or above which the carcass is in full condition
     * @param minMultiplier yield of the worst possible carcass
     * @return factor to apply to every drop count, 0..1
     */
    public static double dropMultiplier(double nutrition, boolean starved,
                                        double threshold, double minMultiplier) {
        double min = clamp01(minMultiplier);
        if (starved) return min;
        if (threshold <= 0.0) return 1.0;
        if (nutrition >= threshold) return 1.0;

        double condition = clamp01(nutrition / threshold);
        return min + (1.0 - min) * condition;
    }

    /**
     * Yield for the body the animal is actually carrying. An adolescent is a smaller animal and
     * gives a smaller carcass, which is what stops the obvious exploit: without it a juvenile
     * dropped exactly as much meat and hide as a full adult, so the optimal play was to slaughter
     * everything one stage early and never raise anything to adult except a pair of breeders - and
     * the fourth stage became decoration.
     *
     * Babies and weanlings are zero because vanilla suppresses loot for anything it considers a
     * baby; killing calves is deliberately not a business.
     */
    public static double stageYield(GrowthStage stage, double juvenileYield) {
        return switch (stage) {
            case BABY, WEANLING -> 0.0;
            case JUVENILE -> clamp01(juvenileYield);
            case ADULT -> 1.0;
        };
    }

    /**
     * The whole carcass in one number: how big the animal was, times what condition it died in. A
     * half-grown animal that starved to death is the worst of both.
     */
    public static double carcassYield(GrowthStage stage, double nutrition, boolean starved,
                                      double threshold, double minMultiplier, double juvenileYield) {
        return stageYield(stage, juvenileYield)
                * dropMultiplier(nutrition, starved, threshold, minMultiplier);
    }

    /**
     * Scales a stack down. The fractional part is settled by a roll rather than rounded, so a
     * single hide at 0.35 condition becomes a 35% chance of a hide instead of always one or always
     * none - across a herd the average comes out right, and no single kill feels arbitrary.
     */
    public static int scaleCount(int count, double multiplier, double roll) {
        if (count <= 0) return 0;
        if (multiplier >= 1.0) return count;
        if (multiplier <= 0.0) return 0;

        double exact = count * multiplier;
        int whole = (int) Math.floor(exact);
        double remainder = exact - whole;
        return roll < remainder ? whole + 1 : whole;
    }

    private static double clamp01(double value) {
        if (value < 0.0) return 0.0;
        return Math.min(value, 1.0);
    }
}
