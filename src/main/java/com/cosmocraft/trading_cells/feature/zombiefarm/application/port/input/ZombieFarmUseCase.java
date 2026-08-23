package com.cosmocraft.trading_cells.feature.zombiefarm.application.port.input;

import com.cosmocraft.trading_cells.feature.zombiefarm.domain.model.ZombieFarmKind;
import com.cosmocraft.trading_cells.feature.zombiefarm.domain.model.ZombieFarmLoot;
import com.cosmocraft.trading_cells.shared.machines.domain.model.TimedProcess;

public interface ZombieFarmUseCase {
    int effectiveCycleTicks(double tierPosition, double effectiveDamageLevel);

    int simulatedKills(int sweepingEdgeLevel);

    boolean isEnabled(int mask, ZombieFarmKind kind, ZombieFarmLoot loot);

    boolean hasEnabledLoot(int mask, ZombieFarmKind kind);

    int toggle(int mask, ZombieFarmLoot loot);

    int rescaleProgress(int ticks, int previousMaximum, int newMaximum);

    TimedProcess.Step advance(int ticks, int durationTicks, boolean canHunt, boolean outputAvailable);
}
