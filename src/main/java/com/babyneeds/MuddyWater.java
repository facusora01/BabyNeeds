package com.babyneeds;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The wallow's effect on the water around it. Water was originally its own block here - a second
 * fluid, tinted brown - so a puddle dug into mud would visibly silt up. It looked right on its own,
 * but two distinct fluids never really meet: Minecraft only merges a fluid's surface height and
 * skips shared faces between two blocks of the <em>same</em> fluid, so wherever our fluid touched
 * plain water there was a seam - a crease in the surface and a lip at the edge - no matter how well
 * the colours themselves were blended. Fixing that needs rewriting vanilla's own fluid renderer,
 * which this project has no way to do without a coremod.
 *
 * So the water here is always ordinary {@code Blocks.WATER}, and the wallow shows in two ways that
 * need no second fluid at all. Soil sitting under water near mud slowly becomes mud itself, which is
 * the mechanic and lives here. And the water is tinted brown by how much mud is around it, which is
 * purely how it is drawn and lives in {@link MurkyWaterRendering} - both read the same
 * {@link #mudDensity} so the colour never disagrees with what the ground is actually doing.
 */
public final class MuddyWater {

    /** How far out to look when judging how "in the wallow" a spot is. */
    private static final int MUD_PROXIMITY_RADIUS = 3;
    /**
     * The density at which water is as murky as it will get. Low on purpose: the raw reading is an
     * average over a whole disc, so one muddy bank scores about 0.07 and four score about 0.28.
     * Saturating here means a single block of mud already makes a puddle visibly dirty and a proper
     * wallow is solid brown, which is what the mud was always supposed to mean.
     */
    public static final double MURK_SATURATION = 0.30;

    private MuddyWater() {
    }

    public static boolean isMud(BlockState state) {
        // Packed mud is a building block, not a wallow, so it deliberately does not count.
        return state.is(Blocks.MUD) || state.is(Blocks.MUDDY_MANGROVE_ROOTS);
    }

    /** Soil a wallow can work on. */
    private static boolean isSoakableSoil(BlockState state) {
        return state.is(Blocks.DIRT) || state.is(ModBlocks.GRAZED_DIRT.get())
                || state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.COARSE_DIRT) || state.is(Blocks.ROOTED_DIRT);
    }

    /**
     * How strongly a position reads as "in the wallow" - 0 with nothing muddy anywhere near it, 1
     * surrounded by mud - see {@link MudDensityCalculator}. Read live off the world every time
     * rather than stored anywhere, so it costs nothing to keep in sync with a wallow a player is
     * actively digging out or filling in.
     *
     * Each column is checked at the water's own level <em>and one below it</em>, and the floor is
     * checked first because that is where the mud usually is. Water in a wallow sits on top of the
     * mud, not beside it, so looking only sideways missed the single commonest case there is: a
     * puddle dug into a mud patch read as perfectly clean, which is exactly what it looked like.
     */
    public static double mudDensity(BlockGetter getter, BlockPos center) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        return MudDensityCalculator.weightedDensity(MUD_PROXIMITY_RADIUS, (dx, dz) -> {
            cursor.set(center.getX() + dx, center.getY() - 1, center.getZ() + dz);
            if (isMud(getter.getBlockState(cursor))) return true;

            cursor.set(center.getX() + dx, center.getY(), center.getZ() + dz);
            return isMud(getter.getBlockState(cursor));
        });
    }

    /**
     * Advances the soaking in a small area. Cheap enough to run whenever a block is placed or broken
     * and on a slow sweep around each player and each pig.
     */
    public static void updateArea(Level level, BlockPos center, int radius) {
        if (!Config.MUDDY_WATER.get()) return;
        if (!(level instanceof ServerLevel server)) return;

        WallowData data = WallowData.get(server);
        long gameTime = server.getGameTime();

        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -radius, -radius),
                center.offset(radius, radius, radius))) {
            BlockState state = server.getBlockState(pos);

            if (isSoakableSoil(state)) {
                tickSoaking(server, data, pos.immutable(), gameTime);
            }
        }
    }


    /**
     * Soil sitting under water that is near mud, soaking through until it is mud itself. This is
     * how a wallow beds itself in over time - a puddle dug into a dirt hollow slowly becomes a
     * proper wallow on its own, at {@link Config#SOIL_TO_MUD_TICKS} pace.
     */
    private static void tickSoaking(ServerLevel level, WallowData data, BlockPos pos, long gameTime) {
        // Any mud within range at all is enough to start soil soaking - one block in contact was
        // the whole point of the rule.
        BlockState above = level.getBlockState(pos.above());
        if (!above.is(Blocks.WATER) || mudDensity(level, pos.above()) <= 0.0) {
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
     * Test tooling: soaks every eligible block in range through to mud at once. Two in-game days is
     * forty real minutes of standing around, which makes the feature impossible to check by hand.
     *
     * @return how many blocks were turned
     */
    static int soakNow(ServerLevel level, BlockPos center, int radius) {
        WallowData data = WallowData.get(level);
        int soaked = 0;

        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -radius, -radius),
                center.offset(radius, radius, radius))) {
            BlockState state = level.getBlockState(pos);
            if (!isSoakableSoil(state)) continue;
            if (!level.getBlockState(pos.above()).is(Blocks.WATER)) continue;
            if (mudDensity(level, pos.above()) <= 0.0) continue;

            level.setBlock(pos, Blocks.MUD.defaultBlockState(), Block.UPDATE_ALL);
            data.forget(pos);
            soaked++;
        }
        return soaked;
    }
}
