package com.cosmocraft.trading_cells.platform.neoforge.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record PipeMenuSyncPayload(int containerId, long revision, long upgradeGeneration, CompoundTag configuration)
        implements CustomPacketPayload {
    public static final Type<PipeMenuSyncPayload> PAYLOAD_TYPE = new Type<>(Identifier.fromNamespaceAndPath("trading_cells", "pipe_menu_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PipeMenuSyncPayload> STREAM_CODEC = StreamCodec.of((buffer, payload) -> {
        buffer.writeContainerId(payload.containerId());
        buffer.writeVarLong(payload.revision());
        buffer.writeVarLong(payload.upgradeGeneration());
        buffer.writeNbt(payload.configuration());
    }, buffer -> new PipeMenuSyncPayload(buffer.readContainerId(), buffer.readVarLong(), buffer.readVarLong(), buffer.readNbt()));

    public PipeMenuSyncPayload {
        if (containerId < 0 || revision < 0 || upgradeGeneration < 0 || configuration == null) { throw new IllegalArgumentException("Invalid pipe menu state"); }
        configuration = configuration.copy();
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return PAYLOAD_TYPE; }
}
