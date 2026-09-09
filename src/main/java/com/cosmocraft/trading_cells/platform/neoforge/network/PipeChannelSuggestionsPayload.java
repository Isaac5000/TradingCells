package com.cosmocraft.trading_cells.platform.neoforge.network;

import com.cosmocraft.trading_cells.feature.logistics.domain.model.LogisticsResourceType;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record PipeChannelSuggestionsPayload(int containerId, LogisticsResourceType resource, String prefix, List<String> channels, boolean complete)
        implements CustomPacketPayload {
    public static final Type<PipeChannelSuggestionsPayload> PAYLOAD_TYPE = new Type<>(Identifier.fromNamespaceAndPath("trading_cells", "pipe_channel_suggestions"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PipeChannelSuggestionsPayload> STREAM_CODEC = StreamCodec.of((buffer, payload) -> {
        buffer.writeContainerId(payload.containerId()); buffer.writeEnum(payload.resource()); buffer.writeUtf(payload.prefix(), 256);
        buffer.writeVarInt(payload.channels().size()); for (String channel : payload.channels()) { buffer.writeUtf(channel, 256); }
        buffer.writeBoolean(payload.complete());
    }, buffer -> {
        int id = buffer.readContainerId(); var type = buffer.readEnum(LogisticsResourceType.class); String prefix = buffer.readUtf(256);
        int count = buffer.readVarInt(); if (count < 0 || count > 16) { throw new IllegalArgumentException("Too many channel results"); }
        var channels = new ArrayList<String>(count); for (int index = 0; index < count; index++) { channels.add(buffer.readUtf(256)); }
        return new PipeChannelSuggestionsPayload(id, type, prefix, channels, buffer.readBoolean());
    });
    public PipeChannelSuggestionsPayload {
        if (containerId < 0 || resource == null || prefix == null || prefix.length() > 256 || channels.size() > 16
                || channels.stream().anyMatch(channel -> channel == null || channel.length() > 256)) { throw new IllegalArgumentException("Invalid channel results"); }
        channels = List.copyOf(channels);
    }
    @Override
    public Type<? extends CustomPacketPayload> type() { return PAYLOAD_TYPE; }
}
