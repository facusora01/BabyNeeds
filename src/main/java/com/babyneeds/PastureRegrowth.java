package com.babyneeds;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Keeps grazed ground bare for as long as it is supposed to stay bare.
 *
 * This is the piece the pasture was missing, and without it the whole degradation chain was dead
 * on arrival. Vanilla grass creeps back onto plain dirt within a couple of minutes, and the mod
 * reads grass underfoot as "the pasture recovered" and throws the block's trampling record away.
 * So the record could never survive the day of grace that has to pass before soil can be lost, and
 * coarse dirt was unreachable no matter how many animals stood on the same square.
 *
 * There is no event to cancel - vanilla spreads grass from a random tick on the neighbouring grass
 * block, with no hook of any kind - so the turf is simply taken off again while the ground is still
 * in its recovery window. The mod is already keeping a list of exactly which blocks those are, so
 * this costs one block lookup per tracked block per sweep and touches nothing else in the world.
 *
 * The window closing is what makes rotation work: move the herd on and the grass comes back by
 * itself; leave them there and the tramples pile up on ground that is not allowed to heal.
 */
@Mod.EventBusSubscriber(modid = BabyNeeds.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class PastureRegrowth {

    /**
     * How often the sweep runs. Grass spread is a random tick, so a block only rarely turns during
     * any one window; a couple of seconds is short enough that a blade that does come up early is
     * gone again before it reads as anything but a flicker.
     */
    private static final int SWEEP_TICKS = 40;

    private PastureRegrowth() {
    }

    @SubscribeEvent
    static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.level instanceof ServerLevel level)) return;
        if (!Config.PASTURE_DEGRADATION.get()) return;
        if (level.getGameTime() % SWEEP_TICKS != 0) return;

        sweep(level);
    }

    private static void sweep(ServerLevel level) {
        PastureData pasture = PastureData.get(level);
        long gameTime = level.getGameTime();
        int hold = Config.PASTURE_REGROWTH_DELAY_TICKS.get();
        if (hold <= 0) return;

        for (long packed : pasture.trackedPositions()) {
            BlockPos pos = BlockPos.of(packed);
            // Ground in an unloaded chunk is nobody's problem yet, and asking for its state would
            // drag the chunk back in.
            if (!level.isLoaded(pos)) continue;
            if (pasture.bareTicks(pos, gameTime) >= hold) continue;

            BlockState state = level.getBlockState(pos);
            if (!state.is(Blocks.GRASS_BLOCK)) continue;

            level.setBlockAndUpdate(pos, Blocks.DIRT.defaultBlockState());
        }
    }
}
