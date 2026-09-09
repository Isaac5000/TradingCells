package com.cosmocraft.trading_cells.feature.logistics.domain.model;

import java.util.List;

public record PipeResourceProfile(boolean enabled, String channel, List<PipeFilterRule> filters, FilterMode filterMode,
                                  PipeRoutingMode routingMode) {
    public static final int MAX_FILTERS = 16;
    public enum FilterMode { RULES, OFF, WHITELIST, BLACKLIST }

    public PipeResourceProfile(boolean enabled, String channel, List<PipeFilterRule> filters) {
        this(enabled, channel, filters, FilterMode.RULES);
    }

    public PipeResourceProfile(boolean enabled, String channel, List<PipeFilterRule> filters, FilterMode filterMode) {
        this(enabled, channel, filters, filterMode, PipeRoutingMode.NEAREST);
    }

    public PipeResourceProfile {
        channel = PipeFilterRule.normalizeChannel(channel);
        filters = filters == null ? List.of() : List.copyOf(filters.subList(0, Math.min(filters.size(), MAX_FILTERS)));
        filterMode = filterMode == null ? FilterMode.RULES : filterMode;
        routingMode = routingMode == null ? PipeRoutingMode.NEAREST : routingMode;
    }

    public PipeResourceProfile forTier(PipeUpgradeTier tier, LogisticsResourceType type) {
        List<PipeFilterRule> available = !tier.allowsFilters() ? List.of() : tier.allowsAdvancedRules() ? filters
                : filters.stream().map(rule -> new PipeFilterRule(rule.action(), rule.matchKind(), rule.key(),
                        PipeFilterRule.ComponentMatch.IGNORE, "", "")).toList();
        FilterMode mode = tier.allowsFilters() ? filterMode : FilterMode.OFF;
        if (!tier.allowsAdvancedRules() && mode == FilterMode.RULES) {
            mode = available.isEmpty() ? FilterMode.OFF : available.stream().anyMatch(rule -> rule.action() == PipeFilterRule.Action.DENY)
                    ? FilterMode.BLACKLIST : FilterMode.WHITELIST;
        }
        return new PipeResourceProfile(enabled, tier.allowsChannels() ? channel : "", available, mode,
                tier.allowsRouting() ? routingMode : defaultRouting(type));
    }

    public boolean allows(java.util.function.Predicate<PipeFilterRule> matches) {
        if (filterMode == FilterMode.OFF) { return true; }
        for (PipeFilterRule rule : filters) {
            if (matches.test(rule)) {
                boolean allow = filterMode == FilterMode.RULES ? rule.action() == PipeFilterRule.Action.ALLOW
                        : filterMode == FilterMode.WHITELIST;
                return allow != rule.inverted();
            }
        }
        return filterMode != FilterMode.WHITELIST;
    }

    public PipeResourceProfile withChannel(String value) {
        String parent = PipeFilterRule.validatedChannel(value);
        if (parent.equals(channel)) { return this; }
        var rewritten = new java.util.ArrayList<PipeFilterRule>(filters.size());
        for (var rule : filters) {
            String route = rule.routeChannel();
            if (!route.isEmpty() && (channel.isEmpty() || route.equals(channel) || route.startsWith(channel + "/"))) {
                String suffix = channel.isEmpty() ? route : route.equals(channel) ? "" : route.substring(channel.length() + 1);
                route = parent.isEmpty() ? suffix : suffix.isEmpty() ? parent : parent + "/" + suffix;
                if (route.length() > PipeFilterRule.MAX_CHANNEL_LENGTH || !PipeFilterRule.normalizeChannel(route).equals(route)) {
                    throw new IllegalArgumentException("Parent channel exceeds a rule's channel budget");
                }
            }
            rewritten.add(new PipeFilterRule(rule.action(), rule.matchKind(), rule.key(), rule.componentMatch(),
                    rule.componentFingerprint(), route, rule.inverted(), rule.target()));
        }
        return new PipeResourceProfile(enabled, parent, rewritten, filterMode, routingMode);
    }

    public static PipeResourceProfile defaultProfile() {
        return new PipeResourceProfile(true, "", List.of(), FilterMode.OFF);
    }

    public static PipeRoutingMode defaultRouting(LogisticsResourceType type) {
        return type == LogisticsResourceType.ITEM ? PipeRoutingMode.NEAREST : PipeRoutingMode.EQUAL;
    }

    public static PipeResourceProfile defaultProfile(LogisticsResourceType type) {
        return new PipeResourceProfile(true, "", List.of(), FilterMode.OFF, defaultRouting(type));
    }
}
