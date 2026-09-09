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
- Players share the udder: a bucket takes real milk out of it, and milk is held back while a baby
  still nurses, so you cannot milk a fresh cow dry and starve her calf.
- Bottle feeding a milk bucket to an orphan is allowed by default (`milk.allowBottleFeeding`).

## Pasture and rotation

Animals graze for themselves. Loose grass and ferns are eaten whole and cost the soil nothing;
eating the turf off a grass block leaves **bare dirt**, and doing that where the surrounding
pasture is already stripped compacts it into **coarse dirt**, which vanilla grass never spreads
back onto until a player hoes it.

That is the whole rotation pressure. A generously sized or rotated pasture recovers on its own; a
packed pen turns to coarse dirt, stops feeding the herd, the mothers dry up, and the babies go
with them.

**Pigs and mud**: a pig standing in mud is wallowing - it burns noticeably less nutrition, and a
sow with mud access produces more milk. A pig pen with a mud puddle is genuinely better husbandry.

## Commands

All require permission level 2, and act on animals within 8 blocks:

- `/babyneeds info` - stage, nutrition and milk of the animals around you
- `/babyneeds stage <baby|weanling|juvenile|adult>` - force a stage (balance testing)
- `/babyneeds feed` - top up nutrition, and milk for mothers in lactation
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
- `pasture.coarseBaseChance` / `pasture.coarseMaxChance` - how punishing overgrazing is
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
- The lying sow is drawn by tipping the pig model on its side; there is no vanilla lying pose.
- Vanilla grass spread speed is untouched. Rotation pressure comes from coarse dirt instead, which
  needs no mixin. Slowing vanilla regrowth itself would.
- Horses, llamas, rabbits and tamed animals still use vanilla growth.
