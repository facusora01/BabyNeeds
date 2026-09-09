package com.babyneeds;

import java.util.EnumSet;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Looking for food, and wearing the pasture out while doing it. Loose grass and ferns are eaten
 * whole and cost the soil nothing; eating the turf off a grass block leaves bare dirt, and doing
 * that where the pasture is already stripped compacts it into coarse dirt, which vanilla grass
 * never spreads back onto. That is the pressure to rotate animals instead of parking a herd in one
 * pen forever.
 */
public class GrazeGoal extends Goal {

    /** Random probes per search instead of scanning the whole box - cheap for a big herd. */
    private static final int SEARCH_SAMPLES = 48;
    private static final int SEARCH_COOLDOWN_TICKS = 60;
    private static final double REACH_SQ = 3.0;

    private final Animal animal;
    private final Species species;
    private BlockPos target;
    private int chewTicks;
    private int searchCooldown;
    private int repathTicks;

    public GrazeGoal(Animal animal, Species species) {
        this.animal = animal;
        this.species = species;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        // A newborn lives on milk alone; everything past that grazes.
        if (!GrowthCalculator.acceptsSolids(AnimalState.stage(animal))) return false;
        if (AnimalState.isLying(animal)) return false;
        if (!HungerCalculator.wantsToGraze(AnimalState.nutrition(animal), Config.GRAZE_THRESHOLD.get())) {
            return false;
        }
        if (searchCooldown-- > 0) return false;
        searchCooldown = SEARCH_COOLDOWN_TICKS;

        this.target = findGrass();
        return target != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (target == null) return false;
        if (AnimalState.isLying(animal)) return false;
        return isEdible(animal.level().getBlockState(target));
    }

    @Override
    public void start() {
        chewTicks = 0;
        repathTicks = 0;
        moveToTarget();
    }

    @Override
    public void stop() {
        target = null;
        chewTicks = 0;
        animal.getNavigation().stop();
    }

    @Override
    public void tick() {
        animal.getLookControl().setLookAt(target.getX() + 0.5, target.getY(), target.getZ() + 0.5);

        if (animal.blockPosition().distSqr(target) > REACH_SQ) {
            chewTicks = 0;
            if (--repathTicks <= 0) {
                repathTicks = 20;
                moveToTarget();
            }
            return;
        }

        animal.getNavigation().stop();
        if (++chewTicks < Config.GRAZE_CHEW_TICKS.get()) return;
        eat();
        target = null;
    }

    private void moveToTarget() {
        animal.getNavigation().moveTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, 1.0);
    }

    private void eat() {
        Level level = animal.level();
        BlockState state = level.getBlockState(target);
        if (!isEdible(state)) return;

        double food;
        if (state.is(Blocks.GRASS_BLOCK)) {
            double grassRatio = grassRatioAround(target);
            food = PastureCalculator.foodValue(grassRatio, species.grassFoodValue());
            level.setBlock(target, degradedSoil(grassRatio), Block.UPDATE_ALL);
        } else {
            // Loose grass and ferns grow back on their own; eating them costs the soil nothing.
            food = species.grassFoodValue();
            level.destroyBlock(target, false);
        }

        level.levelEvent(2001, target, Block.getId(state));
        // Mob#ate is vanilla's "I just ate a block" hook - it is what regrows a sheep's wool, and
        // a no-op for everything else, so replacing the vanilla eat goal costs the sheep nothing.
        animal.ate();
        AnimalEvents.feedNutrition(animal, food);
        AnimalEvents.addGrowth(animal, food, AnimalState.stage(animal));
        AnimalEvents.eatEffects(animal, net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER);
    }

    private BlockState degradedSoil(double grassRatio) {
        if (!Config.PASTURE_DEGRADATION.get()) return Blocks.DIRT.defaultBlockState();
        double chance = PastureCalculator.coarseChance(grassRatio,
                Config.COARSE_BASE_CHANCE.get(), Config.COARSE_MAX_CHANCE.get());
        PastureCalculator.Outcome outcome =
                PastureCalculator.graze(chance, animal.getRandom().nextDouble());
        return outcome == PastureCalculator.Outcome.COARSE_DIRT
                ? Blocks.COARSE_DIRT.defaultBlockState()
                : Blocks.DIRT.defaultBlockState();
    }

    /** How much of the surrounding surface is still grass - the pasture's health at this spot. */
    private double grassRatioAround(BlockPos center) {
        int radius = Config.PASTURE_SCAN_RADIUS.get();
        Level level = animal.level();
        int grass = 0;
        int total = 0;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                BlockPos pos = center.offset(dx, 0, dz);
                BlockState state = level.getBlockState(pos);
                // Only soil counts; walls, water and paths are not pasture either way.
                if (state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT) || state.is(Blocks.COARSE_DIRT)) {
                    total++;
                    if (state.is(Blocks.GRASS_BLOCK)) grass++;
                }
            }
        }
        return total == 0 ? 1.0 : (double) grass / total;
    }

    private BlockPos findGrass() {
        RandomSource random = animal.getRandom();
        Level level = animal.level();
        int radius = species.grazeRadius();
        BlockPos origin = animal.blockPosition();

        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;

        for (int i = 0; i < SEARCH_SAMPLES; i++) {
            BlockPos pos = origin.offset(
                    random.nextInt(radius * 2 + 1) - radius,
                    random.nextInt(5) - 2,
                    random.nextInt(radius * 2 + 1) - radius);
            if (!isEdible(level.getBlockState(pos))) continue;

            double distance = origin.distSqr(pos);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = pos.immutable();
            }
        }
        return best;
    }

    private boolean isEdible(BlockState state) {
        if (state.is(Blocks.GRASS) || state.is(Blocks.TALL_GRASS) || state.is(Blocks.FERN)) return true;
        // Turf is only worth eating where something can actually grow back on top of it.
        return state.is(Blocks.GRASS_BLOCK);
    }
}
