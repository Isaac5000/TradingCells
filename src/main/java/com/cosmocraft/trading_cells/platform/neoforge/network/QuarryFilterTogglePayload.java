package com.cosmocraft.trading_cells.platform.neoforge.network;

import com.cosmocraft.trading_cells.feature.quarry.adapters.input.QuarryMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record QuarryFilterTogglePayload(int containerId, int revision, Identifier material) implements CustomPacketPayload {
    public static final Type<QuarryFilterTogglePayload> PAYLOAD_TYPE = new Type<>(Identifier.fromNamespaceAndPath("trading_cells", "quarry_filter_toggle"));
    public static final StreamCodec<RegistryFriendlyByteBuf, QuarryFilterTogglePayload> STREAM_CODEC = StreamCodec.of((buffer, payload) -> {
        buffer.writeContainerId(payload.containerId());
        buffer.writeVarInt(payload.revision());
        buffer.writeIdentifier(payload.material());
    }, buffer -> new QuarryFilterTogglePayload(buffer.readContainerId(), buffer.readVarInt(), buffer.readIdentifier()));

    public static void handle(QuarryFilterTogglePayload payload, IPayloadContext context) {
        if (context.player().containerMenu instanceof QuarryMenu menu && menu.containerId == payload.containerId()) {
            menu.toggleMaterial(context.player(), payload.revision(), payload.material());
        }
    }

    @Override public Type<? extends CustomPacketPayload> type() { return PAYLOAD_TYPE; }
}
