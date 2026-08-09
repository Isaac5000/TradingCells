package com.cosmocraft.trading_cells.platform.neoforge.network;

import com.cosmocraft.trading_cells.feature.experience.adapters.input.ExperienceStorageMenu;
import com.cosmocraft.trading_cells.feature.experience.domain.model.ExperienceTransferAction;
import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.TradingCells;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ExperienceStorageTransferPayload(int containerId, byte actionId, int requestedLevels)
        implements CustomPacketPayload {
    public static final Type<ExperienceStorageTransferPayload> PAYLOAD_TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(TradingCells.MOD_ID, "experience_storage_transfer")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, ExperienceStorageTransferPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buffer, payload) -> {
                        buffer.writeContainerId(payload.containerId());
                        buffer.writeByte(payload.actionId());
                        buffer.writeVarInt(payload.requestedLevels());
                    },
                    buffer -> new ExperienceStorageTransferPayload(
                            buffer.readContainerId(),
                            buffer.readByte(),
                            buffer.readVarInt()
                    )
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return PAYLOAD_TYPE;
    }

    public static void handle(ExperienceStorageTransferPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)
                    || !(player.containerMenu instanceof ExperienceStorageMenu menu)
                    || menu.containerId != payload.containerId()) {
                return;
            }
            ExperienceTransferAction.fromId(payload.actionId()).ifPresent(action ->
                    menu.handleTransfer(player, action, payload.requestedLevels())
            );
        });
    }
}
