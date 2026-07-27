package com.cosmocraft.trading_cells.feature.trader.application.service;

import com.cosmocraft.trading_cells.feature.trader.application.port.input.VillagerTraderUseCase;
import com.cosmocraft.trading_cells.feature.trader.application.port.output.TraderSettingsPort;
import java.util.Objects;

public final class VillagerTraderService implements VillagerTraderUseCase {
    private final TraderSettingsPort settings;

    public VillagerTraderService(TraderSettingsPort settings) {
        this.settings = Objects.requireNonNull(settings);
    }

    @Override
    public boolean infiniteTrades() {
        return settings.villagerInfiniteTrades();
    }
}
