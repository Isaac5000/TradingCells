package com.cosmocraft.trading_cells.feature.trader.domain.model;

public final class PiglinBarterUpgradeYield {
    private static final int MAXIMUM_UPGRADE_LEVEL = 5;
    private static final int[] NUMERATORS = {1, 1, 1, 1, 3, 2};
    private static final int[] DENOMINATORS = {1, 1, 1, 1, 2, 1};

    private PiglinBarterUpgradeYield() {
    }

    public static float multiplier(int upgradeLevel) {
        int level = safeLevel(upgradeLevel);
        return (float) NUMERATORS[level] / DENOMINATORS[level];
    }

    public static int upgradedAmount(int baseAmount, int maximumAmount, int upgradeLevel) {
        int level = safeLevel(upgradeLevel);
        long increased = (long) baseAmount * NUMERATORS[level] / DENOMINATORS[level];
        return (int) Math.min(maximumAmount, increased);
    }

    private static int safeLevel(int upgradeLevel) {
        return Math.clamp(upgradeLevel, 0, MAXIMUM_UPGRADE_LEVEL);
    }
}
