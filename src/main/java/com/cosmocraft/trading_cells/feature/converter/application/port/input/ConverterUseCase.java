package com.cosmocraft.trading_cells.feature.converter.application.port.input;

import com.cosmocraft.trading_cells.feature.converter.domain.model.ConverterCycle;
import com.cosmocraft.trading_cells.feature.converter.domain.model.ConverterStage;

public interface ConverterUseCase {
    int durationTicks(ConverterStage stage);

    ConverterCycle.Step advance(
            ConverterStage stage,
            int ticks,
            boolean hasValidVillager,
            boolean canStart,
            boolean curedReady
    );

    int increasedCureDiscount(int current);
}
