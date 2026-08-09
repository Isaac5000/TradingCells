package com.cosmocraft.trading_cells.feature.farmer.application.service;

import com.cosmocraft.trading_cells.feature.farmer.application.port.input.FarmerUseCase;
import com.cosmocraft.trading_cells.feature.farmer.application.port.output.FarmerSettingsPort;
import com.cosmocraft.trading_cells.feature.farmer.domain.model.FarmerCrop;
import com.cosmocraft.trading_cells.feature.farmer.domain.model.FarmerCycle;
import com.cosmocraft.trading_cells.feature.farmer.domain.model.FarmerHarvest;
import com.cosmocraft.trading_cells.shared.machines.domain.model.TimedProcess;
import java.util.Objects;

/** Application boundary for crop timing and harvest rules. */
public final class FarmerService implements FarmerUseCase {
    private final FarmerSettingsPort settings;

    public FarmerService(FarmerSettingsPort settings) {
        this.settings = Objects.requireNonNull(settings);
    }

    @Override
    public boolean damagesHoe() {
        return settings.farmerDamagesHoe();
    }

    @Override
    public int baseGrowthTicks() {
        return FarmerCycle.effectiveGrowthTicks(
                settings.farmerGrowthTicks(),
                0.0D,
                0.0D,
                0
        );
    }

    @Override
    public int effectiveGrowthTicks(
            double toolSpeed,
            double tierPosition,
            int efficiencyLevel
    ) {
        return FarmerCycle.effectiveGrowthTicks(
                settings.farmerGrowthTicks(),
                toolSpeed,
                tierPosition,
                efficiencyLevel
        );
    }

    @Override
    public FarmerHarvest harvest(FarmerCrop crop, int fortuneLevel) {
        return FarmerCycle.harvest(crop, fortuneLevel);
    }

    @Override
    public TimedProcess.Step advance(
            int ticks,
            int durationTicks,
            boolean canCultivate,
            boolean outputAvailable
    ) {
        return FarmerCycle.advance(ticks, durationTicks, canCultivate, outputAvailable);
    }

    @Override
    public int rescaleProgress(int ticks, int previousMaximum, int newMaximum) {
        return FarmerCycle.rescaleProgress(ticks, previousMaximum, newMaximum);
    }
}
