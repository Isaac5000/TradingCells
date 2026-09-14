package com.cosmocraft.trading_cells.feature.logistics.domain.model;

import java.util.List;
import java.util.TreeSet;

/** Combines a bounded server window with the profile currently being edited. */
public final class PipeChannelChoices {
    private PipeChannelChoices() { }

    public static List<String> merge(String prefix, List<String> remote, PipeResourceProfile local) {
        String normalized = PipeFilterRule.searchPrefix(prefix);
        var channels = new TreeSet<String>();
        channels.addAll(remote);
        channels.add(local.channel());
        local.filters().forEach(rule -> channels.add(rule.routeChannel()));
        return channels.stream().filter(value -> !value.isEmpty() && value.startsWith(normalized)).limit(16).toList();
    }
}
