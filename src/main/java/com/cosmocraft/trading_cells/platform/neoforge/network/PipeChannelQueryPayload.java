package com.cosmocraft.trading_cells.platform.neoforge.network;

import com.cosmocraft.trading_cells.feature.logistics.adapters.input.PipeConfigurationMenu;
import com.cosmocraft.trading_cells.feature.logistics.domain.model.LogisticsResourceType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PipeChannelQueryPayload(int containerId, LogisticsResourceType resource, String prefix, boolean active) implements CustomPacketPayload {
    public static final Type<PipeChannelQueryPayload> PAYLOAD_TYPE = new Type<>(Identifier.fromNamespaceAndPath("trading_cells", "pipe_channel_query"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PipeChannelQueryPayload> STREAM_CODEC = StreamCodec.of((buffer, payload) -> {
        buffer.writeContainerId(payload.containerId()); buffer.writeEnum(payload.resource()); buffer.writeUtf(payload.prefix(), 256); buffer.writeBoolean(payload.active());
    }, buffer -> new PipeChannelQueryPayload(buffer.readContainerId(), buffer.readEnum(LogisticsResourceType.class), buffer.readUtf(256), buffer.readBoolean()));
    public PipeChannelQueryPayload {
        if (containerId < 0 || resource == null || prefix == null || prefix.length() > 256) { throw new IllegalArgumentException("Invalid channel query"); }
    }
    @Override
    public Type<? extends CustomPacketPayload> type() { return PAYLOAD_TYPE; }
    public static void handle(PipeChannelQueryPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player && player.containerMenu instanceof PipeConfigurationMenu menu
                    && menu.containerId == payload.containerId() && menu.stillValid(player)) {
                menu.queryChannels(payload.resource(), payload.prefix(), payload.active());
            }
        });
    }
}
