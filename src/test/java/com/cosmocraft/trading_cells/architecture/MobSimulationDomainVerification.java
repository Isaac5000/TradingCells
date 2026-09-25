package com.cosmocraft.trading_cells.architecture;

import com.cosmocraft.trading_cells.feature.mobfarm.domain.model.MobFarmRules;
import com.cosmocraft.trading_cells.feature.mobfarm.domain.model.EssenceClassification;
import com.cosmocraft.trading_cells.feature.mobfarm.domain.model.EssenceTier;

final class MobSimulationDomainVerification {
    private MobSimulationDomainVerification() { }
    static void verify() {
        classification();
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
    private static void classification() {
        double previous = -1;
        for (double health : new double[]{20, 100, 500, 5_000, 100_000, Double.MAX_VALUE}) {
            var result = EssenceClassification.classify(new EssenceClassification.Attributes(health, 0, 0, 0, 0, 0), 0, 0, false, false);
            require(Double.isFinite(result.score()) && result.score() > previous, "Health scaling is finite and monotonic");
            require(result.version() == 1, "Classification is versioned");
            previous = result.score();
        }
        var absent = new EssenceClassification.Attributes(Double.NaN, -1, Double.POSITIVE_INFINITY, 0, 0, 0);
        require(EssenceClassification.classify(absent, 0, 0, false, false).tier() == EssenceTier.I, "Missing attributes are safe");
        require(EssenceClassification.classify(absent, 0, 0, true, false).tier() == EssenceTier.III, "Elite minimum");
        require(EssenceClassification.classify(absent, 0, 0, false, true).tier() == EssenceTier.IV, "Boss minimum");
        for (int tier = 1; tier <= 4; tier++) {
            require(EssenceClassification.classify(absent, 100, tier, true, true).tier().id() == tier, "Overrides win");
        }
        var creeper = new EssenceClassification.Attributes(20, 0, 0, 0, 0, 5);
        require(EssenceClassification.classify(creeper, 6, 0, false, false).tier() == EssenceTier.II, "Ordinary hostile tier");
        require(EssenceClassification.classify(creeper, 31, 0, false, false).tier() == EssenceTier.III, "Charged instance tier");
    }
    private static void require(boolean condition, String message) {
        if (!condition) { throw new AssertionError(message); }
    }
}
