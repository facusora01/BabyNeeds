package com.babyneeds;

import org.junit.Test;

import static org.junit.Assert.*;

public class GrowthCalculatorTest {

    private static final double BASE_INTAKE = 60.0;
    private static final int BASE_TICKS = 6000;
    private static final double NORMAL_SPEED = 1.0;

    @Test
    public void babyDrinksMilkAndCannotEatGrass() {
        assertTrue(GrowthCalculator.acceptsMilk(GrowthStage.BABY));
        assertFalse(GrowthCalculator.acceptsSolids(GrowthStage.BABY));
    }

    @Test
    public void weanlingDoesBoth() {
        assertTrue(GrowthCalculator.acceptsMilk(GrowthStage.WEANLING));
        assertTrue(GrowthCalculator.acceptsSolids(GrowthStage.WEANLING));
    }

    @Test
    public void juvenileIsOffMilk() {
        assertFalse(GrowthCalculator.acceptsMilk(GrowthStage.JUVENILE));
        assertTrue(GrowthCalculator.acceptsSolids(GrowthStage.JUVENILE));
    }

    @Test
    public void eachStageCostsMoreThanTheOneBefore() {
        double baby = GrowthCalculator.requiredIntake(GrowthStage.BABY, BASE_INTAKE, NORMAL_SPEED);
        double weanling = GrowthCalculator.requiredIntake(GrowthStage.WEANLING, BASE_INTAKE, NORMAL_SPEED);
        double juvenile = GrowthCalculator.requiredIntake(GrowthStage.JUVENILE, BASE_INTAKE, NORMAL_SPEED);
        assertTrue(baby < weanling);
        assertTrue(weanling < juvenile);
    }

    @Test
    public void speedMultiplierScalesBothRequirements() {
        assertEquals(30.0, GrowthCalculator.requiredIntake(GrowthStage.BABY, BASE_INTAKE, 2.0), 0.001);
        assertEquals(3000, GrowthCalculator.requiredTicks(GrowthStage.BABY, BASE_TICKS, 2.0));
    }

    @Test
    public void zeroSpeedIsTreatedAsNormalInsteadOfDividingByZero() {
        assertEquals(BASE_INTAKE, GrowthCalculator.requiredIntake(GrowthStage.BABY, BASE_INTAKE, 0.0), 0.001);
    }

    @Test
    public void intakeNeverOvershootsWhatTheStageNeeds() {
        assertEquals(60.0, GrowthCalculator.addIntake(55.0, 20.0, 60.0), 0.001);
    }

    @Test
    public void negativeIntakeCannotUndoGrowth() {
        assertEquals(30.0, GrowthCalculator.addIntake(30.0, -10.0, 60.0), 0.001);
    }

    @Test
    public void wellFedButTooYoungDoesNotAdvance() {
        // The whole point of the minimum: food alone cannot rush a stage.
        assertFalse(GrowthCalculator.canAdvance(GrowthStage.BABY, 60.0, 60.0, 100, BASE_TICKS));
    }

    @Test
    public void oldEnoughButUnderfedDoesNotAdvance() {
        // A calf that never found milk stays a calf instead of ageing up on a timer.
        assertFalse(GrowthCalculator.canAdvance(GrowthStage.BABY, 10.0, 60.0, BASE_TICKS * 10, BASE_TICKS));
    }

    @Test
    public void fedAndOldEnoughAdvances() {
        assertTrue(GrowthCalculator.canAdvance(GrowthStage.BABY, 60.0, 60.0, BASE_TICKS, BASE_TICKS));
    }

    @Test
    public void adultNeverAdvances() {
        assertFalse(GrowthCalculator.canAdvance(GrowthStage.ADULT, 999.0, 1.0, 999999, 0));
    }

    @Test
    public void stagesChainToAdultAndStop() {
        assertEquals(GrowthStage.WEANLING, GrowthStage.BABY.next());
        assertEquals(GrowthStage.JUVENILE, GrowthStage.WEANLING.next());
        assertEquals(GrowthStage.ADULT, GrowthStage.JUVENILE.next());
        assertEquals(GrowthStage.ADULT, GrowthStage.ADULT.next());
    }

    @Test
    public void onlyTheFirstTwoStagesRenderAsVanillaBabies() {
        assertTrue(GrowthStage.BABY.isVanillaBaby());
        assertTrue(GrowthStage.WEANLING.isVanillaBaby());
        assertFalse(GrowthStage.JUVENILE.isVanillaBaby());
        assertFalse(GrowthStage.ADULT.isVanillaBaby());
    }

    @Test
    public void stageIndexRoundTripsAndClampsGarbage() {
        for (GrowthStage stage : GrowthStage.values()) {
            assertEquals(stage, GrowthStage.byIndex(stage.index()));
        }
        assertEquals(GrowthStage.BABY, GrowthStage.byIndex(-3));
        assertEquals(GrowthStage.ADULT, GrowthStage.byIndex(99));
    }
}
