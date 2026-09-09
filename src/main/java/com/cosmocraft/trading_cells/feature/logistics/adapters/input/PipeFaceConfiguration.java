package com.cosmocraft.trading_cells.feature.logistics.adapters.input;

import com.cosmocraft.trading_cells.feature.logistics.domain.model.LogisticsResourceType;
import com.cosmocraft.trading_cells.feature.logistics.domain.model.PipeFilterRule;
import com.cosmocraft.trading_cells.feature.logistics.domain.model.PipeResourceProfile;
import com.cosmocraft.trading_cells.feature.logistics.domain.model.PipeSideMode;
import com.cosmocraft.trading_cells.feature.logistics.domain.model.PipeUpgradeTier;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;

public final class PipeFaceConfiguration {
    private static final String PROFILE_PREFIX = "Profile";

    private PipeSideMode mode = PipeSideMode.NONE;
    private boolean explicitMode;
    private boolean pipeDisconnected;
    private int priority;
    private PipeUpgradeTier upgradeTier = PipeUpgradeTier.BARE;
    private final Map<LogisticsResourceType, PipeResourceProfile> bareProfiles = defaultProfiles();
    private final Map<LogisticsResourceType, PipeResourceProfile> upgradeProfiles = defaultProfiles();
    private final Map<LogisticsResourceType, PipeResourceProfile> effectiveProfiles = defaultProfiles();

    public PipeFaceConfiguration() { refreshEffectiveProfiles(); }

    public PipeSideMode mode() {
        return mode;
    }

    public boolean explicitMode() {
        return explicitMode;
    }

    public boolean pipeDisconnected() {
        return pipeDisconnected;
    }

    public int priority() {
        return priority;
    }

    public PipeUpgradeTier upgradeTier() {
        return upgradeTier;
    }

    public PipeResourceProfile profile(LogisticsResourceType type) {
        return activeProfiles().getOrDefault(type, PipeResourceProfile.defaultProfile());
    }

    public PipeResourceProfile effectiveProfile(LogisticsResourceType type) {
        return effectiveProfiles.get(type);
    }

    private void refreshEffectiveProfiles() {
        for (LogisticsResourceType type : LogisticsResourceType.values()) {
            effectiveProfiles.put(type, profile(type).forTier(upgradeTier, type));
        }
    }

    public void setMode(PipeSideMode mode, boolean explicitMode) {
        this.mode = mode == null ? PipeSideMode.NONE : mode;
        this.explicitMode = explicitMode;
    }

    public void setPipeDisconnected(boolean pipeDisconnected) {
        this.pipeDisconnected = pipeDisconnected;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public void setUpgradeTier(PipeUpgradeTier upgradeTier) {
        PipeUpgradeTier sanitized = upgradeTier == null ? PipeUpgradeTier.BARE : upgradeTier;
        if (this.upgradeTier == PipeUpgradeTier.BARE && sanitized != PipeUpgradeTier.BARE) {
            copyProfiles(bareProfiles, upgradeProfiles);
        }
        this.upgradeTier = sanitized;
        refreshEffectiveProfiles();
    }

    public void setProfile(LogisticsResourceType type, PipeResourceProfile profile) {
        activeProfiles().put(type, profile == null ? PipeResourceProfile.defaultProfile(type) : profile);
        effectiveProfiles.put(type, profile(type).forTier(upgradeTier, type));
    }

    public Map<LogisticsResourceType, PipeResourceProfile> activeProfilesCopy() {
        return Map.copyOf(activeProfiles());
    }

    public PipeFaceConfiguration copy() {
        return load(save());
    }

    public PipeFaceConfiguration withReplacementUpgrade(
            PipeUpgradeTier tier, @org.jspecify.annotations.Nullable CompoundTag storedProfiles
    ) {
        PipeFaceConfiguration result = copy();
        result.setUpgradeTier(tier);
        if (tier != PipeUpgradeTier.BARE && storedProfiles != null) {
            result.loadActiveProfiles(storedProfiles);
        }
        return result;
    }

    public void applyEditableSettings(PipeFaceConfiguration source) {
        setMode(source.mode(), true);
        setPriority(source.priority());
        copyProfiles(source.activeProfiles(), activeProfiles());
        refreshEffectiveProfiles();
    }

    public CompoundTag saveActiveProfiles() {
        return saveProfiles(activeProfiles());
    }

    public void loadActiveProfiles(CompoundTag profiles) {
        Map<LogisticsResourceType, PipeResourceProfile> loaded = defaultProfiles();
        loadProfiles(profiles, loaded);
        copyProfiles(loaded, activeProfiles());
        refreshEffectiveProfiles();
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("SchemaVersion", 1);
        tag.putString("Mode", mode.serializedName());
        tag.putBoolean("ExplicitMode", explicitMode);
        tag.putBoolean("PipeDisconnected", pipeDisconnected);
        tag.putInt("Priority", priority);
        tag.putInt("UpgradeTier", upgradeTier.id());
        tag.put("BareProfiles", saveProfiles(bareProfiles));
        tag.put("UpgradeProfiles", saveProfiles(upgradeProfiles));
        return tag;
    }

    public static PipeFaceConfiguration load(CompoundTag tag) {
        PipeFaceConfiguration configuration = new PipeFaceConfiguration();
        configuration.mode = PipeSideMode.fromSerializedName(tag.getStringOr("Mode", "none"));
        configuration.explicitMode = tag.getBooleanOr("ExplicitMode", false);
        configuration.pipeDisconnected = tag.getBooleanOr("PipeDisconnected", false);
        configuration.priority = tag.getIntOr("Priority", 0);
        configuration.upgradeTier = PipeUpgradeTier.fromId(tag.getIntOr("UpgradeTier", 0));
        tag.getCompound("BareProfiles").ifPresent(value -> loadProfiles(value, configuration.bareProfiles));
        tag.getCompound("UpgradeProfiles").ifPresent(value -> loadProfiles(value, configuration.upgradeProfiles));
        configuration.refreshEffectiveProfiles();
        return configuration;
    }

    public static boolean isValidNetworkConfiguration(CompoundTag tag) {
        if (tag.getIntOr("SchemaVersion", 0) != 1 || !load(tag).save().equals(tag)) {
            return false;
        }
        PipeFaceConfiguration configuration = load(tag);
        for (var profiles : List.of(configuration.bareProfiles, configuration.upgradeProfiles)) {
            for (PipeResourceProfile profile : profiles.values()) {
                for (PipeFilterRule rule : profile.filters()) {
                    String[] identity = rule.key().split("\\|", -1);
                    if (identity.length > 2 || java.util.Arrays.stream(identity)
                            .anyMatch(value -> net.minecraft.resources.Identifier.tryParse(value) == null)) {
                        return false;
                    }
                    if (rule.target() != null && net.minecraft.resources.Identifier.tryParse(rule.target().dimension()) == null) { return false; }
                    if (rule.componentMatch() != PipeFilterRule.ComponentMatch.IGNORE
                            && !rule.componentFingerprint().isEmpty()) {
                        try {
                            net.minecraft.nbt.TagParser.parseCompoundFully(rule.componentFingerprint());
                        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException exception) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    private Map<LogisticsResourceType, PipeResourceProfile> activeProfiles() {
        return upgradeTier == PipeUpgradeTier.BARE ? bareProfiles : upgradeProfiles;
    }

    private static Map<LogisticsResourceType, PipeResourceProfile> defaultProfiles() {
        Map<LogisticsResourceType, PipeResourceProfile> profiles = new EnumMap<>(LogisticsResourceType.class);
        for (LogisticsResourceType type : LogisticsResourceType.values()) {
            profiles.put(type, PipeResourceProfile.defaultProfile(type));
        }
        return profiles;
    }

    private static void copyProfiles(
            Map<LogisticsResourceType, PipeResourceProfile> source,
            Map<LogisticsResourceType, PipeResourceProfile> destination
    ) {
        destination.clear();
        destination.putAll(source);
    }

    private static CompoundTag saveProfiles(Map<LogisticsResourceType, PipeResourceProfile> profiles) {
        CompoundTag root = new CompoundTag();
        for (LogisticsResourceType type : LogisticsResourceType.values()) {
            PipeResourceProfile profile = profiles.getOrDefault(type, PipeResourceProfile.defaultProfile());
            CompoundTag tag = new CompoundTag();
            tag.putBoolean("Enabled", profile.enabled());
            tag.putString("Channel", profile.channel());
            tag.putString("FilterMode", profile.filterMode().name());
            tag.putString("RoutingMode", profile.routingMode().name());
            tag.putInt("FilterCount", profile.filters().size());
            for (int index = 0; index < profile.filters().size(); index++) {
                PipeFilterRule rule = profile.filters().get(index);
                CompoundTag filter = new CompoundTag();
                filter.putInt("Action", rule.action().ordinal());
                filter.putInt("MatchKind", rule.matchKind().ordinal());
                filter.putString("Key", rule.key());
                filter.putInt("ComponentMatch", rule.componentMatch().ordinal());
                filter.putString("ComponentFingerprint", rule.componentFingerprint());
                filter.putString("RouteChannel", rule.routeChannel());
                filter.putBoolean("Inverted", rule.inverted());
                if (rule.target() != null) {
                    CompoundTag target = new CompoundTag();
                    target.putString("Dimension", rule.target().dimension());
                    target.putInt("X", rule.target().x());
                    target.putInt("Y", rule.target().y());
                    target.putInt("Z", rule.target().z());
                    filter.put("Target", target);
                }
                tag.put("Filter" + index, filter);
            }
            root.put(PROFILE_PREFIX + type.serializedName(), tag);
        }
        return root;
    }

    private static void loadProfiles(
            CompoundTag root,
            Map<LogisticsResourceType, PipeResourceProfile> destination
    ) {
        for (LogisticsResourceType type : LogisticsResourceType.values()) {
            root.getCompound(PROFILE_PREFIX + type.serializedName()).ifPresent(tag -> {
                int count = Math.clamp(tag.getIntOr("FilterCount", 0), 0, PipeResourceProfile.MAX_FILTERS);
                List<PipeFilterRule> filters = new ArrayList<>(count);
                for (int index = 0; index < count; index++) {
                    tag.getCompound("Filter" + index).ifPresent(filter -> filters.add(new PipeFilterRule(
                            enumValue(PipeFilterRule.Action.values(), filter.getIntOr("Action", 0)),
                            enumValue(PipeFilterRule.MatchKind.values(), filter.getIntOr("MatchKind", 0)),
                            filter.getStringOr("Key", ""),
                            enumValue(PipeFilterRule.ComponentMatch.values(),
                                    filter.getIntOr("ComponentMatch", 0)),
                            filter.getStringOr("ComponentFingerprint", ""),
                            filter.getStringOr("RouteChannel", ""),
                            filter.getBooleanOr("Inverted", false),
                            loadTarget(filter)
                    )));
                }
                destination.put(type, new PipeResourceProfile(
                        tag.getBooleanOr("Enabled", true),
                        tag.getStringOr("Channel", ""),
                        filters,
                        java.util.Arrays.stream(PipeResourceProfile.FilterMode.values())
                                .filter(mode -> mode.name().equals(tag.getStringOr("FilterMode", "RULES")))
                                .findFirst().orElse(PipeResourceProfile.FilterMode.RULES),
                        java.util.Arrays.stream(com.cosmocraft.trading_cells.feature.logistics.domain.model.PipeRoutingMode.values())
                                .filter(mode -> mode.name().equals(tag.getStringOr("RoutingMode", PipeResourceProfile.defaultRouting(type).name())))
                                .findFirst().orElse(PipeResourceProfile.defaultRouting(type))
                ));
            });
        }
    }

    private static <T> T enumValue(T[] values, int index) {
        return values[Math.clamp(index, 0, values.length - 1)];
    }

    private static com.cosmocraft.trading_cells.feature.logistics.domain.model.PipeRuleTarget loadTarget(CompoundTag filter) {
        return filter.getCompound("Target").map(target -> {
            try {
                return new com.cosmocraft.trading_cells.feature.logistics.domain.model.PipeRuleTarget(
                        target.getStringOr("Dimension", ""), target.getIntOr("X", 0), target.getIntOr("Y", 0), target.getIntOr("Z", 0));
            } catch (IllegalArgumentException ignored) { return null; }
        }).orElse(null);
    }
}
