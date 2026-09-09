package com.babyneeds;

import java.util.UUID;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.core.BlockPos;

/**
 * All BabyNeeds state lives in the entity's Forge persistent data, so it saves and loads with the
 * animal and no capability or mixin is needed. Vanilla's age field is not storage here - it is an
 * output: {@link #applyStageToAge} pins it to whatever the stage says, which is how the mod takes
 * the growth timer away from vanilla without touching the entity classes.
 */
public final class AnimalState {

    private static final String ROOT = "BabyNeeds";

    private static final String STAGE = "Stage";
    private static final String PROGRESS = "Progress";
    private static final String NUTRITION = "Nutrition";
    private static final String MILK = "Milk";
    private static final String STAGE_TICKS = "StageTicks";
    private static final String STARVE_TICKS = "StarveTicks";
    private static final String NURSE_COOLDOWN = "NurseCooldown";
    private static final String LACTATION_TICKS = "LactationTicks";
    private static final String MOTHER = "Mother";
    private static final String DEPENDENTS = "Dependents";
    private static final String LYING = "Lying";
    private static final String LIE_COOLDOWN = "LieCooldown";
    private static final String INITIALIZED = "Init";

    /** Vanilla age pinned on a stage that should read as a baby. */
    private static final int BABY_AGE = -24000;
    /** Positive age = vanilla breeding cooldown, which is exactly what a juvenile needs. */
    private static final int JUVENILE_AGE = 24000;

    private AnimalState() {
    }

    // ------------------------------------------------------------------ species

    public static Species speciesOf(Entity entity) {
        // MushroomCow extends Cow, so it is covered by the Cow branch.
        if (entity instanceof Cow) return Species.COW;
        if (entity instanceof Sheep) return Species.SHEEP;
        if (entity instanceof Pig) return Species.PIG;
        if (entity instanceof Goat) return Species.GOAT;
        return null;
    }

    public static boolean isSupported(Entity entity) {
        return entity instanceof Animal && speciesOf(entity) != null;
    }

    // ------------------------------------------------------------------ raw access

    private static CompoundTag data(Entity entity) {
        CompoundTag root = entity.getPersistentData();
        if (!root.contains(ROOT, CompoundTag.TAG_COMPOUND)) {
            root.put(ROOT, new CompoundTag());
        }
        return root.getCompound(ROOT);
    }

    public static boolean isInitialized(Entity entity) {
        return entity.getPersistentData().getCompound(ROOT).getBoolean(INITIALIZED);
    }

    public static void clear(Entity entity) {
        entity.getPersistentData().remove(ROOT);
    }

    // ------------------------------------------------------------------ stage

    public static GrowthStage stage(Entity entity) {
        return GrowthStage.byIndex(data(entity).getInt(STAGE));
    }

    public static void setStage(Entity entity, GrowthStage stage) {
        CompoundTag tag = data(entity);
        tag.putInt(STAGE, stage.index());
        tag.putDouble(PROGRESS, 0.0);
        tag.putInt(STAGE_TICKS, 0);
    }

    public static double progress(Entity entity) {
        return data(entity).getDouble(PROGRESS);
    }

    public static void setProgress(Entity entity, double progress) {
        data(entity).putDouble(PROGRESS, progress);
    }

    public static int stageTicks(Entity entity) {
        return data(entity).getInt(STAGE_TICKS);
    }

    public static void addStageTicks(Entity entity, int ticks) {
        CompoundTag tag = data(entity);
        tag.putInt(STAGE_TICKS, tag.getInt(STAGE_TICKS) + ticks);
    }

    // ------------------------------------------------------------------ nutrition

    public static double nutrition(Entity entity) {
        return data(entity).getDouble(NUTRITION);
    }

    public static void setNutrition(Entity entity, double nutrition) {
        data(entity).putDouble(NUTRITION, nutrition);
    }

    public static int starvingTicks(Entity entity) {
        return data(entity).getInt(STARVE_TICKS);
    }

    public static void setStarvingTicks(Entity entity, int ticks) {
        data(entity).putInt(STARVE_TICKS, ticks);
    }

    // ------------------------------------------------------------------ milk

    public static double milk(Entity entity) {
        return data(entity).getDouble(MILK);
    }

    public static void setMilk(Entity entity, double milk) {
        data(entity).putDouble(MILK, milk);
    }

    public static int lactationTicks(Entity entity) {
        return data(entity).getInt(LACTATION_TICKS);
    }

    public static void setLactationTicks(Entity entity, int ticks) {
        data(entity).putInt(LACTATION_TICKS, Math.max(0, ticks));
    }

    public static boolean isLactating(Entity entity) {
        return lactationTicks(entity) > 0;
    }

    /** Called on birth, on every nursing bout and on every bucket taken: demand keeps her in milk. */
    public static void refreshLactation(Entity entity) {
        setLactationTicks(entity, Config.LACTATION_DURATION_TICKS.get());
    }

    /** Babies currently nursing from this mother; drives both her yield and how milk is shared. */
    public static int dependents(Entity entity) {
        return data(entity).getInt(DEPENDENTS);
    }

    public static void setDependents(Entity entity, int count) {
        data(entity).putInt(DEPENDENTS, Math.max(0, count));
    }

    public static int nurseCooldown(Entity entity) {
        return data(entity).getInt(NURSE_COOLDOWN);
    }

    public static void setNurseCooldown(Entity entity, int ticks) {
        data(entity).putInt(NURSE_COOLDOWN, Math.max(0, ticks));
    }

    // ------------------------------------------------------------------ motherhood

    public static UUID mother(Entity entity) {
        CompoundTag tag = data(entity);
        return tag.hasUUID(MOTHER) ? tag.getUUID(MOTHER) : null;
    }

    public static void setMother(Entity entity, UUID mother) {
        data(entity).putUUID(MOTHER, mother);
    }

    /** Resolves the baby's own mother, and only her - a calf never nurses from a stranger. */
    public static Animal findMother(Animal baby) {
        UUID id = mother(baby);
        if (id == null) return null;
        if (!(baby.level() instanceof ServerLevel server)) return null;
        Entity found = server.getEntity(id);
        if (!(found instanceof Animal animal) || !animal.isAlive()) return null;
        double radius = Config.NURSE_SEARCH_RADIUS.get();
        if (baby.distanceToSqr(animal) > radius * radius) return null;
        return animal;
    }

    public static boolean isLying(Entity entity) {
        return data(entity).getBoolean(LYING);
    }

    public static void setLying(Entity entity, boolean lying) {
        data(entity).putBoolean(LYING, lying);
    }

    public static int lieCooldown(Entity entity) {
        return data(entity).getInt(LIE_COOLDOWN);
    }

    public static void setLieCooldown(Entity entity, int ticks) {
        data(entity).putInt(LIE_COOLDOWN, Math.max(0, ticks));
    }

    // ------------------------------------------------------------------ setup

    /**
     * First contact with an animal that has no BabyNeeds data: everything already in the world is
     * adopted at the stage its vanilla age implies, so adding the mod mid-save does not wipe a farm.
     */
    public static void initializeExisting(Animal animal) {
        GrowthStage stage = animal.isBaby() ? GrowthStage.BABY : GrowthStage.ADULT;
        CompoundTag tag = data(animal);
        tag.putBoolean(INITIALIZED, true);
        setStage(animal, stage);
        // Existing animals start comfortably fed rather than instantly starving on mod install.
        setNutrition(animal, HungerCalculator.MAX_NUTRITION * 0.75);
        setMilk(animal, 0.0);
        setLactationTicks(animal, 0);
    }

    /** A newborn from breeding: hungry, milk-dependent, and bound to the mother that bore it. */
    public static void initializeNewborn(AgeableMob child, Animal mother) {
        CompoundTag tag = data(child);
        tag.putBoolean(INITIALIZED, true);
        setStage(child, GrowthStage.BABY);
        setNutrition(child, HungerCalculator.MAX_NUTRITION * 0.6);
        setMilk(child, 0.0);
        if (mother != null) {
            setMother(child, mother.getUUID());
        }
    }

    // ------------------------------------------------------------------ vanilla age

    /**
     * Keeps the vanilla age in sync with our stage. Negative age makes vanilla render and treat the
     * animal as a baby; a positive age is vanilla's own post-breeding cooldown, which conveniently
     * means "grown but not allowed to breed" - exactly a juvenile. Only ADULT is left free, so a
     * bred cow keeps its normal breeding cooldown.
     */
    public static void applyStageToAge(AgeableMob animal, GrowthStage stage) {
        switch (stage) {
            case BABY, WEANLING -> {
                if (animal.getAge() != BABY_AGE) {
                    animal.setAge(BABY_AGE);
                }
            }
            case JUVENILE -> {
                if (animal.getAge() < JUVENILE_AGE / 2) {
                    animal.setAge(JUVENILE_AGE);
                }
            }
            case ADULT -> {
                if (animal.getAge() < 0) {
                    animal.setAge(0);
                }
            }
        }
    }

    // ------------------------------------------------------------------ environment

    /** A pig standing in mud, or in the silty water of a proper wallow, is wallowing. */
    public static boolean isInMud(Animal animal) {
        BlockPos at = animal.blockPosition();
        return MuddyWater.isWallow(animal.level().getBlockState(at))
                || MuddyWater.isWallow(animal.level().getBlockState(at.below()));
    }
}
