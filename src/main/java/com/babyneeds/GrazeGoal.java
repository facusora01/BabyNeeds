package com.babyneeds;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;

/**
 * Looking for food, and wearing the pasture down while doing it. Every mouthful - loose grass or
 * the turf itself - strips the sward off the block underneath, leaving bare dirt and starting that
 * block's recovery clock. No soil is lost here: dirt grows back on its own. It is only lost later,
 * and only if the herd keeps standing on the same bare ground long after grass should have returned.
 *
 * Two rules decide what an animal walks to, and both exist because the naive version starved cows
 * that were standing on grass:
 *
 * <ol>
 *   <li>What is under its feet comes first. It needs no pathfinding and it cannot fail, so a packed
 *       pen keeps feeding its herd until the pen itself is bare.</li>
 *   <li>Otherwise, the nearest grass it can actually <em>reach</em>. Candidates go through the
 *       navigator's own A* before one is picked, so an animal never fixates on grass beyond the
 *       fence - or on a tuft its herdmates are stood on - and starve staring at it.</li>
 * </ol>
 */
public class GrazeGoal extends Goal {

    /** Blocks around the animal that are looked at exhaustively, nearest first. */
    private static final int CLOSE_SCAN_RADIUS = 4;
    /** Random probes over the wider range, for when nothing close is left. */
    private static final int FAR_SCAN_SAMPLES = 48;
    /** Pathfinding is not free, so only the closest few standing spots are ever tested. */
    private static final int MAX_PATH_CHECKS = 8;
    private static final int SEARCH_COOLDOWN_TICKS = 60;
    /** Give up on a target that is taking too long: something is in the way. */
    private static final int MAX_TRAVEL_TICKS = 120;
    private static final double REACH_SQ = 3.0;

    private final Animal animal;
    private final Species species;
    private BlockPos target;
    private BlockPos standSpot;
    private Path path;
    private int chewTicks;
    private int travelTicks;
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

        this.path = null;
        this.standSpot = null;
        this.target = findGrassUnderfoot();
        if (target == null) {
            this.target = findReachableGrass();
        }
        return target != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (target == null) return false;
        if (AnimalState.isLying(animal)) return false;
        // Dropped rather than starved over: if it has not arrived by now, it is not getting there.
        if (travelTicks > MAX_TRAVEL_TICKS) return false;
        return isEdible(animal.level().getBlockState(target));
    }

    @Override
    public void start() {
        chewTicks = 0;
        travelTicks = 0;
        repathTicks = 0;
        if (withinReach()) return;

        if (path != null) {
            animal.getNavigation().moveTo(path, 1.0);
        } else {
            moveToTarget();
        }
    }

    @Override
    public void stop() {
        target = null;
        standSpot = null;
        path = null;
        chewTicks = 0;
        travelTicks = 0;
        animal.getNavigation().stop();
    }

    @Override
    public void tick() {
        animal.getLookControl().setLookAt(target.getX() + 0.5, target.getY(), target.getZ() + 0.5);

        if (!withinReach()) {
            chewTicks = 0;
            travelTicks++;
            // The navigator says it is finished and we are still not there, so the way is blocked.
            // Burn the budget now and let the next search pick something it can actually get to.
            if (travelTicks > 20 && animal.getNavigation().isDone()) {
                travelTicks = MAX_TRAVEL_TICKS + 1;
                return;
            }
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

    private boolean withinReach() {
        return animal.blockPosition().distSqr(target) <= REACH_SQ;
    }

    private void moveToTarget() {
        BlockPos stand = standSpot != null ? standSpot : target;
        animal.getNavigation().moveTo(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, 1.0);
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

    /**
     * The mouthful that can never be blocked, missed or contested: the ground the animal is already
     * standing on, or the tuft it is standing in. Checked before anything else, which is what keeps
     * a crowded pen fed - a cow hemmed in by five others still has grass under her hooves.
     */
    private BlockPos findGrassUnderfoot() {
        Level level = animal.level();
        BlockPos at = animal.blockPosition();
        if (isEdible(level.getBlockState(at))) return at;

        BlockPos below = at.below();
        return isEdible(level.getBlockState(below)) ? below : null;
    }

    /**
     * Nearest grass the animal can actually walk to. Candidates are sorted by distance and handed to
     * the navigator's A* one at a time; the first that comes back with a path that truly reaches its
     * destination wins. Anything fenced off, walled in or otherwise unreachable is skipped instead of
     * being stared at until the animal starves.
     */
    private BlockPos findReachableGrass() {
        List<BlockPos> candidates = collectCandidates();
        if (candidates.isEmpty()) return null;

        BlockPos origin = animal.blockPosition();
        candidates.sort(Comparator.comparingDouble(origin::distSqr));

        int checks = 0;
        for (BlockPos candidate : candidates) {
            for (BlockPos spot : standingSpotsFor(candidate)) {
                if (checks++ >= MAX_PATH_CHECKS) return null;

                Path candidatePath = animal.getNavigation().createPath(spot, 0);
                if (candidatePath == null || !candidatePath.canReach()) continue;

                this.path = candidatePath;
                this.standSpot = spot;
                return candidate;
            }
        }
        return null;
    }

    /**
     * Every spot the animal could stand in to reach this mouthful, best first. Turf is normally
     * grazed from on top of it and a tuft from inside it, but when something is standing on the
     * block - a fence post, a wall, a torch - that spot does not exist and the naive version wrote
     * the block off as unreachable. That is what left a ring of untouched grass under the fence
     * line of a pen whose animals were starving inside it, so the neighbouring spots count too:
     * cattle graze right up to and through a fence.
     */
    private List<BlockPos> standingSpotsFor(BlockPos pos) {
        BlockPos onTop = animal.level().getBlockState(pos).is(Blocks.GRASS_BLOCK) ? pos.above() : pos;

        List<BlockPos> spots = new ArrayList<>(5);
        if (isStandable(onTop)) {
            spots.add(onTop);
        }
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos beside = onTop.relative(direction);
            if (isStandable(beside)) {
                spots.add(beside);
            }
        }
        return spots;
    }

    /**
     * Cheap filter before spending a pathfind: somewhere the animal could plausibly stand. Grass and
     * ferns count - it walks straight through them - fences, walls and slabs do not.
     */
    private boolean isStandable(BlockPos pos) {
        BlockState state = animal.level().getBlockState(pos);
        return state.isAir() || isEdible(state);
    }

    private List<BlockPos> collectCandidates() {
        Level level = animal.level();
        BlockPos origin = animal.blockPosition();
        List<BlockPos> found = new ArrayList<>();

        // Everything close by, exhaustively: in a grazed-down pen the last few tufts are what matter.
        for (int dx = -CLOSE_SCAN_RADIUS; dx <= CLOSE_SCAN_RADIUS; dx++) {
            for (int dz = -CLOSE_SCAN_RADIUS; dz <= CLOSE_SCAN_RADIUS; dz++) {
                for (int dy = -2; dy <= 1; dy++) {
                    BlockPos pos = origin.offset(dx, dy, dz);
                    if (isEdible(level.getBlockState(pos))) {
                        found.add(pos.immutable());
                    }
                }
            }
        }
        if (!found.isEmpty()) return found;

        // Nothing close: probe the wider range at random rather than scanning thousands of blocks.
        RandomSource random = animal.getRandom();
        int radius = species.grazeRadius();
        for (int i = 0; i < FAR_SCAN_SAMPLES; i++) {
            BlockPos pos = origin.offset(
                    random.nextInt(radius * 2 + 1) - radius,
                    random.nextInt(5) - 2,
                    random.nextInt(radius * 2 + 1) - radius);
            if (isEdible(level.getBlockState(pos))) {
                found.add(pos.immutable());
            }
        }
        return found;
    }

    private boolean isEdible(BlockState state) {
        return PastureBlocks.isEdible(state);
    }
}
