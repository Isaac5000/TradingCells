package com.cosmocraft.trading_cells.feature.quarry.application.port.input;

import com.cosmocraft.trading_cells.shared.machines.domain.model.TimedProcess;

public interface QuarryUseCase {
    int durationTicks(double tierPosition, int efficiencyLevel);

    int rescaleProgress(int ticks, int previousMaximum, int newMaximum);

    TimedProcess.Step advance(int currentTicks, int durationTicks, boolean canMine, boolean outputAvailable);
}
