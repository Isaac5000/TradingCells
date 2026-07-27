package com.cosmocraft.trading_cells.feature.incubators.domain.model;

import com.cosmocraft.trading_cells.shared.machines.domain.model.TimedProcess;

public final class IncubationCycle {
    private IncubationCycle() {
    }

    public static TimedProcess.Step advance(
            int currentTicks,
            int durationTicks,
            boolean validBaby,
            boolean outputAvailable
    ) {
        TimedProcess.Availability availability = TimedProcess.availability(validBaby, outputAvailable);
        return TimedProcess.advance(currentTicks, durationTicks, availability);
    }
}
