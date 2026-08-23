package com.cosmocraft.trading_cells.feature.skeletonfarm.domain.model;

/** Charged-creeper Storm Shard amount rules. */
public final class StormShardDropRules {
    private StormShardDropRules() {
    }

    public static int maximumAmount(int lootingLevel) {
        return 1 + Math.max(0, lootingLevel);
    }
}
