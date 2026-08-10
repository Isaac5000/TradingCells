package com.cosmocraft.trading_cells.feature.skeletonfarm.application.service;

import com.cosmocraft.trading_cells.feature.skeletonfarm.application.port.input.SkeletonFarmUseCase;
import com.cosmocraft.trading_cells.feature.skeletonfarm.domain.model.SkeletonFarmCycle;
import com.cosmocraft.trading_cells.feature.skeletonfarm.domain.model.SkeletonFarmKind;
import com.cosmocraft.trading_cells.feature.skeletonfarm.domain.model.SkeletonFarmLoot;
import com.cosmocraft.trading_cells.shared.machines.domain.model.TimedProcess;

public final class SkeletonFarmService implements SkeletonFarmUseCase {
    @Override
    public int effectiveCycleTicks(double tierPosition, int smiteLevel) {
        return SkeletonFarmCycle.effectiveCycleTicks(tierPosition, smiteLevel);
    }

    @Override
    public int simulatedKills(int sweepingEdgeLevel) {
        return SkeletonFarmCycle.simulatedKills(sweepingEdgeLevel);
    }

    @Override
    public boolean isEnabled(int mask, SkeletonFarmKind kind, SkeletonFarmLoot loot) {
        return SkeletonFarmCycle.isEnabled(mask, kind, loot);
    }

    @Override
    public int toggle(int mask, SkeletonFarmLoot loot) {
        return SkeletonFarmCycle.toggle(mask, loot);
    }

    @Override
    public int rescaleProgress(int ticks, int previousMaximum, int newMaximum) {
        return SkeletonFarmCycle.rescaleProgress(ticks, previousMaximum, newMaximum);
    }

    @Override
    public TimedProcess.Step advance(int ticks, int durationTicks, boolean canHunt, boolean outputAvailable) {
        return SkeletonFarmCycle.advance(ticks, durationTicks, canHunt, outputAvailable);
    }
}
