package com.cosmocraft.trading_cells.feature.skeletonfarm.domain.model;

public enum SkeletonFarmLoot {
    WEAPONS,
    BONES,
    ARROWS,
    SKULLS,
    COAL;

    public int bit() {
        return 1 << ordinal();
    }

    public static int allEnabledMask() {
        int mask = 0;
        for (SkeletonFarmLoot loot : values()) {
            mask |= loot.bit();
        }
        return mask;
    }
}
