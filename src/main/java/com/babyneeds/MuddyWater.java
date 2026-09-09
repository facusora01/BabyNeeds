package com.babyneeds;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Turns clean water into a wallow. Water dug into mud goes brown, and runs clear again if the mud
 * around it is taken away, so a player builds a pig wallow the obvious way - dig a hole in mud, pour
 * a bucket in - and it simply looks right.
 *
 * The decision is made for a whole connected body of water at once, never block by block. Judging
 * single blocks bites its own tail: "muddy water reverts next to clean water" is a fine rule for
 * keeping silt out of a lake, but applied to one block at a time it means every block of a fresh
 * puddle refuses to turn because its neighbours have not turned yet, and a hole dug entirely in mud
 * stays stubbornly blue. Flood-filling the pool first and deciding once fixes that, and it is also
 * the only way to tell a puddle from the edge of an ocean.
 */
public final class MuddyWater {

    /** A wallow is a puddle. Anything bigger than this is a pond and stays clean. */
    private static final int MAX_WALLOW_BLOCKS = 32;
    /** Hard stop on the flood fill so brushing an ocean cannot walk the whole thing. */
    private static final int MAX_SEARCH_BLOCKS = MAX_WALLOW_BLOCKS + 24;
    /** Minimum mud faces around the pool before it counts as dug into mud at all. */
    private static final int MIN_MUD_FACES = 2;

    private MuddyWater() {
    }

    public static boolean isMud(BlockState state) {
        // Packed mud is a building block, not a wallow, so it deliberately does not count.
        return state.is(Blocks.MUD) || state.is(Blocks.MUDDY_MANGROVE_ROOTS);
    }

    public static boolean isMuddyWater(BlockState state) {
        return state.is(ModBlocks.MUDDY_WATER_BLOCK.get());
    }

    /** A wallow to lie in: mud underfoot, or standing in the silty water itself. */
    public static boolean isWallow(BlockState state) {
        return isMud(state) || isMuddyWater(state);
    }

    /**
     * Re-evaluates every pool touching the given area and converts each one whichever way the mud
     * around it now calls for. Cheap enough to run when a block is placed or broken, and every few
     * seconds around a pig.
     */
    public static void updateArea(Level level, BlockPos center, int radius) {
        if (!Config.MUDDY_WATER.get()) return;

        Set<BlockPos> handled = new HashSet<>();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -radius, -radius),
                center.offset(radius, radius, radius))) {
            BlockPos immutable = pos.immutable();
            if (handled.contains(immutable)) continue;
            if (!isPoolSource(level.getBlockState(immutable))) continue;

            List<BlockPos> body = collectBody(level, immutable);
            handled.addAll(body);
            applyToBody(level, body, shouldBeMuddy(level, body));
        }
    }

    /** Only still water forms a pool; a flowing edge is left to sort itself out. */
    private static boolean isPoolSource(BlockState state) {
        if (!state.is(Blocks.WATER) && !isMuddyWater(state)) return false;
        return state.getFluidState().isSource();
    }

    /**
     * The connected pool this block belongs to, clean and muddy alike. Bounded: past the cap this
     * is open water rather than a puddle, and the caller treats the oversized result as such.
     */
    private static List<BlockPos> collectBody(Level level, BlockPos start) {
        List<BlockPos> body = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();

        queue.add(start);
        seen.add(start);

        while (!queue.isEmpty() && body.size() < MAX_SEARCH_BLOCKS) {
            BlockPos pos = queue.poll();
            body.add(pos);

            for (Direction direction : Direction.values()) {
                BlockPos next = pos.relative(direction).immutable();
                if (!seen.add(next)) continue;
                if (isPoolSource(level.getBlockState(next))) {
                    queue.add(next);
                }
            }
        }
        return body;
    }

    /**
     * A pool is a wallow when it is small enough to be a puddle and mud is what is holding it. Mud
     * has to be the majority of what contains it, so a hole dug in mud qualifies while a bay at the
     * edge of a lake with one muddy bank does not.
     */
    private static boolean shouldBeMuddy(Level level, List<BlockPos> body) {
        if (body.size() > MAX_WALLOW_BLOCKS) return false;

        Set<BlockPos> pool = new HashSet<>(body);
        int mudFaces = 0;
        int otherFaces = 0;

        for (BlockPos pos : body) {
            for (Direction direction : Direction.values()) {
                if (direction == Direction.UP) continue; // open sky is not what contains a puddle
                BlockPos neighbour = pos.relative(direction);
                if (pool.contains(neighbour)) continue;

                BlockState state = level.getBlockState(neighbour);
                if (isMud(state)) {
                    mudFaces++;
                } else if (state.is(Blocks.WATER)) {
                    // Open water feeding into the pool: silt would simply wash away.
                    return false;
                } else if (!state.isAir()) {
                    otherFaces++;
                }
            }
        }
        return mudFaces >= MIN_MUD_FACES && mudFaces >= otherFaces;
    }

    private static void applyToBody(LevelAccessor level, List<BlockPos> body, boolean muddy) {
        Block target = muddy ? ModBlocks.MUDDY_WATER_BLOCK.get() : Blocks.WATER;
        for (BlockPos pos : body) {
            BlockState state = level.getBlockState(pos);
            if (isMuddyWater(state) == muddy) continue;
            level.setBlock(pos, target.defaultBlockState(), Block.UPDATE_ALL);
        }
    }
}
