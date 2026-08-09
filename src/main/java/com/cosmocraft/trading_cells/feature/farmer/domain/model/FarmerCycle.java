package com.cosmocraft.trading_cells.feature.farmer.domain.model;

import com.cosmocraft.trading_cells.shared.machines.domain.model.TimedProcess;

public final class FarmerCycle {
    private static final int MAX_EFFECTIVE_EFFICIENCY_LEVEL = 5;
    private static final int MINIMUM_GROWTH_TICKS = 20;
    private static final int TICKS_PER_SECOND = 20;
    private static final double WOODEN_BASE_SECONDS = 120.0D;
    private static final double WOODEN_MAX_EFFICIENCY_SECONDS = 30.0D;
    private static final double NETHERITE_BASE_SECONDS = 20.0D;
    private static final double NETHERITE_MAX_EFFICIENCY_SECONDS = 5.0D;
    private static final double MINIMUM_DURATION_SECONDS = 1.0D;
    private static final double NETHERITE_POSITION = VanillaHoeTier.NETHERITE.timingPosition();
    private static final double TIER_RATIO = Math.pow(
            NETHERITE_BASE_SECONDS / WOODEN_BASE_SECONDS,
            1.0D / NETHERITE_POSITION
    );
    private static final double ROUNDING_EPSILON = 1.0E-9D;
    private static final int FUNGUS_BASE_CHANCE = 3_500;
    private static final int FUNGUS_FORTUNE_BONUS = 1_000;
    private static final int FUNGUS_MAX_CHANCE = 7_500;
    private static final int SHROOMLIGHT_BASE_CHANCE = 2_000;
    private static final int SHROOMLIGHT_FORTUNE_BONUS = 750;
    private static final int SHROOMLIGHT_MAX_CHANCE = 5_000;

    private FarmerCycle() {
    }

    public static FarmerHarvest harvest(FarmerCrop crop, int fortuneLevel) {
        int fortune = Math.max(0, fortuneLevel);
        return switch (crop) {
            case WHEAT -> FarmerHarvest.of(
                    FarmerYield.guaranteed(FarmerProduct.WHEAT, 1),
                    FarmerYield.guaranteed(FarmerProduct.WHEAT_SEEDS, 1 + fortune)
            );
            case CARROT -> single(FarmerProduct.CARROT, 2 + fortune);
            case POTATO -> single(FarmerProduct.POTATO, 2 + fortune);
            case BEETROOT -> FarmerHarvest.of(
                    FarmerYield.guaranteed(FarmerProduct.BEETROOT, 1),
                    FarmerYield.guaranteed(FarmerProduct.BEETROOT_SEEDS, 1 + fortune)
            );
            case CRIMSON_FUNGUS -> fungusHarvest(
                    FarmerProduct.CRIMSON_FUNGUS,
                    FarmerProduct.CRIMSON_STEM,
                    FarmerProduct.NETHER_WART_BLOCK,
                    fortune
            );
            case WARPED_FUNGUS -> fungusHarvest(
                    FarmerProduct.WARPED_FUNGUS,
                    FarmerProduct.WARPED_STEM,
                    FarmerProduct.WARPED_WART_BLOCK,
                    fortune
            );
            case CRIMSON_ROOTS -> single(FarmerProduct.CRIMSON_ROOTS, 2 + fortune);
            case NETHER_WART -> single(FarmerProduct.NETHER_WART, 3 + fortune);
            case WEEPING_VINES -> single(FarmerProduct.WEEPING_VINES, 2 + fortune);
            case NETHER_SPROUTS -> single(FarmerProduct.NETHER_SPROUTS, 3 + fortune);
            case WARPED_ROOTS -> single(FarmerProduct.WARPED_ROOTS, 2 + fortune);
            case TWISTING_VINES -> single(FarmerProduct.TWISTING_VINES, 2 + fortune);
            case NONE -> FarmerHarvest.of();
        };
    }

    private static FarmerHarvest single(FarmerProduct product, int count) {
        return FarmerHarvest.of(FarmerYield.guaranteed(product, count));
    }

    private static FarmerHarvest fungusHarvest(
            FarmerProduct fungus,
            FarmerProduct stem,
            FarmerProduct wartBlock,
            int fortune
    ) {
        int fungusChance = Math.min(
                FUNGUS_MAX_CHANCE,
                FUNGUS_BASE_CHANCE + fortune * FUNGUS_FORTUNE_BONUS
        );
        int shroomlightChance = Math.min(
                SHROOMLIGHT_MAX_CHANCE,
                SHROOMLIGHT_BASE_CHANCE + fortune * SHROOMLIGHT_FORTUNE_BONUS
        );
        return FarmerHarvest.of(
                FarmerYield.guaranteed(stem, 4 + fortune),
                FarmerYield.guaranteed(wartBlock, 2 + fortune),
                FarmerYield.chance(fungus, 1, fungusChance),
                FarmerYield.chance(FarmerProduct.SHROOMLIGHT, 1, shroomlightChance)
        );
    }

    public static int effectiveGrowthTicks(
            int configuredBaseTicks,
            double toolSpeed,
            double tierPosition,
            int efficiencyLevel
    ) {
        int configuredMaximum = Math.max(MINIMUM_GROWTH_TICKS, configuredBaseTicks);
        if (toolSpeed <= 0.0D) {
            return configuredMaximum;
        }
        return Math.min(
                configuredMaximum,
                villagerToolDurationSeconds(tierPosition, efficiencyLevel) * TICKS_PER_SECOND
        );
    }

    private static int villagerToolDurationSeconds(double tierPosition, int efficiencyLevel) {
        int efficiency = Math.clamp(efficiencyLevel, 0, MAX_EFFECTIVE_EFFICIENCY_LEVEL);
        double position = Double.isFinite(tierPosition) ? Math.max(0.0D, tierPosition) : 0.0D;
        double startingDuration = villagerStartingDuration(position);
        double maximumEfficiencyDuration = villagerMaximumEfficiencyDuration(position);
        double progress = efficiency / (double) MAX_EFFECTIVE_EFFICIENCY_LEVEL;
        double startingDistance = Math.max(
                Double.MIN_NORMAL,
                startingDuration - MINIMUM_DURATION_SECONDS
        );
        double endingDistance = Math.max(
                Double.MIN_NORMAL,
                Math.min(startingDistance, maximumEfficiencyDuration - MINIMUM_DURATION_SECONDS)
        );
        double duration = MINIMUM_DURATION_SECONDS
                + startingDistance * Math.pow(endingDistance / startingDistance, progress);
        return Math.max(
                (int) MINIMUM_DURATION_SECONDS,
                (int) Math.floor(duration + ROUNDING_EPSILON)
        );
    }

    private static double villagerStartingDuration(double position) {
        if (position <= NETHERITE_POSITION) {
            return WOODEN_BASE_SECONDS * Math.pow(TIER_RATIO, position);
        }
        return MINIMUM_DURATION_SECONDS
                + (NETHERITE_BASE_SECONDS - MINIMUM_DURATION_SECONDS)
                * Math.pow(TIER_RATIO, position - NETHERITE_POSITION);
    }

    private static double villagerMaximumEfficiencyDuration(double position) {
        if (position <= NETHERITE_POSITION) {
            return WOODEN_MAX_EFFICIENCY_SECONDS * Math.pow(TIER_RATIO, position);
        }
        return MINIMUM_DURATION_SECONDS
                + (NETHERITE_MAX_EFFICIENCY_SECONDS - MINIMUM_DURATION_SECONDS)
                * Math.pow(TIER_RATIO, position - NETHERITE_POSITION);
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
