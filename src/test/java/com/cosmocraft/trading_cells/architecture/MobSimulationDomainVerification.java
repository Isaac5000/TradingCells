package com.cosmocraft.trading_cells.architecture;

import com.cosmocraft.trading_cells.feature.mobfarm.domain.model.MobFarmRules;
import com.cosmocraft.trading_cells.feature.mobfarm.domain.model.EssenceClassification;
import com.cosmocraft.trading_cells.feature.mobfarm.domain.model.EssenceTier;

final class MobSimulationDomainVerification {
    private MobSimulationDomainVerification() { }
    static void verify() {
        classification();
        syringeAnimation();
        int[] interactionDurations = {1200, 600, 300, 150, 75, 38};
        for (int tier = 0; tier < interactionDurations.length; tier++) {
            require(com.cosmocraft.trading_cells.feature.mobfarm.domain.model.InteractionFarmRules.operations(tier)
                    == 1 << tier, "Interaction capacity controls real operations independently");
            require(com.cosmocraft.trading_cells.feature.mobfarm.domain.model.InteractionFarmRules.cycleDuration(tier)
                    == interactionDurations[tier], "Interaction speed never changes batch size");
        }
        require(com.cosmocraft.trading_cells.feature.mobfarm.domain.model.InteractionFarmRules.operations(Integer.MIN_VALUE) == 1
                && com.cosmocraft.trading_cells.feature.mobfarm.domain.model.InteractionFarmRules.operations(Integer.MAX_VALUE) == 32,
                "Interaction capacity bounded before shifting");
        require(com.cosmocraft.trading_cells.feature.mobfarm.domain.model.InteractionFarmRules.cycleDuration(Integer.MIN_VALUE) == 1200,
                "Negative interaction tiers are bounded");
        require(com.cosmocraft.trading_cells.feature.mobfarm.domain.model.InteractionFarmRules.cycleDuration(Integer.MAX_VALUE) == 38,
                "Extreme interaction tiers cannot overflow");
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
    private static void syringeAnimation() {
        for (int tier = 1; tier <= 4; tier++) {
            require(com.cosmocraft.trading_cells.feature.mobfarm.domain.model.SyringeAnimation.extractionTicks(tier) == tier * 40,
                    "Syringe extraction takes two seconds per tier");
            for (int tick = 0; tick <= 6; tick++) {
                require(com.cosmocraft.trading_cells.feature.mobfarm.domain.model.SyringeAnimation.modelProgress(tier, tick)
                        == com.cosmocraft.trading_cells.feature.mobfarm.domain.model.SyringeAnimation.modelProgress(1, tick),
                        "Approach speed is independent of tier");
            }
            require(com.cosmocraft.trading_cells.feature.mobfarm.domain.model.SyringeAnimation.modelProgress(tier, 6) == 0.25F,
                    "Filling starts after the fixed approach");
            require(com.cosmocraft.trading_cells.feature.mobfarm.domain.model.SyringeAnimation.modelProgress(tier, tier * 40) == 1,
                    "Filling ends at the tier deadline");
            for (float tick = 6; tick <= tier * 40; tick += 0.25F) {
                require(com.cosmocraft.trading_cells.feature.mobfarm.domain.model.SyringeAnimation.armExtension(tick) == 1,
                        "Extraction arm stays fully extended until the vial has finished filling");
            }
        }
        require(com.cosmocraft.trading_cells.feature.mobfarm.domain.model.SyringeAnimation.modelProgress(4, 20)
                < com.cosmocraft.trading_cells.feature.mobfarm.domain.model.SyringeAnimation.modelProgress(1, 20),
                "Only filling slows down at higher tiers");
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
