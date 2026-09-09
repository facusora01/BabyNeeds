package com.babyneeds;

import org.junit.Test;

import static org.junit.Assert.*;

public class PastureCalculatorTest {

    private static final double BASE = 0.10;
    private static final double MAX = 0.60;

    @Test
    public void lushPastureOnlyDegradesAtTheBaseRate() {
        assertEquals(BASE, PastureCalculator.coarseChance(1.0, BASE, MAX), 0.0001);
    }

    @Test
    public void strippedPastureDegradesAtTheMaximumRate() {
        // An overstocked pen: everything around the spot is already eaten down.
        assertEquals(MAX, PastureCalculator.coarseChance(0.0, BASE, MAX), 0.0001);
    }

    @Test
    public void degradationRisesAsThePastureIsEatenDown() {
        double lush = PastureCalculator.coarseChance(0.9, BASE, MAX);
        double thin = PastureCalculator.coarseChance(0.3, BASE, MAX);
        assertTrue(thin > lush);
    }

    @Test
    public void ratiosOutsideZeroToOneAreClamped() {
        assertEquals(BASE, PastureCalculator.coarseChance(5.0, BASE, MAX), 0.0001);
        assertEquals(MAX, PastureCalculator.coarseChance(-2.0, BASE, MAX), 0.0001);
    }

    @Test
    public void theRollDecidesTheOutcome() {
        assertEquals(PastureCalculator.Outcome.COARSE_DIRT, PastureCalculator.graze(0.5, 0.49));
        assertEquals(PastureCalculator.Outcome.DIRT, PastureCalculator.graze(0.5, 0.51));
    }

    @Test
    public void aStrippedPastureFeedsWorseThanALushOne() {
        double lush = PastureCalculator.foodValue(1.0, 22.0);
        double bare = PastureCalculator.foodValue(0.0, 22.0);
        assertEquals(22.0, lush, 0.001);
        assertEquals(11.0, bare, 0.001);
    }
}
