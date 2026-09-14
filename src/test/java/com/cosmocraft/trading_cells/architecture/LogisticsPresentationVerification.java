package com.cosmocraft.trading_cells.architecture;

import com.cosmocraft.trading_cells.feature.logistics.domain.model.*;
import java.util.List;

final class LogisticsPresentationVerification {
    private LogisticsPresentationVerification() { }

    static void verify() {
        equal(NetworkAmountFormat.compact(999, "es_es"), "999");
        equal(NetworkAmountFormat.compact(999_000, "es_es"), "999k");
        equal(NetworkAmountFormat.compact(305_000_000, "es_es"), "305M");
        equal(NetworkAmountFormat.compact(1_000_000_000, "es_es"), "1000M");
        equal(NetworkAmountFormat.compact(999_999_999_999L, "es_es"), "999999M");
        equal(NetworkAmountFormat.compact(1_000_000_000_000L, "es_es"), "1B");
        equal(NetworkAmountFormat.compact(1_000_000_000, "en_us"), "1B");
        equal(NetworkAmountFormat.compact(1_000_000_000_000L, "en_us"), "1T");
        equal(NetworkAmountFormat.compact(Long.MAX_VALUE, "es_es"), "9T");
        equal(NetworkAmountFormat.compact(Long.MAX_VALUE, "en_us"), "9Qi");
        for (int width : List.of(1, 17, 18, 24, 30, 42, 120)) {
            float scale = NetworkAmountFormat.scaleToFit(width, 18);
            if (scale <= 0 || scale > 1 || width * scale > 18.001F) { throw new AssertionError("Count exceeds cell width"); }
        }
        equal(NetworkAmountFormat.scaleToFit(18, 18), 1.0F);
        var rule = new PipeFilterRule(PipeFilterRule.Action.ALLOW, PipeFilterRule.MatchKind.ID,
                "minecraft:diamond", PipeFilterRule.ComponentMatch.IGNORE, "", "lel");
        var draft = new PipeResourceProfile(false, "lem", List.of(rule), PipeResourceProfile.FilterMode.OFF);
        equal(PipeChannelChoices.merge("LE", List.of("le", "len"), draft), List.of("le", "lel", "lem", "len"));
        equal(PipeChannelChoices.merge("lel", List.of("le", "len"), draft), List.of("lel"));
        var many = java.util.stream.IntStream.range(0, 100).mapToObj(index -> "le" + String.format(java.util.Locale.ROOT, "%02d", index)).toList();
        if (PipeChannelChoices.merge("le", many, draft).size() != 16) { throw new AssertionError("Completion window is unbounded"); }
        if (!draft.allows(candidate -> true) || draft.filters().isEmpty()) { throw new AssertionError("Off must preserve rules while bypassing them"); }
        if (new PipeResourceProfile(true, "", List.of(), PipeResourceProfile.FilterMode.WHITELIST).allows(candidate -> false)) {
            throw new AssertionError("An empty whitelist is not equivalent to disabled filtering");
        }
    }

    private static void equal(Object actual, Object expected) {
        if (!expected.equals(actual)) { throw new AssertionError("Expected " + expected + ", got " + actual); }
    }
}
