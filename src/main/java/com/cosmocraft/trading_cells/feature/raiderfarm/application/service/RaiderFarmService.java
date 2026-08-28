package com.cosmocraft.trading_cells.feature.raiderfarm.application.service;

import com.cosmocraft.trading_cells.feature.raiderfarm.application.port.input.RaiderFarmUseCase;
import com.cosmocraft.trading_cells.feature.raiderfarm.domain.model.RaiderFarmCycle;
import com.cosmocraft.trading_cells.feature.raiderfarm.domain.model.RaiderFarmKind;
import com.cosmocraft.trading_cells.feature.raiderfarm.domain.model.RaiderFarmLoot;
import com.cosmocraft.trading_cells.shared.machines.domain.model.TimedProcess;

public final class RaiderFarmService implements RaiderFarmUseCase {
    @Override
    public int effectiveCycleTicks(double tierPosition, double effectiveDamageLevel) {
        return RaiderFarmCycle.effectiveCycleTicks(tierPosition, effectiveDamageLevel);
    }

    @Override
    public int simulatedKills(int sweepingEdgeLevel) {
        return RaiderFarmCycle.simulatedKills(sweepingEdgeLevel);
    }

    @Override
    public boolean isEnabled(int mask, RaiderFarmKind kind, RaiderFarmLoot loot) {
        return RaiderFarmCycle.isEnabled(mask, kind, loot);
    }

    @Override
    public boolean hasEnabledLoot(int mask, RaiderFarmKind kind) {
        return RaiderFarmCycle.hasEnabledLoot(mask, kind);
    }

    @Override
    public int toggle(int mask, RaiderFarmLoot loot) {
        return RaiderFarmCycle.toggle(mask, loot);
    }

    @Override
    public int rescaleProgress(int ticks, int previousMaximum, int newMaximum) {
        return RaiderFarmCycle.rescaleProgress(ticks, previousMaximum, newMaximum);
    }

    @Override
    public TimedProcess.Step advance(int ticks, int durationTicks, boolean canHunt, boolean outputAvailable) {
        return RaiderFarmCycle.advance(ticks, durationTicks, canHunt, outputAvailable);
    }
}
