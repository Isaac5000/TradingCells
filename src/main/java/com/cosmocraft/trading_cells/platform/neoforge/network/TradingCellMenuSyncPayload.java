package com.cosmocraft.trading_cells.platform.neoforge.network;

import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.TradingCells;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.item.trading.MerchantOffers;

/** Complete client snapshot for the custom Villager Trading Cell menu. */
public record TradingCellMenuSyncPayload(
        int containerId,
        MerchantOffers offers,
        VillagerData villagerData,
        int villagerLevel,
        int villagerXp,
        int storedExperience,
        int selectedOfferIndex,
        boolean showProgress,
        boolean canRestock,
        boolean canResetTrades,
        int offersRevision
) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<TradingCellMenuSyncPayload> PAYLOAD_TYPE = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(
            TradingCells.MOD_ID,
            "trading_cell_menu_sync"
    ));

    public static final StreamCodec<RegistryFriendlyByteBuf, TradingCellMenuSyncPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeContainerId(payload.containerId());
                MerchantOffers.STREAM_CODEC.encode(buffer, payload.offers());
                VillagerData.STREAM_CODEC.encode(buffer, payload.villagerData());
                buffer.writeVarInt(payload.villagerLevel());
                buffer.writeVarInt(payload.villagerXp());
                buffer.writeVarInt(payload.storedExperience());
                buffer.writeVarInt(payload.selectedOfferIndex());
                buffer.writeBoolean(payload.showProgress());
                buffer.writeBoolean(payload.canRestock());
                buffer.writeBoolean(payload.canResetTrades());
                buffer.writeVarInt(payload.offersRevision());
            },
            buffer -> new TradingCellMenuSyncPayload(
                    buffer.readContainerId(),
                    MerchantOffers.STREAM_CODEC.decode(buffer),
                    VillagerData.STREAM_CODEC.decode(buffer),
                    buffer.readVarInt(),
                    buffer.readVarInt(),
                    buffer.readVarInt(),
                    buffer.readVarInt(),
                    buffer.readBoolean(),
                    buffer.readBoolean(),
                    buffer.readBoolean(),
                    buffer.readVarInt()
            )
    );

    public TradingCellMenuSyncPayload {
        offers = offers.copy();
        storedExperience = Math.max(0, storedExperience);
        selectedOfferIndex = Math.max(0, selectedOfferIndex);
        offersRevision = Math.max(0, offersRevision);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return PAYLOAD_TYPE;
    }
}
