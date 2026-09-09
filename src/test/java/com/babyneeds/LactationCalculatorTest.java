package com.babyneeds;

import org.junit.Test;

import static org.junit.Assert.*;

public class LactationCalculatorTest {

    private static final double MIN_NUTRITION = 35.0;
    private static final double BUCKET_COST = 34.0;
    private static final double CHILD_RESERVE = 20.0;

    @Test
    public void aHungryMotherMakesNoMilk() {
        // The core chain of the mod: no grass, no nutrition, no milk, no calf.
        assertFalse(LactationCalculator.canProduce(34.9, MIN_NUTRITION));
        assertTrue(LactationCalculator.canProduce(35.0, MIN_NUTRITION));
    }

    @Test
    public void productionMatchesTheConfiguredRatePerMinute() {
        double perTick = LactationCalculator.productionPerTick(7.0, 1.0);
        assertEquals(7.0, perTick * 1200.0, 0.0001);
    }

    @Test
    public void udderCapacityCapsProduction() {
        assertEquals(LactationCalculator.MAX_MILK, LactationCalculator.produce(98.0, 20.0), 0.001);
    }

    @Test
    public void milkIsPaidForOutOfHerOwnNutrition() {
        assertEquals(8.0, LactationCalculator.nutritionCost(10.0, 0.8), 0.001);
    }

    @Test
    public void aBabyOnlyGetsWhatIsActuallyThere() {
        assertEquals(12.0, LactationCalculator.drawMilk(50.0, 12.0), 0.001);
        assertEquals(4.0, LactationCalculator.drawMilk(4.0, 12.0), 0.001);
        assertEquals(0.0, LactationCalculator.drawMilk(0.0, 12.0), 0.001);
    }

    @Test
    public void aFullUdderWithNoBabyGivesTheWholeBucket() {
        assertTrue(LactationCalculator.canBeMilkedByPlayer(40.0, BUCKET_COST, false, CHILD_RESERVE));
    }

    @Test
    public void milkingCannotEatIntoWhatTheCalfNeeds() {
        // 40 milk minus the 20 held back for the calf is not enough for a 34 unit bucket.
        assertFalse(LactationCalculator.canBeMilkedByPlayer(40.0, BUCKET_COST, true, CHILD_RESERVE));
        assertTrue(LactationCalculator.canBeMilkedByPlayer(60.0, BUCKET_COST, true, CHILD_RESERVE));
    }

    @Test
    public void anEmptyUdderGivesNothing() {
        assertFalse(LactationCalculator.canBeMilkedByPlayer(0.0, BUCKET_COST, false, CHILD_RESERVE));
    }
}
