package com.cosmocraft.trading_cells.feature.converter.application.service;

import com.cosmocraft.trading_cells.feature.converter.application.port.input.ConverterUseCase;
import com.cosmocraft.trading_cells.feature.converter.application.port.output.ConverterSettingsPort;
import com.cosmocraft.trading_cells.feature.converter.domain.model.ConverterCycle;
import com.cosmocraft.trading_cells.feature.converter.domain.model.ConverterStage;
import java.util.Objects;

/** Application boundary for converter state and configurable cure strength. */
public final class ConverterService implements ConverterUseCase {
    private final ConverterSettingsPort settings;

    public ConverterService(ConverterSettingsPort settings) {
        this.settings = Objects.requireNonNull(settings);
    }

    public int durationTicks(ConverterStage stage) {
        return stage.durationTicks(
                settings.converterInfectionTicks(),
                settings.converterCureTicks()
        );
    }

    public ConverterCycle.Step advance(
            ConverterStage stage,
            int ticks,
            boolean hasValidVillager,
            boolean canStart,
            boolean curedReady
    ) {
        return ConverterCycle.advance(
                stage,
                ticks,
                hasValidVillager,
                canStart,
                curedReady,
                settings.converterInfectionTicks(),
                settings.converterCureTicks()
        );
    }

    public int increasedCureDiscount(int current) {
        long increased = (long) Math.max(0, current)
                + Math.max(0, settings.converterCureDiscountPerCycle());
        int maximum = Math.max(0, settings.converterMaximumCureDiscount());
        return (int) Math.clamp(increased, 0L, maximum);
    }
}
