package com.cosmocraft.trading_cells.platform.neoforge.network;

import com.cosmocraft.trading_cells.feature.quarry.adapters.input.QuarryMenu;
import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.TradingCells;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record RequestQuarryCatalogPayload(int containerId) implements CustomPacketPayload {
    public static final Type<RequestQuarryCatalogPayload> PAYLOAD_TYPE = new Type<>(Identifier.fromNamespaceAndPath(
            TradingCells.MOD_ID,
            "request_quarry_catalog"
    ));
    public static final StreamCodec<RegistryFriendlyByteBuf, RequestQuarryCatalogPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> buffer.writeContainerId(payload.containerId()),
            buffer -> new RequestQuarryCatalogPayload(buffer.readContainerId())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return PAYLOAD_TYPE;
    }

    public static void handle(RequestQuarryCatalogPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer serverPlayer)
                    || !(serverPlayer.containerMenu instanceof QuarryMenu menu)
                    || menu.containerId != payload.containerId()) {
                return;
            }
            PacketDistributor.sendToPlayer(serverPlayer, QuarryCatalogSyncPayload.from(menu));
        });
    }
}
