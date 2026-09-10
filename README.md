# BabyNeeds

Minecraft Forge mod (1.20.1) that replaces the vanilla "baby turns into an adult on a timer" rule
with the real thing: a newborn depends on its mother, its mother depends on the pasture, and the
pasture depends on how many animals you crammed into it.

Supported species in this version: **cow (and mooshroom), sheep, pig, goat**.

## The idea

Vanilla growth is a countdown that runs whether or not the animal ever eats. BabyNeeds makes
growth a consequence of food, and food a consequence of land:

```
grass  ->  mother's nutrition  ->  milk  ->  the baby grows
  ^                                            |
  |                                            v
overgrazing degrades the pasture        no milk = it stops growing, then starves
```

## Four life stages

| Stage | Diet | Body | Breeds |
|---|---|---|---|
| **Baby** | milk only | baby model, extra small | no |
| **Weanling** | milk + grass | baby model, near full baby size | no |
| **Juvenile** | grass | adult model at 80% | no |
| **Adult** | grass | full size | yes |

A stage is only left once the animal has eaten the intake that stage requires **and** spent a
minimum time in it. An underfed calf simply stays a calf: there is no timer that saves it.

The vanilla age field is not storage here, it is an output. Baby stages pin a negative age (so
vanilla renders and treats the animal as a baby), a juvenile is pinned to a positive age (vanilla's
own breeding cooldown, which conveniently means "grown but not breeding"), and only adults are left
alone so normal breeding cooldowns still work.

## Nursing

- The **baby walks to its mother**, latches on, and only then gets fed. She never goes to it.
- It nurses from **its own mother only** - the parent that bore it, recorded at birth. A calf
  separated from her by more than the search radius will not find another cow to drink from.
- A **sow must lie down**. Piglets cannot reach the teats of a standing pig, so a litter feeds in
  bouts, when the mother settles, and only if she has milk to give.
- Milk is manufactured out of the mother's own nutrition. Below the lactation threshold she makes
  none at all, and a mother with no demand (no nursing baby, no one milking her) dries up.
- **A whole litter feeds off one udder.** Yield and capacity scale with the number of babies
  nursing from her, so a sow with four piglets makes four times what a cow with one calf does, and
  no bout may take more than its even share of what is in there - with a guaranteed floor per bout
  on top, so the last piglet to reach her is never the one that starves. The cost is hers: four
  piglets means four times the nutrition burnt, so a big litter needs real pasture behind it.
- Players share the udder: a bucket takes real milk out of it, and milk is held back while a baby
  still nurses, so you cannot milk a fresh cow dry and starve her calf.
- Bottle feeding a milk bucket to an orphan is allowed by default (`milk.allowBottleFeeding`).

## Pasture and rotation

Animals graze for themselves. What they walk to follows two rules: **whatever is under their feet
first** - it needs no pathfinding and cannot fail, so a crowded pen keeps feeding its herd until the
pen itself is bare - and otherwise the nearest grass they can actually *reach*, verified with the
navigator's own A* before it is chosen. An animal never fixates on grass beyond the fence, or on a
tuft its herdmates are stood on, and starve staring at it.

Every mouthful strips the sward: whether they crop the turf
directly or pull up a tuft of loose grass, the block underneath is left as **bare dirt** and its
recovery clock starts. A herd standing in a pen visibly eats it down to soil.

Grazing never ruins soil on its own. Ground is only lost when it stays bare well past the recovery
window (a full in-game day by default) **and** keeps being trampled by the herd standing on it.
Only then can it compact into **coarse dirt**, which vanilla grass never spreads back onto until a
player hoes it.

That split is the whole design: a pasture the animals leave alone for a while always grows back, so
rotating a herd - or simply giving it room to wander - costs you nothing. A pen the animals never
leave gets worked past the grace period every day, turns to coarse dirt, stops feeding them, the
mothers dry up, and the babies go with them.

Each level keeps a small record of its bare ground (when the turf came off, how much it has been
walked on), pruned automatically and dropped the moment grass grows back over a block.

## Pigs

Pigs are the one species that does not live off pasture, because in a wallow pen no grass grows at
all. They feed themselves three other ways, in the order they prefer:

1. **Scraps on the ground.** Anything edible dropped near them, worth its own food value - a thrown
   loaf of bread is several potatoes. This is how you feed a herd without hand-feeding each animal.
2. **Ripe crops they can reach.** Only mature ones, and they still have to path to them, so a fenced
   garden is safe. An open pen door is not.
3. **Rooting.** Nose down, turning the soil over for roots and grubs. It works on dirt, coarse dirt,
   podzol and mud, and it is a living rather than a feast: each patch pays less every time it is
   worked over and is exhausted until it recovers. Rooted-over pasture comes up as bare dirt - a pig
   ploughs the field it feeds on, so a pig pen should not be your good grass.

The upshot is that a pig pen supports fewer animals than a pasture of the same size unless the
farmer brings crops, which is exactly how pigs are actually kept.

**The wallow.** Mud is not just decoration for them. It is **richer to root** than dry ground, and a
sow with access to one sometimes farrows a **litter of up to four** instead of a single piglet -
about 8% of births, and the bigger the litter the rarer it is (roughly 69% twins, 18% triplets, 13%
quadruplets). Every extra piglet is bound to her and competes for the same udder, though her yield
scales with the litter so none of them goes hungry for it. Without mud anywhere near her, a sow
throws one piglet like everything else.

**Water near mud turns brown**, and it is still ordinary `Blocks.WATER` while it does. Only the
colour changes: the tint is read per block from how much mud is around that spot, blended against the
biome's own water colour, so a pool shades from its banks inwards instead of meeting clean water at a
hard line. Put your head under and the fog goes brown and closes in, thicker the deeper into the
wallow you are.

It was its own fluid once, which is the obvious way to do it and the wrong one. Two distinct fluids
never merge: vanilla decides whether a neighbour counts towards forming a source with `Fluid.isSame`,
and `WaterFluid.isSame` compares by hand against `Fluids.WATER` and `Fluids.FLOWING_WATER`, so clean
water can never count a muddy neighbour whatever a mod does. Two clean sources make a source between
them, two muddy ones do, **one of each never does** - and the surface mesh has the same rule, which is
where the seam at the edge came from. Leaving it as vanilla water means every one of those mechanics
is vanilla's, because it is vanilla's fluid.

The cost is one hack, kept small and loud about itself: Forge stores a fluid's rendering in a private
field written once at registration, so tinting vanilla water means reaching into that field and
putting a wrapper there that delegates everything except the tint and the fog. It is client-only,
runs once at startup, and if the reflection is ever refused it logs and gives up - water goes back to
looking like water and nothing else in the mod notices. A second mod that recolours water the same
way would conflict with it.

**Soil beds itself in.** Dirt, grass or coarse dirt sitting under water near mud soaks through and
becomes mud on its own, at a pace timestamped rather than ticked - a hollow flooded ten minutes ago has
already made progress by the time you walk back to it. Flood a dirt hollow near a mud patch and come
back two in-game days later to a proper wallow: it is something a farm grows into, not something that
appears over a lunch break.

## What pigs do with their time

Pigs have body poses of their own, and they choose them - nothing here is a player command:

- **Sitting.** A fed, unbothered pig sits back on its haunches for a while, roughly once a minute of
  having nothing better to do. Rump on the ground, hind legs folded forward under it, front legs
  planted, head level - posed part by part rather than by tipping the whole animal backwards, which
  is what used to bury its hind legs in the floor and point its snout at the sky.
- **Wallowing.** A pig standing in mud lies over and rocks from flank to flank, which is what
  actually works mud into a hide. It comes out **coated in mud**, drawn as a translucent brown wash
  over its own model that dries off over a few minutes and washes away much faster in clean water.
- **Nursing.** A sow lies flat so her litter can reach her, because piglets cannot nurse from a
  standing pig. A piglet can nurse from a wallowing sow too - she is on her flank either way.
- **Eating.** The head goes down, and only the head. Sheep get their real vanilla eating animation
  back (replacing their eat goal had taken it off them); everything else has its own head pitch
  driven down, which bends the neck through vanilla's rig without rotating the body.

Every one of those is animated rather than switched. The server only ever sends the target pose; the
client eases into it over about a second on a smoothstep curve, so a sow folds down under her own
weight instead of teleporting onto her side. A whole lie-down costs exactly one packet.

## Orphans, foster mothers and angry cows

An orphan trails after any grown animal of its kind, which is vanilla behaviour and looks exactly
like what it is: a calf looking for a mother. So it gets an answer. An adult that an orphan gets
close to decides, once, whether to take it on:

- **Accepted** (50% by default): it becomes her baby, and she comes into milk for it despite never
  having given birth.
- **Refused**: a headbutt. Head down, snapped back up, and the calf goes flying with a heart of
  damage. The answer stands for a few minutes, and the orphan will not pester the same animal again
  in that time, so it wanders off to try its luck elsewhere.

And **a mother goes after whoever hurts her baby**, at a run rather than a walk. Farm animals have no
attack damage in vanilla, so the mod gives them some.

A cow has one move beyond the chase: she **rams**, the way a goat does, and **the further away you
are the likelier she is to do it** - a ram is a run-up, so standing on top of her there is nothing to
run and she mostly shoves, while backing off across her range is the worst thing you can do. Halfway
across it the odds are about a third.
She plants herself, drops her head - a wind-up you can see coming and dodge - and then runs the whole
distance in a straight line, hitting whatever is at the end of it and throwing them clear. The charge
does not steer once committed, because a charge that follows a dodging player is not a charge, it is
a magnet; and like a goat's, it is spent if she hits a fence instead of you. Vanilla builds the goat
version out of brain behaviours, which cows do not have, so this is the same shape rebuilt as a goal.

She gives it all up when the attacker gets far enough away or her temper runs out, and she never
starts anything: she only ever has a target because something hurt her calf.

**Feeding is not courting.** A pig - the mod's one omnivore - will take anything edible from your
hand and it counts as a meal: nutrition by the food's own value, and its effects carried through, so
a golden apple is a golden apple. Cows, sheep and goats are herbivores and keep vanilla's diet
exactly: wheat and nothing else.

What feeding never does, for any of them, is breed the animal or make it follow you. Those are
vanilla's rules, keyed to vanilla's breeding items, and they stay vanilla's.

## Carcass yield

What a carcass gives is **how big the animal was, times what condition it died in**.

| Stage | Yield |
|---|---|
| Baby, Weanling | nothing at all - vanilla suppresses loot for babies, and killing calves is deliberately not a business |
| Juvenile | 45% of an adult: it is a smaller animal |
| Adult | full |

Without the adolescent penalty the fourth stage is decoration - a juvenile dropped exactly as much
meat and hide as an adult, so the optimal play was to slaughter everything one stage early and keep
only a pair of breeders.

An animal that starved to death is skin and bone, and drops accordingly: a quarter of its normal
meat and hide by default. The same scale runs on animals butchered while underfed, sliding from a
full carcass at good condition down to the starved floor, so neglecting a herd costs the player
real yield even when they slaughter before hunger does.

Fractions are settled by a roll rather than rounded, so a single hide at quarter condition is a 25%
chance of a hide, not a guaranteed nothing. Turn the whole thing off with `loot.conditionAffectsDrops`.

**Playing with other loot mods.** Condition is a property of the animal, not a bonus on the kill, so
BabyNeeds settles what the carcass is *first* and everyone else works from that number. Concretely:

- Mods that hook `LivingDropsEvent` (looting boosters, drop multipliers, extra-drop mods) run at the
  usual NORMAL priority; BabyNeeds runs at HIGHEST, so it goes first and they multiply the real,
  thin carcass. A x3 multiplier on a starved cow gives three quarter-carcasses, not a full one.
  It also means the stacks BabyNeeds scales are only the animal's own vanilla loot - items other
  mods add afterwards are theirs and are left untouched.
- Mods that rewrite the **loot table** instead (global loot modifiers, datapacks) resolve during
  loot generation, before any drop event exists, so they necessarily run before BabyNeeds. The
  result is still the same for a plain multiplier - x3 then x0.25 is x0.25 then x3 - but if such a
  mod replaces beef with something else entirely, BabyNeeds simply scales whatever it finds.

## Commands

All require permission level 2, and act on animals within 8 blocks:

- `/babyneeds info` - stage, nutrition and milk of the animals around you
- `/babyneeds stage <baby|weanling|juvenile|adult>` - force a stage (balance testing)
- `/babyneeds feed` - top up nutrition, and milk for mothers in lactation
- `/babyneeds drain` - empty nutrition and push straight to starving (testing the failure path)
- `/babyneeds uninstall` / `/babyneeds uninstall cancel` - strip BabyNeeds data and return animals
  to vanilla ages before removing the jar

Sneak + right-click an animal with an empty hand for the same status readout in chat.

## Configuration

Everything is in `config/babyneeds-common.toml`, grouped as `growth`, `hunger`, `milk`, `nursing`,
`pasture`, `pig` and `misc`. Every value carries a comment explaining what it does and what the
default implies in real minutes. The headline knobs:

- `growth.growthSpeed` - global multiplier over the whole four-stage ramp
- `hunger.decayPerMinute` - how fast animals get hungry
- `milk.minNutrition` - how well fed a mother must be to make any milk
- `milk.milkPerMinute` - yield per nursing baby; `milk.minRation` - the per-bout floor that keeps a
  litter fed
- `pasture.recoveryGraceTicks` - how long bare ground is safe before it can ever be lost
- `pasture.trampleThreshold` / `pasture.compactMaxChance` - how much traffic ruins soil, and how fast
- `pig.rootFoodValue` / `pig.maxRootsPerBlock` - how well a pig lives off bare ground
- `pig.mudLitterChance` / `pig.mudRootMultiplier` - the mud wallow payoff
- `pig.eatsCrops` / `pig.eatsScraps` - whether pigs raid gardens and eat dropped food
- `pig.sitChance` / `pig.wallowChance` - how often pigs sit down and roll in the mud
- `pig.mudDriesPerMinute` - how long a mud coating lasts
- `pig.siltingTicks` / `pig.soilToMudTicks` - how fast a wallow forms and beds itself in
- `motherhood.adoptionChance` - the odds an adult takes an orphan on
- `motherhood.defendBabies` / `motherhood.defendRadius` - whether mothers retaliate, and how far
- `motherhood.ramChanceNear` / `motherhood.ramChanceFar` - how the odds of a ram climb with distance
- `motherhood.ramKnockback` / `motherhood.ramDamage` - how hard it lands
- `loot.juvenileYield` - how much less an adolescent carcass is worth than a grown one
- `loot.starvedDropMultiplier` / `loot.fullConditionNutrition` - how much a neglected herd costs you
- `misc.uninstallMode` - clean removal

## Building

```
./gradlew build      # jar in build/libs
./gradlew test       # balance logic unit tests
./gradlew runClient  # dev client
```

Requires JDK 17. The balance logic (`GrowthCalculator`, `HungerCalculator`,
`LactationCalculator`, `PastureCalculator`, `Species`, `GrowthStage`) is deliberately free of
Minecraft types: it is unit tested, and it is the part that a future port to a newer Minecraft
version can carry over unchanged.

## Status

v0.1.0 - first playable version. Known limits:

- The four sizes are a render-time scale; hitboxes stay vanilla (baby-sized or adult-sized).
- Sitting is posed part by part, which needs the model itself: BabyNeeds registers its own pig
  renderer (vanilla's, with the model swapped) because the render event fires before `setupAnim`
  writes the standing pose over anything set from outside. A mod that also replaces the pig renderer
  will conflict; a saddled pig keeps its saddle in the standing pose, since that is a separate model.
- Lying and wallowing are still whole-body transforms, which is what they genuinely are.
- Vanilla grass spread speed is untouched. Rotation pressure comes from trampled ground compacting
  into coarse dirt instead, which needs no mixin. Slowing vanilla regrowth itself would.
- Horses, llamas, rabbits and tamed animals still use vanilla growth.
- A wallow's water is ordinary vanilla water; lava, farmland hydration, source forming and
  everything else reacts exactly as vanilla does, because it is vanilla water. Colouring it needs
  reflection into a private Forge field, so a second mod that recolours water will conflict, and a
  future Forge version could move the field - in which case water simply stops looking muddy.
