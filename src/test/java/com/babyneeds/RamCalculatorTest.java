package com.babyneeds;

import org.junit.Test;

import static org.junit.Assert.*;

public class RamCalculatorTest {

    private static final double MIN = 3.0;
    private static final double MAX = 12.0;
    private static final double NEAR = 0.08;
    private static final double FAR = 0.55;

    @Test
    public void closeUpSheShovesRatherThanCharges() {
        assertEquals(NEAR, RamCalculator.chance(MIN, MIN, MAX, NEAR, FAR), 0.001);
    }

    @Test
    public void atFullDistanceAChargeIsTheObviousMove() {
        assertEquals(FAR, RamCalculator.chance(MAX, MIN, MAX, NEAR, FAR), 0.001);
    }

    @Test
    public void theOddsClimbWithDistance() {
        double near = RamCalculator.chance(4.0, MIN, MAX, NEAR, FAR);
        double middle = RamCalculator.chance(7.5, MIN, MAX, NEAR, FAR);
        double far = RamCalculator.chance(11.0, MIN, MAX, NEAR, FAR);
        assertTrue(middle > near);
        assertTrue(far > middle);
    }

    @Test
    public void halfwayIsAboutTheOldFlatThirty() {
        assertEquals(0.315, RamCalculator.chance(7.5, MIN, MAX, NEAR, FAR), 0.01);
    }

    @Test
    public void distancesOutsideTheBandAreClamped() {
        assertEquals(NEAR, RamCalculator.chance(0.5, MIN, MAX, NEAR, FAR), 0.001);
        assertEquals(FAR, RamCalculator.chance(64.0, MIN, MAX, NEAR, FAR), 0.001);
    }

    @Test
    public void aChanceIsAlwaysAProbability() {
        assertEquals(1.0, RamCalculator.chance(MAX, MIN, MAX, 0.0, 5.0), 0.001);
        assertEquals(0.0, RamCalculator.chance(MIN, MIN, MAX, -3.0, 0.5), 0.001);
    }

    @Test
    public void degenerateRangesFallBackToTheFarOdds() {
        assertEquals(FAR, RamCalculator.chance(5.0, 10.0, 10.0, NEAR, FAR), 0.001);
    }
}
