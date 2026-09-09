package com.cosmocraft.trading_cells.feature.logistics.domain.model;

import java.util.Locale;

public record PipeFilterRule(
        Action action,
        MatchKind matchKind,
        String key,
        ComponentMatch componentMatch,
        String componentFingerprint,
        String routeChannel,
        boolean inverted,
        PipeRuleTarget target
) {
    public static final int MAX_KEY_LENGTH = 256;
    public static final int MAX_CHANNEL_LENGTH = 256;
    public static final int MAX_CHANNEL_DEPTH = 16;

    public PipeFilterRule(Action action, MatchKind matchKind, String key, ComponentMatch componentMatch,
                          String componentFingerprint, String routeChannel) {
        this(action, matchKind, key, componentMatch, componentFingerprint, routeChannel, false, null);
    }

    public PipeFilterRule {
        action = action == null ? Action.ALLOW : action;
        matchKind = matchKind == null ? MatchKind.ID : matchKind;
        key = sanitize(key, MAX_KEY_LENGTH);
        componentMatch = componentMatch == null ? ComponentMatch.IGNORE : componentMatch;
        componentFingerprint = sanitize(componentFingerprint, 2_048);
        routeChannel = normalizeChannel(routeChannel);
    }

    public static String normalizeChannel(String channel) {
        return normalizeChannel(channel, false);
    }

    public static String validatedChannel(String channel) {
        return normalizeChannel(channel, true);
    }

    private static String normalizeChannel(String channel, boolean strict) {
        if (strict && channel != null && (channel.trim().length() > MAX_CHANNEL_LENGTH
                || channel.trim().toLowerCase(Locale.ROOT).length() > MAX_CHANNEL_LENGTH)) {
            throw new IllegalArgumentException("Channel exceeds its length budget");
        }
        String bounded = sanitize(sanitize(channel, MAX_CHANNEL_LENGTH).toLowerCase(Locale.ROOT), MAX_CHANNEL_LENGTH);
        StringBuilder result = new StringBuilder(bounded.length());
        int depth = 1;
        for (int index = 0; index < bounded.length(); index++) {
            char value = bounded.charAt(index);
            if (Character.isISOControl(value)) { continue; }
            if (value == '/') {
                if (result.isEmpty() || result.charAt(result.length() - 1) == '/') { continue; }
            } else if (!result.isEmpty() && result.charAt(result.length() - 1) == '/' && ++depth > MAX_CHANNEL_DEPTH) {
                if (strict) { throw new IllegalArgumentException("Channel exceeds its depth budget"); }
                break;
            }
            result.append(value);
        }
        if (!result.isEmpty() && result.charAt(result.length() - 1) == '/') { result.setLength(result.length() - 1); }
        return result.toString();
    }

    public static String subchannel(String parent, String child) {
        String prefix = validatedChannel(parent), suffix = validatedChannel(child);
        return suffix.isEmpty() ? prefix : prefix.isEmpty() ? suffix : validatedChannel(prefix + "/" + suffix);
    }

    public static String searchPrefix(String text) {
        String normalized = normalizeChannel(text);
        return text != null && text.endsWith("/") && !normalized.isEmpty() && normalized.length() < MAX_CHANNEL_LENGTH
                ? normalized + "/" : normalized;
    }

    private static String sanitize(String value, int maximumLength) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        return trimmed.length() <= maximumLength ? trimmed : trimmed.substring(0, maximumLength);
    }

    public enum Action {
        ALLOW,
        DENY
    }

    public enum MatchKind {
        ID,
        TAG
    }

    public enum ComponentMatch {
        IGNORE,
        SUBSET,
        EXACT
    }
}
