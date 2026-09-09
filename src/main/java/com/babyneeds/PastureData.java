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
 * Per-level memory of bare ground: when the turf came off a block, and how much the herd has stood
 * on it since. Without this, "overgrazed" could only mean "eaten right now"; with it, soil loss
 * takes sustained pressure on the same spot, which is what the player can actually avoid by
 * rotating animals or giving them room.
 *
 * A plot is forgotten as soon as grass grows back over it or the soil is finally lost, so the map
 * only ever holds ground that is currently bare and being walked on.
 */
public class PastureData extends SavedData {

    private static final String FILE_ID = "babyneeds_pasture";
    /** Hard ceiling so a very large or very old world cannot grow this without bound. */
    private static final int MAX_PLOTS = 4096;
    private static final long STALE_TICKS = 48000L; // two in-game days untouched

    private static class Plot {
        long bareSince;
        int tramples;
        long lastTouched;

        Plot(long bareSince, int tramples, long lastTouched) {
            this.bareSince = bareSince;
            this.tramples = tramples;
            this.lastTouched = lastTouched;
        }
    }

    private final Map<Long, Plot> plots = new HashMap<>();

    public static PastureData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(PastureData::load, PastureData::new, FILE_ID);
    }

    /** The turf just came off this block: the recovery clock starts now. */
    public void markBare(BlockPos pos, long gameTime) {
        plots.put(pos.asLong(), new Plot(gameTime, 0, gameTime));
        prune(gameTime);
        setDirty();
    }

    /** An animal is standing here. Returns how many times that has been recorded on this block. */
    public int trample(BlockPos pos, long gameTime) {
        Plot plot = plots.get(pos.asLong());
        if (plot == null) {
            // Bare ground the mod did not see being grazed (a dirt path, a player's dig, an old
            // world). It still counts, its clock simply starts the first time it is walked on.
            plot = new Plot(gameTime, 0, gameTime);
            plots.put(pos.asLong(), plot);
            prune(gameTime);
        }
        plot.tramples++;
        plot.lastTouched = gameTime;
        setDirty();
        return plot.tramples;
    }

    /** Ticks this block has been bare, or 0 if it is not being tracked. */
    public long bareTicks(BlockPos pos, long gameTime) {
        Plot plot = plots.get(pos.asLong());
        return plot == null ? 0L : Math.max(0L, gameTime - plot.bareSince);
    }

    /** Grass grew back, or the soil was lost: either way there is nothing left to track. */
    public void forget(BlockPos pos) {
        if (plots.remove(pos.asLong()) != null) {
            setDirty();
        }
    }

    private void prune(long gameTime) {
        plots.entrySet().removeIf(entry -> gameTime - entry.getValue().lastTouched > STALE_TICKS);
        if (plots.size() <= MAX_PLOTS) return;

        // Still over the ceiling: drop the least recently visited ground first, since that is the
        // pasture the herd has already abandoned.
        List<Map.Entry<Long, Plot>> byAge = new ArrayList<>(plots.entrySet());
        byAge.sort((a, b) -> Long.compare(a.getValue().lastTouched, b.getValue().lastTouched));
        int excess = plots.size() - MAX_PLOTS;
        for (int i = 0; i < excess; i++) {
            plots.remove(byAge.get(i).getKey());
        }
    }

    // ------------------------------------------------------------------ persistence

    public static PastureData load(CompoundTag tag) {
        PastureData data = new PastureData();
        ListTag list = tag.getList("Plots", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            data.plots.put(entry.getLong("Pos"), new Plot(
                    entry.getLong("Since"),
                    entry.getInt("Tramples"),
                    entry.getLong("Touched")));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        plots.forEach((pos, plot) -> {
            CompoundTag entry = new CompoundTag();
            entry.putLong("Pos", pos);
            entry.putLong("Since", plot.bareSince);
            entry.putInt("Tramples", plot.tramples);
            entry.putLong("Touched", plot.lastTouched);
            list.add(entry);
        });
        tag.put("Plots", list);
        return tag;
    }
}
