package com.cosmocraft.trading_cells.feature.mobfarm.domain.model;

/** Upgrade throughput is primary; weapon quality supplies a bounded timing bonus. */
public final class MobFarmRules {
    private MobFarmRules() { }

    public static int multiplier(int upgradeTier) {
        return 1 << Math.clamp(upgradeTier, 0, 5);
    }

    public static int killsPerCycle(int capacityTier) {
        return multiplier(capacityTier);
    }

    public static int cycleDuration(int speedTier, double weaponTier, double damageLevel, int sweepingLevel) {
        double tier = Double.isFinite(weaponTier) ? Math.clamp(weaponTier, 0.0, 6.0) : 0.0;
        double damage = Double.isFinite(damageLevel) ? Math.clamp(damageLevel, 0.0, 10.0) : 0.0;
        double bonus = 1.0 + tier * 0.08 + damage * 0.03 + Math.clamp(sweepingLevel, 0, 5) * 0.03;
        return Math.max(20, (int) Math.ceil(1_200.0 / multiplier(speedTier) / bonus));
    }

    public static int rescaleProgress(int progress, int oldDuration, int newDuration) {
        return (int) Math.clamp((long) Math.max(0, progress) * Math.max(1, newDuration)
                / Math.max(1, oldDuration), 0L, Math.max(1, newDuration));
    }

    public static int essenceExperienceCost(boolean highLevel) { return highLevel ? 3_000 : 300; }
    public static int essenceShardCost(boolean highLevel) { return highLevel ? 16 : 4; }
    public static int essenceMaterialCost(boolean highLevel) { return highLevel ? 1 : 4; }
}
