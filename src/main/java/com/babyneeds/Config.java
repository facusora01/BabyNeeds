package com.babyneeds;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Every balance number the mod uses. The defaults are tuned for a pasture that comfortably feeds
 * roughly one animal per 25 surface blocks of grass; pack them tighter than that and the pen
 * degrades to coarse dirt, the mothers dry up, and the babies stop growing.
 */
public class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    // ---------------------------------------------------------------- growth

    public static final ForgeConfigSpec.DoubleValue GROWTH_SPEED = BUILDER
            .comment("Global growth speed multiplier. 1.0 = default (a well fed calf reaches adult",
                    "in roughly two and a half in-game days of steady nursing and grazing).",
                    "2.0 = twice as fast, 0.5 = twice as slow.")
            .defineInRange("growth.growthSpeed", 1.0, 0.1, 20.0);

    public static final ForgeConfigSpec.DoubleValue BASE_STAGE_INTAKE = BUILDER
            .comment("Food units a BABY must drink to leave its first stage. Later stages need",
                    "1.6x and 2.2x this amount, because a bigger animal eats more to grow.")
            .defineInRange("growth.baseStageIntake", 60.0, 1.0, 10000.0);

    public static final ForgeConfigSpec.IntValue BASE_STAGE_TICKS = BUILDER
            .comment("Minimum ticks a BABY spends in its stage even with unlimited food (6000 = 5",
                    "real minutes). Later stages use 1.5x and 2.0x. This is what stops a player from",
                    "bucket-feeding a newborn straight to adult.")
            .defineInRange("growth.baseStageTicks", 6000, 20, 1000000);

    // ---------------------------------------------------------------- hunger

    public static final ForgeConfigSpec.DoubleValue HUNGER_DECAY_PER_MINUTE = BUILDER
            .comment("Nutrition (0-100) lost per real minute. 1.2 = a full animal empties in about",
                    "83 minutes, so it must find grass a few times per in-game day.")
            .defineInRange("hunger.decayPerMinute", 1.2, 0.0, 100.0);

    public static final ForgeConfigSpec.DoubleValue GRAZE_THRESHOLD = BUILDER
            .comment("Nutrition below which an animal actively goes looking for grass.")
            .defineInRange("hunger.grazeThreshold", 70.0, 0.0, 100.0);

    public static final ForgeConfigSpec.IntValue STARVATION_GRACE_TICKS = BUILDER
            .comment("Ticks an animal survives at zero nutrition before starvation damage starts.",
                    "6000 = 5 real minutes of grace after it has already run completely empty.")
            .defineInRange("hunger.starvationGraceTicks", 6000, 0, 1000000);

    public static final ForgeConfigSpec.IntValue STARVATION_DAMAGE_INTERVAL = BUILDER
            .comment("Ticks between starvation damage ticks once starving. Rounded to a multiple of 20.")
            .defineInRange("hunger.starvationDamageIntervalTicks", 200, 20, 100000);

    public static final ForgeConfigSpec.DoubleValue STARVATION_DAMAGE = BUILDER
            .comment("Damage per starvation tick.")
            .defineInRange("hunger.starvationDamage", 1.0, 0.0, 100.0);

    public static final ForgeConfigSpec.DoubleValue FEED_ITEM_NUTRITION = BUILDER
            .comment("Nutrition restored when a player hand-feeds a breeding item (wheat, carrot...).",
                    "Hand feeding is meant as a rescue, not as a replacement for pasture.")
            .defineInRange("hunger.feedItemNutrition", 25.0, 0.0, 100.0);

    // ---------------------------------------------------------------- lactation

    public static final ForgeConfigSpec.DoubleValue MILK_PER_MINUTE = BUILDER
            .comment("Milk units a mother produces per real minute while she is well fed.")
            .defineInRange("milk.milkPerMinute", 7.0, 0.0, 100.0);

    public static final ForgeConfigSpec.DoubleValue LACTATION_MIN_NUTRITION = BUILDER
            .comment("A mother below this nutrition makes no milk at all. This is the link between",
                    "pasture and calves: a hungry mother's babies starve with her.")
            .defineInRange("milk.minNutrition", 35.0, 0.0, 100.0);

    public static final ForgeConfigSpec.DoubleValue MILK_NUTRITION_COST = BUILDER
            .comment("Nutrition burnt per unit of milk produced. A nursing mother eats noticeably more.")
            .defineInRange("milk.nutritionCost", 0.8, 0.0, 10.0);

    public static final ForgeConfigSpec.DoubleValue SUCKLE_AMOUNT = BUILDER
            .comment("Milk drawn in one nursing bout, which is also the growth intake the baby gains.")
            .defineInRange("milk.suckleAmount", 12.0, 0.1, 100.0);

    public static final ForgeConfigSpec.IntValue SUCKLE_DURATION_TICKS = BUILDER
            .comment("Ticks the baby must stay latched onto its mother to finish a bout.")
            .defineInRange("milk.suckleDurationTicks", 60, 1, 6000);

    public static final ForgeConfigSpec.IntValue SUCKLE_COOLDOWN_TICKS = BUILDER
            .comment("Ticks before the same baby nurses again (600 = 30 seconds).")
            .defineInRange("milk.suckleCooldownTicks", 600, 0, 100000);

    public static final ForgeConfigSpec.DoubleValue MILK_NUTRITION_VALUE = BUILDER
            .comment("Nutrition a baby gains per unit of milk. Milk is its only food while BABY.")
            .defineInRange("milk.nutritionValue", 2.5, 0.0, 100.0);

    public static final ForgeConfigSpec.DoubleValue PLAYER_BUCKET_MILK_COST = BUILDER
            .comment("Milk a player's bucket takes out of the udder. An empty udder gives no bucket.")
            .defineInRange("milk.playerBucketCost", 34.0, 0.0, 100.0);

    public static final ForgeConfigSpec.DoubleValue MILK_RESERVE_FOR_CHILD = BUILDER
            .comment("Milk kept out of the player's reach while the mother still has a nursing baby,",
                    "so milking a fresh cow cannot starve her calf outright.")
            .defineInRange("milk.reserveForChild", 20.0, 0.0, 100.0);

    public static final ForgeConfigSpec.IntValue LACTATION_DURATION_TICKS = BUILDER
            .comment("Ticks a mother keeps lactating with no demand before she dries up. Every nursing",
                    "bout and every bucket a player takes refreshes it, so a milked or nursed cow stays",
                    "in milk and one whose calf died stops producing (96000 = 80 real minutes).")
            .defineInRange("milk.lactationDurationTicks", 96000, 20, 10000000);

    public static final ForgeConfigSpec.BooleanValue ALLOW_BOTTLE_FEEDING = BUILDER
            .comment("Allow a player to feed a milk bucket to a BABY or WEANLING as a substitute for",
                    "its mother. Turn off for a harder run where an orphan is simply lost.")
            .define("milk.allowBottleFeeding", true);

    // ---------------------------------------------------------------- nursing behaviour

    public static final ForgeConfigSpec.DoubleValue NURSE_SEARCH_RADIUS = BUILDER
            .comment("How far a baby will walk to find its own mother. It never nurses from another",
                    "female, so a calf separated from its mother by more than this dies.")
            .defineInRange("nursing.searchRadius", 24.0, 1.0, 128.0);

    public static final ForgeConfigSpec.IntValue SOW_LIE_DOWN_TICKS = BUILDER
            .comment("Ticks a sow stays lying down for a nursing bout. Pigs cannot nurse standing up,",
                    "so a litter only feeds when the mother settles.")
            .defineInRange("nursing.sowLieDownTicks", 400, 20, 100000);

    public static final ForgeConfigSpec.IntValue SOW_LIE_COOLDOWN_TICKS = BUILDER
            .comment("Ticks before a sow lies down again after a bout.")
            .defineInRange("nursing.sowLieCooldownTicks", 1200, 0, 100000);

    // ---------------------------------------------------------------- pasture

    public static final ForgeConfigSpec.BooleanValue PASTURE_DEGRADATION = BUILDER
            .comment("Grazed grass blocks can degrade into coarse dirt, which vanilla grass never",
                    "spreads back onto until a player hoes it. This is what forces rotation instead",
                    "of one permanent pen.")
            .define("pasture.degradation", true);

    public static final ForgeConfigSpec.DoubleValue COARSE_BASE_CHANCE = BUILDER
            .comment("Chance a grazed block degrades to coarse dirt in a lush pasture.")
            .defineInRange("pasture.coarseBaseChance", 0.10, 0.0, 1.0);

    public static final ForgeConfigSpec.DoubleValue COARSE_MAX_CHANCE = BUILDER
            .comment("Chance a grazed block degrades to coarse dirt when everything around it has",
                    "already been eaten down. Overstocked pens hit this number constantly.")
            .defineInRange("pasture.coarseMaxChance", 0.60, 0.0, 1.0);

    public static final ForgeConfigSpec.IntValue PASTURE_SCAN_RADIUS = BUILDER
            .comment("Radius in blocks used to judge how healthy the pasture around a grazed spot is.")
            .defineInRange("pasture.scanRadius", 3, 1, 8);

    public static final ForgeConfigSpec.IntValue GRAZE_CHEW_TICKS = BUILDER
            .comment("Ticks an animal spends head-down on a block before the mouthful counts.")
            .defineInRange("pasture.grazeChewTicks", 40, 1, 6000);

    // ---------------------------------------------------------------- pigs and mud

    public static final ForgeConfigSpec.DoubleValue MUD_HUNGER_REDUCTION = BUILDER
            .comment("Fraction of hunger decay a pig saves while standing in mud (0.35 = 35% slower).",
                    "Wallowing is how a real pig regulates temperature instead of burning feed.")
            .defineInRange("pig.mudHungerReduction", 0.35, 0.0, 1.0);

    public static final ForgeConfigSpec.DoubleValue MUD_MILK_BONUS = BUILDER
            .comment("Extra milk production for a sow with access to mud (0.25 = +25%).")
            .defineInRange("pig.mudMilkBonus", 0.25, 0.0, 5.0);

    // ---------------------------------------------------------------- misc

    public static final ForgeConfigSpec.BooleanValue SHOW_STATUS_ON_SNEAK = BUILDER
            .comment("Sneak and right-click an animal with an empty hand to read its stage, nutrition",
                    "and milk in chat.")
            .define("misc.showStatusOnSneak", true);

    public static final ForgeConfigSpec.BooleanValue RESTRICT_PRODUCE_TO_ADULTS = BUILDER
            .comment("Block milking and shearing of animals that are not ADULT yet.")
            .define("misc.restrictProduceToAdults", true);

    public static final ForgeConfigSpec.BooleanValue UNINSTALL_MODE = BUILDER
            .comment("Set to true before removing the mod: BabyNeeds stops applying its systems and",
                    "strips its data from every animal as its chunk loads, returning them to vanilla",
                    "ages. Let the world run, visit your farms, then remove the jar.")
            .define("misc.uninstallMode", false);

    static final ForgeConfigSpec SPEC = BUILDER.build();
}
