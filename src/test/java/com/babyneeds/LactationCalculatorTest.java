package com.babyneeds;

import org.junit.Test;

import static org.junit.Assert.*;

public class LactationCalculatorTest {

    private static final double MIN_NUTRITION = 35.0;
    private static final double BUCKET_COST = 34.0;
    private static final double CHILD_RESERVE = 20.0;
    private static final double BOUT = 12.0;
    private static final double MIN_RATION = 4.0;

    @Test
    public void aHungryMotherMakesNoMilk() {
        // The core chain of the mod: no grass, no nutrition, no milk, no calf.
        assertFalse(LactationCalculator.canProduce(34.9, MIN_NUTRITION));
        assertTrue(LactationCalculator.canProduce(35.0, MIN_NUTRITION));
    }

    @Test
    public void productionMatchesTheConfiguredRatePerBabyPerMinute() {
        assertEquals(7.0, LactationCalculator.productionPerTick(7.0, 1.0, 1) * 1200.0, 0.0001);
    }

    @Test
    public void aSowWithALitterMakesEnoughForTheWholeLitter() {
        double one = LactationCalculator.productionPerTick(7.0, 1.0, 1);
        double four = LactationCalculator.productionPerTick(7.0, 1.0, 4);
        assertEquals(one * 4.0, four, 0.000001);
        assertEquals(LactationCalculator.MAX_MILK * 4.0, LactationCalculator.capacity(4), 0.001);
    }

    @Test
    public void aMotherWithNoBabiesIsStillTreatedAsOneUdder() {
        assertEquals(LactationCalculator.MAX_MILK, LactationCalculator.capacity(0), 0.001);
        assertEquals(LactationCalculator.productionPerTick(7.0, 1.0, 1),
                LactationCalculator.productionPerTick(7.0, 1.0, 0), 0.000001);
    }

    @Test
    public void udderCapacityCapsProduction() {
        assertEquals(100.0, LactationCalculator.produce(98.0, 20.0, 100.0), 0.001);
        assertEquals(400.0, LactationCalculator.produce(398.0, 20.0, 400.0), 0.001);
    }

    @Test
    public void milkIsPaidForOutOfHerOwnNutrition() {
        assertEquals(8.0, LactationCalculator.nutritionCost(10.0, 0.8), 0.001);
    }

    @Test
    public void anOnlyChildDrinksAFullBout() {
        assertEquals(BOUT, LactationCalculator.ration(90.0, 1, BOUT, MIN_RATION), 0.001);
    }

    @Test
    public void noPigletCanDrinkTheLitterDry() {
        // Four piglets, 40 milk in the udder: the first to latch takes its quarter, not the lot.
        assertEquals(10.0, LactationCalculator.ration(40.0, 4, BOUT, MIN_RATION), 0.001);
    }

    @Test
    public void everyPigletInALitterOfFourGetsFedFromOneUdder() {
        // Walk the whole litter through in sequence, worst case: the same udder, no refill.
        double milk = 40.0;
        double[] drawn = new double[4];
        for (int i = 0; i < 4; i++) {
            drawn[i] = LactationCalculator.ration(milk, 4, BOUT, MIN_RATION);
            milk -= drawn[i];
        }
        for (int i = 0; i < 4; i++) {
            assertTrue("piglet " + i + " went hungry", drawn[i] >= MIN_RATION);
        }
        assertTrue(milk >= 0.0);
    }

    @Test
    public void theLastPigletStillGetsItsFloorFromANearlyDryUdder() {
        // A quarter of 6 is 1.5, but the guaranteed ration is what actually gets handed over.
        assertEquals(MIN_RATION, LactationCalculator.ration(6.0, 4, BOUT, MIN_RATION), 0.001);
    }

    @Test
    public void anEmptyUdderGivesNothing() {
        assertEquals(0.0, LactationCalculator.ration(0.0, 4, BOUT, MIN_RATION), 0.001);
        assertFalse(LactationCalculator.canBeMilkedByPlayer(0.0, BUCKET_COST, 0, CHILD_RESERVE));
    }

    @Test
    public void aRationNeverExceedsWhatIsActuallyInTheUdder() {
        assertEquals(2.0, LactationCalculator.ration(2.0, 4, BOUT, MIN_RATION), 0.001);
    }

    @Test
    public void aFullUdderWithNoBabyGivesTheWholeBucket() {
        assertTrue(LactationCalculator.canBeMilkedByPlayer(40.0, BUCKET_COST, 0, CHILD_RESERVE));
    }

    @Test
    public void milkingCannotEatIntoWhatTheCalfNeeds() {
        // 40 milk minus the 20 held back for the calf is not enough for a 34 unit bucket.
        assertFalse(LactationCalculator.canBeMilkedByPlayer(40.0, BUCKET_COST, 1, CHILD_RESERVE));
        assertTrue(LactationCalculator.canBeMilkedByPlayer(60.0, BUCKET_COST, 1, CHILD_RESERVE));
    }

    @Test
    public void aLitterIsProtectedOncePerPiglet() {
        // Four piglets hold back 80, so a 100 unit udder cannot spare a 34 unit bucket.
        assertFalse(LactationCalculator.canBeMilkedByPlayer(100.0, BUCKET_COST, 4, CHILD_RESERVE));
        assertTrue(LactationCalculator.canBeMilkedByPlayer(120.0, BUCKET_COST, 4, CHILD_RESERVE));
    }
}
