package com.babyneeds;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;

/**
 * Walking to a block and eating it. Grazing a sward, rooting up soil and raiding a carrot patch are
 * the same problem three times over, and the hard part is not the eating - it is choosing a block
 * the animal can actually get to.
 *
 * Two rules do that, and both exist because the naive version starved animals standing on food:
 *
 * <ol>
 *   <li>What is under its feet comes first. It needs no pathfinding and it cannot fail, so a packed
 *       pen keeps feeding its herd until the pen itself is stripped.</li>
 *   <li>Otherwise, the nearest block it can actually <em>reach</em>. Candidates go through the
 *       navigator's own A* before one is picked, so an animal never fixates on food beyond the fence
 *       - or on a spot its herdmates are stood on - and starve staring at it.</li>
 * </ol>
 */
public abstract class BlockConsumingGoal extends Goal {

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

    protected final Animal animal;
    protected final Species species;

    private BlockPos target;
    private BlockPos standSpot;
    private Path path;
    private int chewTicks;
    private int travelTicks;
    private int searchCooldown;
    private int repathTicks;

    protected BlockConsumingGoal(Animal animal, Species species) {
        this.animal = animal;
        this.species = species;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    /** Blocks this animal will walk to and eat. */
    protected abstract boolean isTarget(BlockState state);

    /** Take the mouthful: change the world, feed the animal. */
    protected abstract void consume(BlockPos pos, BlockState state);

    /** Ticks head-down on the block before the mouthful counts. */
    protected abstract int chewDuration();

    /** How far out this animal is willing to look. */
    protected int searchRadius() {
        return species.grazeRadius();
    }

    /** Whether the animal is interested at all right now; hunger by default. */
    protected boolean wantsToEat() {
        return HungerCalculator.wantsToGraze(AnimalState.nutrition(animal), Config.GRAZE_THRESHOLD.get());
    }

    @Override
    public boolean canUse() {
        // A newborn lives on milk alone; everything past that feeds itself.
        if (!GrowthCalculator.acceptsSolids(AnimalState.stage(animal))) return false;
        if (AnimalState.isLying(animal)) return false;
        if (!wantsToEat()) return false;
        if (searchCooldown-- > 0) return false;
        searchCooldown = SEARCH_COOLDOWN_TICKS;

        this.path = null;
        this.standSpot = null;
        this.target = findUnderfoot();
        if (target == null) {
            this.target = findReachable();
        }
        return target != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (target == null) return false;
        if (AnimalState.isLying(animal)) return false;
        // Dropped rather than starved over: if it has not arrived by now, it is not getting there.
        if (travelTicks > MAX_TRAVEL_TICKS) return false;
        return isTarget(animal.level().getBlockState(target));
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
        if (++chewTicks < chewDuration()) return;

        BlockState state = animal.level().getBlockState(target);
        if (isTarget(state)) {
            consume(target, state);
        }
        target = null;
    }

    private boolean withinReach() {
        return animal.blockPosition().distSqr(target) <= REACH_SQ;
    }

    private void moveToTarget() {
        BlockPos stand = standSpot != null ? standSpot : target;
        animal.getNavigation().moveTo(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, 1.0);
    }

    /**
     * The mouthful that can never be blocked, missed or contested: the ground the animal is already
     * standing on, or what it is standing in. Checked before anything else, which is what keeps a
     * crowded pen fed - a cow hemmed in by five others still has grass under her hooves.
     */
    private BlockPos findUnderfoot() {
        Level level = animal.level();
        BlockPos at = animal.blockPosition();
        if (isTarget(level.getBlockState(at))) return at;

        BlockPos below = at.below();
        return isTarget(level.getBlockState(below)) ? below : null;
    }

    /**
     * Nearest block the animal can actually walk to. Candidates are sorted by distance and handed to
     * the navigator's A* one at a time; the first that comes back with a path that truly reaches its
     * destination wins. Anything fenced off or walled in is skipped instead of being stared at until
     * the animal starves.
     */
    private BlockPos findReachable() {
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
     * Every spot the animal could stand in to reach this mouthful, best first. A full block is eaten
     * from on top of it and a plant from inside it, but when something is standing on the block - a
     * fence post, a wall, a torch - that spot does not exist, and the naive version wrote the block
     * off as unreachable. That is what left a ring of untouched grass under the fence line of a pen
     * whose animals were starving inside it, so the neighbouring spots count too: livestock feed
     * right up to and through a fence.
     */
    private List<BlockPos> standingSpotsFor(BlockPos pos) {
        BlockPos onTop = animal.level().getBlockState(pos).isSolidRender(animal.level(), pos)
                ? pos.above() : pos;

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
     * Cheap filter before spending a pathfind: somewhere the animal could plausibly stand. Plants it
     * walks straight through count; fences, walls and slabs do not.
     */
    private boolean isStandable(BlockPos pos) {
        BlockState state = animal.level().getBlockState(pos);
        return state.isAir() || PastureBlocks.isEdible(state) || !state.isSolidRender(animal.level(), pos);
    }

    private List<BlockPos> collectCandidates() {
        Level level = animal.level();
        BlockPos origin = animal.blockPosition();
        List<BlockPos> found = new ArrayList<>();

        // Everything close by, exhaustively: in a stripped pen the last few mouthfuls are what matter.
        for (int dx = -CLOSE_SCAN_RADIUS; dx <= CLOSE_SCAN_RADIUS; dx++) {
            for (int dz = -CLOSE_SCAN_RADIUS; dz <= CLOSE_SCAN_RADIUS; dz++) {
                for (int dy = -2; dy <= 1; dy++) {
                    BlockPos pos = origin.offset(dx, dy, dz);
                    if (isTarget(level.getBlockState(pos))) {
                        found.add(pos.immutable());
                    }
                }
            }
        }
        if (!found.isEmpty()) return found;

        // Nothing close: probe the wider range at random rather than scanning thousands of blocks.
        RandomSource random = animal.getRandom();
        int radius = searchRadius();
        for (int i = 0; i < FAR_SCAN_SAMPLES; i++) {
            BlockPos pos = origin.offset(
                    random.nextInt(radius * 2 + 1) - radius,
                    random.nextInt(5) - 2,
                    random.nextInt(radius * 2 + 1) - radius);
            if (isTarget(level.getBlockState(pos))) {
                found.add(pos.immutable());
            }
        }
        return found;
    }

    /** Shared by the subclasses: what a mouthful does to the animal that took it. */
    protected void feed(double food) {
        AnimalEvents.feedNutrition(animal, food);
        AnimalEvents.addGrowth(animal, food, AnimalState.stage(animal));
        AnimalEvents.eatEffects(animal, net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER);
        // Mob#ate is vanilla's "I just ate a block" hook - it is what regrows a sheep's wool, and a
        // no-op for everything else.
        animal.ate();
    }

    /** Turns the sward on a block into bare dirt and starts its recovery clock. */
    protected void stripTurf(BlockPos turf) {
        Level level = animal.level();
        if (!level.getBlockState(turf).is(Blocks.GRASS_BLOCK)) return;

        level.setBlock(turf, Blocks.DIRT.defaultBlockState(),
                net.minecraft.world.level.block.Block.UPDATE_ALL);
        level.levelEvent(2001, turf,
                net.minecraft.world.level.block.Block.getId(Blocks.GRASS_BLOCK.defaultBlockState()));
        if (level instanceof net.minecraft.server.level.ServerLevel server
                && Config.PASTURE_DEGRADATION.get()) {
            PastureData.get(server).markBare(turf, server.getGameTime());
        }
    }
}
