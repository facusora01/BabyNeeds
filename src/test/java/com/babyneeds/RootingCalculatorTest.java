package com.babyneeds;

import org.junit.Test;

import static org.junit.Assert.*;

public class RootingCalculatorTest {

    private static final double BASE = 9.0;
    private static final double MUD = 1.6;
    private static final int MAX_ROOTS = 3;

    @Test
    public void aFreshPatchPaysInFull() {
        assertEquals(BASE, RootingCalculator.yield(BASE, MUD, false, 0, MAX_ROOTS), 0.001);
    }

    @Test
    public void mudIsRicherThanDryGround() {
        // The practical reason to build a wallow, on top of the litters it brings.
        assertEquals(BASE * MUD, RootingCalculator.yield(BASE, MUD, true, 0, MAX_ROOTS), 0.001);
    }

    @Test
    public void eachRootPaysLessThanTheLast() {
        double first = RootingCalculator.yield(BASE, MUD, false, 0, MAX_ROOTS);
        double second = RootingCalculator.yield(BASE, MUD, false, 1, MAX_ROOTS);
        double third = RootingCalculator.yield(BASE, MUD, false, 2, MAX_ROOTS);
        assertTrue(first > second);
        assertTrue(second > third);
        assertTrue(third > 0.0);
    }

    @Test
    public void anExhaustedPatchPaysNothing() {
        // Without this, one square of dirt would feed a herd of pigs forever.
        assertEquals(0.0, RootingCalculator.yield(BASE, MUD, false, MAX_ROOTS, MAX_ROOTS), 0.001);
        assertEquals(0.0, RootingCalculator.yield(BASE, MUD, true, MAX_ROOTS + 5, MAX_ROOTS), 0.001);
    }

    @Test
    public void rootingIsThinnerThanGrazing() {
        // A pig pen supports fewer animals than a pasture unless the farmer brings crops.
        double rooted = RootingCalculator.yield(BASE, MUD, false, 0, MAX_ROOTS);
        assertTrue(rooted < Species.COW.grassFoodValue());
    }

    @Test
    public void evenARichWallowRunsOut() {
        double mudTotal = 0.0;
        for (int i = 0; i < MAX_ROOTS; i++) {
            mudTotal += RootingCalculator.yield(BASE, MUD, true, i, MAX_ROOTS);
        }
        // Three roots of mud is a meal, not an endless trough.
        assertEquals(BASE * MUD * 2.0, mudTotal, 0.001);
        assertEquals(0.0, RootingCalculator.yield(BASE, MUD, true, MAX_ROOTS, MAX_ROOTS), 0.001);
    }

    @Test
    public void degenerateSettingsAreHandled() {
        assertEquals(0.0, RootingCalculator.yield(BASE, MUD, false, 0, 0), 0.001);
        assertEquals(BASE, RootingCalculator.yield(BASE, MUD, false, -3, MAX_ROOTS), 0.001);
        assertEquals(0.0, RootingCalculator.yield(-5.0, MUD, false, 0, MAX_ROOTS), 0.001);
    }
}
