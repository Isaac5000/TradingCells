package com.cosmocraft.trading_cells.feature.raiderfarm.domain.model;

/** Stable grouped filters; every other table drop is exposed as an individual dynamic filter. */
public enum RaiderFarmLoot {
    WEAPONS,
    OMINOUS_BANNER;

    public int bit() {
        return 1 << ordinal();
    }

    public static int allEnabledMask() {
        int mask = 0;
        for (RaiderFarmLoot loot : values()) {
            mask |= loot.bit();
        }
        return mask;
    }
}
