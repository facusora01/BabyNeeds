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
            .comment("Milk units a mother produces per real minute, per baby nursing from her, while",
                    "she is well fed. A sow with a litter of four yields four times this, and burns",
                    "four times the nutrition making it, so a big litter needs real pasture behind it.")
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

    public static final ForgeConfigSpec.DoubleValue MIN_RATION = BUILDER
            .comment("Milk a baby is guaranteed per bout while the udder is not completely empty, even",
                    "when its siblings drank first. Sized to cover survival, so a litter sharing one",
                    "udder never loses a piglet to its brothers being faster.")
            .defineInRange("milk.minRation", 4.0, 0.0, 100.0);

    public static final ForgeConfigSpec.DoubleValue MILK_RESERVE_FOR_CHILD = BUILDER
            .comment("Milk kept out of the player's reach per nursing baby, so milking a fresh cow",
                    "cannot starve her calf - and a sow with a litter is protected four times over.")
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
            .comment("Ticks before a sow lies down again after a bout. Kept in step with the babies'",
                    "own nursing cooldown so a litter is never left waiting on the mother.")
            .defineInRange("nursing.sowLieCooldownTicks", 600, 0, 100000);

    // ---------------------------------------------------------------- pasture

    public static final ForgeConfigSpec.BooleanValue PASTURE_DEGRADATION = BUILDER
            .comment("Grazed grass blocks can degrade into coarse dirt, which vanilla grass never",
                    "spreads back onto until a player hoes it. This is what forces rotation instead",
                    "of one permanent pen.")
            .define("pasture.degradation", true);

    public static final ForgeConfigSpec.IntValue PASTURE_RECOVERY_GRACE_TICKS = BUILDER
            .comment("Ticks bare ground is safe after being grazed, no matter how much it is walked",
                    "on. This is the window for grass to creep back if the herd moves on, so a",
                    "rotated pasture never loses soil at all (24000 = one full in-game day).")
            .defineInRange("pasture.recoveryGraceTicks", 24000, 0, 10000000);

    public static final ForgeConfigSpec.IntValue PASTURE_TRAMPLE_THRESHOLD = BUILDER
            .comment("Times an animal must be recorded standing on the same bare block (once per",
                    "second per animal) before that soil can be lost. 60 = a full minute of an animal",
                    "parked on the same spot, or four animals crossing it repeatedly.")
            .defineInRange("pasture.trampleThreshold", 60, 1, 100000);

    public static final ForgeConfigSpec.DoubleValue PASTURE_COMPACT_MAX_CHANCE = BUILDER
            .comment("Chance per recorded trample that badly overworked bare ground turns to coarse",
                    "dirt, once the grace period is over. Scaled down by any grass still left around",
                    "the spot, so damage concentrates where the herd actually crowds.")
            .defineInRange("pasture.compactMaxChance", 0.02, 0.0, 1.0);

    public static final ForgeConfigSpec.IntValue PASTURE_SCAN_RADIUS = BUILDER
            .comment("Radius in blocks used to judge how healthy the pasture around a grazed spot is.")
            .defineInRange("pasture.scanRadius", 3, 1, 8);

    public static final ForgeConfigSpec.IntValue GRAZE_CHEW_TICKS = BUILDER
            .comment("Ticks an animal spends head-down on a block before the mouthful counts.")
            .defineInRange("pasture.grazeChewTicks", 40, 1, 6000);

    // ---------------------------------------------------------------- pigs and mud

    public static final ForgeConfigSpec.DoubleValue ROOT_FOOD_VALUE = BUILDER
            .comment("Nutrition a pig gets from turning over a fresh patch of dry ground. Well below",
                    "a mouthful of grass, because rooting is a living, not a feast: a pig pen keeps",
                    "fewer animals than a pasture the same size unless the farmer brings crops.")
            .defineInRange("pig.rootFoodValue", 9.0, 0.0, 100.0);

    public static final ForgeConfigSpec.DoubleValue MUD_ROOT_MULTIPLIER = BUILDER
            .comment("How much richer mud is to root than dry ground. 1.6 = a wallow feeds 60% better,",
                    "which is the practical reason to build one, on top of the litters it brings.")
            .defineInRange("pig.mudRootMultiplier", 1.6, 0.0, 10.0);

    public static final ForgeConfigSpec.IntValue MAX_ROOTS_PER_BLOCK = BUILDER
            .comment("How many times a patch can be worked over before it is exhausted. Each root",
                    "yields less than the last, so the third is worth a third of the first.")
            .defineInRange("pig.maxRootsPerBlock", 3, 1, 100);

    public static final ForgeConfigSpec.IntValue ROOT_RECOVERY_TICKS = BUILDER
            .comment("Ticks before an exhausted patch is worth rooting again (24000 = one in-game day).")
            .defineInRange("pig.rootRecoveryTicks", 24000, 20, 10000000);

    public static final ForgeConfigSpec.IntValue ROOT_TICKS = BUILDER
            .comment("Ticks a pig spends nose-down before a mouthful counts.")
            .defineInRange("pig.rootTicks", 50, 1, 6000);

    public static final ForgeConfigSpec.BooleanValue PIGS_EAT_CROPS = BUILDER
            .comment("Pigs eat ripe crops they can reach. Leave the pen door open at your own risk.",
                    "Only mature crops are taken, and they still have to path to them.")
            .define("pig.eatsCrops", true);

    public static final ForgeConfigSpec.DoubleValue CROP_FOOD_VALUE = BUILDER
            .comment("Nutrition from one ripe crop - the best food a pig can find on its own.")
            .defineInRange("pig.cropFoodValue", 28.0, 0.0, 100.0);

    public static final ForgeConfigSpec.BooleanValue PIGS_EAT_SCRAPS = BUILDER
            .comment("Pigs go after food dropped on the ground. This is how a player feeds a herd",
                    "without hand-feeding each animal: throw the harvest over the fence.")
            .define("pig.eatsScraps", true);

    public static final ForgeConfigSpec.DoubleValue SCRAP_NUTRITION_PER_POINT = BUILDER
            .comment("Nutrition per hunger point of food eaten off the ground, so a loaf of bread is",
                    "worth several potatoes and a rotten scrap is worth almost nothing.")
            .defineInRange("pig.scrapNutritionPerPoint", 3.0, 0.0, 50.0);

    public static final ForgeConfigSpec.DoubleValue SCAVENGE_RADIUS = BUILDER
            .comment("How far a pig will go for food lying on the ground.")
            .defineInRange("pig.scavengeRadius", 12.0, 1.0, 64.0);

    public static final ForgeConfigSpec.IntValue SILTING_TICKS = BUILDER
            .comment("Ticks for clean water with a single block of mud against it to go brown. Each",
                    "extra mud face divides this, so water boxed in by mud on four sides silts up",
                    "four times faster - fast, but never instant: a wallow that appears the moment",
                    "you pour the bucket reads as a block swap rather than as something settling.",
                    "1200 = one real minute with one muddy bank, fifteen seconds when walled in.")
            .defineInRange("pig.siltingTicks", 1200, 20, 10000000);

    public static final ForgeConfigSpec.IntValue CLEARING_TICKS = BUILDER
            .comment("Ticks for muddy water with no mud and no muddy water left against it to settle",
                    "clear again. Water in the middle of a wallow is held by the pool around it, so",
                    "this only ever reaches blocks that have been genuinely cut off.")
            .defineInRange("pig.clearingTicks", 2400, 20, 10000000);

    public static final ForgeConfigSpec.IntValue SOIL_TO_MUD_TICKS = BUILDER
            .comment("Ticks for dirt, grass or coarse dirt under muddy water to soak through and",
                    "become mud. This is how a wallow beds itself in: every block it converts makes",
                    "the water above silt faster, so a puddle in a dirt hollow turns into a proper",
                    "wallow on its own. Two full in-game days by default: a wallow is something",
                    "a farm grows into, not something that appears over a lunch break.")
            .defineInRange("pig.soilToMudTicks", 48000, 20, 10000000);

    public static final ForgeConfigSpec.BooleanValue MUDDY_WATER = BUILDER
            .comment("Water dug into mud silts up and turns brown, and runs clear again if the mud",
                    "around it is removed. Purely how a wallow looks - a pig gets the same benefit",
                    "from plain mud either way.")
            .define("pig.muddyWater", true);

    public static final ForgeConfigSpec.IntValue SIT_CHANCE = BUILDER
            .comment("One in this many chances per tick that a contented pig sits down on its own.",
                    "900 is roughly once every 45 seconds of standing around with nothing to do.")
            .defineInRange("pig.sitChance", 900, 1, 100000);

    public static final ForgeConfigSpec.IntValue SIT_TICKS = BUILDER
            .comment("Base ticks a pig stays sat down; the real duration is randomised around it.")
            .defineInRange("pig.sitTicks", 200, 20, 100000);

    public static final ForgeConfigSpec.IntValue WALLOW_CHANCE = BUILDER
            .comment("One in this many chances per tick that a pig standing in mud lies over and",
                    "starts rolling. Much likelier than sitting: that is what mud is for.")
            .defineInRange("pig.wallowChance", 200, 1, 100000);

    public static final ForgeConfigSpec.IntValue WALLOW_TICKS = BUILDER
            .comment("Ticks a wallowing pig spends rocking in the mud before getting up.")
            .defineInRange("pig.wallowTicks", 160, 20, 100000);

    public static final ForgeConfigSpec.DoubleValue WALLOW_MUD_PER_TICK = BUILDER
            .comment("Coating picked up per tick of rolling, where 1.0 is caked head to trotter.",
                    "0.012 covers a pig in about four seconds of proper wallowing.")
            .defineInRange("pig.wallowMudPerTick", 0.012, 0.0, 1.0);

    public static final ForgeConfigSpec.DoubleValue MUD_DRIES_PER_MINUTE = BUILDER
            .comment("How much of a mud coating dries off per real minute. Standing in clean water",
                    "scrubs it off four times faster.")
            .defineInRange("pig.mudDriesPerMinute", 0.3, 0.0, 10.0);

    public static final ForgeConfigSpec.DoubleValue MUDDY_WATER_DRAG = BUILDER
            .comment("How much muddy water slows whatever wades through it, as a fraction of normal",
                    "speed (0.65 = a third slower). Mud is thick and crossing a wallow should cost",
                    "something. 1.0 turns the drag off and leaves it behaving exactly like water.")
            .defineInRange("pig.muddyWaterDrag", 0.65, 0.1, 1.0);

    public static final ForgeConfigSpec.DoubleValue MUD_LITTER_CHANCE = BUILDER
            .comment("Chance that a sow with access to a mud wallow farrows a litter instead of a",
                    "single piglet. 0.08 = 8%. This is the payoff for building a pig pen with mud in",
                    "it: not fatter pigs, but more of them.")
            .defineInRange("pig.mudLitterChance", 0.08, 0.0, 1.0);

    public static final ForgeConfigSpec.DoubleValue DRY_LITTER_CHANCE = BUILDER
            .comment("Same chance for a sow with no mud anywhere near her. Zero by default, so mud is",
                    "what makes the difference.")
            .defineInRange("pig.dryLitterChance", 0.0, 0.0, 1.0);

    public static final ForgeConfigSpec.IntValue MAX_LITTER_SIZE = BUILDER
            .comment("Largest possible litter, counting the piglet vanilla already made.")
            .defineInRange("pig.maxLitterSize", 4, 1, 16);

    public static final ForgeConfigSpec.DoubleValue LITTER_RARITY_EXPONENT = BUILDER
            .comment("How much rarer each extra piglet is. Higher = big litters rarer. At 3.0 with a",
                    "max of 4, a litter that happens is about 69% twins, 18% triplets, 13% quads.")
            .defineInRange("pig.litterRarityExponent", 3.0, 1.0, 10.0);

    // ---------------------------------------------------------------- carcass condition

    public static final ForgeConfigSpec.BooleanValue CONDITION_AFFECTS_DROPS = BUILDER
            .comment("Scale what an animal drops by the condition it died in. A starved carcass is",
                    "skin and bone and cannot yield the meat and hide of a well fed one.")
            .define("loot.conditionAffectsDrops", true);

    public static final ForgeConfigSpec.DoubleValue STARVED_DROP_MULTIPLIER = BUILDER
            .comment("What the worst carcass yields, as a fraction of normal. 0.25 = a quarter.",
                    "An animal that starved to death always lands exactly here. Fractions are settled",
                    "by a roll, so a single hide at 0.25 is a 25% chance of a hide, not a guaranteed",
                    "nothing.")
            .defineInRange("loot.starvedDropMultiplier", 0.25, 0.0, 1.0);

    public static final ForgeConfigSpec.DoubleValue FULL_CONDITION_NUTRITION = BUILDER
            .comment("Nutrition at which a carcass is in full condition and drops normally. Below it",
                    "the yield slides towards starvedDropMultiplier, so butchering a hungry herd is",
                    "already a loss before anything actually dies of hunger.")
            .defineInRange("loot.fullConditionNutrition", 50.0, 0.0, 100.0);

    public static final ForgeConfigSpec.DoubleValue JUVENILE_YIELD = BUILDER
            .comment("What an adolescent carcass yields against a full grown one. 0.45 = a bit under",
                    "half. An adolescent is a smaller animal; without this it dropped exactly as much",
                    "as an adult and the optimal play was to slaughter everything one stage early.",
                    "This multiplies with condition, so a half-grown animal that starved is the worst",
                    "of both. Babies and weanlings drop nothing at all (vanilla's own rule).")
            .defineInRange("loot.juvenileYield", 0.45, 0.0, 1.0);

    // ---------------------------------------------------------------- motherhood

    public static final ForgeConfigSpec.BooleanValue ALLOW_ADOPTION = BUILDER
            .comment("An orphan that trails after a grown animal of its own kind may be taken on by",
                    "it. A foster mother comes into milk for it despite never having given birth.")
            .define("motherhood.allowAdoption", true);

    public static final ForgeConfigSpec.DoubleValue ADOPTION_CHANCE = BUILDER
            .comment("Chance that a given adult accepts a given orphan. The answer stands for a while",
                    "either way, so an orphan cannot re-roll the same cow once a second.")
            .defineInRange("motherhood.adoptionChance", 0.5, 0.0, 1.0);

    public static final ForgeConfigSpec.IntValue ADOPTION_RETRY_TICKS = BUILDER
            .comment("Ticks before a refused orphan tries its luck again (6000 = five real minutes).")
            .defineInRange("motherhood.adoptionRetryTicks", 6000, 20, 1000000);

    public static final ForgeConfigSpec.DoubleValue REJECTION_DAMAGE = BUILDER
            .comment("Damage from the headbutt an adult gives an orphan it will not take on. Meant to",
                    "teach rather than to kill.")
            .defineInRange("motherhood.rejectionDamage", 1.0, 0.0, 20.0);

    public static final ForgeConfigSpec.DoubleValue REJECTION_KNOCKBACK = BUILDER
            .comment("How hard that headbutt sends the orphan away.")
            .defineInRange("motherhood.rejectionKnockback", 0.6, 0.0, 5.0);

    public static final ForgeConfigSpec.IntValue HEADBUTT_DIP_TICKS = BUILDER
            .comment("Ticks the adult holds its head down for the butt. Short: the whole gesture is a",
                    "dip and a snap back up.")
            .defineInRange("motherhood.headbuttDipTicks", 8, 1, 200);

    public static final ForgeConfigSpec.BooleanValue MOTHERS_DEFEND_BABIES = BUILDER
            .comment("A mother goes after whoever hurts her baby. She is a farm animal, so this is a",
                    "shove rather than a real threat, but she will not stand and watch.")
            .define("motherhood.defendBabies", true);

    public static final ForgeConfigSpec.DoubleValue DEFEND_RADIUS = BUILDER
            .comment("How far a mother will carry a grudge before she gives up and goes back to grazing.")
            .defineInRange("motherhood.defendRadius", 16.0, 1.0, 64.0);

    public static final ForgeConfigSpec.IntValue DEFEND_TICKS = BUILDER
            .comment("Ticks she stays angry after the last time her baby was hurt (200 = ten seconds).")
            .defineInRange("motherhood.defendTicks", 200, 20, 100000);

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
