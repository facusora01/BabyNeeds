package com.babyneeds;

import net.minecraft.network.FriendlyByteBuf;

/**
 * Growth stage, body pose and how filthy the animal is. Persistent data never reaches the client,
 * so without this the renderer would only know vanilla's baby/adult flag - it could not draw four
 * sizes, could not animate a sow lying down, and could not tell a clean pig from one that has been
 * rolling in a wallow.
 *
 * Only the target pose is sent, never a frame of animation: the client eases towards it on its own,
 * so a whole lie-down costs one packet.
 */
public class StageSyncPayload {
    private final int entityId;
    private final int stageIndex;
    private final int poseIndex;
    private final int muddiness;

    public StageSyncPayload(int entityId, int stageIndex, int poseIndex, double muddiness) {
        this.entityId = entityId;
        this.stageIndex = stageIndex;
        this.poseIndex = poseIndex;
        this.muddiness = (int) Math.round(Math.max(0.0, Math.min(1.0, muddiness)) * 100.0);
    }

    public StageSyncPayload(FriendlyByteBuf buf) {
        this.entityId = buf.readVarInt();
        this.stageIndex = buf.readVarInt();
        this.poseIndex = buf.readVarInt();
        this.muddiness = buf.readUnsignedByte();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeVarInt(this.entityId);
        buf.writeVarInt(this.stageIndex);
        buf.writeVarInt(this.poseIndex);
        buf.writeByte(this.muddiness);
    }

    public int entityId() {
        return entityId;
    }

    public GrowthStage stage() {
        return GrowthStage.byIndex(stageIndex);
    }

    public PigPose pose() {
        return PigPose.byIndex(poseIndex);
    }

    /** 0 clean, 1 caked in mud. */
    public float muddiness() {
        return muddiness / 100.0f;
    }
}
