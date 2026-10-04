package com.cosmocraft.trading_cells.feature.mobfarm.domain.model;

public final class InteractionFarmRules {
    private InteractionFarmRules() { }
    public static int operations(int capacityTier) { return 1 << Math.clamp(capacityTier, 0, 5); }
    public static int cycleDuration(int speedTier) {
        int multiplier = 1 << Math.clamp(speedTier, 0, 5);
        return Math.max(20, (1200 + multiplier - 1) / multiplier);
    }
}
