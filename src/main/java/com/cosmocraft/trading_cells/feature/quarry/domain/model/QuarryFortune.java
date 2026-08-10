package com.cosmocraft.trading_cells.feature.quarry.domain.model;

/** Pure Fortune scaling used when selecting mineable quarry materials. */
public final class QuarryFortune {
    private QuarryFortune() {
    }

    public static int boostSelectionWeight(int baseWeight, int fortuneLevel) {
        int safeWeight = Math.max(0, baseWeight);
        int level = Math.max(0, fortuneLevel);
        if (safeWeight == 0 || level == 0) {
            return safeWeight;
        }
        long denominator = level + 2L;
        long numerator = (level + 2L) * (level + 1L) / 2L + 1L;
        long boosted = Math.round(safeWeight * (double) numerator / denominator);
        return (int) Math.clamp(boosted, 0L, Integer.MAX_VALUE);
    }
}
