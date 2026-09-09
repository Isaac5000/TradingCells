package com.cosmocraft.trading_cells.feature.farmer.adapters.input;

import com.cosmocraft.trading_cells.feature.farmer.domain.model.FarmerKind;
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

/** Loads additive schema-v1 crop definitions before the immutable catalog is rebuilt. */
final class FarmerCropReloadListener extends SimpleJsonResourceReloadListener<JsonElement> {
    static final int SCHEMA_VERSION = 1;
    private static final Codec<JsonElement> JSON_CODEC = Codec.PASSTHROUGH.xmap(
            dynamic -> dynamic.convert(JsonOps.INSTANCE).getValue(),
            element -> new Dynamic<>(JsonOps.INSTANCE, element)
    );
    private static final AtomicReference<List<Definition>> DEFINITIONS = new AtomicReference<>(List.of());

    FarmerCropReloadListener() {
        super(JSON_CODEC, new FileToIdConverter("trading_cells/farmer_crop", ".json"));
    }

    static List<Definition> definitions(FarmerKind kind) {
        return DEFINITIONS.get().stream().filter(definition -> definition.kind() == kind).toList();
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> resources, ResourceManager manager, ProfilerFiller profiler) {
        List<Definition> parsed = new ArrayList<>();
        resources.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            try {
                parsed.add(parse(entry.getKey(), entry.getValue()));
            } catch (RuntimeException exception) {
                TradingCells.LOGGER.warn("Discarding invalid farmer crop descriptor '{}': {}",
                        entry.getKey(), exception.getMessage());
            }
        });
        parsed.sort(Comparator.comparing(definition -> definition.sourceId().toString()));
        DEFINITIONS.set(List.copyOf(parsed));
    }

    private static Definition parse(Identifier sourceId, JsonElement element) {
        if (!element.isJsonObject()) {
            throw new IllegalArgumentException("root must be an object");
        }
        JsonObject object = element.getAsJsonObject();
        int schema = integer(object, "schema_version");
        if (schema != SCHEMA_VERSION) {
            throw new IllegalArgumentException("unsupported schema_version " + schema);
        }
        FarmerKind kind = switch (string(object, "kind")) {
            case "villager" -> FarmerKind.VILLAGER;
            case "piglin" -> FarmerKind.PIGLIN;
            default -> throw new IllegalArgumentException("kind must be villager or piglin");
        };
        Identifier input = identifier(object, "input");
        Identifier block = identifier(object, "block");
        Identifier support = identifier(object, "support");
        String growthStyle = optionalString(object, "growth_style", "natural");
        String renderSupport = optionalString(object, "render_support", "floor");
        int visualStages = optionalInteger(object, "visual_stages", 8);
        if (visualStages < 2 || visualStages > 64) {
            throw new IllegalArgumentException("visual_stages must be between 2 and 64");
        }
        JsonArray outputArray = object.has("outputs") && object.get("outputs").isJsonArray()
                ? object.getAsJsonArray("outputs")
                : new JsonArray();
        List<Output> outputs = new ArrayList<>();
        for (JsonElement outputElement : outputArray) {
            if (!outputElement.isJsonObject()) {
                throw new IllegalArgumentException("outputs entries must be objects");
            }
            JsonObject output = outputElement.getAsJsonObject();
            outputs.add(new Output(
                    identifier(output, "item"),
                    optionalInteger(output, "base_count", 1),
                    optionalInteger(output, "fortune_count", 0),
                    optionalInteger(output, "chance", 10_000),
                    optionalInteger(output, "fortune_chance", 0),
                    optionalInteger(output, "maximum_chance", 10_000),
                    optionalBoolean(output, "requires_silk_touch", false)
            ));
        }
        return new Definition(sourceId, kind, input, block, support, growthStyle,
                renderSupport, visualStages, outputs);
    }

    private static Identifier identifier(JsonObject object, String key) {
        Identifier value = Identifier.tryParse(string(object, key));
        if (value == null) {
            throw new IllegalArgumentException(key + " must be a valid identifier");
        }
        return value;
    }

    private static String string(JsonObject object, String key) {
        JsonElement value = object.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException(key + " must be a string");
        }
        return value.getAsString();
    }

    private static String optionalString(JsonObject object, String key, String fallback) {
        return object.has(key) ? string(object, key) : fallback;
    }

    private static int integer(JsonObject object, String key) {
        JsonElement value = object.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            throw new IllegalArgumentException(key + " must be an integer");
        }
        try {
            return value.getAsBigDecimal().intValueExact();
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException(key + " must be an integer", exception);
        }
    }

    private static int optionalInteger(JsonObject object, String key, int fallback) {
        return object.has(key) ? integer(object, key) : fallback;
    }

    private static boolean optionalBoolean(JsonObject object, String key, boolean fallback) {
        JsonElement value = object.get(key);
        if (value == null) {
            return fallback;
        }
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean()) {
            throw new IllegalArgumentException(key + " must be a boolean");
        }
        return value.getAsBoolean();
    }

    record Definition(
            Identifier sourceId,
            FarmerKind kind,
            Identifier input,
            Identifier block,
            Identifier support,
            String growthStyle,
            String renderSupport,
            int visualStages,
            List<Output> outputs
    ) {
        Definition {
            outputs = List.copyOf(outputs);
        }
    }

    record Output(
            Identifier item,
            int baseCount,
            int fortuneCount,
            int chance,
            int fortuneChance,
            int maximumChance,
            boolean requiresSilkTouch
    ) {
    }
}
