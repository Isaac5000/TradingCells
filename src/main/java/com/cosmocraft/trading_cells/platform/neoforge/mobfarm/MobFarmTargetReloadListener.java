package com.cosmocraft.trading_cells.platform.neoforge.mobfarm;

import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.TradingCells;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

/** Loads the public schema-v1 descriptors used to extend registered mob-farm families. */
public final class MobFarmTargetReloadListener extends SimpleJsonResourceReloadListener<JsonElement> {
    static final int SCHEMA_VERSION = 1;
    private static final Codec<JsonElement> JSON_CODEC = Codec.PASSTHROUGH.xmap(
            dynamic -> dynamic.convert(JsonOps.INSTANCE).getValue(),
            element -> new Dynamic<>(JsonOps.INSTANCE, element)
    );
    private static final AtomicReference<List<Definition>> DEFINITIONS = new AtomicReference<>(List.of());

    MobFarmTargetReloadListener() {
        super(JSON_CODEC, new FileToIdConverter("trading_cells/mob_farm_target", ".json"));
    }

    static List<Definition> definitions(MobFarmCatalog.Family family) {
        return DEFINITIONS.get().stream().filter(definition -> definition.family() == family).toList();
    }

    @Override
    protected void apply(
            Map<Identifier, JsonElement> resources,
            ResourceManager resourceManager,
            ProfilerFiller profiler
    ) {
        DEFINITIONS.set(parseValid(resources));
    }

    /** Returns the descriptors accepted from one reload batch in deterministic source order. */
    public static List<Identifier> validDescriptorIds(Map<Identifier, JsonElement> resources) {
        return parseValid(resources).stream().map(Definition::sourceId).toList();
    }

    private static List<Definition> parseValid(Map<Identifier, JsonElement> resources) {
        List<Definition> parsed = new ArrayList<>();
        resources.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    try {
                        parsed.add(parse(entry.getKey(), entry.getValue()));
                    } catch (RuntimeException exception) {
                        TradingCells.LOGGER.warn(
                                "Discarding invalid mob-farm target descriptor '{}': {}",
                                entry.getKey(),
                                exception.getMessage()
                        );
                    }
                });
        parsed.sort(Comparator.comparing(definition -> definition.sourceId().toString()));
        return List.copyOf(parsed);
    }

    private static Definition parse(Identifier sourceId, JsonElement element) {
        if (!element.isJsonObject()) {
            throw new IllegalArgumentException("root must be an object");
        }
        JsonObject object = element.getAsJsonObject();
        int schemaVersion = requiredInt(object, "schema_version");
        if (schemaVersion != SCHEMA_VERSION) {
            throw new IllegalArgumentException("unsupported schema_version " + schemaVersion);
        }
        Identifier familyId = requiredIdentifier(object, "family");
        MobFarmCatalog.Family family = MobFarmCatalog.Family.fromId(familyId)
                .orElseThrow(() -> new IllegalArgumentException("unknown family " + familyId));
        Identifier entityTypeId = requiredIdentifier(object, "entity_type");
        Identifier generatorItemId = requiredIdentifier(object, "generator_item");
        int order = requiredInt(object, "order");

        JsonObject loot = optionalObject(object, "loot");
        List<ItemReference> included = loot == null ? List.of() : references(loot, "include");
        List<ItemReference> excluded = loot == null ? List.of() : references(loot, "exclude");
        return new Definition(
                sourceId,
                family,
                entityTypeId,
                generatorItemId,
                order,
                included,
                excluded
        );
    }

    private static List<ItemReference> references(JsonObject object, String key) {
        JsonElement value = object.get(key);
        if (value == null) {
            return List.of();
        }
        if (!value.isJsonArray()) {
            throw new IllegalArgumentException("loot." + key + " must be an array");
        }
        JsonArray array = value.getAsJsonArray();
        List<ItemReference> references = new ArrayList<>(array.size());
        for (JsonElement entry : array) {
            if (!entry.isJsonPrimitive() || !entry.getAsJsonPrimitive().isString()) {
                throw new IllegalArgumentException("loot." + key + " entries must be strings");
            }
            String raw = entry.getAsString();
            boolean tag = raw.startsWith("#");
            Identifier id = Identifier.tryParse(tag ? raw.substring(1) : raw);
            if (id == null) {
                throw new IllegalArgumentException("invalid item reference " + raw);
            }
            ItemReference reference = new ItemReference(id, tag);
            if (!references.contains(reference)) {
                references.add(reference);
            }
        }
        return List.copyOf(references);
    }

    private static JsonObject optionalObject(JsonObject object, String key) {
        JsonElement value = object.get(key);
        if (value == null) {
            return null;
        }
        if (!value.isJsonObject()) {
            throw new IllegalArgumentException(key + " must be an object");
        }
        return value.getAsJsonObject();
    }

    private static int requiredInt(JsonObject object, String key) {
        JsonElement value = object.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            throw new IllegalArgumentException(key + " must be an integer");
        }
        try {
            return value.getAsBigDecimal().intValueExact();
        } catch (ArithmeticException | NumberFormatException exception) {
            throw new IllegalArgumentException(key + " must be an integer", exception);
        }
    }

    private static Identifier requiredIdentifier(JsonObject object, String key) {
        JsonElement value = object.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException(key + " must be an identifier string");
        }
        Identifier id = Identifier.tryParse(value.getAsString());
        if (id == null) {
            throw new IllegalArgumentException("invalid " + key + " identifier");
        }
        return id;
    }

    record Definition(
            Identifier sourceId,
            MobFarmCatalog.Family family,
            Identifier entityTypeId,
            Identifier generatorItemId,
            int order,
            List<ItemReference> includedLoot,
            List<ItemReference> excludedLoot
    ) {
        Definition {
            includedLoot = List.copyOf(includedLoot);
            excludedLoot = List.copyOf(excludedLoot);
        }
    }

    record ItemReference(Identifier id, boolean tag) {
    }
}
