package com.babyneeds;

import org.junit.Test;

import static org.junit.Assert.*;

public class PastureCalculatorTest {

    private static final int GRACE = 24000;
    private static final int TRAMPLE_THRESHOLD = 60;
    private static final double MAX_CHANCE = 0.02;

    @Test
    public void freshlyGrazedGroundIsNeverAtRisk() {
        // The grace period is the promise that a rotated pasture keeps its soil.
        assertFalse(PastureCalculator.canCompact(0, GRACE, 999, TRAMPLE_THRESHOLD));
        assertFalse(PastureCalculator.canCompact(GRACE - 1, GRACE, 999, TRAMPLE_THRESHOLD));
    }

    @Test
    public void groundTheHerdLeftAloneIsNeverAtRisk() {
        // Bare for days, but nothing walks on it: grass gets its chance to come back.
        assertFalse(PastureCalculator.canCompact(GRACE * 10L, GRACE, 0, TRAMPLE_THRESHOLD));
        assertFalse(PastureCalculator.canCompact(GRACE * 10L, GRACE, TRAMPLE_THRESHOLD - 1, TRAMPLE_THRESHOLD));
    }

    @Test
    public void onlyOldBareGroundUnderConstantTrafficCompacts() {
        assertTrue(PastureCalculator.canCompact(GRACE, GRACE, TRAMPLE_THRESHOLD, TRAMPLE_THRESHOLD));
    }

    @Test
    public void chanceIsZeroWhileEitherConditionIsUnmet() {
        assertEquals(0.0, PastureCalculator.compactChance(0, GRACE, 999, TRAMPLE_THRESHOLD, 0.0, MAX_CHANCE), 0.0);
        assertEquals(0.0, PastureCalculator.compactChance(GRACE * 5L, GRACE, 1, TRAMPLE_THRESHOLD, 0.0, MAX_CHANCE), 0.0);
    }

    @Test
    public void aPenWithGrassLeftAroundItSurvivesLonger() {
        double stripped = PastureCalculator.compactChance(GRACE * 2L, GRACE, 180, TRAMPLE_THRESHOLD, 0.0, MAX_CHANCE);
        double patchy = PastureCalculator.compactChance(GRACE * 2L, GRACE, 180, TRAMPLE_THRESHOLD, 0.5, MAX_CHANCE);
        assertTrue(stripped > patchy);
    }

    @Test
    public void grassAllAroundMeansNoSoilLossAtAll() {
        assertEquals(0.0,
                PastureCalculator.compactChance(GRACE * 2L, GRACE, 999, TRAMPLE_THRESHOLD, 1.0, MAX_CHANCE), 0.0);
    }

    @Test
    public void theWorstCaseNeverExceedsTheConfiguredCeiling() {
        double worst = PastureCalculator.compactChance(
                GRACE * 100L, GRACE, 100000, TRAMPLE_THRESHOLD, 0.0, MAX_CHANCE);
        assertEquals(MAX_CHANCE, worst, 0.0001);
    }

    @Test
    public void harderTrampledGroundGoesFaster() {
        double light = PastureCalculator.compactChance(GRACE * 2L, GRACE, 60, TRAMPLE_THRESHOLD, 0.0, MAX_CHANCE);
        double heavy = PastureCalculator.compactChance(GRACE * 2L, GRACE, 150, TRAMPLE_THRESHOLD, 0.0, MAX_CHANCE);
        assertTrue(heavy > light);
    }

    @Test
    public void theRollDecidesTheOutcome() {
        assertTrue(PastureCalculator.compact(0.02, 0.019));
        assertFalse(PastureCalculator.compact(0.02, 0.021));
        assertFalse(PastureCalculator.compact(0.0, 0.0));
    }

    @Test
    public void aStrippedPastureFeedsWorseThanALushOne() {
        assertEquals(22.0, PastureCalculator.foodValue(1.0, 22.0), 0.001);
        assertEquals(11.0, PastureCalculator.foodValue(0.0, 22.0), 0.001);
    }
}
