package com.cosmocraft.trading_cells.feature.skeletonfarm.application.port.input;

import com.cosmocraft.trading_cells.feature.skeletonfarm.domain.model.SkeletonFarmKind;
import com.cosmocraft.trading_cells.feature.skeletonfarm.domain.model.SkeletonFarmLoot;
import com.cosmocraft.trading_cells.shared.machines.domain.model.TimedProcess;

public interface SkeletonFarmUseCase {
    int effectiveCycleTicks(double tierPosition, int smiteLevel);

    int simulatedKills(int sweepingEdgeLevel);

    boolean isEnabled(int mask, SkeletonFarmKind kind, SkeletonFarmLoot loot);

    int toggle(int mask, SkeletonFarmLoot loot);

    int rescaleProgress(int ticks, int previousMaximum, int newMaximum);

    TimedProcess.Step advance(int ticks, int durationTicks, boolean canHunt, boolean outputAvailable);
}
