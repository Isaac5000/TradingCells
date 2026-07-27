package com.cosmocraft.trading_cells.platform.neoforge.network;

import com.cosmocraft.trading_cells.feature.trader.adapters.input.VillagerTradingCellBlockEntity;
import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.TradingCells;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server-authoritative selection for the manual Villager Trading Cell. */
public record SelectTradingCellOfferPayload(
        int containerId,
        int selectedOfferIndex,
        int knownOffersRevision
) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SelectTradingCellOfferPayload> PAYLOAD_TYPE = new CustomPacketPayload.Type<>(
            Identifier.fromNamespaceAndPath(TradingCells.MOD_ID, "select_trading_cell_offer")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, SelectTradingCellOfferPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeContainerId(payload.containerId());
                buffer.writeVarInt(payload.selectedOfferIndex());
                buffer.writeVarInt(payload.knownOffersRevision());
            },
            buffer -> new SelectTradingCellOfferPayload(
                    buffer.readContainerId(),
                    buffer.readVarInt(),
                    buffer.readVarInt()
            )
    );

    public SelectTradingCellOfferPayload {
        selectedOfferIndex = Math.max(0, selectedOfferIndex);
        knownOffersRevision = Math.max(0, knownOffersRevision);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return PAYLOAD_TYPE;
    }

    public static void handle(SelectTradingCellOfferPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> VillagerTradingCellBlockEntity.handleSelectOfferRequest(
                context.player(),
                payload.containerId(),
                payload.selectedOfferIndex(),
                payload.knownOffersRevision()
        ));
    }
}
