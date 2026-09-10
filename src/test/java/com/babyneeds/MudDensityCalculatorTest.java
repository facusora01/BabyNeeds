package com.babyneeds;

import org.junit.Test;

import static org.junit.Assert.*;

public class MudDensityCalculatorTest {

    @Test
    public void nothingMuddyAnywhereIsZero() {
        assertEquals(0.0, MudDensityCalculator.weightedDensity(3, (dx, dz) -> false), 0.001);
    }

    @Test
    public void muddyEverywhereIsOne() {
        assertEquals(1.0, MudDensityCalculator.weightedDensity(3, (dx, dz) -> true), 0.001);
    }

    @Test
    public void closerMuddySpotsWeighMoreThanFarOnes() {
        // A single muddy sample right at the centre pulls the reading up far more than the same
        // single sample out near the edge of the disc.
        double near = MudDensityCalculator.weightedDensity(3, (dx, dz) -> dx == 0 && dz == 0);
        double far = MudDensityCalculator.weightedDensity(3, (dx, dz) -> dx == 2 && dz == 0);
        assertTrue(near > far);
        assertTrue(far > 0.0);
    }

    @Test
    public void densityIsSymmetric() {
        // Mud due north of the sample should count exactly as much as mud due south of it - the
        // falloff cares about distance, not direction.
        double north = MudDensityCalculator.weightedDensity(2, (dx, dz) -> dz == -2 && dx == 0);
        double south = MudDensityCalculator.weightedDensity(2, (dx, dz) -> dz == 2 && dx == 0);
        assertEquals(north, south, 0.0001);
    }

    @Test
    public void easeHoldsBothEnds() {
        assertEquals(0.0, MudDensityCalculator.ease(0.0), 0.0001);
        assertEquals(1.0, MudDensityCalculator.ease(1.0), 0.0001);
    }

    @Test
    public void easePassesThroughTheMiddle() {
        assertEquals(0.5, MudDensityCalculator.ease(0.5), 0.0001);
    }

    @Test
    public void easeClampsOutOfRangeInput() {
        assertEquals(0.0, MudDensityCalculator.ease(-0.5), 0.0001);
        assertEquals(1.0, MudDensityCalculator.ease(1.5), 0.0001);
    }
}
