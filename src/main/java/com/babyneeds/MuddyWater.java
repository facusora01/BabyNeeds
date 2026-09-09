package com.babyneeds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Turns clean water into a wallow. Water dug into mud goes brown, and goes clear again if the mud
 * around it is taken away, so a player builds a pig wallow the obvious way - dig a hole in mud,
 * pour a bucket in - and it simply looks right.
 */
public final class MuddyWater {

    /** Mud faces needed for the water to silt up: the floor plus a wall, or two walls. */
    private static final int MUD_CONTACT_NEEDED = 2;

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
     * Re-evaluates every water source in a small area and converts it in whichever direction the
     * mud around it now calls for. Cheap enough to run when a block is placed or broken, and every
     * few seconds around a pig.
     */
    public static void updateArea(Level level, BlockPos center, int radius) {
        if (!Config.MUDDY_WATER.get()) return;

        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -radius, -radius),
                center.offset(radius, radius, radius))) {
            BlockState state = level.getBlockState(pos);
            boolean muddy = isMuddyWater(state);
            boolean clean = state.is(Blocks.WATER);
            if (!muddy && !clean) continue;
            // Only still water silts up; a flowing edge is left to sort itself out.
            if (!state.getFluidState().isSource()) continue;

            boolean shouldBeMuddy = mudContact(level, pos) >= MUD_CONTACT_NEEDED
                    && !touchesCleanWater(level, pos);
            if (shouldBeMuddy == muddy) continue;

            Block target = shouldBeMuddy ? ModBlocks.MUDDY_WATER_BLOCK.get() : Blocks.WATER;
            level.setBlock(pos.immutable(), target.defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    /**
     * Silt only stays put in a puddle of its own. Open water dilutes it, so a wallow dug into the
     * edge of a pond runs clear instead of leaving half a pool brown against half a pool blue with
     * a hard seam down the middle - which is both wrong to look at and wrong about how silt works.
     * The rule the player learns is simple: a wallow is a hole in the mud, sealed off from the lake.
     */
    private static boolean touchesCleanWater(LevelAccessor level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            if (level.getBlockState(pos.relative(direction)).is(Blocks.WATER)) {
                return true;
            }
        }
        return false;
    }

    private static int mudContact(LevelAccessor level, BlockPos pos) {
        int contact = isMud(level.getBlockState(pos.below())) ? 1 : 0;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (isMud(level.getBlockState(pos.relative(direction)))) {
                contact++;
            }
        }
        return contact;
    }
}
