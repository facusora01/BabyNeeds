package com.babyneeds;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * When the slow changes of a wallow get looked at. Nothing here scans the world at large: the checks
 * run where a player just changed a block, and on a slow sweep around each player and each pig, which
 * between them cover every way a wallow is actually built or disturbed.
 *
 * Because {@link WallowData} stores timestamps rather than counters, an infrequent look is enough -
 * a hole flooded ten minutes ago has already made its mud by the time anyone comes back to it. How
 * the water looks needs no tick at all: that is read straight off the world as it is drawn, in
 * {@link MurkyWaterRendering}.
 */
@Mod.EventBusSubscriber(modid = BabyNeeds.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class WallowEvents {

    /** How far around a change or a player the wallow logic looks. */
    private static final int UPDATE_RADIUS = 5;
    /** Ticks between sweeps around players. */
    private static final int SWEEP_INTERVAL = 40;

    @SubscribeEvent
    static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        scheduleUpdate(event.getLevel(), event.getPos());
    }

    @SubscribeEvent
    static void onBlockBroken(BlockEvent.BreakEvent event) {
        scheduleUpdate(event.getLevel(), event.getPos());
    }

    /** Keeps a wallow the player is standing over moving along while they watch it. */
    @SubscribeEvent
    static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!Config.MUDDY_WATER.get()) return;

        MinecraftServer server = event.getServer();
        if (server.getTickCount() % SWEEP_INTERVAL != 0) return;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            MuddyWater.updateArea(player.serverLevel(), player.blockPosition(), UPDATE_RADIUS);
        }
    }

    /**
     * Deferred by one tick on purpose: during these events the block being placed or broken is not
     * settled yet, so looking at the world now would judge the wallow on a state that is about to
     * change.
     */
    private static void scheduleUpdate(LevelAccessor levelAccessor, BlockPos pos) {
        if (!Config.MUDDY_WATER.get()) return;
        if (!(levelAccessor instanceof ServerLevel level)) return;

        MinecraftServer server = level.getServer();
        server.tell(new TickTask(server.getTickCount() + 1,
                () -> MuddyWater.updateArea(level, pos, UPDATE_RADIUS)));
    }
}
