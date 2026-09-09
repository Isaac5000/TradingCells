package com.cosmocraft.trading_cells.platform.neoforge.network;

import com.cosmocraft.trading_cells.feature.logistics.domain.model.LogisticsResourceType;
import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.TradingCells;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public record NetworkTerminalSyncPayload(
        int protocolVersion,
        int containerId,
        LogisticsResourceType resourceType,
        int page,
        int totalPages,
        long revision,
        boolean replace,
        List<Entry> entries,
        List<String> removedKeys
) implements CustomPacketPayload {
    public static final int CURRENT_PROTOCOL_VERSION = 1;
    public static final int MAX_ENTRIES = 80;
    public static final int MAX_REMOVED = 96;
    public static final Type<NetworkTerminalSyncPayload> PAYLOAD_TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(TradingCells.MOD_ID, "network_terminal_sync")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, NetworkTerminalSyncPayload> STREAM_CODEC =
            StreamCodec.of(NetworkTerminalSyncPayload::encode, NetworkTerminalSyncPayload::decode);

    public NetworkTerminalSyncPayload {
        if (protocolVersion != CURRENT_PROTOCOL_VERSION
                || containerId < 0
                || resourceType == null
                || page < 0
                || totalPages < 1
                || entries == null
                || entries.size() > MAX_ENTRIES
                || removedKeys == null
                || removedKeys.size() > MAX_REMOVED) {
            throw new IllegalArgumentException("Invalid network terminal synchronization");
        }
        entries = List.copyOf(entries);
        removedKeys = List.copyOf(removedKeys);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return PAYLOAD_TYPE;
    }

    private static void encode(RegistryFriendlyByteBuf buffer, NetworkTerminalSyncPayload payload) {
        buffer.writeVarInt(payload.protocolVersion());
        buffer.writeContainerId(payload.containerId());
        buffer.writeByte(payload.resourceType().ordinal());
        buffer.writeVarInt(payload.page());
        buffer.writeVarInt(payload.totalPages());
        buffer.writeVarLong(payload.revision());
        buffer.writeBoolean(payload.replace());
        buffer.writeVarInt(payload.entries().size());
        for (Entry entry : payload.entries()) {
            entry.encode(buffer);
        }
        buffer.writeVarInt(payload.removedKeys().size());
        for (String key : payload.removedKeys()) {
            buffer.writeUtf(key, 2_560);
        }
    }

    private static NetworkTerminalSyncPayload decode(RegistryFriendlyByteBuf buffer) {
        int protocol = buffer.readVarInt();
        int containerId = buffer.readContainerId();
        int typeId = buffer.readUnsignedByte();
        int page = buffer.readVarInt();
        int totalPages = buffer.readVarInt();
        long revision = buffer.readVarLong();
        boolean replace = buffer.readBoolean();
        if (protocol != CURRENT_PROTOCOL_VERSION || typeId >= LogisticsResourceType.values().length) {
            throw new IllegalArgumentException("Unsupported network terminal protocol");
        }
        int entryCount = bounded(buffer.readVarInt(), MAX_ENTRIES);
        List<Entry> entries = new ArrayList<>(entryCount);
        for (int index = 0; index < entryCount; index++) {
            entries.add(Entry.decode(buffer));
        }
        int removedCount = bounded(buffer.readVarInt(), MAX_REMOVED);
        List<String> removed = new ArrayList<>(removedCount);
        for (int index = 0; index < removedCount; index++) {
            removed.add(buffer.readUtf(2_560));
        }
        return new NetworkTerminalSyncPayload(
                protocol,
                containerId,
                LogisticsResourceType.values()[typeId],
                page,
                totalPages,
                revision,
                replace,
                entries,
                removed
        );
    }

    private static int bounded(int count, int maximum) {
        if (count < 0 || count > maximum) {
            throw new IllegalArgumentException("Invalid network terminal entry count " + count);
        }
        return count;
    }

    public record Entry(
            Identifier adapterId,
            Identifier resourceId,
            String componentFingerprint,
            String displayName,
            ItemStack icon,
            long amount
    ) {
        public Entry {
            if (adapterId == null || resourceId == null || componentFingerprint == null
                    || componentFingerprint.length() > 2_048 || displayName == null
                    || displayName.length() > 256 || icon == null || amount < 0) {
                throw new IllegalArgumentException("Invalid network terminal entry");
            }
            icon = icon.copyWithCount(icon.isEmpty() ? 0 : 1);
        }

        public String key() {
            return adapterId + "\u0000" + resourceId + "\u0000" + componentFingerprint;
        }

        private void encode(RegistryFriendlyByteBuf buffer) {
            buffer.writeIdentifier(adapterId);
            buffer.writeIdentifier(resourceId);
            buffer.writeUtf(componentFingerprint, 2_048);
            buffer.writeUtf(displayName, 256);
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, icon);
            buffer.writeVarLong(amount);
        }

        private static Entry decode(RegistryFriendlyByteBuf buffer) {
            return new Entry(
                    buffer.readIdentifier(),
                    buffer.readIdentifier(),
                    buffer.readUtf(2_048),
                    buffer.readUtf(256),
                    ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer),
                    buffer.readVarLong()
            );
        }
    }
}
