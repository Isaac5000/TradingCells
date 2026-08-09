package com.cosmocraft.trading_cells.platform.neoforge.network;

import com.cosmocraft.trading_cells.feature.quarry.adapters.input.QuarryMaterialCatalog;
import com.cosmocraft.trading_cells.feature.quarry.adapters.input.QuarryMaterialDefinition;
import com.cosmocraft.trading_cells.feature.quarry.adapters.input.QuarryMenu;
import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.TradingCells;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public record QuarryCatalogSyncPayload(
        int containerId,
        int revision,
        boolean deepMining,
        List<Entry> entries
) implements CustomPacketPayload {
    private static final int MAX_ENTRIES = 2_048;
    public static final Type<QuarryCatalogSyncPayload> PAYLOAD_TYPE = new Type<>(Identifier.fromNamespaceAndPath(
            TradingCells.MOD_ID,
            "quarry_catalog_sync"
    ));
    public static final StreamCodec<RegistryFriendlyByteBuf, QuarryCatalogSyncPayload> STREAM_CODEC = StreamCodec.of(
            QuarryCatalogSyncPayload::encode,
            QuarryCatalogSyncPayload::decode
    );

    public QuarryCatalogSyncPayload {
        entries = List.copyOf(entries);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return PAYLOAD_TYPE;
    }

    public static QuarryCatalogSyncPayload from(QuarryMenu menu) {
        QuarryMaterialCatalog.CatalogSnapshot snapshot = menu.serverCatalogSnapshot();
        List<Entry> entries = snapshot.entries().stream()
                .map(entry -> Entry.from(entry, snapshot.deepMining()))
                .toList();
        return new QuarryCatalogSyncPayload(
                menu.containerId,
                snapshot.revision(),
                snapshot.deepMining(),
                entries
        );
    }

    private static void encode(RegistryFriendlyByteBuf buffer, QuarryCatalogSyncPayload payload) {
        buffer.writeContainerId(payload.containerId());
        buffer.writeVarInt(payload.revision());
        buffer.writeBoolean(payload.deepMining());
        buffer.writeVarInt(Math.min(MAX_ENTRIES, payload.entries().size()));
        for (int index = 0; index < payload.entries().size() && index < MAX_ENTRIES; index++) {
            payload.entries().get(index).encode(buffer);
        }
    }

    private static QuarryCatalogSyncPayload decode(RegistryFriendlyByteBuf buffer) {
        int containerId = buffer.readContainerId();
        int revision = buffer.readVarInt();
        boolean deepMining = buffer.readBoolean();
        int count = Math.clamp(buffer.readVarInt(), 0, MAX_ENTRIES);
        List<Entry> entries = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            entries.add(Entry.decode(buffer));
        }
        return new QuarryCatalogSyncPayload(containerId, revision, deepMining, entries);
    }

    public record Entry(
            Identifier materialId,
            ItemStack preview,
            ItemStack normalResult,
            ItemStack silkResult,
            ItemStack deepResult,
            int probabilityPartsPerMillion,
            int minimumAmount,
            int maximumAmount,
            int minimumUpgrade,
            double minimumPickaxeSpeed,
            boolean fortuneCompatible,
            boolean deepVariantAvailable,
            boolean defaultDeepFallback,
            int blockedReason,
            String sourceMod
    ) {
        public Entry {
            preview = preview.copy();
            normalResult = normalResult.copy();
            silkResult = silkResult.copy();
            deepResult = deepResult.copy();
            sourceMod = sourceMod == null ? "" : sourceMod;
        }

        private static Entry from(
                QuarryMaterialCatalog.CatalogEntry entry,
                boolean deepMining
        ) {
            QuarryMaterialDefinition definition = entry.definition();
            ItemStack silk = stack(definition.silkResult());
            ItemStack deep = definition.deepSilkResult() == null
                    ? ItemStack.EMPTY
                    : stack(definition.deepSilkResult());
            ItemStack preview = stack(definition.silkResult(deepMining));
            boolean fallback = deepMining && !definition.rock() && !definition.hasDeepVariant();
            return new Entry(
                    definition.id(),
                    preview,
                    stack(definition.normalResult()),
                    silk,
                    deep,
                    entry.probabilityPartsPerMillion(),
                    definition.minimumAmount(),
                    definition.maximumAmount(),
                    definition.minimumUpgrade().ordinal(),
                    definition.minimumPickaxeSpeed(),
                    QuarryMaterialCatalog.fortuneAffectsSelection(definition),
                    definition.hasDeepVariant(),
                    fallback,
                    entry.blockedReason().ordinal(),
                    definition.sourceMod()
            );
        }

        private void encode(RegistryFriendlyByteBuf buffer) {
            buffer.writeIdentifier(materialId);
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, preview);
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, normalResult);
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, silkResult);
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, deepResult);
            buffer.writeVarInt(probabilityPartsPerMillion);
            buffer.writeVarInt(minimumAmount);
            buffer.writeVarInt(maximumAmount);
            buffer.writeVarInt(minimumUpgrade);
            buffer.writeDouble(minimumPickaxeSpeed);
            buffer.writeBoolean(fortuneCompatible);
            buffer.writeBoolean(deepVariantAvailable);
            buffer.writeBoolean(defaultDeepFallback);
            buffer.writeVarInt(blockedReason);
            buffer.writeUtf(sourceMod, 128);
        }

        private static Entry decode(RegistryFriendlyByteBuf buffer) {
            return new Entry(
                    buffer.readIdentifier(),
                    ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer),
                    ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer),
                    ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer),
                    ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer),
                    buffer.readVarInt(),
                    buffer.readVarInt(),
                    buffer.readVarInt(),
                    buffer.readVarInt(),
                    buffer.readDouble(),
                    buffer.readBoolean(),
                    buffer.readBoolean(),
                    buffer.readBoolean(),
                    buffer.readVarInt(),
                    buffer.readUtf(128)
            );
        }

        private static ItemStack stack(Identifier id) {
            Item item = BuiltInRegistries.ITEM.getOptional(id).orElse(null);
            return item == null ? ItemStack.EMPTY : new ItemStack(item);
        }
    }
}
