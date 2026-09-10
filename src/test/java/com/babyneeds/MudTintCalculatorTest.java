package com.babyneeds;

import org.junit.Test;

import static org.junit.Assert.*;

public class MudTintCalculatorTest {

    private static final int WATER = 0x1E5AA8;
    private static final int MUD = 0x6B5230;

    // ------------------------------------------------------------------ weightedDensity

    @Test
    public void nothingMuddyAnywhereIsZero() {
        assertEquals(0.0, MudTintCalculator.weightedDensity(3, (dx, dz) -> false), 0.001);
    }

    @Test
    public void muddyEverywhereIsOne() {
        assertEquals(1.0, MudTintCalculator.weightedDensity(3, (dx, dz) -> true), 0.001);
    }

    @Test
    public void closerMuddySpotsWeighMoreThanFarOnes() {
        // A single muddy sample right at the centre pulls the reading up far more than the same
        // single sample out at the edge of the disc.
        double near = MudTintCalculator.weightedDensity(3, (dx, dz) -> dx == 0 && dz == 0);
        double far = MudTintCalculator.weightedDensity(3, (dx, dz) -> dx == 2 && dz == 0);
        assertTrue(near > far);
        assertTrue(far > 0.0);
    }

    @Test
    public void densityIsSymmetric() {
        // Mud due north of the sample should count exactly as much as mud due south of it - the
        // falloff cares about distance, not direction.
        double north = MudTintCalculator.weightedDensity(2, (dx, dz) -> dz == -2 && dx == 0);
        double south = MudTintCalculator.weightedDensity(2, (dx, dz) -> dz == 2 && dx == 0);
        assertEquals(north, south, 0.0001);
    }

    // ------------------------------------------------------------------ ease

    @Test
    public void easeHoldsBothEnds() {
        assertEquals(0.0, MudTintCalculator.ease(0.0), 0.0001);
        assertEquals(1.0, MudTintCalculator.ease(1.0), 0.0001);
    }

    @Test
    public void easePassesThroughTheMiddle() {
        assertEquals(0.5, MudTintCalculator.ease(0.5), 0.0001);
    }

    @Test
    public void easeClampsOutOfRangeInput() {
        assertEquals(0.0, MudTintCalculator.ease(-0.5), 0.0001);
        assertEquals(1.0, MudTintCalculator.ease(1.5), 0.0001);
    }

    // ------------------------------------------------------------------ blend

    @Test
    public void blendAtZeroIsTheFirstColourFullyOpaque() {
        assertEquals(0xFF000000 | WATER, MudTintCalculator.blend(WATER, MUD, 0.0));
    }

    @Test
    public void blendAtOneIsTheSecondColourFullyOpaque() {
        assertEquals(0xFF000000 | MUD, MudTintCalculator.blend(WATER, MUD, 1.0));
    }

    @Test
    public void blendAtHalfSitsBetweenBothChannelByChannel() {
        int mid = MudTintCalculator.blend(0x000000, 0xFFFFFF, 0.5);
        // Rounds to the same midpoint on every channel, since both ends are 0x00 and 0xFF.
        assertEquals(0xFF808080, mid);
    }

    @Test
    public void blendIgnoresWhateverIsInTheLeadingByteOfTheInputs() {
        int withAlpha = MudTintCalculator.blend(0xAA1E5AA8, 0x776B5230, 0.0);
        int without = MudTintCalculator.blend(0x001E5AA8, 0x006B5230, 0.0);
        assertEquals(without, withAlpha);
    }
}
