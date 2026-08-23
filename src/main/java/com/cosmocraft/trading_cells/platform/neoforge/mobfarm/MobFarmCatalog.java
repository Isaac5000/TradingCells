package com.cosmocraft.trading_cells.platform.neoforge.mobfarm;

import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.TradingCells;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.neoforge.event.TagsUpdatedEvent;

/** Server-authoritative, tag-backed target and loot catalog shared by mob farms. */
public final class MobFarmCatalog {
    private static final List<Identifier> SKELETON_BASE = ids(
            "skeleton", "wither_skeleton", "stray", "bogged", "parched", "skeleton_horse"
    );
    private static final List<Identifier> ZOMBIE_BASE = ids(
            "zombie", "zombie_villager", "husk", "drowned", "zombified_piglin", "zoglin"
    );
    private static final Map<Family, List<Identifier>> BASE_IDS = Map.of(
            Family.SKELETON, SKELETON_BASE,
            Family.ZOMBIE, ZOMBIE_BASE
    );
    private static final AtomicReference<Map<Family, List<Target>>> TARGETS =
            new AtomicReference<>(fallbackTargets());
    private static final AtomicInteger REVISION = new AtomicInteger();

    private MobFarmCatalog() {
    }

    public static int revision() {
        return REVISION.get();
    }

    public static List<Target> targets(Family family) {
        return TARGETS.get().getOrDefault(family, List.of());
    }

    public static Optional<Target> target(Family family, Identifier entityTypeId) {
        return targets(family).stream()
                .filter(entry -> entry.entityTypeId().equals(entityTypeId))
                .findFirst();
    }

    public static boolean contains(Family family, Identifier entityTypeId) {
        return target(family, entityTypeId).isPresent();
    }

    public static void refresh(TagsUpdatedEvent.ServerDataLoad event) {
        try {
            Registry<EntityType<?>> entityTypes = event.getRegistries().lookupOrThrow(Registries.ENTITY_TYPE);
            Registry<Item> items = event.getRegistries().lookupOrThrow(Registries.ITEM);
            Map<Family, List<Target>> refreshed = new EnumMap<>(Family.class);
            for (Family family : Family.values()) {
                refreshed.put(family, discoverFamily(entityTypes, items, family));
            }
            TARGETS.set(Map.copyOf(refreshed));
            REVISION.incrementAndGet();
        } catch (RuntimeException | LinkageError exception) {
            TradingCells.LOGGER.warn(
                    "Mob-farm catalogs could not be rebuilt; fixed vanilla targets remain active.",
                    exception
            );
            TARGETS.set(fallbackTargets());
            REVISION.incrementAndGet();
        }
    }

    private static List<Target> discoverFamily(
            Registry<EntityType<?>> entityTypes,
            Registry<Item> items,
            Family family
    ) {
        LinkedHashMap<Identifier, Target> discovered = new LinkedHashMap<>();
        for (Identifier baseId : BASE_IDS.get(family)) {
            addTarget(entityTypes, items, family, baseId, discovered);
        }
        for (Holder<EntityType<?>> holder : entityTypes.getTagOrEmpty(family.tag())) {
            EntityType<?> type = holder.value();
            Identifier id = entityTypes.getKey(type);
            if (type.getCategory() != MobCategory.MONSTER
                    && !BASE_IDS.get(family).contains(id)) {
                continue;
            }
            if (id != null && !discovered.containsKey(id)) {
                addTarget(entityTypes, items, family, id, discovered);
            }
        }

        Map<Identifier, Integer> fixedOrder = new LinkedHashMap<>();
        List<Identifier> baseIds = BASE_IDS.get(family);
        for (int index = 0; index < baseIds.size(); index++) {
            fixedOrder.put(baseIds.get(index), index);
        }
        List<Target> result = new ArrayList<>(discovered.values());
        result.sort(Comparator
                .comparingInt((Target target) -> fixedOrder.getOrDefault(
                        target.entityTypeId(),
                        Integer.MAX_VALUE
                ))
                .thenComparing(target -> target.entityTypeId().toString()));
        return List.copyOf(result);
    }

    private static void addTarget(
            Registry<EntityType<?>> entityTypes,
            Registry<Item> items,
            Family family,
            Identifier id,
            Map<Identifier, Target> targetMap
    ) {
        try {
            EntityType<?> type = entityTypes.getOptional(id).orElse(null);
            if (type == null || type.getCategory() != MobCategory.MONSTER
                    && !BASE_IDS.get(family).contains(id)) {
                return;
            }
            List<Identifier> loot = lootItems(items, type, id);
            targetMap.put(id, new Target(id, loot));
        } catch (RuntimeException | LinkageError exception) {
            TradingCells.LOGGER.warn(
                    "Discarding invalid {} mob-farm target '{}'.",
                    family.name().toLowerCase(java.util.Locale.ROOT),
                    id,
                    exception
            );
        }
    }

    private static List<Identifier> lootItems(
            Registry<Item> items,
            EntityType<?> type,
            Identifier entityTypeId
    ) {
        Optional<ResourceKey<LootTable>> lootTableKey = type.getDefaultLootTable();
        if (lootTableKey.isEmpty()) {
            return List.of();
        }
        LinkedHashMap<Identifier, Boolean> result = new LinkedHashMap<>();
        for (MobFarmLootTableReloadListener.LootReference reference
                : MobFarmLootTableReloadListener.references(lootTableKey.orElseThrow().identifier())) {
            if (reference.tag()) {
                TagKey<Item> tag = TagKey.create(Registries.ITEM, reference.id());
                for (Holder<Item> holder : items.getTagOrEmpty(tag)) {
                    Identifier itemId = items.getKey(holder.value());
                    if (itemId != null) {
                        result.put(itemId, Boolean.TRUE);
                    }
                }
            } else if (items.containsKey(reference.id())
                    && !isImpossibleFarmDrop(entityTypeId, reference.id())) {
                result.put(reference.id(), Boolean.TRUE);
            }
        }
        return List.copyOf(result.keySet());
    }

    private static boolean isImpossibleFarmDrop(Identifier entityTypeId, Identifier itemId) {
        String entity = entityTypeId.toString();
        String item = itemId.toString();
        return "minecraft:zombie".equals(entity)
                        && ("minecraft:red_mushroom".equals(item)
                        || "minecraft:music_disc_lava_chicken".equals(item))
                || "minecraft:husk".equals(entity) && "minecraft:rabbit_foot".equals(item);
    }

    private static Map<Family, List<Target>> fallbackTargets() {
        Map<Family, List<Target>> fallback = new EnumMap<>(Family.class);
        for (Family family : Family.values()) {
            fallback.put(family, BASE_IDS.get(family).stream()
                    .filter(BuiltInRegistries.ENTITY_TYPE::containsKey)
                    .map(id -> new Target(id, List.of()))
                    .toList());
        }
        return Map.copyOf(fallback);
    }

    private static List<Identifier> ids(String... paths) {
        return java.util.Arrays.stream(paths)
                .map(path -> Identifier.withDefaultNamespace(path))
                .toList();
    }

    public enum Family {
        SKELETON(EntityTypeTags.SKELETONS),
        ZOMBIE(EntityTypeTags.ZOMBIES);

        private final TagKey<EntityType<?>> tag;

        Family(TagKey<EntityType<?>> tag) {
            this.tag = tag;
        }

        public TagKey<EntityType<?>> tag() {
            return tag;
        }
    }

    public record Target(Identifier entityTypeId, List<Identifier> lootItemIds) {
        public Target {
            lootItemIds = List.copyOf(lootItemIds);
        }
    }
}
