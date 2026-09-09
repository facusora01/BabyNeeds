package com.babyneeds;

import net.minecraft.network.FriendlyByteBuf;

/**
 * Growth stage and nursing pose for one animal. Persistent data never reaches the client, so the
 * renderer would otherwise only know vanilla's baby/adult flag and could not draw four sizes.
 */
public class StageSyncPayload {
    private final int entityId;
    private final int stageIndex;
    private final boolean lying;

    public StageSyncPayload(int entityId, int stageIndex, boolean lying) {
        this.entityId = entityId;
        this.stageIndex = stageIndex;
        this.lying = lying;
    }

    public StageSyncPayload(FriendlyByteBuf buf) {
        this.entityId = buf.readVarInt();
        this.stageIndex = buf.readVarInt();
        this.lying = buf.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeVarInt(this.entityId);
        buf.writeVarInt(this.stageIndex);
        buf.writeBoolean(this.lying);
    }

    public int entityId() {
        return entityId;
    }

    public GrowthStage stage() {
        return GrowthStage.byIndex(stageIndex);
    }

    public boolean lying() {
        return lying;
    }
}
