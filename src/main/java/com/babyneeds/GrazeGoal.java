package com.babyneeds;

import java.util.EnumSet;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Looking for food, and wearing the pasture down while doing it. Loose grass and ferns are eaten
 * whole and cost the soil nothing; eating the turf off a grass block leaves bare dirt and starts
 * that block's recovery clock. Nothing is destroyed here - soil is only lost later, and only if the
 * herd keeps standing on the same bare ground long after grass should have come back.
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
            // Eating the turf never ruins the soil on its own: it leaves plain dirt, which vanilla
            // grass creeps back onto. Only ground that stays bare and keeps being trampled is lost,
            // and PastureData is what remembers that.
            double grassRatio = PastureBlocks.grassRatioAround(level, target, Config.PASTURE_SCAN_RADIUS.get());
            food = PastureCalculator.foodValue(grassRatio, species.grassFoodValue());
            level.setBlock(target, Blocks.DIRT.defaultBlockState(), Block.UPDATE_ALL);
            if (level instanceof ServerLevel server && Config.PASTURE_DEGRADATION.get()) {
                PastureData.get(server).markBare(target, server.getGameTime());
            }
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
        return PastureBlocks.isEdible(state);
    }
}
