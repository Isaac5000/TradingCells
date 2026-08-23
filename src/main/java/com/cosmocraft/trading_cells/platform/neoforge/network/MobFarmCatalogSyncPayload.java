package com.cosmocraft.trading_cells.platform.neoforge.network;

import com.cosmocraft.trading_cells.feature.skeletonfarm.adapters.input.SkeletonFarmMenu;
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
        int containerId,
        int revision,
        MobFarmCatalog.Family family,
        Identifier selectedTargetId,
        List<TargetEntry> targets,
        Set<Identifier> disabledDynamicLootIds
) implements CustomPacketPayload {
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
        targets = List.copyOf(targets);
        disabledDynamicLootIds = Set.copyOf(disabledDynamicLootIds);
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

    private static MobFarmCatalogSyncPayload create(
            int containerId,
            MobFarmCatalog.Family family,
            Identifier selectedTargetId,
            List<MobFarmCatalog.Target> targets,
            Set<Identifier> disabledDynamicLootIds
    ) {
        return new MobFarmCatalogSyncPayload(
                containerId,
                MobFarmCatalog.revision(),
                family,
                selectedTargetId,
                targets.stream().map(TargetEntry::from).toList(),
                disabledDynamicLootIds
        );
    }

    private static void encode(RegistryFriendlyByteBuf buffer, MobFarmCatalogSyncPayload payload) {
        buffer.writeContainerId(payload.containerId());
        buffer.writeVarInt(payload.revision());
        buffer.writeVarInt(payload.family().ordinal());
        buffer.writeIdentifier(payload.selectedTargetId());
        buffer.writeVarInt(Math.min(MAX_TARGETS, payload.targets().size()));
        for (int index = 0; index < payload.targets().size() && index < MAX_TARGETS; index++) {
            payload.targets().get(index).encode(buffer);
        }
        List<Identifier> disabled = payload.disabledDynamicLootIds().stream().sorted().toList();
        buffer.writeVarInt(Math.min(MAX_DISABLED_LOOT, disabled.size()));
        for (int index = 0; index < disabled.size() && index < MAX_DISABLED_LOOT; index++) {
            buffer.writeIdentifier(disabled.get(index));
        }
    }

    private static MobFarmCatalogSyncPayload decode(RegistryFriendlyByteBuf buffer) {
        int containerId = buffer.readContainerId();
        int revision = buffer.readVarInt();
        MobFarmCatalog.Family[] families = MobFarmCatalog.Family.values();
        MobFarmCatalog.Family family = families[Math.clamp(buffer.readVarInt(), 0, families.length - 1)];
        Identifier selectedTargetId = buffer.readIdentifier();
        int targetCount = Math.clamp(buffer.readVarInt(), 0, MAX_TARGETS);
        List<TargetEntry> targets = new ArrayList<>(targetCount);
        for (int index = 0; index < targetCount; index++) {
            targets.add(TargetEntry.decode(buffer));
        }
        int disabledCount = Math.clamp(buffer.readVarInt(), 0, MAX_DISABLED_LOOT);
        java.util.LinkedHashSet<Identifier> disabled = new java.util.LinkedHashSet<>();
        for (int index = 0; index < disabledCount; index++) {
            disabled.add(buffer.readIdentifier());
        }
        return new MobFarmCatalogSyncPayload(
                containerId,
                revision,
                family,
                selectedTargetId,
                targets,
                disabled
        );
    }

    public record TargetEntry(Identifier entityTypeId, List<Identifier> lootItemIds) {
        public TargetEntry {
            lootItemIds = List.copyOf(lootItemIds);
        }

        private static TargetEntry from(MobFarmCatalog.Target target) {
            return new TargetEntry(target.entityTypeId(), target.lootItemIds());
        }

        private void encode(RegistryFriendlyByteBuf buffer) {
            buffer.writeIdentifier(entityTypeId);
            buffer.writeVarInt(Math.min(MAX_LOOT_PER_TARGET, lootItemIds.size()));
            for (int index = 0; index < lootItemIds.size() && index < MAX_LOOT_PER_TARGET; index++) {
                buffer.writeIdentifier(lootItemIds.get(index));
            }
        }

        private static TargetEntry decode(RegistryFriendlyByteBuf buffer) {
            Identifier entityTypeId = buffer.readIdentifier();
            int lootCount = Math.clamp(buffer.readVarInt(), 0, MAX_LOOT_PER_TARGET);
            List<Identifier> loot = new ArrayList<>(lootCount);
            for (int index = 0; index < lootCount; index++) {
                loot.add(buffer.readIdentifier());
            }
            return new TargetEntry(entityTypeId, loot);
        }
    }
}
