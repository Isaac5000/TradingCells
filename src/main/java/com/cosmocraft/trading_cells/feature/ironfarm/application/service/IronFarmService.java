package com.cosmocraft.trading_cells.feature.ironfarm.application.service;

import com.cosmocraft.trading_cells.feature.ironfarm.application.port.input.IronFarmUseCase;
import com.cosmocraft.trading_cells.feature.ironfarm.application.port.output.IronFarmSettingsPort;
import com.cosmocraft.trading_cells.feature.ironfarm.domain.model.IronFarmCycle;
import com.cosmocraft.trading_cells.shared.machines.domain.model.TimedProcess;
import java.util.Objects;

/** Application boundary for iron farm production and animation timing. */
public final class IronFarmService implements IronFarmUseCase {
    private final IronFarmSettingsPort settings;
    private IronFarmCycle cachedCycle;

    public IronFarmService(IronFarmSettingsPort settings) {
        this.settings = Objects.requireNonNull(settings);
    }

    public IronFarmCycle cycle() {
        int cycleTicks = settings.ironFarmCycleTicks();
        int oneVillagerMultiplier = settings.ironFarmOneVillagerMultiplier();
        int twoVillagerMultiplier = settings.ironFarmTwoVillagerMultiplier();
        int threeVillagerMultiplier = settings.ironFarmThreeVillagerMultiplier();
        int attackTicks = settings.ironGolemAttackTicks();
        int hitInterval = settings.ironGolemHitInterval();
        int redFlashTicks = settings.ironGolemRedFlashTicks();
        if (cachedCycle == null
                || cachedCycle.cycleTicks() != cycleTicks
                || cachedCycle.oneVillagerMultiplier() != oneVillagerMultiplier
                || cachedCycle.twoVillagerMultiplier() != twoVillagerMultiplier
                || cachedCycle.threeVillagerMultiplier() != threeVillagerMultiplier
                || cachedCycle.golemAttackTicks() != attackTicks
                || cachedCycle.golemHitInterval() != hitInterval
                || cachedCycle.golemRedFlashTicks() != redFlashTicks) {
            cachedCycle = new IronFarmCycle(
                    cycleTicks,
                    oneVillagerMultiplier,
                    twoVillagerMultiplier,
                    threeVillagerMultiplier,
                    attackTicks,
                    hitInterval,
                    redFlashTicks
            );
        }
        return cachedCycle;
    }

    @Override
    public TimedProcess.Step advance(
            int ticks,
            int villagerCount,
            boolean outputAvailable,
            IronFarmCycle cycle
    ) {
        TimedProcess.Availability availability = TimedProcess.availability(villagerCount > 0, outputAvailable);
        return TimedProcess.advance(ticks, cycle.cycleTicks(), availability);
    }

    public int baseIron() {
        return Math.max(0, settings.ironFarmBaseIron());
    }

    public int maximumPoppies() {
        return Math.max(0, settings.ironFarmMaximumPoppies());
    }
}
