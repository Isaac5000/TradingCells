package com.cosmocraft.trading_cells.feature.farmer.domain.model;

import com.cosmocraft.trading_cells.shared.machines.domain.model.TimedProcess;
import com.cosmocraft.trading_cells.shared.machines.domain.model.ToolMachineSpeed;

public final class FarmerCycle {
    private static final int MINIMUM_GROWTH_TICKS = 20;
    private static final int FUNGUS_BASE_CHANCE = 3_500;
    private static final int FUNGUS_FORTUNE_BONUS = 1_000;
    private static final int FUNGUS_MAX_CHANCE = 7_500;
    private static final int SHROOMLIGHT_BASE_CHANCE = 2_000;
    private static final int SHROOMLIGHT_FORTUNE_BONUS = 750;
    private static final int SHROOMLIGHT_MAX_CHANCE = 5_000;
    private static final int WART_BLOCK_BASE_CHANCE = 6_500;
    private static final int WART_BLOCK_FORTUNE_BONUS = 500;
    private static final int WART_BLOCK_MAX_CHANCE = 9_000;

    private FarmerCycle() {
    }

    public static FarmerHarvest harvest(FarmerCrop crop, int fortuneLevel) {
        return harvest(crop, fortuneLevel, false);
    }

    public static FarmerHarvest harvest(FarmerCrop crop, int fortuneLevel, boolean silkTouch) {
        int fortune = Math.max(0, fortuneLevel);
        return switch (crop) {
            case WHEAT -> cropAndSeed(
                    FarmerProduct.WHEAT,
                    FarmerProduct.WHEAT_SEEDS,
                    fortune
            );
            case CARROT -> single(FarmerProduct.CARROT, 2 + fortune);
            case POTATO -> single(FarmerProduct.POTATO, 2 + fortune);
            case BEETROOT -> cropAndSeed(
                    FarmerProduct.BEETROOT,
                    FarmerProduct.BEETROOT_SEEDS,
                    fortune
            );
            case PUMPKIN -> single(FarmerProduct.PUMPKIN, 1 + fortune);
            case MELON -> single(
                    silkTouch ? FarmerProduct.MELON : FarmerProduct.MELON_SLICE,
                    silkTouch ? 1 + fortune : 3 + fortune
            );
            case SUGAR_CANE -> single(FarmerProduct.SUGAR_CANE, 2 + fortune);
            case COCOA -> single(FarmerProduct.COCOA_BEANS, 3 + fortune);
            case TORCHFLOWER -> cropAndSeed(
                    FarmerProduct.TORCHFLOWER,
                    FarmerProduct.TORCHFLOWER_SEEDS,
                    fortune
            );
            case PITCHER_PLANT -> cropAndSeed(
                    FarmerProduct.PITCHER_PLANT,
                    FarmerProduct.PITCHER_POD,
                    fortune
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

    private static FarmerHarvest cropAndSeed(
            FarmerProduct crop,
            FarmerProduct seed,
            int fortune
    ) {
        int count = 1 + fortune;
        return FarmerHarvest.of(
                FarmerYield.guaranteed(crop, count),
                FarmerYield.guaranteed(seed, count)
        );
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
        int wartBlockChance = Math.min(
                WART_BLOCK_MAX_CHANCE,
                WART_BLOCK_BASE_CHANCE + fortune * WART_BLOCK_FORTUNE_BONUS
        );
        return FarmerHarvest.of(
                FarmerYield.guaranteed(stem, 4 + fortune),
                FarmerYield.chance(wartBlock, 2 + fortune, wartBlockChance),
                FarmerYield.chance(fungus, 1, fungusChance),
                FarmerYield.chance(FarmerProduct.SHROOMLIGHT, 1, shroomlightChance)
        );
    }

    public static int effectiveGrowthTicks(
            int configuredBaseTicks,
            double toolSpeed,
            double tierPosition,
            int speedTier
    ) {
        int configuredMaximum = Math.max(MINIMUM_GROWTH_TICKS, configuredBaseTicks);
        if (toolSpeed <= 0.0D) {
            return ToolMachineSpeed.apply(configuredMaximum, speedTier);
        }
        return ToolMachineSpeed.apply(Math.min(configuredMaximum,
                ToolMachineSpeed.baseTicks(tierPosition, VanillaHoeTier.NETHERITE.timingPosition())), speedTier);
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
