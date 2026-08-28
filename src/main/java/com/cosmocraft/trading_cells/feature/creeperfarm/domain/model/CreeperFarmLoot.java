package com.cosmocraft.trading_cells.feature.creeperfarm.domain.model;

public enum CreeperFarmLoot {
    GUNPOWDER,
    STORM_SHARDS,
    HEADS;

    public int bit() {
        return 1 << ordinal();
    }

    public static int allEnabledMask() {
        int mask = 0;
        for (CreeperFarmLoot loot : values()) {
            mask |= loot.bit();
        }
        return mask;
    }
}
