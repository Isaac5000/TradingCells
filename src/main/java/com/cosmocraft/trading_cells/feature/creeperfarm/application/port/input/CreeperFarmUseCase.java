package com.cosmocraft.trading_cells.feature.creeperfarm.application.port.input;

import com.cosmocraft.trading_cells.feature.creeperfarm.domain.model.CreeperFarmKind;
import com.cosmocraft.trading_cells.feature.creeperfarm.domain.model.CreeperFarmLoot;
import com.cosmocraft.trading_cells.shared.machines.domain.model.TimedProcess;

public interface CreeperFarmUseCase {
    int effectiveCycleTicks(double tierPosition, double effectiveDamageLevel);

    int simulatedKills(int sweepingEdgeLevel);

    boolean isEnabled(int mask, CreeperFarmKind kind, CreeperFarmLoot loot);

    boolean hasEnabledLoot(int mask, CreeperFarmKind kind);

    int toggle(int mask, CreeperFarmLoot loot);

    int rescaleProgress(int ticks, int previousMaximum, int newMaximum);

    TimedProcess.Step advance(int ticks, int durationTicks, boolean canHunt, boolean outputAvailable);
}
