package com.babyneeds;

/**
 * Per-species traits. Kept free of Minecraft types so the balance numbers can be unit tested;
 * {@link AnimalState} maps a live entity onto one of these.
 */
public enum Species {
    //      key       hunger  milk   lie   graze  grass  omni
    //                rate    rate   down  radius food   vore
    COW   ("cow",     1.00,   1.00,  false, 12,   22.0,  false),
    SHEEP ("sheep",   0.85,   0.80,  false, 10,   20.0,  false),
    // A sow physically cannot nurse standing up: the piglets can only reach the teats when she
    // lies down, so a litter feeds in bouts instead of whenever a piglet feels like it.
    PIG   ("pig",     1.15,   0.90,  true,  10,   18.0,  true),
    GOAT  ("goat",    0.90,   0.75,  false, 14,   20.0,  false);

    private final String key;
    private final double hungerRate;
    private final double milkRate;
    private final boolean nursesLyingDown;
    private final int grazeRadius;
    private final double grassFoodValue;
    private final boolean omnivore;

    Species(String key, double hungerRate, double milkRate, boolean nursesLyingDown,
            int grazeRadius, double grassFoodValue, boolean omnivore) {
        this.key = key;
        this.hungerRate = hungerRate;
        this.milkRate = milkRate;
        this.nursesLyingDown = nursesLyingDown;
        this.grazeRadius = grazeRadius;
        this.grassFoodValue = grassFoodValue;
        this.omnivore = omnivore;
    }

    /**
     * Whether this animal eats anything edible or only what vanilla feeds it. The pig is the mod's
     * one omnivore - it roots, it raids the vegetable patch and it takes scraps off the ground -
     * and a cow is not a pig: a herbivore keeps vanilla's diet exactly, wheat and nothing else.
     */
    public boolean omnivore() {
        return omnivore;
    }

    public String key() {
        return key;
    }

    /** Multiplier on the configured hunger decay. Pigs burn through food fastest. */
    public double hungerRate() {
        return hungerRate;
    }

    /** Multiplier on the configured milk production rate. */
    public double milkRate() {
        return milkRate;
    }

    public boolean nursesLyingDown() {
        return nursesLyingDown;
    }

    public int grazeRadius() {
        return grazeRadius;
    }

    public double grassFoodValue() {
        return grassFoodValue;
    }

    public static Species byKey(String key) {
        for (Species s : values()) {
            if (s.key.equals(key)) return s;
        }
        return COW;
    }
}
