package com.cosmocraft.trading_cells.platform.neoforge.network;

import com.cosmocraft.trading_cells.feature.trader.adapters.input.AutotraderMenu;
import com.cosmocraft.trading_cells.feature.trader.adapters.input.VillagerTradingCellBlockEntity;
import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.TradingCells;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ResetTradesPayload(int containerId, int knownOffersRevision) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ResetTradesPayload> PAYLOAD_TYPE = new CustomPacketPayload.Type<>(
            Identifier.fromNamespaceAndPath(TradingCells.MOD_ID, "reset_trades")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, ResetTradesPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeContainerId(payload.containerId());
                buffer.writeVarInt(payload.knownOffersRevision());
            },
            buffer -> new ResetTradesPayload(buffer.readContainerId(), buffer.readVarInt())
    );

    public ResetTradesPayload {
        knownOffersRevision = Math.max(0, knownOffersRevision);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return PAYLOAD_TYPE;
    }

    public static void handle(ResetTradesPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().containerMenu instanceof AutotraderMenu menu
                    && menu.containerId == payload.containerId()) {
                menu.resetTradesFromPacket(context.player(), payload.knownOffersRevision());
                return;
            }
            VillagerTradingCellBlockEntity.handleResetTradesRequest(
                    context.player(),
                    payload.containerId(),
                    payload.knownOffersRevision()
            );
        });
    }
}
