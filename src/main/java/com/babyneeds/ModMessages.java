package com.babyneeds;

import java.util.Optional;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModMessages {
    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(BabyNeeds.MODID, "messages"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;

    public static void register() {
        INSTANCE.registerMessage(
                packetId++,
                StageSyncPayload.class,
                (msg, buf) -> msg.toBytes(buf),
                StageSyncPayload::new,
                (msg, ctx) -> handleStageSync(msg, ctx.get()),
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
    }

    private static void handleStageSync(StageSyncPayload payload, NetworkEvent.Context context) {
        context.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientStageCache.accept(payload))
        );
        context.setPacketHandled(true);
    }

    /** Tells everyone who can see this animal what stage and pose it is in. */
    public static void syncToTracking(Entity animal) {
        if (animal.level().isClientSide()) return;
        INSTANCE.send(PacketDistributor.TRACKING_ENTITY.with(() -> animal), payloadFor(animal));
    }

    /** Catches a player up when an animal enters their tracking range. */
    public static void syncToPlayer(Entity animal, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), payloadFor(animal));
    }

    private static StageSyncPayload payloadFor(Entity animal) {
        return new StageSyncPayload(animal.getId(),
                AnimalState.stage(animal).index(),
                AnimalState.isLying(animal));
    }
}
