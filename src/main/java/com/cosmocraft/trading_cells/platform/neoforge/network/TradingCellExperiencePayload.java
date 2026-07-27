package com.cosmocraft.trading_cells.platform.neoforge.network;

import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.TradingCells;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record TradingCellExperiencePayload(int containerId, int experience) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<TradingCellExperiencePayload> PAYLOAD_TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(
                    TradingCells.MOD_ID,
                    "trading_cell_experience"
            ));
    public static final StreamCodec<RegistryFriendlyByteBuf, TradingCellExperiencePayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    TradingCellExperiencePayload::containerId,
                    ByteBufCodecs.VAR_INT,
                    TradingCellExperiencePayload::experience,
                    TradingCellExperiencePayload::new
            );

    public TradingCellExperiencePayload {
        experience = Math.max(0, experience);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return PAYLOAD_TYPE;
    }
}
