package com.cosmocraft.trading_cells.feature.quarry.application.service;

import com.cosmocraft.trading_cells.feature.quarry.application.port.input.QuarryUseCase;
import com.cosmocraft.trading_cells.feature.quarry.domain.model.QuarryCycle;
import com.cosmocraft.trading_cells.shared.machines.domain.model.TimedProcess;

public final class QuarryService implements QuarryUseCase {
    @Override
    public int durationTicks(double tierPosition, int efficiencyLevel) {
        return QuarryCycle.durationTicks(tierPosition, efficiencyLevel);
    }

    @Override
    public int rescaleProgress(int ticks, int previousMaximum, int newMaximum) {
        return QuarryCycle.rescaleProgress(ticks, previousMaximum, newMaximum);
    }

    @Override
    public TimedProcess.Step advance(
            int currentTicks,
            int durationTicks,
            boolean canMine,
            boolean outputAvailable
    ) {
        return QuarryCycle.advance(currentTicks, durationTicks, canMine, outputAvailable);
    }
}
