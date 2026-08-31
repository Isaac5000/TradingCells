package com.cosmocraft.trading_cells.feature.configuredmobfarm.application.port.input;

import com.cosmocraft.trading_cells.feature.configuredmobfarm.domain.model.ConfiguredMobFarmKind;
import com.cosmocraft.trading_cells.feature.configuredmobfarm.domain.model.ConfiguredMobFarmLoot;
import com.cosmocraft.trading_cells.shared.machines.domain.model.TimedProcess;

public interface ConfiguredMobFarmUseCase {
    int effectiveCycleTicks(double tierPosition, double effectiveDamageLevel);

    int simulatedKills(int sweepingEdgeLevel);

    boolean isEnabled(int mask, ConfiguredMobFarmKind kind, ConfiguredMobFarmLoot loot);

    boolean hasEnabledLoot(int mask, ConfiguredMobFarmKind kind);

    int toggle(int mask, ConfiguredMobFarmLoot loot);

    int rescaleProgress(int ticks, int previousMaximum, int newMaximum);

    TimedProcess.Step advance(int ticks, int durationTicks, boolean canHunt, boolean outputAvailable);
}
