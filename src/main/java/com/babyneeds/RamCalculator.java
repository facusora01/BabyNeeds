package com.babyneeds;

/**
 * How likely a defending cow is to ram rather than simply chase. A ram is a run-up: standing on top
 * of someone there is nothing to run, so she shoves instead, while distance is exactly what the move
 * is for. The odds therefore climb across her rammable range instead of being flat.
 */
public final class RamCalculator {

    private RamCalculator() {
    }

    /**
     * @param distance   how far the target is right now
     * @param minRange   below this a ram is not attempted at all
     * @param maxRange   the far edge of her range, where the odds are highest
     * @param nearChance odds at the near edge, where there is barely room to build up speed
     * @param farChance  odds at the far edge, where a charge is the obvious move
     */
    public static double chance(double distance, double minRange, double maxRange,
                                double nearChance, double farChance) {
        if (maxRange <= minRange) return clamp01(farChance);

        double clamped = Math.max(minRange, Math.min(maxRange, distance));
        double t = (clamped - minRange) / (maxRange - minRange);
        return clamp01(nearChance + (clamp01(farChance) - clamp01(nearChance)) * t);
    }

    private static double clamp01(double value) {
        if (value < 0.0) return 0.0;
        return Math.min(value, 1.0);
    }
}
