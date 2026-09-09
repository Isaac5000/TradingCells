package com.cosmocraft.trading_cells.architecture;

import com.cosmocraft.trading_cells.feature.farmer.domain.model.FarmerCycle;
import com.cosmocraft.trading_cells.feature.farmer.domain.model.FarmerCrop;
import com.cosmocraft.trading_cells.feature.farmer.domain.model.FarmerHarvest;
import com.cosmocraft.trading_cells.feature.farmer.domain.model.FarmerProduct;
import com.cosmocraft.trading_cells.feature.farmer.domain.model.FarmerYield;
import com.cosmocraft.trading_cells.feature.farmer.domain.model.VanillaHoeTier;
import java.util.List;

final class FarmerDomainVerification {
    private FarmerDomainVerification() {
    }

    static void verify() {
        verifyFarmerRules();
    }

    private static void verifyFarmerRules() {
        require(FarmerHarvest.MAX_DISTINCT_OUTPUTS == 18,
                "Villager and piglin crop farms must expose eighteen output slots");
        require(FarmerCycle.rescaleProgress(50, 100, 200) == 100,
                "Changing a stack count or tool must preserve proportional progress");
        require(FarmerCycle.effectiveGrowthTicks(
                        3_000,
                        VanillaHoeTier.WOODEN.miningSpeed(),
                        VanillaHoeTier.WOODEN.timingPosition(),
                        0
                ) == 2_400,
                "A wooden hoe must give both crop farms the quarry's 120-second duration");
        require(FarmerCycle.effectiveGrowthTicks(
                        3_000,
                        VanillaHoeTier.NETHERITE.miningSpeed(),
                        VanillaHoeTier.NETHERITE.timingPosition(),
                        0
                ) == 400,
                "A netherite hoe must give both crop farms the quarry's 20-second duration");
        require(FarmerCycle.effectiveGrowthTicks(
                        3_000,
                        VanillaHoeTier.NETHERITE.miningSpeed(),
                        VanillaHoeTier.NETHERITE.timingPosition(),
                        5
                ) == 100,
                "A netherite Efficiency V hoe must give both crop farms a five-second duration");
        require(FarmerCycle.effectiveGrowthTicks(3_000, 0.0D, 0.0D, 5) == 3_000,
                "A crop farm without a hoe must retain its configured duration");
        require(FarmerCycle.effectiveGrowthTicks(
                        3_000,
                        VanillaHoeTier.IRON.miningSpeed(),
                        VanillaHoeTier.IRON.timingPosition(),
                        5
                ) == FarmerCycle.effectiveGrowthTicks(
                        3_000,
                        VanillaHoeTier.IRON.miningSpeed(),
                        VanillaHoeTier.IRON.timingPosition(),
                        7
                ),
                "Crop-farm Efficiency levels above five must not reduce duration further");
        require(FarmerCycle.effectiveGrowthTicks(
                        3_000,
                        VanillaHoeTier.GOLDEN.miningSpeed(),
                        VanillaHoeTier.GOLDEN.timingPosition(),
                        0
                ) > FarmerCycle.effectiveGrowthTicks(
                        3_000,
                        VanillaHoeTier.STONE.miningSpeed(),
                        VanillaHoeTier.STONE.timingPosition(),
                        0
                ),
                "The golden hoe must use its low tier instead of its high mining speed");
        int netheriteDuration = FarmerCycle.effectiveGrowthTicks(3_000, 9.0D, 6.0D, 0);
        int tierSevenDuration = FarmerCycle.effectiveGrowthTicks(3_000, 10.0D, 7.0D, 0);
        int tierEightDuration = FarmerCycle.effectiveGrowthTicks(3_000, 11.0D, 8.0D, 0);
        require(tierSevenDuration < netheriteDuration && tierEightDuration < tierSevenDuration,
                "Crop-farm hoe tiers above netherite must keep reducing duration");
        require(netheriteDuration - tierSevenDuration > tierSevenDuration - tierEightDuration,
                "Crop-farm tiers above netherite must provide diminishing reductions");
        require(FarmerCycle.effectiveGrowthTicks(3_000, 103.0D, 100.0D, 5) >= 20,
                "A modded hoe must never make a crop cycle faster than one second");
        require(VanillaHoeTier.COPPER.miningSpeed() == 5.0D
                        && VanillaHoeTier.COPPER.timingPosition() == 3.0D,
                "The fixed vanilla catalog must include the copper hoe tier");
        require(VanillaHoeTier.NETHERITE.miningSpeed() == 9.0D
                        && VanillaHoeTier.NETHERITE.timingPosition() == 6.0D
                        && VanillaHoeTier.GOLDEN.miningSpeed() == 12.0D
                        && VanillaHoeTier.GOLDEN.timingPosition() == 1.0D,
                "The fixed vanilla catalog must preserve netherite and gold tier values");

        FarmerHarvest baseFungus = FarmerCycle.harvest(FarmerCrop.CRIMSON_FUNGUS, 0);
        require(farmerYield(baseFungus, FarmerProduct.CRIMSON_STEM).count() == 4
                        && farmerYield(baseFungus, FarmerProduct.CRIMSON_STEM).isGuaranteed(),
                "A crimson fungus cycle must always produce four stems");
        require(farmerYield(baseFungus, FarmerProduct.NETHER_WART_BLOCK).count() == 2
                        && farmerYield(baseFungus, FarmerProduct.NETHER_WART_BLOCK).chanceBasisPoints() == 6_500,
                "A crimson fungus cycle must offer two wart blocks at a 65 percent base chance");
        require(farmerYield(baseFungus, FarmerProduct.CRIMSON_FUNGUS).chanceBasisPoints() == 3_500,
                "The base fungus return chance must remain balanced at 35 percent");
        require(farmerYield(baseFungus, FarmerProduct.SHROOMLIGHT).chanceBasisPoints() == 2_000,
                "The base shroomlight chance must remain balanced at 20 percent");

        FarmerHarvest fortuneThree = FarmerCycle.harvest(FarmerCrop.WARPED_FUNGUS, 3);
        require(farmerYield(fortuneThree, FarmerProduct.WARPED_FUNGUS).chanceBasisPoints() == 6_500,
                "Fortune III must raise fungus chance to 65 percent");
        require(farmerYield(fortuneThree, FarmerProduct.SHROOMLIGHT).chanceBasisPoints() == 4_250,
                "Fortune III must raise shroomlight chance to 42.5 percent");
        require(farmerYield(fortuneThree, FarmerProduct.WARPED_STEM).count() == 7,
                "Fortune III must increase guaranteed stem yield");
        require(farmerYield(fortuneThree, FarmerProduct.WARPED_WART_BLOCK).count() == 5
                        && farmerYield(fortuneThree, FarmerProduct.WARPED_WART_BLOCK).chanceBasisPoints() == 8_000,
                "Fortune III must increase wart block amount and chance");
        FarmerHarvest fortuneSeven = FarmerCycle.harvest(FarmerCrop.WARPED_FUNGUS, 7);
        require(farmerYield(fortuneSeven, FarmerProduct.WARPED_STEM).count() == 11
                        && farmerYield(fortuneSeven, FarmerProduct.WARPED_WART_BLOCK).count() == 9
                        && farmerYield(fortuneSeven, FarmerProduct.WARPED_WART_BLOCK).chanceBasisPoints() == 9_000,
                "Fortune VII must continue increasing stems and cap wart block chance at 90 percent");
        for (FarmerCrop crop : List.of(
                FarmerCrop.CRIMSON_ROOTS,
                FarmerCrop.NETHER_WART,
                FarmerCrop.WEEPING_VINES,
                FarmerCrop.NETHER_SPROUTS,
                FarmerCrop.WARPED_ROOTS,
                FarmerCrop.TWISTING_VINES
        )) {
            int baseCount = FarmerCycle.harvest(crop, 0).yields().getFirst().count();
            int fortuneCount = FarmerCycle.harvest(crop, 3).yields().getFirst().count();
            require(fortuneCount == baseCount + 3,
                    "Fortune III must add three items to regular Nether crop " + crop);
        }
        require(farmerYield(FarmerCycle.harvest(FarmerCrop.PUMPKIN, 0), FarmerProduct.PUMPKIN).count() == 1,
                "A pumpkin cycle must produce one base pumpkin");
        require(farmerYield(FarmerCycle.harvest(FarmerCrop.MELON, 3), FarmerProduct.MELON_SLICE).count() == 6,
                "Fortune III must increase melon-slice output without Silk Touch");
        require(farmerYield(FarmerCycle.harvest(FarmerCrop.MELON, 3, true), FarmerProduct.MELON).count() == 4,
                "Fortune III must increase melon-block output with Silk Touch");
        require(farmerYield(FarmerCycle.harvest(FarmerCrop.SUGAR_CANE, 0), FarmerProduct.SUGAR_CANE).count() == 2,
                "Sugar cane must produce two items before Fortune");
        require(farmerYield(FarmerCycle.harvest(FarmerCrop.COCOA, 3), FarmerProduct.COCOA_BEANS).count() == 6,
                "Fortune III must increase cocoa-bean output");
        FarmerHarvest wheat = FarmerCycle.harvest(FarmerCrop.WHEAT, 3);
        require(farmerYield(wheat, FarmerProduct.WHEAT).count() == 4
                        && farmerYield(wheat, FarmerProduct.WHEAT_SEEDS).count() == 4,
                "Fortune III must increase both wheat and seed output");
        FarmerHarvest beetroot = FarmerCycle.harvest(FarmerCrop.BEETROOT, 3);
        require(farmerYield(beetroot, FarmerProduct.BEETROOT).count() == 4
                        && farmerYield(beetroot, FarmerProduct.BEETROOT_SEEDS).count() == 4,
                "Fortune III must increase both beetroot and seed output");
        FarmerHarvest torchflower = FarmerCycle.harvest(FarmerCrop.TORCHFLOWER, 3);
        require(farmerYield(torchflower, FarmerProduct.TORCHFLOWER).count() == 4
                        && farmerYield(torchflower, FarmerProduct.TORCHFLOWER_SEEDS).count() == 4,
                "Fortune III must increase both torchflower and seed output");
        FarmerHarvest pitcherPlant = FarmerCycle.harvest(FarmerCrop.PITCHER_PLANT, 3);
        require(farmerYield(pitcherPlant, FarmerProduct.PITCHER_PLANT).count() == 4
                        && farmerYield(pitcherPlant, FarmerProduct.PITCHER_POD).count() == 4,
                "Fortune III must increase both pitcher plant and pod output");
        FarmerHarvest extremeTorchflower = FarmerCycle.harvest(FarmerCrop.TORCHFLOWER, 255);
        require(farmerYield(extremeTorchflower, FarmerProduct.TORCHFLOWER).count() == 256
                        && farmerYield(extremeTorchflower, FarmerProduct.TORCHFLOWER_SEEDS).count() == 256,
                "Fortune 255 must scale torchflowers and their seeds equally");
        FarmerHarvest extremePitcherPlant = FarmerCycle.harvest(FarmerCrop.PITCHER_PLANT, 255);
        require(farmerYield(extremePitcherPlant, FarmerProduct.PITCHER_PLANT).count() == 256
                        && farmerYield(extremePitcherPlant, FarmerProduct.PITCHER_POD).count() == 256,
                "Fortune 255 must scale pitcher plants and their pods equally");

        List<FarmerCrop> pairedCrops = List.of(
                FarmerCrop.WHEAT,
                FarmerCrop.BEETROOT,
                FarmerCrop.TORCHFLOWER,
                FarmerCrop.PITCHER_PLANT
        );
        List<FarmerProduct> pairedProducts = List.of(
                FarmerProduct.WHEAT,
                FarmerProduct.BEETROOT,
                FarmerProduct.TORCHFLOWER,
                FarmerProduct.PITCHER_PLANT
        );
        List<FarmerProduct> pairedSeeds = List.of(
                FarmerProduct.WHEAT_SEEDS,
                FarmerProduct.BEETROOT_SEEDS,
                FarmerProduct.TORCHFLOWER_SEEDS,
                FarmerProduct.PITCHER_POD
        );
        for (int index = 0; index < pairedCrops.size(); index++) {
            for (int fortune : new int[]{0, 3, 7, 255}) {
                FarmerHarvest harvest = FarmerCycle.harvest(pairedCrops.get(index), fortune);
                int expected = fortune + 1;
                require(farmerYield(harvest, pairedProducts.get(index)).count() == expected,
                        pairedCrops.get(index) + " product must scale at Fortune " + fortune);
                require(farmerYield(harvest, pairedSeeds.get(index)).count() == expected,
                        pairedCrops.get(index) + " seed must scale at Fortune " + fortune);
            }
        }
    }

    private static FarmerYield farmerYield(FarmerHarvest harvest, FarmerProduct product) {
        return harvest.yields().stream()
                .filter(yield -> yield.product() == product)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing farmer product " + product));
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
