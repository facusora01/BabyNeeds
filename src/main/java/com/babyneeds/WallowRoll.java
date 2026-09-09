package com.babyneeds;

import net.minecraft.util.Mth;

/**
 * A pig rolling in a wallow, the way someone on fire rolls to put themselves out: it turns over on
 * itself and travels sideways as it goes, one way and then the other.
 *
 * Turning and travelling are the same motion here, not two effects layered on each other - a body
 * rolling on the ground covers exactly its own radius for every radian it turns, and that is what
 * makes it read as rolling rather than as spinning on the spot.
 *
 * Both sides compute this from the game time the pose began, so the client's picture and the
 * server's movement agree without an angle being sent over the wire every tick.
 */
public final class WallowRoll {

    private WallowRoll() {
    }

    /** How far into the roll, in radians of the swing that carries it from one side to the other. */
    public static double phase(long gameTime, double partialTick, long poseStart, double speed) {
        double seconds = (gameTime + partialTick - poseStart) / 20.0;
        return seconds * speed;
    }

    /**
     * The roll itself, in radians. A swing rather than a spin: it rolls one way, comes back through
     * upright, and rolls the other, so a wallowing animal works the whole hollow without wandering
     * off across the field.
     */
    public static double angle(double phase, double amplitudeDegrees) {
        return Math.toRadians(amplitudeDegrees) * Mth.sin((float) phase);
    }

    /**
     * How far it has travelled sideways for that much turning. Distance is the angle times the
     * radius, which is what rolling without slipping means; a negative radius simply rolls it the
     * other way.
     */
    public static double travel(double angleRadians, double radius) {
        return angleRadians * radius;
    }
}
