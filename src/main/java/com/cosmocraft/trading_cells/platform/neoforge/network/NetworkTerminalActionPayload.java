package com.cosmocraft.trading_cells.platform.neoforge.network;

import com.cosmocraft.trading_cells.feature.logistics.adapters.input.NetworkTerminalMenu;
import com.cosmocraft.trading_cells.feature.logistics.domain.model.LogisticsResourceType;
import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.TradingCells;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jspecify.annotations.Nullable;

public record NetworkTerminalActionPayload(
        int containerId,
        Action action,
        LogisticsResourceType resourceType,
        int page,
        String query,
        InteractionHand hand,
        @Nullable Identifier adapterId,
        @Nullable Identifier resourceId,
        String componentFingerprint,
        long amount
) implements CustomPacketPayload {
    public static final Type<NetworkTerminalActionPayload> PAYLOAD_TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(TradingCells.MOD_ID, "network_terminal_action")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, NetworkTerminalActionPayload> STREAM_CODEC =
            StreamCodec.of(NetworkTerminalActionPayload::encode, NetworkTerminalActionPayload::decode);

    public NetworkTerminalActionPayload {
        if (containerId < 0 || action == null || resourceType == null || page < 0 || page > 1_000_000
                || query == null || query.length() > 64 || hand == null
                || componentFingerprint == null || componentFingerprint.length() > 2_048
                || amount < 0) {
            throw new IllegalArgumentException("Invalid network terminal action");
        }
    }

    public static NetworkTerminalActionPayload view(
            int containerId,
            LogisticsResourceType type,
            int page,
            String query
    ) {
        return new NetworkTerminalActionPayload(
                containerId, Action.VIEW, type, page, query, InteractionHand.MAIN_HAND,
                null, null, "", 0
        );
    }

    public static NetworkTerminalActionPayload insert(
            int containerId,
            LogisticsResourceType type,
            InteractionHand hand,
            boolean inventory
    ) {
        return new NetworkTerminalActionPayload(
                containerId,
                inventory ? Action.INSERT_INVENTORY : Action.INSERT_HAND,
                type,
                0,
                "",
                hand,
                null,
                null,
                "",
                Long.MAX_VALUE
        );
    }

    public static NetworkTerminalActionPayload withdraw(
            int containerId,
            LogisticsResourceType type,
            InteractionHand hand,
            NetworkTerminalSyncPayload.Entry entry,
            long amount
    ) {
        return new NetworkTerminalActionPayload(
                containerId, Action.WITHDRAW, type, 0, "", hand,
                entry.adapterId(), entry.resourceId(), entry.componentFingerprint(), Math.max(1, amount)
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return PAYLOAD_TYPE;
    }

    public static void handle(NetworkTerminalActionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().containerMenu instanceof NetworkTerminalMenu menu
                    && menu.containerId == payload.containerId()) {
                menu.handleAction(context.player(), payload);
            }
        });
    }

    private static void encode(RegistryFriendlyByteBuf buffer, NetworkTerminalActionPayload payload) {
        buffer.writeContainerId(payload.containerId());
        buffer.writeByte(payload.action().ordinal());
        buffer.writeByte(payload.resourceType().ordinal());
        buffer.writeVarInt(payload.page());
        buffer.writeUtf(payload.query(), 64);
        buffer.writeByte(payload.hand().ordinal());
        buffer.writeBoolean(payload.adapterId() != null);
        if (payload.adapterId() != null) {
            buffer.writeIdentifier(payload.adapterId());
        }
        buffer.writeBoolean(payload.resourceId() != null);
        if (payload.resourceId() != null) {
            buffer.writeIdentifier(payload.resourceId());
        }
        buffer.writeUtf(payload.componentFingerprint(), 2_048);
        buffer.writeVarLong(payload.amount());
    }

    private static NetworkTerminalActionPayload decode(RegistryFriendlyByteBuf buffer) {
        int containerId = buffer.readContainerId();
        int actionId = buffer.readUnsignedByte();
        int typeId = buffer.readUnsignedByte();
        int page = buffer.readVarInt();
        String query = buffer.readUtf(64);
        int handId = buffer.readUnsignedByte();
        if (actionId >= Action.values().length
                || typeId >= LogisticsResourceType.values().length
                || handId >= InteractionHand.values().length) {
            throw new IllegalArgumentException("Unknown network terminal action");
        }
        Identifier adapterId = buffer.readBoolean() ? buffer.readIdentifier() : null;
        Identifier resourceId = buffer.readBoolean() ? buffer.readIdentifier() : null;
        return new NetworkTerminalActionPayload(
                containerId,
                Action.values()[actionId],
                LogisticsResourceType.values()[typeId],
                page,
                query,
                InteractionHand.values()[handId],
                adapterId,
                resourceId,
                buffer.readUtf(2_048),
                buffer.readVarLong()
        );
    }

    public enum Action {
        VIEW,
        INSERT_HAND,
        INSERT_INVENTORY,
        WITHDRAW,
        RECIPES,
        SELECT_RECIPE,
        CRAFT,
        GHOST,
        CURSOR_DEPOSIT,
        CURSOR_WITHDRAW,
        CONTAINER_DEPOSIT,
        CONTAINER_WITHDRAW,
        CRAFT_CURSOR,
        CRAFT_STACK
    }
}
