package com.babyneeds;

/**
 * Pasture wear. Grazing itself never ruins soil - it just leaves bare dirt, and vanilla grass
 * creeps back onto it if the animals move on. Soil is only lost when bare ground stays bare for a
 * long time AND keeps being walked over: that is a pen the herd never leaves, and it compacts into
 * coarse dirt, which grass will not spread onto until a player hoes it.
 *
 * The grace period is the whole point: a player who rotates the herd, or simply has room enough
 * that the animals wander, never sees coarse dirt at all.
 */
public final class PastureCalculator {

    private PastureCalculator() {
    }

    /**
     * Bare ground is only at risk once it has had time to recover on its own and has still been
     * trampled hard.
     *
     * @param bareTicks       ticks since the turf was eaten off this block
     * @param graceTicks      ticks of recovery time given before the soil can be lost at all
     * @param tramples        times an animal has been recorded standing on it since then
     * @param trampleThreshold tramples below which the soil is never at risk
     */
    public static boolean canCompact(long bareTicks, int graceTicks, int tramples, int trampleThreshold) {
        return bareTicks >= graceTicks && tramples >= trampleThreshold;
    }

    /**
     * Chance, per recorded trample, that the block turns to coarse dirt. Zero during the grace
     * period, and scaled by how stripped the surrounding pasture is and how far past the trample
     * threshold this spot has gone, so the damage lands where the herd actually congregates.
     */
    public static double compactChance(long bareTicks, int graceTicks, int tramples,
                                       int trampleThreshold, double grassRatio, double maxChance) {
        if (!canCompact(bareTicks, graceTicks, tramples, trampleThreshold)) return 0.0;

        double bareFactor = 1.0 - clamp01(grassRatio);
        int ramp = Math.max(1, trampleThreshold * 3);
        double trampleFactor = Math.min(1.0, (double) tramples / ramp);
        return Math.max(0.0, maxChance) * bareFactor * trampleFactor;
    }

    public static boolean compact(double chance, double roll) {
        return roll < chance;
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
