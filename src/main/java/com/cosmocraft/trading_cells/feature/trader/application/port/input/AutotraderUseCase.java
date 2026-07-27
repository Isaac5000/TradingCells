package com.cosmocraft.trading_cells.feature.trader.application.port.input;

import com.cosmocraft.trading_cells.feature.trader.domain.model.AutotraderOfferLifecycle;
import java.util.function.IntUnaryOperator;

public interface AutotraderUseCase {
    AutotraderOfferLifecycle.Decision offers(boolean adult, boolean employed, boolean hasOffers);

    int normalizeSelection(int selectedIndex, int offerCount);

    int offerRefreshTicks();

    boolean infiniteTrades();

    int automaticExperience(IntUnaryOperator randomBelow, boolean rewardExperience, boolean leveledUp);
}
