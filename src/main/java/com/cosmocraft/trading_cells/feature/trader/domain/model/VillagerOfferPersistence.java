package com.cosmocraft.trading_cells.feature.trader.domain.model;

public final class VillagerOfferPersistence {
    public static final int MINIMUM_REFRESH_TICKS = 12_000;

    private VillagerOfferPersistence() {
    }

    public static int refreshIntervalTicks(int configuredTicks) {
        return Math.max(MINIMUM_REFRESH_TICKS, configuredTicks);
    }
}
