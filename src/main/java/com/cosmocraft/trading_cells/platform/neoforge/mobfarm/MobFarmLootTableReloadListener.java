package com.cosmocraft.trading_cells.platform.neoforge.mobfarm;

import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.TradingCells;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

/** Retains only item and item-tag references needed to describe loaded entity loot tables. */
final class MobFarmLootTableReloadListener extends SimpleJsonResourceReloadListener<JsonElement> {
    private static final int MAX_JSON_NODES = 16_384;
    private static final int MAX_LINKED_TABLES = 256;
    private static final Codec<JsonElement> JSON_CODEC = Codec.PASSTHROUGH.xmap(
            dynamic -> dynamic.convert(JsonOps.INSTANCE).getValue(),
            element -> new Dynamic<>(JsonOps.INSTANCE, element)
    );
    private static final AtomicReference<Map<Identifier, List<LootReference>>> REFERENCES =
            new AtomicReference<>(Map.of());
    static final AtomicReference<Map<Identifier, List<Identifier>>> MODIFIER_TABLES =
            new AtomicReference<>(Map.of());

    MobFarmLootTableReloadListener() {
        super(JSON_CODEC, new FileToIdConverter("loot_table", ".json"));
    }

    static List<LootReference> references(Identifier lootTableId) {
        return REFERENCES.get().getOrDefault(lootTableId, List.of());
    }

    static List<Identifier> modifierTables(Identifier lootTableId) {
        return MODIFIER_TABLES.get().getOrDefault(lootTableId, List.of());
    }

    @Override
    protected void apply(
            Map<Identifier, JsonElement> resources,
            ResourceManager resourceManager,
            ProfilerFiller profiler
    ) {
        Map<Identifier, TableReferences> direct = new LinkedHashMap<>();
        resources.forEach((id, json) -> {
            try {
                direct.put(id, collectReferences(json));
            } catch (RuntimeException exception) {
                TradingCells.LOGGER.warn(
                        "Could not inspect loot table '{}'; its dynamic mob-farm filters will be omitted.",
                        id,
                        exception
                );
            }
        });
        Map<Identifier, List<LootReference>> refreshed = new LinkedHashMap<>();
        direct.keySet().forEach(id -> {
            var pending = new ArrayDeque<Identifier>();
            var visited = new LinkedHashSet<Identifier>();
            var references = new LinkedHashSet<LootReference>();
            pending.add(id);
            while (!pending.isEmpty() && visited.size() < MAX_LINKED_TABLES) {
                Identifier next = pending.removeFirst();
                if (!visited.add(next)) { continue; }
                TableReferences table = direct.get(next);
                if (table == null) { continue; }
                references.addAll(table.items());
                table.tables().stream().filter(link -> !visited.contains(link)).forEach(pending::addLast);
            }
            if (!pending.isEmpty()) {
                TradingCells.LOGGER.warn("Mob-farm filter inspection reached the linked-table limit for '{}'", id);
            }
            refreshed.put(id, List.copyOf(references));
        });
        REFERENCES.set(Map.copyOf(refreshed));
    }

    private static TableReferences collectReferences(JsonElement root) {
        var items = new LinkedHashSet<LootReference>();
        var tables = new LinkedHashSet<Identifier>();
        var pending = new ArrayDeque<JsonElement>();
        pending.add(root);
        int nodes = 0;
        while (!pending.isEmpty()) {
            if (++nodes > MAX_JSON_NODES) { throw new IllegalArgumentException("Loot table inspection limit exceeded"); }
            JsonElement element = pending.removeFirst();
            if (element.isJsonArray()) {
                element.getAsJsonArray().forEach(pending::addLast);
            } else if (element.isJsonObject()) {
                JsonObject object = element.getAsJsonObject();
                String type = string(object, "type");
                Identifier id = Identifier.tryParse(string(object, "name"));
                if (id != null && (type.equals("minecraft:item") || type.equals("item"))) {
                    items.add(new LootReference(id, false));
                } else if (id != null && (type.equals("minecraft:tag") || type.equals("tag"))) {
                    items.add(new LootReference(id, true));
                } else if (type.equals("minecraft:loot_table") || type.equals("loot_table")) {
                    Identifier table = Identifier.tryParse(string(object, "value"));
                    if (table != null) { tables.add(table); }
                }
                object.entrySet().forEach(entry -> pending.addLast(entry.getValue()));
            }
        }
        return new TableReferences(List.copyOf(items), List.copyOf(tables));
    }

    private static String string(JsonObject object, String key) {
        JsonElement value = object.get(key);
        return value == null || !value.isJsonPrimitive() ? "" : value.getAsString();
    }

    record LootReference(Identifier id, boolean tag) {
    }

    private record TableReferences(List<LootReference> items, List<Identifier> tables) { }
}
