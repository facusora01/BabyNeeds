package com.babyneeds;

/**
 * The four life stages an animal moves through. Vanilla only has baby and adult and flips
 * between them on a timer; here an animal only advances by actually eating, so the stage is
 * stored on the entity and the vanilla age field is pinned to whatever matches the stage.
 */
public enum GrowthStage {
    /** Newborn. Milk only - it cannot digest grass yet, so a motherless calf dies. */
    BABY(0, "baby", 0.62f, true),
    /** Weaned. Still nurses, but starts nibbling grass, so it can survive losing its mother. */
    WEANLING(1, "weanling", 0.85f, true),
    /** Adolescent. Adult body, adult diet, but not old enough to breed. */
    JUVENILE(2, "juvenile", 0.80f, false),
    /** Grown. Breeds, gives milk/wool, drops full loot. */
    ADULT(3, "adult", 1.0f, false);

    private final int index;
    private final String key;
    private final float renderScale;
    private final boolean vanillaBaby;

    GrowthStage(int index, String key, float renderScale, boolean vanillaBaby) {
        this.index = index;
        this.key = key;
        this.renderScale = renderScale;
        this.vanillaBaby = vanillaBaby;
    }

    public int index() {
        return index;
    }

    public String key() {
        return key;
    }

    /**
     * Extra scaling applied on top of the vanilla model. BABY and WEANLING render on the vanilla
     * baby model (already about half size), JUVENILE and ADULT on the adult one, so the four
     * stages read as a smooth ramp instead of the vanilla baby-to-adult jump.
     */
    public float renderScale() {
        return renderScale;
    }

    /** Whether the entity should report {@code isBaby()} to vanilla while in this stage. */
    public boolean isVanillaBaby() {
        return vanillaBaby;
    }

    public boolean isBefore(GrowthStage other) {
        return index < other.index;
    }

    public GrowthStage next() {
        return this == ADULT ? ADULT : values()[index + 1];
    }

    public static GrowthStage byIndex(int index) {
        if (index < 0) return BABY;
        if (index >= values().length) return ADULT;
        return values()[index];
    }
}
