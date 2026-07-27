package com.cosmocraft.trading_cells.platform.neoforge.network;

import com.cosmocraft.trading_cells.feature.trader.adapters.input.AutotraderMenu;
import com.cosmocraft.trading_cells.feature.trader.adapters.input.VillagerTradingCellBlockEntity;
import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.TradingCells;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ExtractTradingCellExperiencePayload(int containerId, byte mode) implements CustomPacketPayload {
    public static final byte ALL = 0;
    public static final byte NEXT_LEVEL = 1;

    public static final CustomPacketPayload.Type<ExtractTradingCellExperiencePayload> PAYLOAD_TYPE = new CustomPacketPayload.Type<>(
            Identifier.fromNamespaceAndPath(TradingCells.MOD_ID, "extract_trading_cell_experience")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, ExtractTradingCellExperiencePayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeContainerId(payload.containerId());
                buffer.writeByte(payload.mode());
            },
            buffer -> new ExtractTradingCellExperiencePayload(buffer.readContainerId(), buffer.readByte())
    );

    public ExtractTradingCellExperiencePayload {
        mode = mode == NEXT_LEVEL ? NEXT_LEVEL : ALL;
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return PAYLOAD_TYPE;
    }

    public static void handle(ExtractTradingCellExperiencePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().containerMenu instanceof AutotraderMenu menu
                    && menu.containerId == payload.containerId()) {
                menu.extractExperienceFromPacket(context.player(), payload.mode());
                return;
            }
            VillagerTradingCellBlockEntity.handleExtractExperienceRequest(
                    context.player(),
                    payload.containerId(),
                    payload.mode()
            );
        });
    }
}
