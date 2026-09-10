package com.babyneeds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * How a wallow forms, and how it spreads. Three slow changes, all of them driven by the same idea:
 * mud and water in contact work on each other over time.
 *
 * <ul>
 *   <li><b>Water silts up.</b> A single block of mud touching the water is enough to start it going
 *       brown. More mud touching it only makes it faster - water walled in on every side turns in a
 *       fraction of the time, but still not instantly, because a wallow that appears the moment you
 *       pour the bucket reads as a block swap rather than as something settling.</li>
 *   <li><b>Silt settles out.</b> Muddy water cut off from every scrap of mud runs clear again, but
 *       water in the middle of a wallow is held by the silt around it, so a pool does not clear from
 *       the inside out.</li>
 *   <li><b>Soil soaks through.</b> Dirt or grass under muddy water eventually becomes mud itself,
 *       so a wallow slowly beds itself in - and every new mud block makes the water above silt
 *       faster. A puddle in a dirt hollow becomes a proper wallow on its own.</li>
 * </ul>
 */
public final class MuddyWater {

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

    /** Soil a wallow can work on. */
    private static boolean isSoakableSoil(BlockState state) {
        return state.is(Blocks.DIRT) || state.is(ModBlocks.GRAZED_DIRT.get())
                || state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.COARSE_DIRT) || state.is(Blocks.ROOTED_DIRT);
    }

    /**
     * Advances every slow change in a small area. Cheap enough to run whenever a block is placed or
     * broken, every few seconds around a pig, and on a slow sweep around each player.
     */
    public static void updateArea(Level level, BlockPos center, int radius) {
        if (!Config.MUDDY_WATER.get()) return;
        if (!(level instanceof ServerLevel server)) return;

        WallowData data = WallowData.get(server);
        long gameTime = server.getGameTime();

        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -radius, -radius),
                center.offset(radius, radius, radius))) {
            BlockState state = server.getBlockState(pos);

            if (state.is(Blocks.WATER)) {
                tickSilting(server, data, pos.immutable(), gameTime);
            } else if (isMuddyWater(state)) {
                tickClearing(server, data, pos.immutable(), gameTime);
            } else if (isSoakableSoil(state)) {
                tickSoaking(server, data, pos.immutable(), gameTime);
            }
        }
    }

    /**
     * Clean water going brown. One mud face is enough; each extra one divides the time, so a block
     * boxed in by mud silts up four times faster than one with a single muddy bank.
     */
    private static void tickSilting(ServerLevel level, WallowData data, BlockPos pos, long gameTime) {
        if (!level.getFluidState(pos).isSource()) return;

        int mudFaces = countFaces(level, pos, true);
        if (mudFaces == 0) {
            data.forget(pos);
            return;
        }

        long required = Math.max(1L, Config.SILTING_TICKS.get() / mudFaces);
        if (data.elapsed(pos, WallowData.Change.SILTING, gameTime) < required) return;

        level.setBlock(pos, ModBlocks.MUDDY_WATER_BLOCK.get().defaultBlockState(), Block.UPDATE_ALL);
        data.forget(pos);
    }

    /**
     * Silt settling out of water that has nothing muddy left against it. Muddy neighbours count as
     * well as mud here, and only here: a block in the middle of a pool is held brown by the pool
     * around it, so a wallow does not clear itself from the inside while its banks are still mud.
     */
    private static void tickClearing(ServerLevel level, WallowData data, BlockPos pos, long gameTime) {
        if (!level.getFluidState(pos).isSource()) return;

        if (countFaces(level, pos, false) > 0) {
            data.forget(pos);
            return;
        }

        if (data.elapsed(pos, WallowData.Change.CLEARING, gameTime) < Config.CLEARING_TICKS.get()) {
            return;
        }
        level.setBlock(pos, Blocks.WATER.defaultBlockState(), Block.UPDATE_ALL);
        data.forget(pos);
    }

    /** Soil sitting under a wallow, soaking through until it is mud itself. */
    private static void tickSoaking(ServerLevel level, WallowData data, BlockPos pos, long gameTime) {
        if (!isMuddyWater(level.getBlockState(pos.above()))) {
            data.forget(pos);
            return;
        }

        if (data.elapsed(pos, WallowData.Change.SOAKING, gameTime) < Config.SOIL_TO_MUD_TICKS.get()) {
            return;
        }
        level.setBlock(pos, Blocks.MUD.defaultBlockState(), Block.UPDATE_ALL);
        data.forget(pos);
    }

    /**
     * @param mudOnly true to count only real mud, which is what starts a change; false to count
     *                muddy water as well, which is what sustains one
     */
    private static int countFaces(ServerLevel level, BlockPos pos, boolean mudOnly) {
        int faces = 0;
        for (Direction direction : Direction.values()) {
            BlockState neighbour = level.getBlockState(pos.relative(direction));
            if (mudOnly ? isMud(neighbour) : isWallow(neighbour)) {
                faces++;
            }
        }
        return faces;
    }
}
