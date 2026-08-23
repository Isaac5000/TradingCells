package com.cosmocraft.trading_cells.feature.zombiefarm.application.service;

import com.cosmocraft.trading_cells.feature.zombiefarm.application.port.input.ZombieFarmUseCase;
import com.cosmocraft.trading_cells.feature.zombiefarm.domain.model.ZombieFarmCycle;
import com.cosmocraft.trading_cells.feature.zombiefarm.domain.model.ZombieFarmKind;
import com.cosmocraft.trading_cells.feature.zombiefarm.domain.model.ZombieFarmLoot;
import com.cosmocraft.trading_cells.shared.machines.domain.model.TimedProcess;

public final class ZombieFarmService implements ZombieFarmUseCase {
    @Override
    public int effectiveCycleTicks(double tierPosition, double effectiveDamageLevel) {
        return ZombieFarmCycle.effectiveCycleTicks(tierPosition, effectiveDamageLevel);
    }

    @Override
    public int simulatedKills(int sweepingEdgeLevel) {
        return ZombieFarmCycle.simulatedKills(sweepingEdgeLevel);
    }

    @Override
    public boolean isEnabled(int mask, ZombieFarmKind kind, ZombieFarmLoot loot) {
        return ZombieFarmCycle.isEnabled(mask, kind, loot);
    }

    @Override
    public boolean hasEnabledLoot(int mask, ZombieFarmKind kind) {
        return ZombieFarmCycle.hasEnabledLoot(mask, kind);
    }

    @Override
    public int toggle(int mask, ZombieFarmLoot loot) {
        return ZombieFarmCycle.toggle(mask, loot);
    }

    @Override
    public int rescaleProgress(int ticks, int previousMaximum, int newMaximum) {
        return ZombieFarmCycle.rescaleProgress(ticks, previousMaximum, newMaximum);
    }

    @Override
    public TimedProcess.Step advance(int ticks, int durationTicks, boolean canHunt, boolean outputAvailable) {
        return ZombieFarmCycle.advance(ticks, durationTicks, canHunt, outputAvailable);
    }
}
