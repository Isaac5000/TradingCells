package com.cosmocraft.trading_cells.feature.farmer.application.port.input;

import com.cosmocraft.trading_cells.feature.farmer.domain.model.FarmerCrop;
import com.cosmocraft.trading_cells.feature.farmer.domain.model.FarmerHarvest;
import com.cosmocraft.trading_cells.shared.machines.domain.model.TimedProcess;

public interface FarmerUseCase {
    boolean damagesHoe();

    int baseGrowthTicks();

    int effectiveGrowthTicks(
            double toolSpeed,
            double tierPosition,
            int efficiencyLevel
    );

    FarmerHarvest harvest(FarmerCrop crop, int fortuneLevel);

    TimedProcess.Step advance(int ticks, int durationTicks, boolean canCultivate, boolean outputAvailable);

    int rescaleProgress(int ticks, int previousMaximum, int newMaximum);
}
