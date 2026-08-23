package com.cosmocraft.trading_cells.platform.neoforge.mobfarm;

import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.TradingCells;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
    private static final Codec<JsonElement> JSON_CODEC = Codec.PASSTHROUGH.xmap(
            dynamic -> dynamic.convert(JsonOps.INSTANCE).getValue(),
            element -> new Dynamic<>(JsonOps.INSTANCE, element)
    );
    private static final AtomicReference<Map<Identifier, List<LootReference>>> REFERENCES =
            new AtomicReference<>(Map.of());

    MobFarmLootTableReloadListener() {
        super(JSON_CODEC, new FileToIdConverter("loot_table", ".json"));
    }

    static List<LootReference> references(Identifier lootTableId) {
        return REFERENCES.get().getOrDefault(lootTableId, List.of());
    }

    @Override
    protected void apply(
            Map<Identifier, JsonElement> resources,
            ResourceManager resourceManager,
            ProfilerFiller profiler
    ) {
        Map<Identifier, List<LootReference>> refreshed = new LinkedHashMap<>();
        resources.forEach((id, json) -> {
            try {
                List<LootReference> references = new ArrayList<>();
                collectReferences(json, references);
                if (!references.isEmpty()) {
                    refreshed.put(id, List.copyOf(references));
                }
            } catch (RuntimeException exception) {
                TradingCells.LOGGER.warn(
                        "Could not inspect loot table '{}'; its dynamic mob-farm filters will be omitted.",
                        id,
                        exception
                );
            }
        });
        REFERENCES.set(Map.copyOf(refreshed));
    }

    private static void collectReferences(JsonElement element, List<LootReference> result) {
        if (element.isJsonArray()) {
            element.getAsJsonArray().forEach(child -> collectReferences(child, result));
            return;
        }
        if (!element.isJsonObject()) {
            return;
        }
        JsonObject object = element.getAsJsonObject();
        String type = string(object, "type");
        Identifier id = Identifier.tryParse(string(object, "name"));
        if (id != null && type.endsWith(":item")) {
            addDistinct(result, new LootReference(id, false));
        } else if (id != null && type.endsWith(":tag")) {
            addDistinct(result, new LootReference(id, true));
        }
        object.entrySet().forEach(entry -> collectReferences(entry.getValue(), result));
    }

    private static void addDistinct(List<LootReference> references, LootReference candidate) {
        if (!references.contains(candidate)) {
            references.add(candidate);
        }
    }

    private static String string(JsonObject object, String key) {
        JsonElement value = object.get(key);
        return value == null || !value.isJsonPrimitive() ? "" : value.getAsString();
    }

    record LootReference(Identifier id, boolean tag) {
    }
}
