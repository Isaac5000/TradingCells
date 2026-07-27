package com.cosmocraft.trading_cells.feature.trader.application.service;

import com.cosmocraft.trading_cells.feature.trader.application.port.input.PiglinBarterUseCase;
import com.cosmocraft.trading_cells.feature.trader.application.port.output.TraderSettingsPort;
import com.cosmocraft.trading_cells.feature.trader.domain.model.PiglinBarterCycle;
import java.util.Objects;

public final class PiglinBarterService implements PiglinBarterUseCase {
    private final TraderSettingsPort settings;

    public PiglinBarterService(TraderSettingsPort settings) {
        this.settings = Objects.requireNonNull(settings);
    }

    @Override
    public PiglinBarterCycle.Step advance(int remainingTicks, boolean canStart) {
        return PiglinBarterCycle.advance(
                remainingTicks,
                settings.piglinBarterTicks(),
                canStart
        );
    }
}
