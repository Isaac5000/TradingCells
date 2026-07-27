package com.cosmocraft.trading_cells.feature.trader.application.port.output;

public interface TraderSettingsPort {
    int piglinBarterTicks();

    int villagerTradeRefreshTicks();

    boolean villagerInfiniteTrades();

    int autotraderMinimumExperience();

    int autotraderMaximumExperience();

    int autotraderLevelUpExperienceBonus();
}
