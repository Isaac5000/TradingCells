package com.cosmocraft.trading_cells.platform.neoforge.config;

import com.cosmocraft.trading_cells.feature.captures.domain.model.CapturerDurability;
import com.cosmocraft.trading_cells.feature.ironfarm.domain.model.IronFarmCycle;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public final class FeatureSettingsProvider {
    private static final AtomicReference<FeatureSettings> SETTINGS = new AtomicReference<>(new Defaults());

    private FeatureSettingsProvider() {
    }

    public static void configure(FeatureSettings settings) {
        SETTINGS.set(Objects.requireNonNull(settings));
    }

    public static FeatureSettings values() {
        return SETTINGS.get();
    }

    private static final class Defaults implements FeatureSettings {
        @Override public int villagerBreederTicks() { return 6_000; }
        @Override public int piglinBreederTicks() { return 6_000; }
        @Override public int villagerIncubatorTicks() { return 3_000; }
        @Override public int piglinIncubatorTicks() { return 3_000; }
        @Override public int farmerGrowthTicks() { return 3_000; }
        @Override public boolean farmerDamagesHoe() { return true; }
        @Override public int capturerDurability() { return CapturerDurability.DEFAULT_MAX_DAMAGE; }
        @Override public int ironFarmCycleTicks() { return 1_200; }
        @Override public int ironFarmOneVillagerMultiplier() {
            return IronFarmCycle.BASE_ONE_VILLAGER_MULTIPLIER;
        }
        @Override public int ironFarmTwoVillagerMultiplier() {
            return IronFarmCycle.BASE_TWO_VILLAGER_MULTIPLIER;
        }
        @Override public int ironFarmThreeVillagerMultiplier() {
            return IronFarmCycle.BASE_THREE_VILLAGER_MULTIPLIER;
        }
        @Override public int ironFarmBaseIron() { return 1; }
        @Override public int ironFarmMaximumPoppies() { return 2; }
        @Override public int ironGolemAttackTicks() { return 80; }
        @Override public int ironGolemHitInterval() { return 16; }
        @Override public int ironGolemRedFlashTicks() { return 5; }
        @Override public int converterInfectionTicks() { return 100; }
        @Override public int converterCureTicks() { return 3_600; }
        @Override public int converterCureDiscountPerCycle() { return 5; }
        @Override public int converterMaximumCureDiscount() { return 25; }
        @Override public int piglinBarterTicks() { return 20; }
        @Override public int villagerTradeRefreshTicks() { return 12_000; }
        @Override public boolean villagerInfiniteTrades() { return true; }
        @Override public int autotraderMinimumExperience() { return 3; }
        @Override public int autotraderMaximumExperience() { return 6; }
        @Override public int autotraderLevelUpExperienceBonus() { return 5; }
        @Override public int villagerBreadCost() { return 3; }
        @Override public int villagerVegetableCost() { return 12; }
        @Override public int maximumPendingBabies() { return 64; }
    }
}
