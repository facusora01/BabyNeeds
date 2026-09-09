package com.babyneeds;

import org.junit.Test;

import static org.junit.Assert.*;

public class LootCalculatorTest {

    private static final double THRESHOLD = 50.0;
    private static final double STARVED = 0.25;
    private static final double JUVENILE = 0.45;

    @Test
    public void aWellFedCarcassDropsEverything() {
        assertEquals(1.0, LootCalculator.dropMultiplier(100.0, false, THRESHOLD, STARVED), 0.001);
        assertEquals(1.0, LootCalculator.dropMultiplier(THRESHOLD, false, THRESHOLD, STARVED), 0.001);
    }

    @Test
    public void starvingToDeathAlwaysGivesTheWorstCarcass() {
        assertEquals(STARVED, LootCalculator.dropMultiplier(0.0, true, THRESHOLD, STARVED), 0.001);
        // Even the odd case of a full animal killed by starvation damage lands on the floor.
        assertEquals(STARVED, LootCalculator.dropMultiplier(100.0, true, THRESHOLD, STARVED), 0.001);
    }

    @Test
    public void butcheringAnUnderfedAnimalIsAlreadyALoss() {
        double half = LootCalculator.dropMultiplier(25.0, false, THRESHOLD, STARVED);
        assertTrue(half < 1.0);
        assertTrue(half > STARVED);
        // Halfway to the threshold sits halfway between the floor and a full carcass.
        assertEquals(STARVED + (1.0 - STARVED) * 0.5, half, 0.001);
    }

    @Test
    public void conditionSlidesSmoothlyTowardsTheFloor() {
        double worse = LootCalculator.dropMultiplier(10.0, false, THRESHOLD, STARVED);
        double better = LootCalculator.dropMultiplier(40.0, false, THRESHOLD, STARVED);
        assertTrue(better > worse);
        assertTrue(worse >= STARVED);
    }

    @Test
    public void aFullStackIsScaledDown() {
        // Three beef at a quarter condition: 0.75, so no guaranteed drop and a 75% chance of one.
        assertEquals(1, LootCalculator.scaleCount(3, 0.25, 0.5));
        assertEquals(0, LootCalculator.scaleCount(3, 0.25, 0.9));
    }

    @Test
    public void theFractionIsSettledByARollRatherThanRounded() {
        // A single hide at 0.25 is a 25% chance of a hide, not a guaranteed nothing.
        assertEquals(1, LootCalculator.scaleCount(1, 0.25, 0.2));
        assertEquals(0, LootCalculator.scaleCount(1, 0.25, 0.3));
    }

    @Test
    public void wholeMultiplesNeedNoRoll() {
        assertEquals(2, LootCalculator.scaleCount(4, 0.5, 0.0));
        assertEquals(2, LootCalculator.scaleCount(4, 0.5, 0.99));
    }

    @Test
    public void fullConditionKeepsTheStackUntouched() {
        assertEquals(3, LootCalculator.scaleCount(3, 1.0, 0.99));
    }

    @Test
    public void babiesAndWeanlingsYieldNothing() {
        assertEquals(0.0, LootCalculator.stageYield(GrowthStage.BABY, JUVENILE), 0.001);
        assertEquals(0.0, LootCalculator.stageYield(GrowthStage.WEANLING, JUVENILE), 0.001);
    }

    @Test
    public void anAdolescentIsWorthLessThanAnAdult() {
        // Without this the fourth stage is decoration: same meat, same hide, one stage earlier.
        assertEquals(JUVENILE, LootCalculator.stageYield(GrowthStage.JUVENILE, JUVENILE), 0.001);
        assertEquals(1.0, LootCalculator.stageYield(GrowthStage.ADULT, JUVENILE), 0.001);
        assertTrue(LootCalculator.stageYield(GrowthStage.JUVENILE, JUVENILE)
                < LootCalculator.stageYield(GrowthStage.ADULT, JUVENILE));
    }

    @Test
    public void aWellFedAdultIsTheFullCarcass() {
        assertEquals(1.0, LootCalculator.carcassYield(
                GrowthStage.ADULT, 100.0, false, THRESHOLD, STARVED, JUVENILE), 0.001);
    }

    @Test
    public void sizeAndConditionMultiplyTogether() {
        // A half-grown animal that starved to death is the worst of both: 0.45 * 0.25.
        assertEquals(JUVENILE * STARVED, LootCalculator.carcassYield(
                GrowthStage.JUVENILE, 0.0, true, THRESHOLD, STARVED, JUVENILE), 0.001);
    }

    @Test
    public void raisingToAdultAlwaysBeatsSlaughteringEarly() {
        double juvenile = LootCalculator.carcassYield(
                GrowthStage.JUVENILE, 100.0, false, THRESHOLD, STARVED, JUVENILE);
        double adult = LootCalculator.carcassYield(
                GrowthStage.ADULT, 100.0, false, THRESHOLD, STARVED, JUVENILE);
        assertTrue(adult > juvenile);
    }

    @Test
    public void degenerateInputsAreHandled() {
        assertEquals(0, LootCalculator.scaleCount(0, 0.5, 0.1));
        assertEquals(0, LootCalculator.scaleCount(3, 0.0, 0.0));
        assertEquals(1.0, LootCalculator.dropMultiplier(0.0, false, 0.0, STARVED), 0.001);
    }
}
