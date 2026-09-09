package com.babyneeds;

import org.junit.Test;

import static org.junit.Assert.*;

public class HungerCalculatorTest {

    private static final double DECAY_PER_MINUTE = 1.2;
    private static final double MUD_REDUCTION = 0.35;

    @Test
    public void decayMatchesTheConfiguredRatePerMinute() {
        double perTick = HungerCalculator.decayPerTick(DECAY_PER_MINUTE, 1.0, false, MUD_REDUCTION);
        assertEquals(DECAY_PER_MINUTE, perTick * 1200.0, 0.0001);
    }

    @Test
    public void hungrierSpeciesDrainFaster() {
        double pig = HungerCalculator.decayPerTick(DECAY_PER_MINUTE, Species.PIG.hungerRate(), false, MUD_REDUCTION);
        double sheep = HungerCalculator.decayPerTick(DECAY_PER_MINUTE, Species.SHEEP.hungerRate(), false, MUD_REDUCTION);
        assertTrue(pig > sheep);
    }

    @Test
    public void mudSavesAPigTheConfiguredShare() {
        double dry = HungerCalculator.decayPerTick(DECAY_PER_MINUTE, 1.0, false, MUD_REDUCTION);
        double wallowing = HungerCalculator.decayPerTick(DECAY_PER_MINUTE, 1.0, true, MUD_REDUCTION);
        assertEquals(dry * (1.0 - MUD_REDUCTION), wallowing, 0.0001);
    }

    @Test
    public void nutritionNeverGoesBelowZeroOrAboveFull() {
        assertEquals(0.0, HungerCalculator.drain(3.0, 10.0), 0.001);
        assertEquals(HungerCalculator.MAX_NUTRITION, HungerCalculator.feed(95.0, 50.0), 0.001);
    }

    @Test
    public void grazingStartsBelowTheThresholdOnly() {
        assertTrue(HungerCalculator.wantsToGraze(69.9, 70.0));
        assertFalse(HungerCalculator.wantsToGraze(70.0, 70.0));
    }

    @Test
    public void aFedAnimalNeverStarves() {
        assertFalse(HungerCalculator.shouldTakeStarvationDamage(0.1, 999999, 6000, 200));
    }

    @Test
    public void starvationWaitsForTheGracePeriod() {
        assertFalse(HungerCalculator.shouldTakeStarvationDamage(0.0, 5980, 6000, 200));
        assertTrue(HungerCalculator.shouldTakeStarvationDamage(0.0, 6000, 6000, 200));
    }

    @Test
    public void starvationThenHitsOnTheInterval() {
        assertTrue(HungerCalculator.shouldTakeStarvationDamage(0.0, 6200, 6000, 200));
        assertFalse(HungerCalculator.shouldTakeStarvationDamage(0.0, 6100, 6000, 200));
    }
}
