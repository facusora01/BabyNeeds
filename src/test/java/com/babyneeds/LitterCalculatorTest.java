package com.babyneeds;

import org.junit.Test;

import static org.junit.Assert.*;

public class LitterCalculatorTest {

    private static final double CHANCE = 0.08;
    private static final int MAX_LITTER = 4;
    private static final double EXPONENT = 3.0;

    @Test
    public void mostFarrowingsAreASinglePiglet() {
        assertEquals(0, LitterCalculator.extraBabies(0.081, CHANCE, 0.5, MAX_LITTER, EXPONENT));
        assertEquals(0, LitterCalculator.extraBabies(0.99, CHANCE, 0.99, MAX_LITTER, EXPONENT));
    }

    @Test
    public void aSowWithNoMudNeverThrowsALitterOnTheDefaultConfig() {
        assertEquals(0, LitterCalculator.extraBabies(0.0001, 0.0, 0.99, MAX_LITTER, EXPONENT));
    }

    @Test
    public void aLitterAlwaysAddsAtLeastOnePiglet() {
        assertEquals(1, LitterCalculator.extraBabies(0.01, CHANCE, 0.0, MAX_LITTER, EXPONENT));
    }

    @Test
    public void aLitterNeverExceedsTheConfiguredMaximum() {
        // Three extra on top of vanilla's one is a litter of four.
        assertEquals(MAX_LITTER - 1, LitterCalculator.extraBabies(0.01, CHANCE, 1.0, MAX_LITTER, EXPONENT));
        assertEquals(MAX_LITTER - 1, LitterCalculator.extraBabies(0.01, CHANCE, 0.9999, MAX_LITTER, EXPONENT));
    }

    @Test
    public void biggerLittersAreRarer() {
        int twins = 0;
        int triplets = 0;
        int quads = 0;
        int samples = 100000;
        for (int i = 0; i < samples; i++) {
            double sizeRoll = (double) i / samples;
            switch (LitterCalculator.extraBabies(0.0, 1.0, sizeRoll, MAX_LITTER, EXPONENT)) {
                case 1 -> twins++;
                case 2 -> triplets++;
                case 3 -> quads++;
                default -> fail("a litter must add between one and three piglets");
            }
        }
        assertTrue(twins > triplets);
        assertTrue(triplets > quads);
        // Roughly the documented 69 / 18 / 13 split.
        assertEquals(0.69, twins / (double) samples, 0.02);
        assertEquals(0.13, quads / (double) samples, 0.02);
    }

    @Test
    public void aHigherExponentPushesLittersDownToTwins() {
        int twinsAtThree = 0;
        int twinsAtEight = 0;
        int samples = 10000;
        for (int i = 0; i < samples; i++) {
            double sizeRoll = (double) i / samples;
            if (LitterCalculator.extraBabies(0.0, 1.0, sizeRoll, MAX_LITTER, 3.0) == 1) twinsAtThree++;
            if (LitterCalculator.extraBabies(0.0, 1.0, sizeRoll, MAX_LITTER, 8.0) == 1) twinsAtEight++;
        }
        assertTrue(twinsAtEight > twinsAtThree);
    }

    @Test
    public void degenerateSettingsAreHandled() {
        assertEquals(0, LitterCalculator.extraBabies(0.0, 1.0, 0.9, 1, EXPONENT));
        assertEquals(1, LitterCalculator.extraBabies(0.0, 1.0, 0.5, 2, EXPONENT));
        // A zero exponent would blow up the curve; it is treated as linear instead.
        assertTrue(LitterCalculator.extraBabies(0.0, 1.0, 0.5, MAX_LITTER, 0.0) >= 1);
    }
}
