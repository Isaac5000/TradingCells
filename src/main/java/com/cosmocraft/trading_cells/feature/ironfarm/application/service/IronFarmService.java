package com.cosmocraft.trading_cells.feature.ironfarm.application.service;

import com.cosmocraft.trading_cells.feature.ironfarm.application.port.input.IronFarmUseCase;
import com.cosmocraft.trading_cells.feature.ironfarm.application.port.output.IronFarmSettingsPort;
import com.cosmocraft.trading_cells.feature.ironfarm.domain.model.IronFarmCycle;
import com.cosmocraft.trading_cells.shared.machines.domain.model.TimedProcess;
import java.util.Objects;

/** Application boundary for iron farm production and animation timing. */
public final class IronFarmService implements IronFarmUseCase {
    private final IronFarmSettingsPort settings;

    public IronFarmService(IronFarmSettingsPort settings) {
        this.settings = Objects.requireNonNull(settings);
    }

    public IronFarmCycle cycle() {
        return new IronFarmCycle(
                settings.ironFarmCycleTicks(),
                settings.ironFarmOneVillagerMultiplier(),
                settings.ironFarmTwoVillagerMultiplier(),
                settings.ironFarmThreeVillagerMultiplier(),
                settings.ironGolemAttackTicks(),
                settings.ironGolemHitInterval(),
                settings.ironGolemRedFlashTicks()
        );
    }

    public TimedProcess.Step advance(
            int ticks,
            int villagerCount,
            boolean outputAvailable
    ) {
        TimedProcess.Availability availability = TimedProcess.availability(villagerCount > 0, outputAvailable);
        return TimedProcess.advance(ticks, cycle().cycleTicks(), availability);
    }

    public int baseIron() {
        return Math.max(0, settings.ironFarmBaseIron());
    }

    public int maximumPoppies() {
        return Math.max(0, settings.ironFarmMaximumPoppies());
    }
}
