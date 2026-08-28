package com.cosmocraft.trading_cells.platform.neoforge.network;

import com.cosmocraft.trading_cells.feature.skeletonfarm.adapters.input.SkeletonFarmMenu;
import com.cosmocraft.trading_cells.feature.raiderfarm.adapters.input.RaiderFarmMenu;
import com.cosmocraft.trading_cells.feature.creeperfarm.adapters.input.CreeperFarmMenu;
import com.cosmocraft.trading_cells.feature.zombiefarm.adapters.input.ZombieFarmMenu;
import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.TradingCells;
import com.cosmocraft.trading_cells.platform.neoforge.mobfarm.MobFarmCatalog;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record MobFarmCatalogSyncPayload(
        int protocolVersion,
        int containerId,
        int revision,
        Identifier familyId,
        Identifier selectedTargetId,
        List<TargetEntry> targets,
        Set<Identifier> disabledDynamicLootIds
) implements CustomPacketPayload {
    public static final int CURRENT_PROTOCOL_VERSION = 1;
    private static final int MAX_TARGETS = 512;
    private static final int MAX_LOOT_PER_TARGET = 2_048;
    private static final int MAX_DISABLED_LOOT = 2_048;
    public static final Type<MobFarmCatalogSyncPayload> PAYLOAD_TYPE = new Type<>(Identifier.fromNamespaceAndPath(
            TradingCells.MOD_ID,
            "mob_farm_catalog_sync"
    ));
    public static final StreamCodec<RegistryFriendlyByteBuf, MobFarmCatalogSyncPayload> STREAM_CODEC = StreamCodec.of(
            MobFarmCatalogSyncPayload::encode,
            MobFarmCatalogSyncPayload::decode
    );

    public MobFarmCatalogSyncPayload {
        if (protocolVersion != CURRENT_PROTOCOL_VERSION) {
            throw new IllegalArgumentException("Unsupported mob-farm catalog protocol " + protocolVersion);
        }
        if (MobFarmCatalog.Family.fromId(familyId).isEmpty()) {
            throw new IllegalArgumentException("Unknown mob-farm family " + familyId);
        }
        targets = List.copyOf(targets);
        disabledDynamicLootIds = Set.copyOf(disabledDynamicLootIds);
        requireMaximum("targets", targets.size(), MAX_TARGETS);
        requireMaximum("disabled loot filters", disabledDynamicLootIds.size(), MAX_DISABLED_LOOT);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return PAYLOAD_TYPE;
    }

    public static MobFarmCatalogSyncPayload from(SkeletonFarmMenu menu) {
        return create(
                menu.containerId,
                MobFarmCatalog.Family.SKELETON,
                menu.selectedTargetId(),
                menu.targetEntries(),
                menu.disabledDynamicLootIds()
        );
    }

    public static MobFarmCatalogSyncPayload from(ZombieFarmMenu menu) {
        return create(
                menu.containerId,
                MobFarmCatalog.Family.ZOMBIE,
                menu.selectedTargetId(),
                menu.targetEntries(),
                menu.disabledDynamicLootIds()
        );
    }

    public static MobFarmCatalogSyncPayload from(RaiderFarmMenu menu) {
        return create(
                menu.containerId,
                MobFarmCatalog.Family.RAIDER,
                menu.selectedTargetId(),
                menu.targetEntries(),
                menu.disabledDynamicLootIds()
        );
    }

    public static MobFarmCatalogSyncPayload from(CreeperFarmMenu menu) {
        return create(
                menu.containerId,
                MobFarmCatalog.Family.CREEPER,
                menu.selectedTargetId(),
                menu.targetEntries(),
                menu.disabledDynamicLootIds()
        );
    }

    private static MobFarmCatalogSyncPayload create(
            int containerId,
            MobFarmCatalog.Family family,
            Identifier selectedTargetId,
            List<MobFarmCatalog.Target> targets,
            Set<Identifier> disabledDynamicLootIds
    ) {
        return new MobFarmCatalogSyncPayload(
                CURRENT_PROTOCOL_VERSION,
                containerId,
                MobFarmCatalog.revision(),
                family.id(),
                selectedTargetId,
                targets.stream().map(TargetEntry::from).toList(),
                disabledDynamicLootIds
        );
    }

    private static void encode(RegistryFriendlyByteBuf buffer, MobFarmCatalogSyncPayload payload) {
        buffer.writeVarInt(payload.protocolVersion());
        buffer.writeContainerId(payload.containerId());
        buffer.writeVarInt(payload.revision());
        buffer.writeIdentifier(payload.familyId());
        buffer.writeIdentifier(payload.selectedTargetId());
        buffer.writeVarInt(payload.targets().size());
        for (TargetEntry target : payload.targets()) {
            target.encode(buffer);
        }
        List<Identifier> disabled = payload.disabledDynamicLootIds().stream().sorted().toList();
        buffer.writeVarInt(disabled.size());
        for (Identifier disabledId : disabled) {
            buffer.writeIdentifier(disabledId);
        }
    }

    private static MobFarmCatalogSyncPayload decode(RegistryFriendlyByteBuf buffer) {
        int protocolVersion = buffer.readVarInt();
        if (protocolVersion != CURRENT_PROTOCOL_VERSION) {
            throw new IllegalArgumentException("Unsupported mob-farm catalog protocol " + protocolVersion);
        }
        int containerId = buffer.readContainerId();
        int revision = buffer.readVarInt();
        Identifier familyId = buffer.readIdentifier();
        if (MobFarmCatalog.Family.fromId(familyId).isEmpty()) {
            throw new IllegalArgumentException("Unknown mob-farm family " + familyId);
        }
        Identifier selectedTargetId = buffer.readIdentifier();
        int targetCount = readBoundedCount(buffer, "targets", MAX_TARGETS);
        List<TargetEntry> targets = new ArrayList<>(targetCount);
        for (int index = 0; index < targetCount; index++) {
            targets.add(TargetEntry.decode(buffer));
        }
        int disabledCount = readBoundedCount(buffer, "disabled loot filters", MAX_DISABLED_LOOT);
        java.util.LinkedHashSet<Identifier> disabled = new java.util.LinkedHashSet<>();
        for (int index = 0; index < disabledCount; index++) {
            disabled.add(buffer.readIdentifier());
        }
        return new MobFarmCatalogSyncPayload(
                protocolVersion,
                containerId,
                revision,
                familyId,
                selectedTargetId,
                targets,
                disabled
        );
    }

    private static int readBoundedCount(RegistryFriendlyByteBuf buffer, String label, int maximum) {
        int count = buffer.readVarInt();
        requireMaximum(label, count, maximum);
        return count;
    }

    private static void requireMaximum(String label, int count, int maximum) {
        if (count < 0 || count > maximum) {
            throw new IllegalArgumentException("Invalid mob-farm " + label + " count " + count);
        }
    }

    public record TargetEntry(
            Identifier entityTypeId,
            Identifier generatorItemId,
            List<Identifier> lootItemIds
    ) {
        public TargetEntry {
            lootItemIds = List.copyOf(lootItemIds);
            requireMaximum("loot entries", lootItemIds.size(), MAX_LOOT_PER_TARGET);
        }

        private static TargetEntry from(MobFarmCatalog.Target target) {
            return new TargetEntry(
                    target.entityTypeId(),
                    target.generatorItemId(),
                    target.lootItemIds()
            );
        }

        private void encode(RegistryFriendlyByteBuf buffer) {
            buffer.writeIdentifier(entityTypeId);
            buffer.writeIdentifier(generatorItemId);
            buffer.writeVarInt(lootItemIds.size());
            for (Identifier lootItemId : lootItemIds) {
                buffer.writeIdentifier(lootItemId);
            }
        }

        private static TargetEntry decode(RegistryFriendlyByteBuf buffer) {
            Identifier entityTypeId = buffer.readIdentifier();
            Identifier generatorItemId = buffer.readIdentifier();
            int lootCount = readBoundedCount(buffer, "loot entries", MAX_LOOT_PER_TARGET);
            List<Identifier> loot = new ArrayList<>(lootCount);
            for (int index = 0; index < lootCount; index++) {
                loot.add(buffer.readIdentifier());
            }
            return new TargetEntry(entityTypeId, generatorItemId, loot);
        }
    }
}
