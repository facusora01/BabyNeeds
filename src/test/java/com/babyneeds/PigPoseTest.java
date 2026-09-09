package com.babyneeds;

import org.junit.Test;

import static org.junit.Assert.*;

public class PigPoseTest {

    @Test
    public void onlyFlankPosesLetAPigletNurse() {
        // A piglet cannot reach the teats of a standing or sitting sow - only of one on her side.
        assertTrue(PigPose.LYING.isOnSide());
        assertTrue(PigPose.WALLOWING.isOnSide());
        assertFalse(PigPose.SITTING.isOnSide());
        assertFalse(PigPose.STANDING.isOnSide());
    }

    @Test
    public void anythingButStandingCountsAsSettled() {
        // Settled animals do not wander off to graze, root or chase scraps.
        assertFalse(PigPose.STANDING.isResting());
        assertTrue(PigPose.SITTING.isResting());
        assertTrue(PigPose.LYING.isResting());
        assertTrue(PigPose.WALLOWING.isResting());
    }

    @Test
    public void indexRoundTripsAndClampsGarbage() {
        for (PigPose pose : PigPose.values()) {
            assertEquals(pose, PigPose.byIndex(pose.index()));
        }
        // A packet from a future version must not crash the renderer.
        assertEquals(PigPose.STANDING, PigPose.byIndex(-1));
        assertEquals(PigPose.STANDING, PigPose.byIndex(99));
    }

    @Test
    public void standingIsTheDefaultIndex() {
        assertEquals(0, PigPose.STANDING.index());
    }
}
