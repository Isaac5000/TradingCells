package com.cosmocraft.trading_cells.feature.configuredmobfarm.application.service;

import com.cosmocraft.trading_cells.feature.configuredmobfarm.application.port.input.ConfiguredMobFarmUseCase;
import com.cosmocraft.trading_cells.feature.configuredmobfarm.domain.model.ConfiguredMobFarmCycle;
import com.cosmocraft.trading_cells.feature.configuredmobfarm.domain.model.ConfiguredMobFarmKind;
import com.cosmocraft.trading_cells.feature.configuredmobfarm.domain.model.ConfiguredMobFarmLoot;
import com.cosmocraft.trading_cells.shared.machines.domain.model.TimedProcess;

public final class ConfiguredMobFarmService implements ConfiguredMobFarmUseCase {
    @Override
    public int effectiveCycleTicks(double tierPosition, double effectiveDamageLevel) {
        return ConfiguredMobFarmCycle.effectiveCycleTicks(tierPosition, effectiveDamageLevel);
    }

    @Override
    public int simulatedKills(int sweepingEdgeLevel) {
        return ConfiguredMobFarmCycle.simulatedKills(sweepingEdgeLevel);
    }

    @Override
    public boolean isEnabled(int mask, ConfiguredMobFarmKind kind, ConfiguredMobFarmLoot loot) {
        return ConfiguredMobFarmCycle.isEnabled(mask, kind, loot);
    }

    @Override
    public boolean hasEnabledLoot(int mask, ConfiguredMobFarmKind kind) {
        return ConfiguredMobFarmCycle.hasEnabledLoot(mask, kind);
    }

    @Override
    public int toggle(int mask, ConfiguredMobFarmLoot loot) {
        return ConfiguredMobFarmCycle.toggle(mask, loot);
    }

    @Override
    public int rescaleProgress(int ticks, int previousMaximum, int newMaximum) {
        return ConfiguredMobFarmCycle.rescaleProgress(ticks, previousMaximum, newMaximum);
    }

    @Override
    public TimedProcess.Step advance(int ticks, int durationTicks, boolean canHunt, boolean outputAvailable) {
        return ConfiguredMobFarmCycle.advance(ticks, durationTicks, canHunt, outputAvailable);
    }
}
