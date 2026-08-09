package com.cosmocraft.trading_cells.feature.quarry.domain.model;

public enum QuarryUpgradeTier {
    NONE,
    COPPER,
    IRON,
    GOLD,
    DIAMOND,
    NETHERITE;

    public boolean unlocks(QuarryUpgradeTier required) {
        return ordinal() >= required.ordinal();
    }

    public boolean supportsDeepMining() {
        return unlocks(DIAMOND);
    }

    public static QuarryUpgradeTier fromIndex(int index) {
        return values()[Math.clamp(index, 0, values().length - 1)];
    }
}
