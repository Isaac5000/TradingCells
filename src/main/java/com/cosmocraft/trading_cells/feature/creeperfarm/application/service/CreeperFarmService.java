package com.cosmocraft.trading_cells.feature.creeperfarm.application.service;

import com.cosmocraft.trading_cells.feature.creeperfarm.application.port.input.CreeperFarmUseCase;
import com.cosmocraft.trading_cells.feature.creeperfarm.domain.model.CreeperFarmCycle;
import com.cosmocraft.trading_cells.feature.creeperfarm.domain.model.CreeperFarmKind;
import com.cosmocraft.trading_cells.feature.creeperfarm.domain.model.CreeperFarmLoot;
import com.cosmocraft.trading_cells.shared.machines.domain.model.TimedProcess;

public final class CreeperFarmService implements CreeperFarmUseCase {
    @Override
    public int effectiveCycleTicks(double tierPosition, double effectiveDamageLevel) {
        return CreeperFarmCycle.effectiveCycleTicks(tierPosition, effectiveDamageLevel);
    }

    @Override
    public int simulatedKills(int sweepingEdgeLevel) {
        return CreeperFarmCycle.simulatedKills(sweepingEdgeLevel);
    }

    @Override
    public boolean isEnabled(int mask, CreeperFarmKind kind, CreeperFarmLoot loot) {
        return CreeperFarmCycle.isEnabled(mask, kind, loot);
    }

    @Override
    public boolean hasEnabledLoot(int mask, CreeperFarmKind kind) {
        return CreeperFarmCycle.hasEnabledLoot(mask, kind);
    }

    @Override
    public int toggle(int mask, CreeperFarmLoot loot) {
        return CreeperFarmCycle.toggle(mask, loot);
    }

    @Override
    public int rescaleProgress(int ticks, int previousMaximum, int newMaximum) {
        return CreeperFarmCycle.rescaleProgress(ticks, previousMaximum, newMaximum);
    }

    @Override
    public TimedProcess.Step advance(int ticks, int durationTicks, boolean canHunt, boolean outputAvailable) {
        return CreeperFarmCycle.advance(ticks, durationTicks, canHunt, outputAvailable);
    }
}
