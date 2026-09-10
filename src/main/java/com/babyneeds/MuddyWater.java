package com.babyneeds;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
 * So the water here is always ordinary {@code Blocks.WATER}. What a wallow actually does to it is
 * two things that do not need a second fluid at all: soil sitting under water near mud slowly
 * becomes mud itself, and both an ambient haze and a swimmer's blurred vision are read live off how
 * much mud is nearby, rather than painted onto the water as a different block.
 */
public final class MuddyWater {

    /** How far out to look when judging how "in the wallow" a spot is. */
    private static final int MUD_PROXIMITY_RADIUS = 3;
    /** Some mud has to genuinely be near for any murky effect to kick in at all. */
    private static final double MURK_THRESHOLD = 0.08;
    /** Chance per sweep tick that a water block worth it spawns a haze particle. */
    private static final double PARTICLE_CHANCE_AT_FULL_DENSITY = 0.25;
    /**
     * How long the darkness effect lasts before it needs refreshing. Reapplied every server tick
     * while a player stays submerged near mud, so this only has to outlast a single tick; the
     * margin is what keeps it from visibly flickering if a tick is ever skipped.
     */
    private static final int SUBMERGED_DARKNESS_TICKS = 20;

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
     */
    public static double mudDensity(BlockGetter getter, BlockPos center) {
        return MudDensityCalculator.weightedDensity(MUD_PROXIMITY_RADIUS,
                (dx, dz) -> isMud(getter.getBlockState(center.offset(dx, 0, dz))));
    }

    /**
     * Advances the slow changes in a small area, and throws a little haze into the water while it
     * is at it. Cheap enough to run whenever a block is placed or broken and on a slow sweep around
     * each player and each pig.
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
                ambientHaze(server, pos.immutable());
            } else if (isSoakableSoil(state)) {
                tickSoaking(server, data, pos.immutable(), gameTime);
            }
        }
    }

    /**
     * Suspended mud, thrown into water near a wallow every so often. Purely a look: it tells the
     * player the water is murky without needing the water itself to be a different block, so there
     * is nothing here for a second fluid to fail to connect to.
     */
    private static void ambientHaze(ServerLevel level, BlockPos pos) {
        if (!level.getFluidState(pos).isSource()) return;

        double density = mudDensity(level, pos);
        if (density < MURK_THRESHOLD) return;

        double chance = PARTICLE_CHANCE_AT_FULL_DENSITY * MudDensityCalculator.ease(density);
        if (level.getRandom().nextDouble() >= chance) return;

        level.sendParticles(new net.minecraft.core.particles.BlockParticleOption(
                        net.minecraft.core.particles.ParticleTypes.BLOCK, Blocks.MUD.defaultBlockState()),
                pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5, 2, 0.3, 0.15, 0.3, 0.01);
    }

    /**
     * Soil sitting under water that is near mud, soaking through until it is mud itself. This is
     * how a wallow beds itself in over time - a puddle dug into a dirt hollow slowly becomes a
     * proper wallow on its own, at {@link Config#SOIL_TO_MUD_TICKS} pace.
     */
    private static void tickSoaking(ServerLevel level, WallowData data, BlockPos pos, long gameTime) {
        BlockState above = level.getBlockState(pos.above());
        if (!above.is(Blocks.WATER) || mudDensity(level, pos.above()) < MURK_THRESHOLD) {
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
     * A player's vision fogging up with their head under murky water. Checked every server tick -
     * unlike the area sweep, this has to feel immediate the moment someone dips under, not catch up
     * a couple of seconds later - but it costs nothing when nobody is even in water, since that
     * check short-circuits before any block is looked at.
     */
    public static void tickSubmersion(ServerPlayer player) {
        if (!Config.MUDDY_WATER.get()) return;
        if (player.getEyeInFluidType() != net.minecraftforge.common.ForgeMod.WATER_TYPE.get()) return;

        BlockPos eyePos = BlockPos.containing(player.getEyePosition());
        if (mudDensity(player.serverLevel(), eyePos) < MURK_THRESHOLD) return;

        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, SUBMERGED_DARKNESS_TICKS,
                0, true, false, false));
    }
}
