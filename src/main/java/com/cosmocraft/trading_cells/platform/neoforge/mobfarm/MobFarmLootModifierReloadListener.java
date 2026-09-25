package com.cosmocraft.trading_cells.platform.neoforge.mobfarm;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

/** Indexes loot-table injections so dynamic drops are visible before the first farm cycle. */
final class MobFarmLootModifierReloadListener extends SimpleJsonResourceReloadListener<JsonElement> {
    private static final Codec<JsonElement> JSON_CODEC = Codec.PASSTHROUGH.xmap(
            dynamic -> dynamic.convert(JsonOps.INSTANCE).getValue(),
            element -> new Dynamic<>(JsonOps.INSTANCE, element)
    );

    MobFarmLootModifierReloadListener() {
        super(JSON_CODEC, new FileToIdConverter("loot_modifiers", ".json"));
    }

    @Override
    protected void apply(
            Map<Identifier, JsonElement> resources,
            ResourceManager resourceManager,
            ProfilerFiller profiler
    ) {
        Map<Identifier, LinkedHashSet<Identifier>> indexed = new LinkedHashMap<>();
        resources.values().forEach(json -> collect(json, indexed));
        Map<Identifier, List<Identifier>> result = new LinkedHashMap<>();
        indexed.forEach((target, tables) -> result.put(target, List.copyOf(tables)));
        MobFarmLootTableReloadListener.MODIFIER_TABLES.set(Map.copyOf(result));
    }

    private static void collect(JsonElement element, Map<Identifier, LinkedHashSet<Identifier>> indexed) {
        if (element.isJsonArray()) {
            element.getAsJsonArray().forEach(value -> collect(value, indexed));
        } else if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            Identifier table = Identifier.tryParse(string(object, "table"));
            if (table != null) {
                findTargets(object, table, indexed);
            }
            object.entrySet().forEach(entry -> collect(entry.getValue(), indexed));
        }
    }

    private static void findTargets(
            JsonElement element,
            Identifier table,
            Map<Identifier, LinkedHashSet<Identifier>> indexed
    ) {
        if (element.isJsonArray()) {
            element.getAsJsonArray().forEach(value -> findTargets(value, table, indexed));
        } else if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            if ("neoforge:loot_table_id".equals(string(object, "condition"))) {
                Identifier target = Identifier.tryParse(string(object, "loot_table_id"));
                if (target != null) {
                    indexed.computeIfAbsent(target, ignored -> new LinkedHashSet<>()).add(table);
                }
            }
            object.entrySet().forEach(entry -> findTargets(entry.getValue(), table, indexed));
        }
    }

    private static String string(JsonObject object, String key) {
        JsonElement value = object.get(key);
        return value == null || !value.isJsonPrimitive() ? "" : value.getAsString();
    }
}
