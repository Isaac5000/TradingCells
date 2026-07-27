package com.cosmocraft.trading_cells.feature.farmer.domain.model;

import com.cosmocraft.trading_cells.shared.machines.domain.model.TimedProcess;

public final class FarmerCycle {
    private static final int MAX_EFFECTIVE_EFFICIENCY_LEVEL = 5;
    private static final int MINIMUM_GROWTH_TICKS = 20;
    private static final double NETHERITE_REFERENCE_SPEED = 9.0D;
    private static final double TOOL_TIER_BONUS = 0.35D;
    private static final double MAX_EFFICIENCY_BONUS = 4.42D;
    private static final double EFFICIENCY_TIER_EXPONENT = 1.70D;

    private FarmerCycle() {
    }

    public static FarmerHarvest harvest(FarmerCrop crop, int fortuneLevel) {
        int fortune = Math.max(0, fortuneLevel);
        return switch (crop) {
            case WHEAT, BEETROOT -> new FarmerHarvest(1, 1 + fortune);
            case CARROT, POTATO -> new FarmerHarvest(2 + fortune, 0);
            case NONE -> new FarmerHarvest(0, 0);
        };
    }

    public static int effectiveGrowthTicks(
            int configuredBaseTicks,
            double toolSpeed,
            int efficiencyLevel
    ) {
        long baseTicks = Math.max(MINIMUM_GROWTH_TICKS, configuredBaseTicks);
        double speed = Math.max(0.0D, toolSpeed);
        if (speed <= 0.0D) {
            return (int) Math.min(Integer.MAX_VALUE, baseTicks);
        }

        double normalizedTier = speed / NETHERITE_REFERENCE_SPEED;
        double tierBonus = TOOL_TIER_BONUS * Math.sqrt(normalizedTier);
        double efficiencyRatio = Math.clamp(
                efficiencyLevel,
                0,
                MAX_EFFECTIVE_EFFICIENCY_LEVEL
        ) / (double) MAX_EFFECTIVE_EFFICIENCY_LEVEL;
        double efficiencyBonus = MAX_EFFICIENCY_BONUS
                * efficiencyRatio
                * Math.pow(normalizedTier, EFFICIENCY_TIER_EXPONENT);
        double divisor = Math.max(1.0D, 1.0D + tierBonus + efficiencyBonus);
        return Math.max(
                MINIMUM_GROWTH_TICKS,
                (int) Math.ceil(baseTicks / divisor)
        );
    }

    public static int rescaleProgress(int ticks, int previousMaximum, int newMaximum) {
        if (ticks <= 0) {
            return 0;
        }
        int previous = Math.max(1, previousMaximum);
        int next = Math.max(1, newMaximum);
        long scaled = ((long) ticks * next + previous - 1L) / previous;
        return (int) Math.clamp(scaled, 1L, next);
    }

    public static TimedProcess.Step advance(
            int currentTicks,
            int durationTicks,
            boolean canCultivate,
            boolean outputAvailable
    ) {
        TimedProcess.Availability availability = TimedProcess.availability(canCultivate, outputAvailable);
        return TimedProcess.advance(currentTicks, durationTicks, availability);
    }
}
