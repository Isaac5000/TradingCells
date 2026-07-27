package com.cosmocraft.trading_cells.platform.neoforge.network;

import com.cosmocraft.trading_cells.feature.trader.adapters.input.AutotraderMenu;
import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.TradingCells;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server-authoritative offer selection for the Autotrader. */
public record SelectAutotraderOfferPayload(
        int containerId,
        int selectedOfferIndex,
        int knownOffersRevision
) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SelectAutotraderOfferPayload> PAYLOAD_TYPE = new CustomPacketPayload.Type<>(
            Identifier.fromNamespaceAndPath(TradingCells.MOD_ID, "select_autotrader_offer")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, SelectAutotraderOfferPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeContainerId(payload.containerId());
                buffer.writeVarInt(payload.selectedOfferIndex());
                buffer.writeVarInt(payload.knownOffersRevision());
            },
            buffer -> new SelectAutotraderOfferPayload(
                    buffer.readContainerId(),
                    buffer.readVarInt(),
                    buffer.readVarInt()
            )
    );

    public SelectAutotraderOfferPayload {
        selectedOfferIndex = Math.max(0, selectedOfferIndex);
        knownOffersRevision = Math.max(0, knownOffersRevision);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return PAYLOAD_TYPE;
    }

    public static void handle(SelectAutotraderOfferPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().containerMenu instanceof AutotraderMenu menu
                    && menu.containerId == payload.containerId()
                    && menu.stillValid(context.player())) {
                menu.selectOfferFromPacket(
                        context.player(),
                        payload.selectedOfferIndex(),
                        payload.knownOffersRevision()
                );
            }
        });
    }
}
