package com.cosmocraft.trading_cells.feature.incubators.application.service;

import com.cosmocraft.trading_cells.feature.incubators.application.port.input.IncubatorUseCase;
import com.cosmocraft.trading_cells.feature.incubators.application.port.output.IncubatorSettingsPort;
import com.cosmocraft.trading_cells.feature.incubators.domain.model.IncubationCycle;
import com.cosmocraft.trading_cells.feature.captures.domain.model.CapturedMobKind;
import com.cosmocraft.trading_cells.shared.machines.domain.model.TimedProcess;
import java.util.Objects;

/** Application boundary for incubation timing and transitions. */
public final class IncubatorService implements IncubatorUseCase {
    private final IncubatorSettingsPort settings;

    public IncubatorService(IncubatorSettingsPort settings) {
        this.settings = Objects.requireNonNull(settings);
    }

    public int durationTicks(CapturedMobKind kind) {
        return kind == CapturedMobKind.VILLAGER
                ? Math.max(1, settings.villagerIncubatorTicks())
                : Math.max(1, settings.piglinIncubatorTicks());
    }

    public TimedProcess.Step advance(
            CapturedMobKind kind,
            int ticks,
            boolean validBaby,
            boolean outputAvailable
    ) {
        return IncubationCycle.advance(
                ticks,
                durationTicks(kind),
                validBaby,
                outputAvailable
        );
    }
}
