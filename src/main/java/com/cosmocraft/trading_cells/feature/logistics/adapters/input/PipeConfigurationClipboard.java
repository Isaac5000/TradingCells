package com.cosmocraft.trading_cells.feature.logistics.adapters.input;

import com.cosmocraft.trading_cells.feature.logistics.domain.model.LogisticsResourceType;
import com.cosmocraft.trading_cells.feature.logistics.domain.model.PipeFilterRule;
import com.cosmocraft.trading_cells.feature.logistics.domain.model.PipeResourceProfile;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;

/** User-editable clipboard data excludes physical upgrades and tolerates invalid individual fields. */
public final class PipeConfigurationClipboard {
    private PipeConfigurationClipboard() { }

    public static String copy(PipeFaceConfiguration configuration) {
        JsonObject root = new JsonObject();
        root.addProperty("schema_version", 1);
        root.addProperty("priority", configuration.priority());
        for (LogisticsResourceType type : LogisticsResourceType.values()) {
            var profile = configuration.profile(type);
            JsonObject data = new JsonObject();
            data.addProperty("enabled", profile.enabled());
            data.addProperty("channel", profile.channel());
            data.addProperty("mode", profile.filterMode().name());
            data.addProperty("routing", profile.routingMode().name());
            JsonArray rules = new JsonArray();
            for (PipeFilterRule rule : profile.filters()) {
                if (!known(type, rule)) { continue; }
                JsonObject entry = new JsonObject();
                entry.addProperty("kind", rule.matchKind().name());
                entry.addProperty("id", rule.key());
                entry.addProperty("action", rule.action().name());
                entry.addProperty("components", rule.componentFingerprint());
                entry.addProperty("component_match", rule.componentMatch().name());
                entry.addProperty("channel", rule.routeChannel());
                entry.addProperty("inverted", rule.inverted());
                if (rule.target() != null) {
                    JsonObject target = new JsonObject();
                    target.addProperty("dimension", rule.target().dimension());
                    target.addProperty("x", rule.target().x());
                    target.addProperty("y", rule.target().y());
                    target.addProperty("z", rule.target().z());
                    entry.add("target", target);
                }
                rules.add(entry);
            }
            data.add("rules", rules);
            root.add(type.serializedName(), data);
        }
        return root.toString();
    }

    public static PipeFaceConfiguration paste(String text, PipeFaceConfiguration current) {
        PipeFaceConfiguration result = current.copy();
        if (text == null || text.length() > 262144) { return result; }
        JsonObject root;
        try {
            root = JsonParser.parseString(text).getAsJsonObject();
            if (root.get("schema_version").getAsInt() != 1) { return result; }
        } catch (RuntimeException ignored) { return result; }
        try { result.setPriority(Integer.parseInt(root.get("priority").getAsString())); }
        catch (RuntimeException ignored) { }
        for (LogisticsResourceType type : LogisticsResourceType.values()) {
            if (!root.has(type.serializedName()) || !root.get(type.serializedName()).isJsonObject()) { continue; }
            JsonObject data = root.getAsJsonObject(type.serializedName());
            var previous = result.profile(type);
            boolean enabled = previous.enabled();
            String channel = previous.channel();
            var mode = previous.filterMode();
            var routing = previous.routingMode();
            if (data.has("enabled") && data.get("enabled").isJsonPrimitive()
                    && data.get("enabled").getAsJsonPrimitive().isBoolean()) { enabled = data.get("enabled").getAsBoolean(); }
            try {
                String value = data.get("channel").getAsString();
                if (value.length() <= PipeFilterRule.MAX_CHANNEL_LENGTH) { channel = value; }
            } catch (RuntimeException ignored) { }
            try { mode = PipeResourceProfile.FilterMode.valueOf(data.get("mode").getAsString()); }
            catch (RuntimeException ignored) { }
            try { routing = com.cosmocraft.trading_cells.feature.logistics.domain.model.PipeRoutingMode.valueOf(data.get("routing").getAsString()); }
            catch (RuntimeException ignored) { }
            var rules = new ArrayList<>(previous.filters());
            if (data.has("rules") && data.get("rules").isJsonArray()) {
                rules.clear();
                for (var value : data.getAsJsonArray("rules")) {
                    if (rules.size() >= PipeResourceProfile.MAX_FILTERS) { break; }
                    try {
                        JsonObject entry = value.getAsJsonObject();
                        var action = enumField(entry, "action", PipeFilterRule.Action.ALLOW);
                        var match = enumField(entry, "component_match", PipeFilterRule.ComponentMatch.IGNORE);
                        String components = stringField(entry, "components", 2048);
                        if (!components.isEmpty()) {
                            try { net.minecraft.nbt.TagParser.parseCompoundFully(components); }
                            catch (com.mojang.brigadier.exceptions.CommandSyntaxException ignored) { components = ""; match = PipeFilterRule.ComponentMatch.IGNORE; }
                        }
                        var rule = new PipeFilterRule(action,
                                PipeFilterRule.MatchKind.valueOf(entry.get("kind").getAsString()),
                                stringField(entry, "id", PipeFilterRule.MAX_KEY_LENGTH), match, components,
                                stringField(entry, "channel", PipeFilterRule.MAX_CHANNEL_LENGTH),
                                entry.has("inverted") && entry.get("inverted").isJsonPrimitive()
                                        && entry.get("inverted").getAsJsonPrimitive().isBoolean() && entry.get("inverted").getAsBoolean(),
                                targetField(entry));
                        if (known(type, rule)) { rules.add(rule); }
                    } catch (RuntimeException ignored) { }
                }
            }
            result.setProfile(type, new PipeResourceProfile(enabled, channel, rules, mode, routing));
        }
        return result;
    }

    private static String stringField(JsonObject object, String key, int maximum) {
        try { String text = object.get(key).getAsString(); return text.length() <= maximum ? text : ""; }
        catch (RuntimeException ignored) { return ""; }
    }

    private static <T extends Enum<T>> T enumField(JsonObject object, String key, T fallback) {
        try { return Enum.valueOf(fallback.getDeclaringClass(), object.get(key).getAsString()); }
        catch (RuntimeException ignored) { return fallback; }
    }

    private static com.cosmocraft.trading_cells.feature.logistics.domain.model.PipeRuleTarget targetField(JsonObject entry) {
        try {
            JsonObject target = entry.getAsJsonObject("target");
            String dimension = target.get("dimension").getAsString();
            if (Identifier.tryParse(dimension) == null) { return null; }
            return new com.cosmocraft.trading_cells.feature.logistics.domain.model.PipeRuleTarget(dimension,
                    Integer.parseInt(target.get("x").getAsString()), Integer.parseInt(target.get("y").getAsString()),
                    Integer.parseInt(target.get("z").getAsString()));
        } catch (RuntimeException ignored) { return null; }
    }

    public static boolean known(LogisticsResourceType type, PipeFilterRule rule) {
        String[] parts = rule.key().split("\\|", -1);
        if (parts.length > 2) { return false; }
        Identifier id = Identifier.tryParse(parts[parts.length - 1]);
        if (id == null) { return false; }
        if (parts.length == 2) {
            var adapter = com.cosmocraft.trading_cells.feature.logistics.adapters.api.LogisticsResourceAdapters.byId(Identifier.tryParse(parts[0]));
            if (adapter == null || adapter.type() != type) { return false; }
        }
        return switch (type) {
            case ITEM -> rule.matchKind() == PipeFilterRule.MatchKind.ID ? BuiltInRegistries.ITEM.containsKey(id)
                    : BuiltInRegistries.ITEM.get(TagKey.create(Registries.ITEM, id)).isPresent();
            case FLUID, GAS -> rule.matchKind() == PipeFilterRule.MatchKind.ID ? BuiltInRegistries.FLUID.containsKey(id)
                    : BuiltInRegistries.FLUID.get(TagKey.create(Registries.FLUID, id)).isPresent();
            case ENERGY -> rule.matchKind() == PipeFilterRule.MatchKind.ID && id.toString().equals("neoforge:energy");
        };
    }
}
