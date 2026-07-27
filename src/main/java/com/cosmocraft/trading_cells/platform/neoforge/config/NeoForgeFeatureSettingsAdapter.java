package com.cosmocraft.trading_cells.platform.neoforge.config;

import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.Config;

public final class NeoForgeFeatureSettingsAdapter implements FeatureSettings {
    @Override public int villagerBreederTicks() { return Config.BREEDER_TICKS.get(); }
    @Override public int piglinBreederTicks() { return Config.BREEDER_TICKS.get(); }
    @Override public int villagerIncubatorTicks() { return Config.INCUBATOR_TICKS.get(); }
    @Override public int piglinIncubatorTicks() { return Config.INCUBATOR_TICKS.get(); }
    @Override public int farmerGrowthTicks() { return Config.FARMER_GROWTH_TICKS.get(); }
    @Override public int ironFarmCycleTicks() { return Config.IRON_FARM_CYCLE_TICKS.get(); }
    @Override public int ironFarmOneVillagerMultiplier() { return 2 + Config.IRON_FARM_MULTIPLIER_BONUS.get(); }
    @Override public int ironFarmTwoVillagerMultiplier() { return 4 + Config.IRON_FARM_MULTIPLIER_BONUS.get(); }
    @Override public int ironFarmThreeVillagerMultiplier() { return 8 + Config.IRON_FARM_MULTIPLIER_BONUS.get(); }
    @Override public int ironFarmBaseIron() { return 1; }
    @Override public int ironFarmMaximumPoppies() { return 2; }
    @Override public int ironGolemAttackTicks() { return 80; }
    @Override public int ironGolemHitInterval() { return 16; }
    @Override public int ironGolemRedFlashTicks() { return 5; }
    @Override public int converterInfectionTicks() { return 100; }
    @Override public int converterCureTicks() { return 3_600; }
    @Override public int converterCureDiscountPerCycle() { return 5; }
    @Override public int converterMaximumCureDiscount() { return Integer.MAX_VALUE; }
    @Override public int piglinBarterTicks() { return 20; }
    @Override public int villagerTradeRefreshTicks() { return 12_000; }
    @Override public boolean villagerInfiniteTrades() { return Config.VILLAGER_INFINITE_TRADES.get(); }
    @Override public int autotraderMinimumExperience() { return 3; }
    @Override public int autotraderMaximumExperience() { return 6; }
    @Override public int autotraderLevelUpExperienceBonus() { return 5; }
    @Override public int villagerBreadCost() { return 3; }
    @Override public int villagerVegetableCost() { return 12; }
    @Override public int piglinPorkCost() { return 2; }
    @Override public int piglinCrimsonFungusCost() { return 4; }
    @Override public int maximumPendingBabies() { return 64; }
}
