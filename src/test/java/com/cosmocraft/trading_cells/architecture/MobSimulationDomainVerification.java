package com.cosmocraft.trading_cells.architecture;

import com.cosmocraft.trading_cells.feature.mobfarm.domain.model.MobFarmRules;

final class MobSimulationDomainVerification {
    private MobSimulationDomainVerification() { }
    static void verify() {
        int previous = Integer.MAX_VALUE;
        for (int tier = 0; tier <= 5; tier++) {
            int duration = MobFarmRules.cycleDuration(tier, 0, 0, 0);
            require(duration < previous, "Each speed tier must improve baseline throughput");
            require(MobFarmRules.killsPerCycle(tier) == 1 << tier, "Capacity is a separate upgrade");
            previous = duration;
        }
        require(MobFarmRules.killsPerCycle(0) == 1, "A weapon never multiplies kills");
        require(MobFarmRules.killsPerCycle(Integer.MAX_VALUE) == 32, "Capacity is bounded");
        require(MobFarmRules.cycleDuration(0, 6, 10, 5) > MobFarmRules.cycleDuration(2, 0, 0, 0),
                "Upgrades must dominate supplementary enchantments");
        require(MobFarmRules.cycleDuration(5, Double.NaN, Double.POSITIVE_INFINITY, Integer.MAX_VALUE) >= 20,
                "Malformed inputs cannot cause zero-time production");
        require(MobFarmRules.rescaleProgress(300, 1200, 600) == 150, "Upgrade swaps retain proportional progress");
        require(MobFarmRules.rescaleProgress(Integer.MAX_VALUE, 1, Integer.MAX_VALUE) == Integer.MAX_VALUE,
                "Progress arithmetic cannot overflow");
        require(MobFarmRules.essenceExperienceCost(true) > MobFarmRules.essenceExperienceCost(false), "Boss synthesis costs more");
        require(MobFarmRules.essenceExperienceCost(true) % 10 == 0 && MobFarmRules.essenceExperienceCost(false) % 10 == 0,
                "Essence XP uses multiples of ten");
    }
    private static void require(boolean condition, String message) {
        if (!condition) { throw new AssertionError(message); }
    }
}
