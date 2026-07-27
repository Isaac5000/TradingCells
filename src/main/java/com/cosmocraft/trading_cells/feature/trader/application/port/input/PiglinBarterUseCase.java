package com.cosmocraft.trading_cells.feature.trader.application.port.input;

import com.cosmocraft.trading_cells.feature.trader.domain.model.PiglinBarterCycle;

public interface PiglinBarterUseCase {
    PiglinBarterCycle.Step advance(int remainingTicks, boolean canStart);
}
