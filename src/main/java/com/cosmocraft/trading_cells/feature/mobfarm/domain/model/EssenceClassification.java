package com.cosmocraft.trading_cells.feature.mobfarm.domain.model;

/** Versioned, deterministic balance profile; no Minecraft objects or random state. */
public final class EssenceClassification {
    public static final int VERSION = 1;
    public static final double TIER_II = 20, TIER_III = 45, TIER_IV = 75;
    public static final double HOSTILE = 6, RANGED = 4, CHARGED = 25;
    private static final double MAX_ATTRIBUTE = 1_000_000_000_000.0;

    private EssenceClassification() { }

    public record Attributes(double health, double armor, double toughness, double attack,
                             double knockbackResistance, double experience) { }
    public record Result(EssenceTier tier, double score, int version) { }

    public static Result classify(Attributes a, double modifier, int override, boolean elite, boolean boss) {
        double score = 10 * log(a.health(), 20) + 6 * log(a.armor(), 5)
                + 4 * log(a.toughness(), 2) + 8 * log(a.attack(), 4)
                + 8 * Math.min(1, safe(a.knockbackResistance())) + 4 * log(a.experience(), 5)
                + safe(modifier);
        int tier = score < TIER_II ? 1 : score < TIER_III ? 2 : score < TIER_IV ? 3 : 4;
        tier = boss ? 4 : elite ? Math.max(3, tier) : tier;
        if (override >= 1 && override <= 4) { tier = override; }
        return new Result(EssenceTier.fromId(tier), score, VERSION);
    }

    private static double log(double value, double scale) { return Math.log1p(safe(value) / scale) / Math.log(2); }
    private static double safe(double value) {
        return Double.isFinite(value) ? Math.clamp(value, 0, MAX_ATTRIBUTE) : 0;
    }
}
