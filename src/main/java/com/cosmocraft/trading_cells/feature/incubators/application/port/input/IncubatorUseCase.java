package com.cosmocraft.trading_cells.feature.incubators.application.port.input;

import com.cosmocraft.trading_cells.feature.captures.domain.model.CapturedMobKind;
import com.cosmocraft.trading_cells.shared.machines.domain.model.TimedProcess;

public interface IncubatorUseCase {
    int durationTicks(CapturedMobKind kind);

    TimedProcess.Step advance(
            CapturedMobKind kind,
            int ticks,
            boolean validBaby,
            boolean outputAvailable
    );
}
