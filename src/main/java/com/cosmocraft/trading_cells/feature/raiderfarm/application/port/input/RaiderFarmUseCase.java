package com.cosmocraft.trading_cells.feature.raiderfarm.application.port.input;

import com.cosmocraft.trading_cells.feature.raiderfarm.domain.model.RaiderFarmKind;
import com.cosmocraft.trading_cells.feature.raiderfarm.domain.model.RaiderFarmLoot;
import com.cosmocraft.trading_cells.shared.machines.domain.model.TimedProcess;

public interface RaiderFarmUseCase {
    int effectiveCycleTicks(double tierPosition, double effectiveDamageLevel);

    int simulatedKills(int sweepingEdgeLevel);

    boolean isEnabled(int mask, RaiderFarmKind kind, RaiderFarmLoot loot);

    boolean hasEnabledLoot(int mask, RaiderFarmKind kind);

    int toggle(int mask, RaiderFarmLoot loot);

    int rescaleProgress(int ticks, int previousMaximum, int newMaximum);

    TimedProcess.Step advance(int ticks, int durationTicks, boolean canHunt, boolean outputAvailable);
}
