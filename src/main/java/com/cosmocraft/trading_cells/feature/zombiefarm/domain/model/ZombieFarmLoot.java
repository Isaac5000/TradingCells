package com.cosmocraft.trading_cells.feature.zombiefarm.domain.model;

public enum ZombieFarmLoot {
    ROTTEN_FLESH,
    IRON_INGOTS,
    CARROTS,
    POTATOES,
    WEAPONS,
    COPPER_INGOTS,
    NAUTILUS_SHELLS,
    GOLD_NUGGETS,
    GOLD_INGOTS,
    ADDITIONAL_DROPS,
    HEADS;

    public int bit() {
        return 1 << ordinal();
    }

    public static int allEnabledMask() {
        int mask = 0;
        for (ZombieFarmLoot loot : values()) {
            mask |= loot.bit();
        }
        return mask;
    }
}
