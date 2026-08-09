package com.cosmocraft.trading_cells.platform.neoforge.network;

import com.cosmocraft.trading_cells.feature.infusion.adapters.input.ArcaneInfuserMenu;
import com.cosmocraft.trading_cells.feature.infusion.domain.model.ArcaneInfusionTransferAction;
import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.TradingCells;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ArcaneInfuserTransferPayload(int containerId, byte actionId, int requestedLevels)
        implements CustomPacketPayload {
    public static final Type<ArcaneInfuserTransferPayload> PAYLOAD_TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(TradingCells.MOD_ID, "arcane_infuser_transfer")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, ArcaneInfuserTransferPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buffer, payload) -> {
                        buffer.writeContainerId(payload.containerId());
                        buffer.writeByte(payload.actionId());
                        buffer.writeVarInt(payload.requestedLevels());
                    },
                    buffer -> new ArcaneInfuserTransferPayload(
                            buffer.readContainerId(),
                            buffer.readByte(),
                            buffer.readVarInt()
                    )
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return PAYLOAD_TYPE;
    }

    public static void handle(ArcaneInfuserTransferPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)
                    || !(player.containerMenu instanceof ArcaneInfuserMenu menu)
                    || menu.containerId != payload.containerId()) {
                return;
            }
            ArcaneInfusionTransferAction.fromId(payload.actionId()).ifPresent(action ->
                    menu.handleTransfer(player, action, payload.requestedLevels())
            );
        });
    }
}
