package com.babyneeds;

/**
 * Which flank an animal goes down on.
 *
 * A sow does not have a good side, so pinning every pig in the world to the same one reads as a
 * rig rather than as an animal. But the choice has to be made twice - the model poses the legs and
 * the renderer rolls the body - and the two have to agree, or she lands on one flank with the other
 * one's legs.
 *
 * So it is not stored and not sent: it is derived from the tick the pose began on, which is already
 * synced for the wallow roll. Same input on both sides, same answer, no packet, and a fresh draw
 * every time she lies down.
 */
public final class LyingSide {

    private LyingSide() {
    }

    /**
     * True when she is down on her right flank, leaving her left side facing the sky - which is the
     * pair of legs that has nothing holding it up.
     *
     * The bit is taken from the middle of the timestamp rather than the bottom of it: game time
     * advances one tick at a time, so the lowest bit would simply alternate with the tick she
     * happened to settle on, and neighbouring pigs would flip sides in lockstep.
     */
    public static boolean leftOnTop(long poseStart) {
        long mixed = poseStart * 0x9E3779B97F4A7C15L;
        return ((mixed >>> 33) & 1L) == 0L;
    }

    /** The roll, signed for the flank she is going down on. */
    public static float roll(float angle, long poseStart) {
        return leftOnTop(poseStart) ? angle : -angle;
    }
}
