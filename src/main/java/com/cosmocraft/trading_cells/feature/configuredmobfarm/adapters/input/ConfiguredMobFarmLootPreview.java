package com.cosmocraft.trading_cells.feature.configuredmobfarm.adapters.input;

import com.cosmocraft.trading_cells.feature.configuredmobfarm.domain.model.ConfiguredMobFarmDropRules.BaseDrop;
import com.cosmocraft.trading_cells.platform.neoforge.mobfarm.MobFarmLootTables;
import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

/** Analyses loaded tables without rolling loot or advancing the world's random sequences. */
public final class ConfiguredMobFarmLootPreview {
    private final ServerLevel level;
    private final LootContext context;
    private final int looting;
    private final com.mojang.serialization.DynamicOps<JsonElement> ops;
    private int visits;

    public record Analysis(Map<Identifier, BaseDrop> drops, boolean complete) { }

    private ConfiguredMobFarmLootPreview(ServerLevel level, LootContext context, int looting) {
        this.level = level;
        this.context = context;
        this.looting = Math.clamp(looting, 0, 255);
        ops = level.registryAccess().createSerializationContext(JsonOps.INSTANCE);
    }

    public static Map<Identifier, BaseDrop> calculate(ServerLevel level, Identifier targetId, ItemStack sword, int looting, int kills) {
        LivingEntity target = MobFarmLootTables.createTarget(level, targetId);
        return calculate(level, target, sword, looting, kills);
    }

    public static Map<Identifier, BaseDrop> calculate(ServerLevel level, LivingEntity target, ItemStack sword, int looting, int kills) {
        return analyse(level, target, sword, looting, kills).drops();
    }

    public static Analysis analyse(ServerLevel level, LivingEntity target, ItemStack sword, int looting, int kills) {
        if (target == null || target.getLootTable().isEmpty()) { return new Analysis(Map.of(), true); }
        var attacker = FakePlayerFactory.getMinecraft(level);
        ItemStack previous = attacker.getMainHandItem().copy();
        try {
            attacker.setItemSlot(EquipmentSlot.MAINHAND, sword.copy());
            var damage = level.damageSources().playerAttack(attacker);
            var params = new LootParams.Builder(level).withParameter(LootContextParams.THIS_ENTITY, target)
                    .withParameter(LootContextParams.ORIGIN, target.position())
                    .withParameter(LootContextParams.DAMAGE_SOURCE, damage)
                    .withParameter(LootContextParams.ATTACKING_ENTITY, attacker)
                    .withParameter(LootContextParams.DIRECT_ATTACKING_ENTITY, attacker)
                    .withParameter(LootContextParams.LAST_DAMAGE_PLAYER, attacker).create(LootContextParamSets.ENTITY);
            var context = new LootContext.Builder(params).withOptionalRandomSeed(1).create(Optional.empty());
            var analyser = new ConfiguredMobFarmLootPreview(level, context, looting);
            var result = new LinkedHashMap<Identifier, BaseDrop>();
            analyser.table(target.getLootTable().orElseThrow(), 0).forEach((id, summary) -> {
                Summary cycle = summary.repeat(Math.clamp(kills, 1, 1024), Math.clamp(kills, 1, 1024));
                if (cycle.maximum > 0 && cycle.zero < 1) {
                    result.put(id, new BaseDrop((int) Math.round((1 - cycle.zero) * 1_000_000),
                            Math.max(1, cycle.minimum), Math.max(1, cycle.maximum)));
                }
            });
            return new Analysis(Map.copyOf(result), true);
        } catch (RuntimeException ignored) {
            return new Analysis(Map.of(), false);
        } finally {
            attacker.setItemSlot(EquipmentSlot.MAINHAND, previous);
        }
    }

    private Map<Identifier, Summary> table(ResourceKey<LootTable> key, int depth) {
        if (depth > 16 || ++visits > 512) { throw new IllegalArgumentException("Preview complexity limit"); }
        var table = level.getServer().reloadableRegistries().getLootTable(key);
        JsonObject json = LootTable.DIRECT_CODEC.encodeStart(ops, table).getOrThrow().getAsJsonObject();
        var result = new LinkedHashMap<Identifier, Summary>();
        for (JsonElement value : array(json, "pools")) {
            JsonObject pool = value.getAsJsonObject();
            double chance = conditions(array(pool, "conditions"));
            if (chance == 0) { continue; }
            List<Map<Identifier, Summary>> entries = new ArrayList<>();
            List<Double> weights = new ArrayList<>();
            double weight = 0;
            for (JsonElement raw : array(pool, "entries")) {
                JsonObject entry = raw.getAsJsonObject();
                double condition = conditions(array(entry, "conditions"));
                if (condition == 0) { continue; }
                if (condition != 1) { throw new IllegalArgumentException("Conditional weighted entry"); }
                double entryWeight = entry.has("weight") ? entry.get("weight").getAsDouble() : 1;
                entries.add(entry(entry, depth));
                weights.add(entryWeight);
                weight += entryWeight;
            }
            if (weight == 0) { continue; }
            Set<Identifier> ids = new HashSet<>();
            entries.forEach(entry -> ids.addAll(entry.keySet()));
            int[] rolls = range(pool.get("rolls"));
            for (Identifier id : ids) {
                double zero = 0;
                int minimum = Integer.MAX_VALUE, maximum = 0;
                for (int index = 0; index < entries.size(); index++) {
                    Summary summary = entries.get(index).getOrDefault(id, Summary.EMPTY);
                    zero += weights.get(index) / weight * summary.zero;
                    if (summary.maximum > 0) { minimum = Math.min(minimum, summary.minimum); }
                    maximum = Math.max(maximum, summary.maximum);
                }
                Summary summary = new Summary(minimum == Integer.MAX_VALUE ? 0 : minimum, maximum, Math.clamp(zero, 0, 1))
                        .repeat(rolls[0], rolls[1]).chance(chance);
                var transformed = functions(Map.of(id, summary), array(pool, "functions"));
                transformed.forEach((item, drop) -> result.merge(item, drop, Summary::plus));
            }
        }
        return functions(result, array(json, "functions"));
    }

    private Map<Identifier, Summary> entry(JsonObject entry, int depth) {
        String type = entry.get("type").getAsString();
        Map<Identifier, Summary> result;
        switch (type) {
            case "minecraft:empty" -> result = Map.of();
            case "minecraft:item" -> result = Map.of(Identifier.parse(entry.get("name").getAsString()), new Summary(1, 1, 0));
            case "minecraft:loot_table" -> result = table(ResourceKey.create(Registries.LOOT_TABLE,
                    Identifier.parse(entry.get("value").getAsString())), depth + 1);
            case "minecraft:alternatives" -> {
                result = Map.of();
                for (JsonElement child : array(entry, "children")) {
                    double condition = conditions(array(child.getAsJsonObject(), "conditions"));
                    if (condition == 0) { continue; }
                    if (condition != 1) { throw new IllegalArgumentException("Random alternative"); }
                    result = entry(child.getAsJsonObject(), depth + 1);
                    break;
                }
            }
            default -> throw new IllegalArgumentException("Unknown loot entry");
        }
        return functions(result, array(entry, "functions"));
    }

    private Map<Identifier, Summary> functions(Map<Identifier, Summary> source, JsonArray functions) {
        var result = new LinkedHashMap<>(source);
        for (JsonElement value : functions) {
            JsonObject function = value.getAsJsonObject();
            double chance = conditions(array(function, "conditions"));
            if (chance == 0) { continue; }
            if (chance != 1) { throw new IllegalArgumentException("Conditional loot function"); }
            switch (function.get("function").getAsString()) {
                case "minecraft:set_count" -> {
                    int[] range = range(function.get("count"), true);
                    boolean add = function.has("add") && function.get("add").getAsBoolean();
                    if (add && range[0] < 0) { throw new IllegalArgumentException("Subtractive count function"); }
                    // ItemStack exposes nonpositive counts as zero before a later looting increase.
                    double zero = range[0] > 0 ? 0 : Math.min(1, (1.0 - range[0]) / (range[1] - range[0] + 1.0));
                    Summary count = new Summary(Math.max(1, range[0]), Math.max(0, range[1]), zero);
                    result.replaceAll((id, old) -> add ? old.plus(count) : count);
                }
                case "minecraft:enchanted_count_increase" -> {
                    if (looting == 0) { continue; }
                    if (!function.get("enchantment").getAsString().equals("minecraft:looting")) { throw new IllegalArgumentException("Other enchantment"); }
                    JsonObject count = function.getAsJsonObject("count");
                    if (!count.get("type").getAsString().equals("minecraft:uniform") || count.get("min").getAsDouble() != 0
                            || count.get("max").getAsDouble() != 1
                            || function.has("limit") && function.get("limit").getAsInt() != 0) { throw new IllegalArgumentException("Custom count increase"); }
                    result.replaceAll((id, old) -> old.plus(new Summary(1, looting, 0.5 / looting)));
                }
                case "minecraft:set_components", "minecraft:set_damage", "minecraft:enchant_randomly", "minecraft:enchant_with_levels" -> { }
                case "minecraft:furnace_smelt" -> {
                    var smelted = new LinkedHashMap<Identifier, Summary>();
                    for (var entry : result.entrySet()) {
                        ItemStack item = BuiltInRegistries.ITEM.getOptional(entry.getKey()).orElseThrow().getDefaultInstance();
                        var input = new net.minecraft.world.item.crafting.SingleRecipeInput(item);
                        Identifier id = level.recipeAccess().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.SMELTING, input, level)
                                .map(recipe -> BuiltInRegistries.ITEM.getKey(recipe.value().assemble(input).getItem())).orElse(entry.getKey());
                        smelted.merge(id, entry.getValue(), Summary::plus);
                    }
                    result = smelted;
                }
                default -> throw new IllegalArgumentException("Unknown count or item transformation");
            }
        }
        return result;
    }

    private double conditions(JsonArray conditions) {
        double result = 1;
        for (JsonElement condition : conditions) { result *= condition(condition.getAsJsonObject()); }
        return result;
    }

    private double condition(JsonObject value) {
        return switch (value.get("condition").getAsString()) {
            case "minecraft:random_chance" -> value.get("chance").getAsDouble();
            case "minecraft:killed_by_player" -> 1;
            case "minecraft:random_chance_with_enchanted_bonus" -> {
                JsonObject enchanted = value.getAsJsonObject("enchanted_chance");
                if (!enchanted.get("type").getAsString().equals("minecraft:linear")) { throw new IllegalArgumentException("Custom chance"); }
                yield Math.clamp(looting == 0 ? value.get("unenchanted_chance").getAsDouble()
                        : enchanted.get("base").getAsDouble() + (looting - 1) * enchanted.get("per_level_above_first").getAsDouble(), 0, 1);
            }
            case "minecraft:entity_properties", "minecraft:match_tool", "minecraft:damage_source_properties" ->
                    LootItemCondition.DIRECT_CODEC.parse(ops, value).getOrThrow().test(context) ? 1 : 0;
            case "minecraft:all_of" -> conditions(array(value, "terms"));
            case "minecraft:any_of" -> {
                double none = 1;
                for (JsonElement term : array(value, "terms")) { none *= 1 - condition(term.getAsJsonObject()); }
                yield 1 - none;
            }
            case "minecraft:inverted" -> 1 - condition(value.getAsJsonObject("term"));
            default -> throw new IllegalArgumentException("Context-dependent condition");
        };
    }

    private static int[] range(JsonElement value) {
        return range(value, false);
    }

    private static int[] range(JsonElement value, boolean allowNegative) {
        if (value == null) { return new int[]{1, 1}; }
        if (value.isJsonPrimitive()) { int count = value.getAsInt(); return checkedRange(count, count, allowNegative); }
        JsonObject number = value.getAsJsonObject();
        if (!number.get("type").getAsString().equals("minecraft:uniform")) { throw new IllegalArgumentException("Unknown number provider"); }
        return checkedRange(number.get("min").getAsInt(), number.get("max").getAsInt(), allowNegative);
    }

    private static int[] checkedRange(int minimum, int maximum, boolean allowNegative) {
        if (minimum < (allowNegative ? -1024 : 0) || maximum < minimum || maximum > 1024) {
            throw new IllegalArgumentException("Unbounded preview");
        }
        return new int[]{minimum, maximum};
    }

    private static JsonArray array(JsonObject object, String key) { return object.has(key) ? object.getAsJsonArray(key) : new JsonArray(); }

    private record Summary(int minimum, int maximum, double zero) {
        static final Summary EMPTY = new Summary(0, 0, 1);
        Summary chance(double probability) { return new Summary(minimum, maximum, 1 - probability + probability * zero); }
        Summary plus(Summary other) {
            if (maximum == 0) { return other; }
            if (other.maximum == 0) { return this; }
            int min = zero > 0 && other.zero > 0 ? Math.min(minimum, other.minimum)
                    : zero > 0 ? other.minimum : other.zero > 0 ? minimum : minimum + other.minimum;
            return new Summary(min, Math.addExact(maximum, other.maximum), zero * other.zero);
        }
        Summary repeat(int min, int max) {
            double none = 0;
            for (int count = min; count <= max; count++) { none += Math.pow(zero, count) / (max - min + 1); }
            return new Summary(zero == 0 ? Math.max(1, min) * minimum : minimum, Math.multiplyExact(maximum, max), none);
        }
    }
}
