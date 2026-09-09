package com.babyneeds;

/**
 * Per-species traits. Kept free of Minecraft types so the balance numbers can be unit tested;
 * {@link AnimalState} maps a live entity onto one of these.
 */
public enum Species {
    //      key       hunger  milk   lie   graze  grass
    //                rate    rate   down  radius food
    COW   ("cow",     1.00,   1.00,  false, 12,   22.0),
    SHEEP ("sheep",   0.85,   0.80,  false, 10,   20.0),
    // A sow physically cannot nurse standing up: the piglets can only reach the teats when she
    // lies down, so a litter feeds in bouts instead of whenever a piglet feels like it.
    PIG   ("pig",     1.15,   0.90,  true,  10,   18.0),
    GOAT  ("goat",    0.90,   0.75,  false, 14,   20.0);

    private final String key;
    private final double hungerRate;
    private final double milkRate;
    private final boolean nursesLyingDown;
    private final int grazeRadius;
    private final double grassFoodValue;

    Species(String key, double hungerRate, double milkRate, boolean nursesLyingDown,
            int grazeRadius, double grassFoodValue) {
        this.key = key;
        this.hungerRate = hungerRate;
        this.milkRate = milkRate;
        this.nursesLyingDown = nursesLyingDown;
        this.grazeRadius = grazeRadius;
        this.grassFoodValue = grassFoodValue;
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
