package com.cosmocraft.trading_cells.platform.neoforge.mobfarm;

import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.TradingCells;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.neoforge.event.TagsUpdatedEvent;

/** Server-authoritative immutable snapshot of targets and filterable loot for registered mob farms. */
public final class MobFarmCatalog {
    public static final int SCHEMA_VERSION = MobFarmTargetReloadListener.SCHEMA_VERSION;
    private static final int DYNAMIC_ORDER = 1_000_000;
    private static final List<Identifier> SKELETON_BASE = ids(
            "skeleton", "wither_skeleton", "stray", "bogged", "parched", "skeleton_horse"
    );
    private static final List<Identifier> ZOMBIE_BASE = ids(
            "zombie", "zombie_villager", "husk", "drowned", "zombified_piglin", "zoglin"
    );
    private static final List<Identifier> RAIDER_BASE = ids(
            "pillager", "evoker", "ravager", "witch"
    );
    private static final List<Identifier> CREEPER_BASE = ids("creeper");
    private static final List<Identifier> ARTHROPOD_BASE = ids(
            "spider", "cave_spider", "silverfish", "endermite"
    );
    private static final List<Identifier> SLIME_BASE = ids("slime", "magma_cube", "sulfur_cube");
    private static final List<Identifier> GUARDIAN_BASE = ids("guardian", "elder_guardian");
    private static final List<Identifier> PIGLIN_BASE = ids("piglin", "piglin_brute");
    private static final List<Identifier> BLAZE_BASE = ids("blaze");
    private static final List<Identifier> GHAST_BASE = ids("ghast", "happy_ghast");
    private static final List<Identifier> ENDERMAN_BASE = ids("enderman");
    private static final List<Identifier> SHULKER_BASE = ids("shulker");
    private static final List<Identifier> BREEZE_BASE = ids("breeze");
    private static final List<Identifier> PHANTOM_BASE = ids("phantom");
    private static final Map<Family, List<Identifier>> BASE_IDS = Map.ofEntries(
            Map.entry(Family.SKELETON, SKELETON_BASE),
            Map.entry(Family.ZOMBIE, ZOMBIE_BASE),
            Map.entry(Family.RAIDER, RAIDER_BASE),
            Map.entry(Family.CREEPER, CREEPER_BASE),
            Map.entry(Family.ARTHROPOD, ARTHROPOD_BASE),
            Map.entry(Family.SLIME, SLIME_BASE),
            Map.entry(Family.GUARDIAN, GUARDIAN_BASE),
            Map.entry(Family.PIGLIN, PIGLIN_BASE),
            Map.entry(Family.BLAZE, BLAZE_BASE),
            Map.entry(Family.GHAST, GHAST_BASE),
            Map.entry(Family.ENDERMAN, ENDERMAN_BASE),
            Map.entry(Family.SHULKER, SHULKER_BASE),
            Map.entry(Family.BREEZE, BREEZE_BASE),
            Map.entry(Family.PHANTOM, PHANTOM_BASE)
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
        Map<Identifier, Integer> order = new LinkedHashMap<>();
        List<Identifier> baseIds = BASE_IDS.get(family);
        for (int index = 0; index < baseIds.size(); index++) {
            Identifier baseId = baseIds.get(index);
            addDiscoveredTarget(entityTypes, items, family, baseId, discovered);
            order.put(baseId, index);
        }
        for (Holder<EntityType<?>> holder : entityTypes.getTagOrEmpty(family.tag())) {
            EntityType<?> type = holder.value();
            Identifier id = entityTypes.getKey(type);
            if (id == null || discovered.containsKey(id)) {
                continue;
            }
            addDiscoveredTarget(entityTypes, items, family, id, discovered);
            order.putIfAbsent(id, DYNAMIC_ORDER);
        }
        applyDefinitions(entityTypes, items, family, discovered, order);

        List<Target> result = new ArrayList<>(discovered.values());
        result.sort(Comparator
                .comparingInt((Target target) -> order.getOrDefault(target.entityTypeId(), DYNAMIC_ORDER))
                .thenComparing(target -> target.entityTypeId().toString()));
        return List.copyOf(result);
    }

    private static void applyDefinitions(
            Registry<EntityType<?>> entityTypes,
            Registry<Item> items,
            Family family,
            Map<Identifier, Target> targets,
            Map<Identifier, Integer> order
    ) {
        Map<Identifier, MobFarmTargetReloadListener.Definition> definitions = new LinkedHashMap<>();
        for (MobFarmTargetReloadListener.Definition definition
                : MobFarmTargetReloadListener.definitions(family)) {
            MobFarmTargetReloadListener.Definition previous = definitions.putIfAbsent(
                    definition.entityTypeId(),
                    definition
            );
            if (previous != null) {
                TradingCells.LOGGER.warn(
                        "Mob-farm descriptors '{}' and '{}' target the same entity '{}'; '{}' wins by ID order.",
                        previous.sourceId(),
                        definition.sourceId(),
                        definition.entityTypeId(),
                        previous.sourceId()
                );
            }
        }
        for (MobFarmTargetReloadListener.Definition definition : definitions.values()) {
            try {
                applyDefinition(entityTypes, items, definition, targets, order);
            } catch (RuntimeException | LinkageError exception) {
                TradingCells.LOGGER.warn(
                        "Discarding mob-farm target descriptor '{}': {}",
                        definition.sourceId(),
                        exception.getMessage()
                );
            }
        }
    }

    private static void applyDefinition(
            Registry<EntityType<?>> entityTypes,
            Registry<Item> items,
            MobFarmTargetReloadListener.Definition definition,
            Map<Identifier, Target> targets,
            Map<Identifier, Integer> order
    ) {
        EntityType<?> type = entityTypes.getOptional(definition.entityTypeId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "unknown entity_type " + definition.entityTypeId()
                ));
        if (!items.containsKey(definition.generatorItemId())) {
            throw new IllegalArgumentException("unknown generator_item " + definition.generatorItemId());
        }

        Target current = targets.get(definition.entityTypeId());
        LinkedHashSet<Identifier> loot = new LinkedHashSet<>(current == null
                ? lootItems(items, type, definition.entityTypeId())
                : current.lootItemIds());
        loot.addAll(resolveReferences(items, definition.includedLoot()));
        loot.removeAll(resolveReferences(items, definition.excludedLoot()));
        targets.put(definition.entityTypeId(), new Target(
                definition.entityTypeId(),
                definition.generatorItemId(),
                List.copyOf(loot)
        ));
        order.put(definition.entityTypeId(), definition.order());
    }

    private static List<Identifier> resolveReferences(
            Registry<Item> items,
            List<MobFarmTargetReloadListener.ItemReference> references
    ) {
        LinkedHashSet<Identifier> resolved = new LinkedHashSet<>();
        for (MobFarmTargetReloadListener.ItemReference reference : references) {
            if (reference.tag()) {
                TagKey<Item> tag = TagKey.create(Registries.ITEM, reference.id());
                for (Holder<Item> holder : items.getTagOrEmpty(tag)) {
                    Identifier itemId = items.getKey(holder.value());
                    if (itemId != null) {
                        resolved.add(itemId);
                    }
                }
            } else {
                if (!items.containsKey(reference.id())) {
                    throw new IllegalArgumentException("unknown loot item " + reference.id());
                }
                resolved.add(reference.id());
            }
        }
        return List.copyOf(resolved);
    }

    private static void addDiscoveredTarget(
            Registry<EntityType<?>> entityTypes,
            Registry<Item> items,
            Family family,
            Identifier id,
            Map<Identifier, Target> targetMap
    ) {
        try {
            EntityType<?> type = entityTypes.getOptional(id).orElse(null);
            if (type == null) {
                return;
            }
            targetMap.put(id, new Target(id, defaultGeneratorItem(items, id), lootItems(items, type, id)));
        } catch (RuntimeException | LinkageError exception) {
            TradingCells.LOGGER.warn(
                    "Discarding invalid {} mob-farm target '{}'.",
                    family.id(),
                    id,
                    exception
            );
        }
    }

    private static Identifier defaultGeneratorItem(Registry<Item> items, Identifier entityTypeId) {
        Identifier spawnEgg = Identifier.fromNamespaceAndPath(
                entityTypeId.getNamespace(),
                entityTypeId.getPath() + "_spawn_egg"
        );
        return items.containsKey(spawnEgg) ? spawnEgg : items.getKey(Items.SPAWNER);
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
        LinkedHashSet<Identifier> result = new LinkedHashSet<>();
        for (MobFarmLootTableReloadListener.LootReference reference
                : MobFarmLootTableReloadListener.references(lootTableKey.orElseThrow().identifier())) {
            if (reference.tag()) {
                TagKey<Item> tag = TagKey.create(Registries.ITEM, reference.id());
                for (Holder<Item> holder : items.getTagOrEmpty(tag)) {
                    Identifier itemId = items.getKey(holder.value());
                    if (itemId != null) {
                        result.add(itemId);
                    }
                }
            } else if (items.containsKey(reference.id())
                    && !isImpossibleFarmDrop(entityTypeId, reference.id())) {
                result.add(reference.id());
            }
        }
        return List.copyOf(result);
    }

    private static boolean isImpossibleFarmDrop(Identifier entityTypeId, Identifier itemId) {
        String entity = entityTypeId.toString();
        String item = itemId.toString();
        return "minecraft:zombie".equals(entity)
                        && ("minecraft:red_mushroom".equals(item)
                        || "minecraft:music_disc_lava_chicken".equals(item))
                || "minecraft:husk".equals(entity) && "minecraft:rabbit_foot".equals(item)
                || "minecraft:ghast".equals(entity) && "minecraft:music_disc_tears".equals(item);
    }

    private static Map<Family, List<Target>> fallbackTargets() {
        Map<Family, List<Target>> fallback = new EnumMap<>(Family.class);
        for (Family family : Family.values()) {
            fallback.put(family, BASE_IDS.get(family).stream()
                    .filter(BuiltInRegistries.ENTITY_TYPE::containsKey)
                    .map(id -> new Target(id, defaultGeneratorItem(BuiltInRegistries.ITEM, id), List.of()))
                    .toList());
        }
        return Map.copyOf(fallback);
    }

    private static List<Identifier> ids(String... paths) {
        return java.util.Arrays.stream(paths).map(Identifier::withDefaultNamespace).toList();
    }

    public enum Family {
        SKELETON("skeleton", EntityTypeTags.SKELETONS),
        ZOMBIE("zombie", EntityTypeTags.ZOMBIES),
        RAIDER("raider", customFamilyTag("raider_farm_targets")),
        CREEPER("creeper", customFamilyTag("creeper_farm_targets")),
        ARTHROPOD("arthropod", customFamilyTag("arthropod_farm_targets")),
        SLIME("slime", customFamilyTag("slime_farm_targets")),
        GUARDIAN("guardian", customFamilyTag("guardian_farm_targets")),
        PIGLIN("piglin", customFamilyTag("piglin_farm_targets")),
        BLAZE("blaze", customFamilyTag("blaze_farm_targets")),
        GHAST("ghast", customFamilyTag("ghast_farm_targets")),
        ENDERMAN("enderman", customFamilyTag("enderman_farm_targets")),
        SHULKER("shulker", customFamilyTag("shulker_farm_targets")),
        BREEZE("breeze", customFamilyTag("breeze_farm_targets")),
        PHANTOM("phantom", customFamilyTag("phantom_farm_targets"));

        private final Identifier id;
        private final TagKey<EntityType<?>> tag;

        Family(String path, TagKey<EntityType<?>> tag) {
            this.id = Identifier.fromNamespaceAndPath(TradingCells.MOD_ID, path);
            this.tag = tag;
        }

        public Identifier id() {
            return id;
        }

        public TagKey<EntityType<?>> tag() {
            return tag;
        }

        public static Optional<Family> fromId(Identifier id) {
            for (Family family : values()) {
                if (family.id.equals(id)) {
                    return Optional.of(family);
                }
            }
            return Optional.empty();
        }

        private static TagKey<EntityType<?>> customFamilyTag(String path) {
            return TagKey.create(
                    Registries.ENTITY_TYPE,
                    Identifier.fromNamespaceAndPath(TradingCells.MOD_ID, path)
            );
        }
    }

    public record Target(
            Identifier entityTypeId,
            Identifier generatorItemId,
            List<Identifier> lootItemIds
    ) {
        public Target {
            lootItemIds = List.copyOf(lootItemIds);
        }
    }
}
