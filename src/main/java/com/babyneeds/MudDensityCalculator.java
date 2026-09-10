package com.babyneeds;

import java.util.function.BiPredicate;

/**
 * How strongly a spot reads as "near mud" - the number that drives every murky-water cue: how
 * often a haze particle shows up, whether soil under the water is soaking through, whether a
 * swimmer's vision gets fogged. One reading, several uses, so all of them agree on where the wallow
 * actually is instead of each rolling its own notion of nearby.
 *
 * Kept free of Minecraft types, the same way every other calculator in this mod is, so the falloff
 * shape can be tested without a running game. {@link MuddyWater#mudDensity} is what wires it to
 * real block lookups.
 */
public final class MudDensityCalculator {

    private MudDensityCalculator() {
    }

    /**
     * A distance-weighted reading from a disc of samples out to {@code radius} blocks: 0 is
     * nothing muddy anywhere near it, 1 is mud on every side. Weighted so the reading changes
     * gradually from block to block instead of jumping the moment a sample point crosses in or out
     * of the disc.
     *
     * @param radius  how far out to sample, in blocks
     * @param isMuddy told, for an offset (dx, dz) from the centre, whether that spot is mud
     */
    public static double weightedDensity(int radius, BiPredicate<Integer, Integer> isMuddy) {
        double weighted = 0.0;
        double total = 0.0;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                double distance = Math.sqrt(dx * dx + dz * dz);
                if (distance > radius) continue;

                double weight = radius - distance;
                total += weight;
                if (isMuddy.test(dx, dz)) {
                    weighted += weight;
                }
            }
        }
        return total <= 0.0 ? 1.0 : weighted / total;
    }

    /**
     * Eases a 0-1 fraction so the middle of the range moves fastest and both ends settle in gently,
     * instead of every effect scaling at a constant rate all the way across a wallow.
     */
    public static double ease(double fraction) {
        double clamped = Math.max(0.0, Math.min(1.0, fraction));
        return clamped * clamped * (3.0 - 2.0 * clamped);
    }

    /**
     * Blends two colours (0xRRGGBB, any leading byte ignored on the way in) by a 0-1 fraction and
     * returns the result fully opaque - 0xFF in the leading byte, which is how the fluid renderer
     * wants a tint packed.
     */
    public static int blend(int fromRgb, int toRgb, double fraction) {
        double t = Math.max(0.0, Math.min(1.0, fraction));
        int r = blendChannel(fromRgb >> 16, toRgb >> 16, t);
        int g = blendChannel(fromRgb >> 8, toRgb >> 8, t);
        int b = blendChannel(fromRgb, toRgb, t);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static int blendChannel(int from, int to, double t) {
        int a = from & 0xFF;
        int b = to & 0xFF;
        return (int) Math.round(a + (b - a) * t);
    }
}
