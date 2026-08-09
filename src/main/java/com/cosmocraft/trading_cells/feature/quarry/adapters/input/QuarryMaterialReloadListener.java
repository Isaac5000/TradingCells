package com.cosmocraft.trading_cells.feature.quarry.adapters.input;

import com.cosmocraft.trading_cells.feature.quarry.domain.model.QuarryKind;
import com.cosmocraft.trading_cells.feature.quarry.domain.model.QuarryUpgradeTier;
import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.TradingCells;
import com.cosmocraft.trading_cells.shared.machines.domain.model.MachineActivityController;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

final class QuarryMaterialReloadListener extends SimpleJsonResourceReloadListener<JsonElement> {
    private static final Codec<JsonElement> JSON_CODEC = Codec.PASSTHROUGH.xmap(
            dynamic -> dynamic.convert(JsonOps.INSTANCE).getValue(),
            element -> new Dynamic<>(JsonOps.INSTANCE, element)
    );

    QuarryMaterialReloadListener() {
        super(JSON_CODEC, new FileToIdConverter("quarry_materials", ".json"));
    }

    @Override
    protected void apply(
            Map<Identifier, JsonElement> resources,
            ResourceManager resourceManager,
            ProfilerFiller profiler
    ) {
        List<QuarryMaterialDefinition> definitions = new ArrayList<>();
        Set<String> exclusions = new HashSet<>();
        resources.forEach((fileId, json) -> {
            try {
                ParsedDefinition parsed = parse(fileId, json.getAsJsonObject());
                if (parsed.excluded()) {
                    exclusions.add(QuarryMaterialCatalog.definitionKey(parsed.kind(), parsed.id()));
                } else {
                    definitions.add(parsed.definition());
                }
            } catch (RuntimeException exception) {
                TradingCells.LOGGER.warn(
                        "Discarding invalid quarry material datapack definition '{}'.",
                        fileId,
                        exception
                );
            }
        });
        QuarryMaterialCatalog.replaceDatapackDefinitions(definitions, exclusions);
        MachineActivityController.wakeAll();
    }

    private static ParsedDefinition parse(Identifier fileId, JsonObject json) {
        QuarryKind kind = parseKind(string(json, "kind", string(json, "dimension", "overworld")));
        Identifier id = identifier(json, "id", fileId);
        boolean excluded = bool(json, "excluded", false) || !bool(json, "enabled", true);
        if (excluded) {
            return new ParsedDefinition(kind, id, true, null);
        }

        QuarryUpgradeTier minimumUpgrade = parseTier(string(json, "minimum_upgrade", "copper"));
        double minimumPickaxeSpeed = decimal(json, "minimum_pickaxe_speed", 2.0D);
        int minimumAmount = integer(json, "minimum_amount", 1);
        int maximumAmount = integer(json, "maximum_amount", minimumAmount);
        Identifier normalResult = requiredIdentifier(json, "normal_result");
        Identifier silkResult = identifier(json, "silk_result", normalResult);
        Identifier configuredDeepResult = optionalIdentifier(json, "deep_silk_result");
        if (!BuiltInRegistries.ITEM.containsKey(normalResult)) {
            throw new JsonParseException("Unknown normal_result item: " + normalResult);
        }
        if (!isBlockItem(silkResult)) {
            throw new JsonParseException("Unknown silk_result block item: " + silkResult);
        }
        Identifier deepResult = isBlockItem(configuredDeepResult) ? configuredDeepResult : null;
        if (configuredDeepResult != null && deepResult == null) {
            TradingCells.LOGGER.warn(
                    "Quarry material '{}' declares missing deep variant '{}'; the default ore will be used.",
                    fileId,
                    configuredDeepResult
            );
        }
        boolean fortune = bool(json, "fortune", true);
        boolean rock = bool(json, "rock", false);
        boolean useBlockLoot = bool(json, "use_block_loot", false);
        String sourceMod = string(json, "source_mod", id.getNamespace());
        boolean dynamic = bool(json, "dynamic", !"minecraft".equals(sourceMod));
        QuarryMaterialDefinition.Pool pool = parsePool(string(json, "pool", "normal"));
        List<Integer> weights = parseWeights(json, minimumUpgrade);

        QuarryMaterialDefinition definition = new QuarryMaterialDefinition(
                id,
                kind,
                minimumUpgrade,
                minimumPickaxeSpeed,
                weights,
                minimumAmount,
                maximumAmount,
                normalResult,
                silkResult,
                deepResult,
                fortune,
                rock,
                dynamic,
                useBlockLoot,
                sourceMod,
                pool
        );
        return new ParsedDefinition(kind, id, false, definition);
    }

    private static List<Integer> parseWeights(JsonObject json, QuarryUpgradeTier minimumUpgrade) {
        JsonElement weightsElement = json.get("weights");
        if (weightsElement != null) {
            JsonArray values = weightsElement.getAsJsonArray();
            if (values.size() != QuarryMaterialDefinition.TIER_COUNT) {
                throw new JsonParseException("weights must contain six values");
            }
            List<Integer> weights = new ArrayList<>(QuarryMaterialDefinition.TIER_COUNT);
            values.forEach(value -> weights.add(Math.max(0, value.getAsInt())));
            return List.copyOf(weights);
        }
        int weight = Math.max(0, integer(json, "weight", 100));
        List<Integer> weights = new ArrayList<>(QuarryMaterialDefinition.TIER_COUNT);
        for (QuarryUpgradeTier tier : QuarryUpgradeTier.values()) {
            weights.add(tier.unlocks(minimumUpgrade) ? weight : 0);
        }
        return List.copyOf(weights);
    }

    private static QuarryKind parseKind(String value) {
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "overworld", "villager", "quarry" -> QuarryKind.VILLAGER;
            case "nether", "piglin", "piglin_quarry" -> QuarryKind.PIGLIN;
            default -> throw new JsonParseException("Unknown quarry kind: " + value);
        };
    }

    private static QuarryUpgradeTier parseTier(String value) {
        try {
            return QuarryUpgradeTier.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new JsonParseException("Unknown quarry upgrade tier: " + value, exception);
        }
    }

    private static QuarryMaterialDefinition.Pool parsePool(String value) {
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "normal" -> QuarryMaterialDefinition.Pool.NORMAL;
            case "protected", "protected_rare", "rare" -> QuarryMaterialDefinition.Pool.PROTECTED_RARE;
            default -> throw new JsonParseException("Unknown quarry rarity pool: " + value);
        };
    }

    private static Identifier requiredIdentifier(JsonObject json, String key) {
        JsonElement value = json.get(key);
        if (value == null) {
            throw new JsonParseException("Missing required field " + key);
        }
        return parseIdentifier(value.getAsString(), key);
    }

    private static Identifier identifier(JsonObject json, String key, Identifier fallback) {
        JsonElement value = json.get(key);
        return value == null ? fallback : parseIdentifier(value.getAsString(), key);
    }

    private static Identifier optionalIdentifier(JsonObject json, String key) {
        JsonElement value = json.get(key);
        return value == null || value.isJsonNull() ? null : parseIdentifier(value.getAsString(), key);
    }

    private static boolean isBlockItem(Identifier id) {
        return id != null
                && BuiltInRegistries.BLOCK.containsKey(id)
                && BuiltInRegistries.ITEM.containsKey(id);
    }

    private static Identifier parseIdentifier(String value, String key) {
        Identifier identifier = Identifier.tryParse(value);
        if (identifier == null) {
            throw new JsonParseException("Invalid identifier in " + key + ": " + value);
        }
        return identifier;
    }

    private static String string(JsonObject json, String key, String fallback) {
        JsonElement value = json.get(key);
        return value == null ? fallback : value.getAsString();
    }

    private static int integer(JsonObject json, String key, int fallback) {
        JsonElement value = json.get(key);
        return value == null ? fallback : value.getAsInt();
    }

    private static double decimal(JsonObject json, String key, double fallback) {
        JsonElement value = json.get(key);
        return value == null ? fallback : value.getAsDouble();
    }

    private static boolean bool(JsonObject json, String key, boolean fallback) {
        JsonElement value = json.get(key);
        return value == null ? fallback : value.getAsBoolean();
    }

    private record ParsedDefinition(
            QuarryKind kind,
            Identifier id,
            boolean excluded,
            QuarryMaterialDefinition definition
    ) {
    }
}
