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
 * Looking for food, and wearing the pasture down while doing it. Every mouthful - loose grass or
 * the turf itself - strips the sward off the block underneath, leaving bare dirt and starting that
 * block's recovery clock. No soil is lost here: dirt grows back on its own. It is only lost later,
 * and only if the herd keeps standing on the same bare ground long after grass should have returned.
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

        // Whether the mouthful is loose grass or the turf itself, the block that pays for it is the
        // soil under it: a grazing animal crops the sward down to the roots. Without this the herd
        // would nibble the decorative grass off a plains pen forever and the ground would never
        // visibly wear, which is the signal the whole rotation mechanic depends on.
        BlockPos turf = state.is(Blocks.GRASS_BLOCK) ? target : target.below();
        boolean turfIsGrass = level.getBlockState(turf).is(Blocks.GRASS_BLOCK);
        double grassRatio = PastureBlocks.grassRatioAround(level, turf, Config.PASTURE_SCAN_RADIUS.get());

        double food;
        if (state.is(Blocks.GRASS_BLOCK)) {
            food = PastureCalculator.foodValue(grassRatio, species.grassFoodValue());
        } else {
            // Loose grass is a fuller mouthful than bare turf, but it takes the sward with it.
            food = species.grassFoodValue();
            level.destroyBlock(target, false);
        }

        if (turfIsGrass) {
            level.setBlock(turf, Blocks.DIRT.defaultBlockState(), Block.UPDATE_ALL);
            level.levelEvent(2001, turf, Block.getId(Blocks.GRASS_BLOCK.defaultBlockState()));
            if (level instanceof ServerLevel server && Config.PASTURE_DEGRADATION.get()) {
                // Bare from now on: this is where the recovery clock starts.
                PastureData.get(server).markBare(turf, server.getGameTime());
            }
        }

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
