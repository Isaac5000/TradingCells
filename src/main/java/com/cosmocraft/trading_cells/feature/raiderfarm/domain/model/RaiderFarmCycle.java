package com.cosmocraft.trading_cells.feature.raiderfarm.domain.model;

import com.cosmocraft.trading_cells.shared.machines.domain.model.TimedProcess;
import com.cosmocraft.trading_cells.shared.mobfarm.domain.model.MobFarmCycleRules;

/** Pure timing, filtering and simulated-kill rules for the Raider Farm. */
public final class RaiderFarmCycle {
    public static final double MAX_EFFECTIVE_DAMAGE_LEVEL = MobFarmCycleRules.MAX_EFFECTIVE_DAMAGE_LEVEL;
    public static final int TICKS_PER_SECOND = MobFarmCycleRules.TICKS_PER_SECOND;

    private RaiderFarmCycle() {
    }

    public static int effectiveCycleTicks(double tierPosition, double effectiveDamageLevel) {
        return MobFarmCycleRules.effectiveCycleTicks(tierPosition, effectiveDamageLevel);
    }

    public static int simulatedKills(int sweepingEdgeLevel) {
        return MobFarmCycleRules.simulatedKills(sweepingEdgeLevel);
    }

    public static boolean isEnabled(int mask, RaiderFarmKind kind, RaiderFarmLoot loot) {
        return MobFarmCycleRules.isFilterEnabled(mask, loot.bit(), kind.supports(loot));
    }

    public static boolean hasEnabledLoot(int mask, RaiderFarmKind kind) {
        for (RaiderFarmLoot loot : kind.availableLoot()) {
            if (MobFarmCycleRules.isFilterEnabled(mask, loot.bit(), true)) {
                return true;
            }
        }
        return false;
    }

    public static int toggle(int mask, RaiderFarmLoot loot) {
        return MobFarmCycleRules.toggleFilter(mask, loot.bit());
    }

    public static int rescaleProgress(int ticks, int previousMaximum, int newMaximum) {
        return MobFarmCycleRules.rescaleProgress(ticks, previousMaximum, newMaximum);
    }

    public static TimedProcess.Step advance(
            int currentTicks,
            int durationTicks,
            boolean canHunt,
            boolean outputAvailable
    ) {
        return MobFarmCycleRules.advance(currentTicks, durationTicks, canHunt, outputAvailable);
    }
}
