package com.cosmocraft.trading_cells.platform.neoforge.network;

import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.TradingCells;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.item.trading.MerchantOffers;

/** Complete client snapshot for the Autotrader menu. */
public record AutotraderMenuSyncPayload(
        int containerId,
        boolean hasVillager,
        MerchantOffers offers,
        VillagerData villagerData,
        int villagerXp,
        boolean canResetTrades,
        int offersRevision
) implements CustomPacketPayload {
    public static final Type<AutotraderMenuSyncPayload> PAYLOAD_TYPE = new Type<>(Identifier.fromNamespaceAndPath(
            TradingCells.MOD_ID,
            "autotrader_menu_sync"
    ));

    public static final StreamCodec<RegistryFriendlyByteBuf, AutotraderMenuSyncPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeContainerId(payload.containerId());
                buffer.writeBoolean(payload.hasVillager());
                MerchantOffers.STREAM_CODEC.encode(buffer, payload.offers());
                VillagerData.STREAM_CODEC.encode(buffer, payload.villagerData());
                buffer.writeVarInt(payload.villagerXp());
                buffer.writeBoolean(payload.canResetTrades());
                buffer.writeVarInt(payload.offersRevision());
            },
            buffer -> new AutotraderMenuSyncPayload(
                    buffer.readContainerId(),
                    buffer.readBoolean(),
                    MerchantOffers.STREAM_CODEC.decode(buffer),
                    VillagerData.STREAM_CODEC.decode(buffer),
                    buffer.readVarInt(),
                    buffer.readBoolean(),
                    buffer.readVarInt()
            )
    );

    public AutotraderMenuSyncPayload {
        offers = offers.copy();
        villagerXp = Math.max(0, villagerXp);
        offersRevision = Math.max(0, offersRevision);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return PAYLOAD_TYPE;
    }
}
