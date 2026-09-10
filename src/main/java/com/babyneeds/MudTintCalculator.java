package com.babyneeds;

import java.util.function.BiPredicate;

/**
 * The visual math behind a wallow's edge: how the render tint of muddy water eases into the plain
 * water beside it instead of jumping straight from one colour to the other.
 *
 * A single flat brown for every muddy water block reads as a block swap rather than as silt in
 * water, because the block right against clean water is exactly as opaque as one boxed in by mud
 * on every side. What actually happens at the edge of a real wallow is a haze that thins out - so
 * this reads a little of the neighbourhood around each block and eases the tint by how "in the
 * wallow" that spot is, rather than treating every muddy water block the same.
 *
 * Kept free of Minecraft types, the same way every other calculator in this mod is, so the falloff
 * shape and the colour interpolation can be tested without a running game.
 * {@link MuddyWater#wallowDensity} is what wires it to real block lookups.
 */
public final class MudTintCalculator {

    private MudTintCalculator() {
    }

    /**
     * How "in the wallow" a spot reads, from a disc of samples out to {@code radius} blocks: 0 is
     * clean water with nothing muddy anywhere near it, 1 is walled in by mud or more muddy water on
     * every side. Distance-weighted so the reading changes gradually from block to block instead of
     * jumping the moment a sample point crosses in or out of the disc.
     *
     * @param radius  how far out to sample, in blocks
     * @param isMuddy told, for an offset (dx, dz) from the centre, whether that spot is mud or
     *                muddy water
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
     * Eases a 0-1 fraction so the middle of the transition moves fastest and both ends settle in
     * gently, instead of the tint changing at a constant rate all the way across the wallow.
     */
    public static double ease(double fraction) {
        double clamped = Math.max(0.0, Math.min(1.0, fraction));
        return clamped * clamped * (3.0 - 2.0 * clamped);
    }

    /**
     * Blends two colours (0xRRGGBB, any leading byte ignored on the way in) by a 0-1 fraction and
     * returns the result fully opaque - 0xFF in the leading byte, matching what the renderer wants
     * a fluid tint packed as.
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
