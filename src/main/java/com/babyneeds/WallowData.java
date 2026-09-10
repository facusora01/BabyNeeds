package com.babyneeds;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Per-level memory of changes that are part-way through happening: water silting up, silt settling
 * back out, and soil soaking into mud under a wallow.
 *
 * None of these are instant, and none of them can be, because the whole point is that a wallow forms
 * over time - the more mud is touching the water, the faster, but never at once. That needs a clock
 * per block, and blocks have nowhere to keep one, so it lives here.
 *
 * The clock is a timestamp rather than a counter, so it keeps running while nobody is looking: come
 * back to a hole you flooded ten minutes ago and it has already gone brown, without the mod having
 * spent a single tick on it in the meantime.
 */
public class WallowData extends SavedData {

    private static final String FILE_ID = "babyneeds_wallow";
    /** Hard ceiling so a very large or very old world cannot grow this without bound. */
    private static final int MAX_ENTRIES = 4096;
    /** Never drop a clock before the change it is timing could possibly have finished. */
    private static final long MINIMUM_STALE_TICKS = 24000L;

    /**
     * The one slow change a wallow drives. Water itself used to silt up and clear again as a
     * distinct fluid, which is why this is an enum of one rather than a plain flag - the timestamp
     * bookkeeping below was built to track several changes at once, and soaking still needs exactly
     * that: a start time, a last-seen time, and a way to tell it apart if anything else is added.
     */
    public enum Change {
        /** Soil under water near mud, soaking through into mud. */
        SOAKING
    }

    private static class Pending {
        long since;
        Change change;
        long lastSeen;

        Pending(long since, Change change) {
            this.since = since;
            this.change = change;
            this.lastSeen = since;
        }
    }

    private final Map<Long, Pending> pending = new HashMap<>();

    public static WallowData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(WallowData::load, WallowData::new, FILE_ID);
    }

    /**
     * How long this block has been undergoing this change, starting the clock if it is new or if the
     * block switched to a different change (mud placed then removed, say).
     */
    public long elapsed(BlockPos pos, Change change, long gameTime) {
        long key = pos.asLong();
        Pending entry = pending.get(key);
        if (entry == null || entry.change != change) {
            entry = new Pending(gameTime, change);
            pending.put(key, entry);
            prune(gameTime);
        }
        entry.lastSeen = gameTime;
        setDirty();
        return Math.max(0L, gameTime - entry.since);
    }

    /** The change finished, or the conditions for it went away. Either way the clock stops. */
    public void forget(BlockPos pos) {
        if (pending.remove(pos.asLong()) != null) {
            setDirty();
        }
    }

    /**
     * How long a clock may go unvisited before it is assumed abandoned. It has to outlast the change
     * it is timing, and by a wide margin, or the timer can never finish: soil takes two in-game days
     * to soak through, so a flat one-day cut-off threw the entry away every time the player wandered
     * off - and the clock silently restarted from zero when they came back. That is why a wallow
     * never actually bedded itself in.
     */
    private static long staleTicks() {
        return Math.max(MINIMUM_STALE_TICKS, Config.SOIL_TO_MUD_TICKS.get() * 2L);
    }

    private void prune(long gameTime) {
        long stale = staleTicks();
        pending.entrySet().removeIf(entry -> gameTime - entry.getValue().lastSeen > stale);
        if (pending.size() <= MAX_ENTRIES) return;

        List<Map.Entry<Long, Pending>> byAge = new ArrayList<>(pending.entrySet());
        byAge.sort((a, b) -> Long.compare(a.getValue().lastSeen, b.getValue().lastSeen));
        int excess = pending.size() - MAX_ENTRIES;
        for (int i = 0; i < excess; i++) {
            pending.remove(byAge.get(i).getKey());
        }
    }

    // ------------------------------------------------------------------ persistence

    public static WallowData load(CompoundTag tag) {
        WallowData data = new WallowData();
        ListTag list = tag.getList("Pending", Tag.TAG_COMPOUND);
        Change[] changes = Change.values();

        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            int ordinal = entry.getInt("Change");
            if (ordinal < 0 || ordinal >= changes.length) continue;

            Pending value = new Pending(entry.getLong("Since"), changes[ordinal]);
            value.lastSeen = entry.getLong("Seen");
            data.pending.put(entry.getLong("Pos"), value);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        pending.forEach((pos, entry) -> {
            CompoundTag saved = new CompoundTag();
            saved.putLong("Pos", pos);
            saved.putLong("Since", entry.since);
            saved.putInt("Change", entry.change.ordinal());
            saved.putLong("Seen", entry.lastSeen);
            list.add(saved);
        });
        tag.put("Pending", list);
        return tag;
    }
}
