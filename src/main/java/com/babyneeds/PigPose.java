package com.babyneeds;

/**
 * What an animal is doing with its body. The server decides the pose; the client eases into it over
 * time rather than snapping, which is the whole reason this is an enum with a sync packet instead of
 * the boolean it started as - a sow that teleports onto her side reads as a bug, not as an animal
 * lying down.
 */
public enum PigPose {
    /** On its feet, walking, eating, being a pig. */
    STANDING(0),
    /** Sat down on its haunches. Idle behaviour: nothing asked it to, it just felt like it. */
    SITTING(1),
    /** Flat on its side. A sow does this to nurse, because piglets cannot reach a standing sow. */
    LYING(2),
    /** On its side in the mud and rocking, which is what actually coats a pig in mud. */
    WALLOWING(3);

    private final int index;

    PigPose(int index) {
        this.index = index;
    }

    public int index() {
        return index;
    }

    /** Poses where the animal is flat on its flank - the ones a piglet can nurse from. */
    public boolean isOnSide() {
        return this == LYING || this == WALLOWING;
    }

    /** Anything that is not standing: the animal is settled and will not walk off. */
    public boolean isResting() {
        return this != STANDING;
    }

    public static PigPose byIndex(int index) {
        PigPose[] values = values();
        if (index < 0 || index >= values.length) return STANDING;
        return values[index];
    }
}
