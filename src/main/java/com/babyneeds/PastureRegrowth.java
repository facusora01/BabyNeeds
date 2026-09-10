package com.babyneeds;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Hands grazed ground back to vanilla once it has served its time bare.
 *
 * Grazing leaves {@link ModBlocks#GRAZED_DIRT}, which vanilla will not spread grass onto - it picks
 * its targets with a hard check against {@code Blocks.DIRT}, so a lookalike that is not that block
 * is simply never chosen. All this has to do, then, is turn the ground back into ordinary dirt when
 * its recovery window is up, and vanilla takes it from there at its own pace.
 *
 * It is worth saying why it is not the other way round. The first version left plain dirt and took
 * the turf off again whenever vanilla put it back. That works on paper and is unusable in practice:
 * grass returns within seconds, so the field flickered between grass and dirt continuously. Ground
 * that was never a valid target has nothing to flicker about.
 *
 * The window is what makes rotation mean something: move the herd on and the pasture comes back by
 * itself; leave them there and the tramples pile up on ground that cannot heal underneath them.
 */
@Mod.EventBusSubscriber(modid = BabyNeeds.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class PastureRegrowth {

    /** Nothing here is urgent any more - the ground is only waiting to be released. */
    private static final int SWEEP_TICKS = 100;

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
            if (pasture.bareTicks(pos, gameTime) < hold) continue;

            BlockState state = level.getBlockState(pos);
            if (!state.is(ModBlocks.GRAZED_DIRT.get())) continue;

            level.setBlockAndUpdate(pos, Blocks.DIRT.defaultBlockState());
        }
    }
}
