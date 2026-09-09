package com.babyneeds;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client-side mirror of what the server told us about each animal. Keyed by entity id, dropped
 * wholesale when the level unloads so a dimension change or disconnect cannot leak stale ids.
 */
@Mod.EventBusSubscriber(modid = BabyNeeds.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientStageCache {

    public record Entry(GrowthStage stage, boolean lying) {
    }

    private static final Map<Integer, Entry> ENTRIES = new ConcurrentHashMap<>();

    private ClientStageCache() {
    }

    public static void accept(StageSyncPayload payload) {
        ENTRIES.put(payload.entityId(), new Entry(payload.stage(), payload.lying()));
    }

    public static Entry get(int entityId) {
        return ENTRIES.get(entityId);
    }

    @SubscribeEvent
    static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            ENTRIES.clear();
        }
    }
}
