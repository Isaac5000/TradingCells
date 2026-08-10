package com.cosmocraft.trading_cells.architecture;

import com.cosmocraft.trading_cells.feature.trader.domain.model.AutotraderOfferLifecycle;
import com.cosmocraft.trading_cells.feature.trader.domain.model.AutotraderOfferSelection;
import com.cosmocraft.trading_cells.feature.breeders.domain.model.BreederFood;
import com.cosmocraft.trading_cells.feature.breeders.domain.model.BreederKind;
import com.cosmocraft.trading_cells.feature.breeders.domain.model.BreederRecipe;
import com.cosmocraft.trading_cells.feature.breeders.domain.model.BreederRules;
import com.cosmocraft.trading_cells.feature.converter.domain.model.ConverterCycle;
import com.cosmocraft.trading_cells.feature.converter.domain.model.ConverterStage;
import com.cosmocraft.trading_cells.feature.captures.domain.model.CapturerDurability;
import com.cosmocraft.trading_cells.feature.captures.adapters.output.client.CapturedEntityGuiTransform;
import com.cosmocraft.trading_cells.feature.farmer.domain.model.FarmerCycle;
import com.cosmocraft.trading_cells.feature.farmer.domain.model.FarmerCrop;
import com.cosmocraft.trading_cells.feature.farmer.domain.model.FarmerHarvest;
import com.cosmocraft.trading_cells.feature.farmer.domain.model.FarmerProduct;
import com.cosmocraft.trading_cells.feature.farmer.domain.model.FarmerYield;
import com.cosmocraft.trading_cells.feature.farmer.domain.model.VanillaHoeTier;
import com.cosmocraft.trading_cells.feature.experience.application.service.ExperienceStorageService;
import com.cosmocraft.trading_cells.feature.experience.domain.model.ExperienceMath;
import com.cosmocraft.trading_cells.feature.incubators.domain.model.IncubationCycle;
import com.cosmocraft.trading_cells.feature.infusion.application.service.ArcaneInfusionService;
import com.cosmocraft.trading_cells.feature.infusion.domain.model.ArcaneInfusionAttempt;
import com.cosmocraft.trading_cells.feature.infusion.domain.model.ArcaneInfusionDecision;
import com.cosmocraft.trading_cells.feature.ironfarm.domain.model.IronFarmCycle;
import com.cosmocraft.trading_cells.feature.quarry.adapters.input.QuarryBlockEntity;
import com.cosmocraft.trading_cells.feature.quarry.domain.model.QuarryCycle;
import com.cosmocraft.trading_cells.feature.quarry.domain.model.QuarryFortune;
import com.cosmocraft.trading_cells.feature.quarry.domain.model.QuarryUpgradeTier;
import com.cosmocraft.trading_cells.feature.quarry.domain.model.VanillaPickaxeTier;
import com.cosmocraft.trading_cells.feature.skeletonfarm.adapters.input.SkeletonFarmBlockEntity;
import com.cosmocraft.trading_cells.feature.skeletonfarm.adapters.input.SkeletonFarmMenu;
import com.cosmocraft.trading_cells.feature.skeletonfarm.domain.model.SkeletonFarmCycle;
import com.cosmocraft.trading_cells.feature.skeletonfarm.domain.model.SkeletonFarmKind;
import com.cosmocraft.trading_cells.feature.skeletonfarm.domain.model.SkeletonFarmLoot;
import com.cosmocraft.trading_cells.feature.skeletonfarm.domain.model.VanillaSwordTier;
import com.cosmocraft.trading_cells.shared.machines.domain.model.MinecraftExperience;
import com.cosmocraft.trading_cells.shared.machines.domain.model.TimedProcess;
import com.cosmocraft.trading_cells.shared.machines.domain.model.MachineActivityController;
import com.cosmocraft.trading_cells.feature.trader.domain.model.PiglinBarterCycle;
import com.cosmocraft.trading_cells.feature.trader.domain.model.VillagerOfferPersistence;
import com.cosmocraft.trading_cells.feature.trader.domain.service.TradeDiscountPolicy;
import com.cosmocraft.trading_cells.feature.trader.domain.model.PiglinBarterUpgradeYield;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.SlotRenderer;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.MachineScreenUtil;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.trader.VillagerTradeScreenLayout;
import com.cosmocraft.trading_cells.platform.neoforge.menu.VillagerTradeMenuLayout;
import com.cosmocraft.trading_cells.platform.neoforge.event.HighLevelEnchantmentPalette;
import java.util.List;

public final class DomainRulesVerification {
    private DomainRulesVerification() {
    }

    public static void main(String[] args) { // NOSONAR - The JVM entry-point signature requires this parameter.
        verifyOfferLifecycle();
        verifyOfferSelection();
        verifyTemporaryTradeDiscounts();
        verifyTimedProcesses();
        verifyMachineActivity();
        verifyMachineActivityEquivalence();
        verifyBreederRules();
        verifyFarmerRules();
        verifyConverterRules();
        verifyIronFarmRules();
        verifySkeletonFarmRules();
        verifyQuarryRules();
        verifyPiglinBarterRules();
        verifyCapturerRules();
        verifyCapturedEntityGuiCenter();
        verifyDurationFormatting();
        verifyVillagerTradeGeometry();
        verifyExperienceStorage();
        verifyArcaneInfusion();
        verifyHighLevelEnchantmentColors();
    }

    private static void verifyArcaneInfusion() {
        ArcaneInfusionService infusion = new ArcaneInfusionService();
        require(infusion.evaluate(new ArcaneInfusionAttempt(true, true, 14_999, 15_000))
                        == ArcaneInfusionDecision.EXPERIENCE_REQUIRED,
                "Arcane infusion must not consume resources with 14,999 XP");
        require(infusion.evaluate(new ArcaneInfusionAttempt(true, true, 15_000, 15_000))
                        == ArcaneInfusionDecision.READY,
                "Arcane infusion must become ready at exactly 15,000 XP");
        require(infusion.evaluate(new ArcaneInfusionAttempt(true, true, 29_999, 30_000))
                        == ArcaneInfusionDecision.EXPERIENCE_REQUIRED,
                "The Miner's Touch infusion must remain blocked with 29,999 XP");
        require(infusion.evaluate(new ArcaneInfusionAttempt(true, true, 30_000, 30_000))
                        == ArcaneInfusionDecision.READY,
                "The Miner's Touch infusion must become ready at exactly 30,000 XP");
        require(infusion.evaluate(new ArcaneInfusionAttempt(true, true, 4_999, 5_000))
                        == ArcaneInfusionDecision.EXPERIENCE_REQUIRED,
                "The Nitwit infusion must remain blocked with 4,999 XP");
        require(infusion.evaluate(new ArcaneInfusionAttempt(true, true, 5_000, 5_000))
                        == ArcaneInfusionDecision.READY,
                "The Nitwit infusion must become ready at exactly 5,000 XP");
        require(infusion.evaluate(new ArcaneInfusionAttempt(true, false, 15_000, 15_000))
                        == ArcaneInfusionDecision.OUTPUT_BLOCKED,
                "An occupied result slot must block the whole atomic infusion");
        require(infusion.evaluate(new ArcaneInfusionAttempt(false, true, 15_000, 15_000))
                        == ArcaneInfusionDecision.INGREDIENTS_REQUIRED,
                "Incomplete ingredients must block the whole atomic infusion");
        require(infusion.depositAll(30, 0.5F, Integer.MAX_VALUE - 1, Integer.MAX_VALUE) == 1,
                "The infuser deposit must clamp safely at the positive int XP limit");
        require(infusion.depositLevels(30, 0.5F, 0, Integer.MAX_VALUE, 0) == 0,
                "Zero and negative level requests must not transfer XP");
    }

    private static void verifyExperienceStorage() {
        ExperienceStorageService storage = new ExperienceStorageService();
        require(ExperienceMath.pointsAtStartOfLevel(10) == 160,
                "Level ten must start at the vanilla total of 160 XP points");
        float sevenOfTwentyThree = 7.0F / 23.0F;
        require(ExperienceMath.totalPoints(8, sevenOfTwentyThree) == 119,
                "Float XP progress must round back to its exact integer point count");
        require(storage.depositAll(8, sevenOfTwentyThree, 0, 1_000_000) == 119,
                "Deposit all must not leave one point behind because of float rounding");
        require(storage.depositLevels(8, sevenOfTwentyThree, 0, 1_000_000, 3) == 59,
                "Storing levels with partial progress must use the exact integer XP total");
        require(storage.withdrawLevels(5, 5.0F / 17.0F, 59, 3) == 59,
                "Withdrawing stored levels must restore the exact previous XP total");
        for (int total : new int[] {0, 1, 7, 119, 1_000_000, Integer.MAX_VALUE}) {
            MinecraftExperience.ExperienceState state = MinecraftExperience.stateForTotalPoints(total);
            float progress = (float) state.pointsIntoLevel()
                    / MinecraftExperience.pointsNeededForNextLevel(state.level());
            require(MinecraftExperience.totalPoints(state.level(), progress) == total,
                    "Every normalized player XP state must preserve its exact integer total");
        }
        for (int level = 0; level <= 21_863; level++) {
            int levelStart = MinecraftExperience.pointsAtStartOfLevel(level);
            int needed = MinecraftExperience.pointsNeededForNextLevel(level);
            int maximumPartial = (int) Math.min(
                    (long) needed - 1L,
                    (long) Integer.MAX_VALUE - levelStart
            );
            for (int partial : new int[] {
                    0,
                    Math.min(1, maximumPartial),
                    maximumPartial / 3,
                    maximumPartial / 2,
                    maximumPartial
            }) {
                float progress = (float) partial / needed;
                require(MinecraftExperience.totalPoints(level, progress) == levelStart + partial,
                        "Sampled vanilla XP bars must reconstruct every tested integer point exactly");
            }
        }
        require(storage.depositLevels(10, 0.0F, 0, 1_000_000, 3) == 69,
                "Storing three levels must transfer their exact vanilla point difference");
        require(storage.withdrawLevels(7, 0.0F, 69, 3) == 69,
                "Withdrawing those levels must restore the same XP points");
        require(storage.depositAll(30, 0.5F, 999_990, 1_000_000) == 10,
                "Deposit all must clamp to the block's remaining capacity");
        require(ExperienceMath.maximumAdditionalLevels(0, 0.0F, 7) == 1,
                "Seven stored points must buy exactly the first player level");
        require(ExperienceMath.pointsAtStartOfLevel(21_863) == 2_147_407_943,
                "The highest complete level representable by int XP must remain exact");
        require(ExperienceMath.pointsAtStartOfLevel(21_864) == Integer.MAX_VALUE,
                "XP totals above the int range must saturate without overflowing");
        require(ExperienceMath.levelForTotalPoints(Integer.MAX_VALUE) == 21_863,
                "The full int capacity must report the highest complete representable level");
        require(storage.depositAll(30, 0.5F, Integer.MAX_VALUE - 3, Integer.MAX_VALUE) == 3,
                "Depositing at int capacity must transfer only the remaining safe points");
        require(storage.withdrawAll(21_863, 0.0F, 100_000) == 75_704,
                "Withdraw all must stop at the player's remaining int XP capacity");
    }

    private static void verifyHighLevelEnchantmentColors() {
        require(HighLevelEnchantmentPalette.colorFor(10, 255) == HighLevelEnchantmentPalette.FIXED_BLUE,
                "Over-level enchantments through level ten must use the fixed blue");
        require(HighLevelEnchantmentPalette.colorFor(255, 255)
                        != HighLevelEnchantmentPalette.colorFor(10, 255),
                "The maximum enchantment level must finish on a color distinct from blue");
        require(HighLevelEnchantmentPalette.colorFor(300, 300)
                        == HighLevelEnchantmentPalette.colorFor(255, 255),
                "Changing the supported maximum must redistribute the same complete color curve");
    }

    private static void verifyDurationFormatting() {
        require("1:56".equals(MachineScreenUtil.formatDuration(2_334)),
                "Machine and REI durations must floor 116.7 seconds to 1:56");
        require("0:05".equals(MachineScreenUtil.formatDuration(100)),
                "Durations below one minute must retain the mm:ss format");
    }

    private static void verifyOfferLifecycle() {
        require(
                AutotraderOfferLifecycle.decide(true, true, false)
                        == AutotraderOfferLifecycle.Decision.INITIALIZE,
                "An employed adult without offers must initialize them"
        );
        require(
                AutotraderOfferLifecycle.decide(true, true, true)
                        == AutotraderOfferLifecycle.Decision.KEEP,
                "Existing offers must be preserved"
        );
        require(
                AutotraderOfferLifecycle.decide(true, false, false)
                        == AutotraderOfferLifecycle.Decision.UNAVAILABLE,
                "An unemployed villager must remain without offers"
        );
        require(
                AutotraderOfferLifecycle.decide(false, true, false)
                        == AutotraderOfferLifecycle.Decision.UNAVAILABLE,
                "A baby villager must remain without offers"
        );
        require(
                VillagerOfferPersistence.refreshIntervalTicks(20) == 12_000,
                "Offer persistence must enforce the ten minute minimum"
        );
    }

    private static void verifyOfferSelection() {
        require(AutotraderOfferSelection.normalize(3, 1) == 0,
                "One offer must always select index zero");
        require(AutotraderOfferSelection.normalize(3, 4) == 3,
                "Four offers must preserve their last valid index");
        require(AutotraderOfferSelection.normalize(5, 6) == 5,
                "Six offers must remain selectable");
        require(AutotraderOfferSelection.normalize(10, 6) == 4,
                "Selection must wrap lists larger than four");
    }

    private static void verifyTemporaryTradeDiscounts() {
        List<TradeDiscountPolicy.ActiveDiscount<String>> initial = List.of(
                new TradeDiscountPolicy.ActiveDiscount<>("emerald-book", 110L),
                new TradeDiscountPolicy.ActiveDiscount<>("emerald-bread", 220L)
        );
        List<TradeDiscountPolicy.ActiveDiscount<String>> active =
                TradeDiscountPolicy.active(initial, 120L);
        require(!TradeDiscountPolicy.appliesTo(active, "emerald-book")
                        && TradeDiscountPolicy.appliesTo(active, "emerald-bread"),
                "Each offer discount must expire independently");

        List<TradeDiscountPolicy.ActiveDiscount<String>> renewed =
                TradeDiscountPolicy.renew(active, "emerald-bread", 130L);
        renewed = TradeDiscountPolicy.renew(renewed, "emerald-bread", 140L);
        require(renewed.size() == 1
                        && renewed.getFirst().expiresAt() == TradeDiscountPolicy.renewedExpiry(140L),
                "Repeated purchases must renew one layer instead of stacking magnitude");

        List<TradeDiscountPolicy.ActiveDiscount<String>> reordered = List.of(
                new TradeDiscountPolicy.ActiveDiscount<>("second-offer", 300L),
                renewed.getFirst()
        );
        require(TradeDiscountPolicy.appliesTo(reordered, "emerald-bread"),
                "Discount lookup must follow stable offer identity rather than list index");
        require(TradeDiscountPolicy.nextExpiry(reordered) == 300L,
                "The earliest independent offer expiry must schedule the next price refresh");
    }

    private static void verifyTimedProcesses() {
        TimedProcess.Step paused = TimedProcess.advance(
                7,
                20,
                TimedProcess.Availability.BLOCKED
        );
        require(paused.ticks() == 7 && paused.transition() == TimedProcess.Transition.PAUSED,
                "A blocked output must pause progress");

        TimedProcess.Step completed = IncubationCycle.advance(9, 10, true, true);
        require(completed.transition() == TimedProcess.Transition.COMPLETED,
                "Incubation must complete on the configured server tick");

        TimedProcess.Step reset = IncubationCycle.advance(5, 10, false, true);
        require(reset.ticks() == 0 && reset.transition() == TimedProcess.Transition.RESET,
                "Invalid incubation input must reset progress");
    }

    private static void verifyMachineActivity() {
        MachineActivityController activity = new MachineActivityController();
        require(activity.activity() == MachineActivityController.Activity.INACTIVE,
                "A newly loaded machine must recalculate from an inactive state");
        activity.transition(MachineActivityController.Activity.BLOCKED);
        require(activity.remainsBlocked(), "A settled blocked machine must skip repeated capacity work");
        activity.wake();
        require(!activity.remainsBlocked(), "Inventory changes must wake a blocked machine immediately");
        activity.transition(MachineActivityController.Activity.ACTIVE);
        require(activity.activity() == MachineActivityController.Activity.ACTIVE,
                "A woken machine must return to active processing");
        activity.transition(MachineActivityController.Activity.INACTIVE);
        require(activity.remainsInactive(), "A settled inactive machine must skip repeated input work");
        MachineActivityController.wakeAll();
        require(!activity.remainsInactive(), "Configuration and datapack reloads must wake inactive machines");
    }

    private static void verifyMachineActivityEquivalence() {
        MachineActivityController activity = new MachineActivityController();
        int baselineTicks = 0;
        int optimizedTicks = 0;
        boolean hasInputs = false;
        boolean outputAvailable = true;
        for (int tick = 0; tick < 500; tick++) {
            if (tick == 3 || tick == 400) {
                hasInputs = true;
                activity.wake();
            } else if (tick == 80) {
                outputAvailable = false;
                activity.wake();
            } else if (tick == 130) {
                outputAvailable = true;
                activity.wake();
            } else if (tick == 260) {
                hasInputs = false;
                activity.wake();
            }

            // Neither implementation progresses while its chunk is unloaded.
            if (tick >= 200 && tick < 220) {
                require(baselineTicks == optimizedTicks,
                        "Chunk unloading must preserve identical machine progress");
                continue;
            }

            TimedProcess.Step baseline = TimedProcess.advance(
                    baselineTicks,
                    37,
                    TimedProcess.availability(hasInputs, outputAvailable)
            );
            baselineTicks = baseline.ticks();

            if (!activity.remainsInactive() && !activity.remainsBlocked()) {
                TimedProcess.Step optimized = TimedProcess.advance(
                        optimizedTicks,
                        37,
                        TimedProcess.availability(hasInputs, outputAvailable)
                );
                optimizedTicks = optimized.ticks();
                activity.transition(!hasInputs
                        ? MachineActivityController.Activity.INACTIVE
                        : outputAvailable
                                ? MachineActivityController.Activity.ACTIVE
                                : MachineActivityController.Activity.BLOCKED);
            }
            require(baselineTicks == optimizedTicks,
                    "Sleeping inactive and blocked machines must remain tick-equivalent");
        }
    }

    private static void verifyBreederRules() {
        BreederRules rules = new BreederRules(100, 200, 3, 12, 64);
        require(BreederRecipe.breedTicks(BreederKind.PIGLIN, rules) == 200,
                "Piglin duration must come from configuration");
        require(BreederRecipe.cost(BreederKind.PIGLIN, BreederFood.COOKED_PORKCHOP, rules) == 2,
                "Cooked porkchops must cost two");
        require(BreederRecipe.cost(BreederKind.PIGLIN, BreederFood.NETHER_WART_BLOCK, rules) == 2,
                "Nether wart blocks must cost two");
        require(BreederRecipe.cost(BreederKind.PIGLIN, BreederFood.RAW_PORKCHOP, rules) == 4,
                "Raw porkchops must cost four");
        require(BreederRecipe.cost(BreederKind.PIGLIN, BreederFood.CRIMSON_FUNGUS, rules) == 6,
                "Crimson fungi must cost six");
        require(BreederRecipe.cost(BreederKind.PIGLIN, BreederFood.NETHER_WART, rules) == 12,
                "Nether wart must cost twelve");
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
                        && farmerYield(baseFungus, FarmerProduct.NETHER_WART_BLOCK).isGuaranteed(),
                "A crimson fungus cycle must always produce two base wart blocks");
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
        require(farmerYield(fortuneThree, FarmerProduct.WARPED_WART_BLOCK).count() == 5,
                "Fortune III must increase guaranteed wart block yield");
        FarmerHarvest fortuneSeven = FarmerCycle.harvest(FarmerCrop.WARPED_FUNGUS, 7);
        require(farmerYield(fortuneSeven, FarmerProduct.WARPED_STEM).count() == 11
                        && farmerYield(fortuneSeven, FarmerProduct.WARPED_WART_BLOCK).count() == 9,
                "Fortune VII must continue increasing guaranteed fungus harvests");
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
        require(farmerYield(FarmerCycle.harvest(FarmerCrop.MELON, 3), FarmerProduct.MELON).count() == 4,
                "Fortune III must increase melon-block output");
        require(farmerYield(FarmerCycle.harvest(FarmerCrop.SUGAR_CANE, 0), FarmerProduct.SUGAR_CANE).count() == 2,
                "Sugar cane must produce two items before Fortune");
        require(farmerYield(FarmerCycle.harvest(FarmerCrop.COCOA, 3), FarmerProduct.COCOA_BEANS).count() == 6,
                "Fortune III must increase cocoa-bean output");
    }

    private static FarmerYield farmerYield(FarmerHarvest harvest, FarmerProduct product) {
        return harvest.yields().stream()
                .filter(yield -> yield.product() == product)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing farmer product " + product));
    }

    private static void verifyConverterRules() {
        ConverterCycle.Step started = ConverterCycle.advance(
                ConverterStage.IDLE,
                0,
                true,
                true,
                false,
                1,
                1
        );
        require(started.transition() == ConverterCycle.Transition.STARTED,
                "A valid converter must start");

        ConverterCycle.Step infected = ConverterCycle.advance(
                started.stage(),
                started.ticks(),
                true,
                false,
                false,
                1,
                1
        );
        require(infected.transition() == ConverterCycle.Transition.INFECTED,
                "Infection must transition into curing");

        ConverterCycle.Step cured = ConverterCycle.advance(
                infected.stage(),
                infected.ticks(),
                true,
                false,
                false,
                1,
                1
        );
        require(cured.transition() == ConverterCycle.Transition.CURED,
                "Curing must return the converter to idle");
    }

    private static void verifyIronFarmRules() {
        IronFarmCycle cycle = new IronFarmCycle(
                1_200,
                IronFarmCycle.BASE_ONE_VILLAGER_MULTIPLIER,
                IronFarmCycle.BASE_TWO_VILLAGER_MULTIPLIER,
                IronFarmCycle.BASE_THREE_VILLAGER_MULTIPLIER,
                80,
                16,
                5
        );
        require(cycle.multiplier(1) == 1, "One villager must use the x1 base multiplier");
        require(cycle.multiplier(3) == 3, "Three villagers must use the x3 base multiplier");
        require(cycle.multiplier(1, 1) == 2, "One Nitwit must produce at x2");
        require(cycle.multiplier(3, 3) == 6, "Three Nitwits must produce at x6");
        require(cycle.isGolemVisible(1_120), "The golem must appear during its attack window");

        int bonus = 15;
        IronFarmCycle configuredCycle = new IronFarmCycle(
                1_200,
                IronFarmCycle.BASE_ONE_VILLAGER_MULTIPLIER + bonus,
                IronFarmCycle.BASE_TWO_VILLAGER_MULTIPLIER + bonus,
                IronFarmCycle.BASE_THREE_VILLAGER_MULTIPLIER + bonus,
                80,
                16,
                5
        );
        require(configuredCycle.multiplier(1) == 16 && configuredCycle.multiplier(3) == 18,
                "The configured iron-farm value must be added to every base multiplier");
    }

    private static void verifySkeletonFarmRules() {
        require(SkeletonFarmMenu.WIDTH == 348 && SkeletonFarmMenu.HEIGHT == 210,
                "The Skeleton Farm must use the Trader's 348x210 menu geometry");
        require(SkeletonFarmBlockEntity.OUTPUT_SLOT_COUNT == 18,
                "The Skeleton Farm must expose eighteen output slots");
        require(SkeletonFarmCycle.effectiveCycleTicks(VanillaSwordTier.WOODEN.timingPosition(), 0) == 2_400,
                "A wooden sword without Smite must take 120 seconds");
        require(SkeletonFarmCycle.effectiveCycleTicks(VanillaSwordTier.NETHERITE.timingPosition(), 0) == 400,
                "A netherite sword without Smite must take 20 seconds");
        require(SkeletonFarmCycle.effectiveCycleTicks(VanillaSwordTier.NETHERITE.timingPosition(), 5) == 100,
                "A netherite Smite V sword must take five seconds");
        require(SkeletonFarmCycle.effectiveCycleTicks(VanillaSwordTier.IRON.timingPosition(), 5)
                        == SkeletonFarmCycle.effectiveCycleTicks(VanillaSwordTier.IRON.timingPosition(), 30),
                "Smite above level five must not further reduce cycle time");
        require(SkeletonFarmCycle.simulatedKills(0) == 1
                        && SkeletonFarmCycle.simulatedKills(3) == 4,
                "Sweeping Edge must add one simulated kill per level");
        require(SkeletonFarmKind.WITHER_SKELETON.supports(SkeletonFarmLoot.SKULLS)
                        && SkeletonFarmKind.WITHER_SKELETON.supports(SkeletonFarmLoot.COAL)
                        && !SkeletonFarmKind.WITHER_SKELETON.supports(SkeletonFarmLoot.ARROWS),
                "Wither Skeleton filters must expose skulls and coal instead of arrows");
        require(SkeletonFarmKind.STRAY.supports(SkeletonFarmLoot.ARROWS)
                        && SkeletonFarmKind.BOGGED.supports(SkeletonFarmLoot.ARROWS)
                        && SkeletonFarmKind.PARCHED.supports(SkeletonFarmLoot.ARROWS),
                "Every ranged skeleton variant must expose its arrow filter");
        require(SkeletonFarmCycle.effectiveCycleTicks(100.0D, 5) >= 20,
                "Modded sword tiers must never make a cycle faster than one second");
    }

    private static void verifyCapturerRules() {
        require(CapturerDurability.maximum(10) == 10,
                "The default capturer durability must allow ten releases");
        require(CapturerDurability.maximum(0) == 1,
                "Capturer durability must never become non-positive");
    }

    private static void verifyQuarryRules() {
        require(QuarryBlockEntity.OUTPUT_SLOT_COUNT == 18,
                "Both quarry variants must always expose eighteen output slots");
        require(QuarryFortune.boostSelectionWeight(100, 0) == 100,
                "A quarry without Fortune must keep the base material weight");
        require(QuarryFortune.boostSelectionWeight(100, 3) == 220,
                "Fortune III must boost ore selection independently of Silk Touch");
        require(QuarryCycle.durationTicks(VanillaPickaxeTier.WOODEN.timingPosition(), 0) == 2_400,
                "A wooden pickaxe without Efficiency must take exactly 120 seconds");
        require(QuarryCycle.durationTicks(VanillaPickaxeTier.NETHERITE.timingPosition(), 0) == 400,
                "A netherite pickaxe without Efficiency must take exactly 20 seconds");
        require(QuarryCycle.durationTicks(VanillaPickaxeTier.NETHERITE.timingPosition(), 5) == 100,
                "A netherite Efficiency V pickaxe must take exactly five seconds");
        require(QuarryCycle.durationSeconds(2.5D, 0) == 56
                        && QuarryCycle.durationTicks(2.5D, 0) == 1_120,
                "Fractional quarry seconds must be rounded down before conversion to ticks");
        require(QuarryCycle.durationTicks(VanillaPickaxeTier.WOODEN.timingPosition(), 0)
                        > QuarryCycle.durationTicks(VanillaPickaxeTier.GOLDEN.timingPosition(), 0)
                        && QuarryCycle.durationTicks(VanillaPickaxeTier.GOLDEN.timingPosition(), 0)
                        > QuarryCycle.durationTicks(VanillaPickaxeTier.STONE.timingPosition(), 0),
                "The golden pickaxe must keep its low harvest tier despite its high mining speed");
        require(QuarryCycle.durationTicks(VanillaPickaxeTier.IRON.timingPosition(), 5)
                        == QuarryCycle.durationTicks(VanillaPickaxeTier.IRON.timingPosition(), 7),
                "Quarry Efficiency levels above five must not reduce duration further");
        require(QuarryCycle.durationTicks(7.0D, 0) < QuarryCycle.durationTicks(6.0D, 0),
                "A tier above netherite must still reduce the quarry duration");
        require(QuarryCycle.durationTicks(6.0D, 0) - QuarryCycle.durationTicks(7.0D, 0)
                        > QuarryCycle.durationTicks(7.0D, 0) - QuarryCycle.durationTicks(8.0D, 0)
                        && QuarryCycle.durationTicks(7.0D, 0) - QuarryCycle.durationTicks(8.0D, 0)
                        > QuarryCycle.durationTicks(8.0D, 0) - QuarryCycle.durationTicks(9.0D, 0),
                "Tool tiers above netherite must provide diminishing time reductions");
        require(QuarryCycle.durationTicks(100.0D, 5) >= QuarryCycle.MINIMUM_DURATION_TICKS
                        && QuarryCycle.durationTicks(Double.MAX_VALUE, 5) >= QuarryCycle.MINIMUM_DURATION_TICKS,
                "The modded-pickaxe curve must never become faster than one second");
        for (double tierPosition : new double[] {0.0D, 2.5D, 6.0D, 7.0D, 100.0D}) {
            for (int efficiency = 0; efficiency <= 7; efficiency++) {
                require(QuarryCycle.durationTicks(tierPosition, efficiency) % 20 == 0,
                        "Every quarry duration must contain a whole number of seconds");
            }
        }
        require(QuarryCycle.rescaleProgress(100, 200, 100) == 50,
                "A saved cycle must preserve proportional progress when duration rules change");
        require(QuarryUpgradeTier.DIAMOND.supportsDeepMining()
                        && QuarryUpgradeTier.NETHERITE.supportsDeepMining()
                        && !QuarryUpgradeTier.GOLD.supportsDeepMining(),
                "Deep Mining must only be available with diamond or netherite upgrades");
        require(QuarryCycle.advance(10, 200, false, true).transition()
                        == TimedProcess.Transition.PAUSED,
                "A quarry without valid inputs must pause its progress");
    }

    private static void verifyPiglinBarterRules() {
        PiglinBarterCycle.Step started = PiglinBarterCycle.advance(0, 20, true);
        require(started.ticksRemaining() == 20
                        && started.transition() == PiglinBarterCycle.Transition.STARTED,
                "A valid barter must use the configured duration");
        require(PiglinBarterCycle.advance(1, 20, false).transition()
                        == PiglinBarterCycle.Transition.COMPLETED,
                "A barter must complete when its countdown reaches zero");
        require(PiglinBarterUpgradeYield.upgradedAmount(10, 64, 4) == 15,
                "The diamond piglin-barter upgrade must apply a x1.5 yield multiplier");
        require(PiglinBarterUpgradeYield.upgradedAmount(10, 64, 5) == 20,
                "The netherite piglin-barter upgrade must apply a x2 yield multiplier");
    }

    private static void verifyCapturedEntityGuiCenter() {
        double adultCenter = CapturedEntityGuiTransform.effectiveCenterX(0.38F);
        require(close(adultCenter, CapturedEntityGuiTransform.effectiveCenterX(0.48F)),
                "Villager adults and babies must share the GUI X center");
        require(close(adultCenter, CapturedEntityGuiTransform.effectiveCenterX(0.55F)),
                "Piglin adults and babies must share the GUI X center");
    }

    private static void verifyVillagerTradeGeometry() {
        require(VillagerTradeMenuLayout.WIDTH == 348 && VillagerTradeMenuLayout.HEIGHT == 210,
                "Villager trade menus must keep their 348x210 visible bounds");
        require(SlotRenderer.FRAME_SIZE == 18 && SlotRenderer.ITEM_SIZE == 16,
                "Trade slots must use an 18x18 frame around a 16x16 item area");
        require(
                VillagerTradeMenuLayout.itemX(VillagerTradeMenuLayout.MANUAL_PAYMENT_A_X)
                        == VillagerTradeMenuLayout.MANUAL_PAYMENT_A_X + 1,
                "Menu item coordinates must match the visual slot inset"
        );
        require(
                VillagerTradeMenuLayout.EQUIPMENT_OFFHAND_Y + SlotRenderer.FRAME_SIZE
                        <= VillagerTradeMenuLayout.INVENTORY_PANEL_Y
                        + VillagerTradeMenuLayout.INVENTORY_PANEL_HEIGHT,
                "Five equipment slots must remain inside the inventory panel"
        );
        require(
                VillagerTradeScreenLayout.AUTOTRADER_DROPDOWN_Y
                        + VillagerTradeScreenLayout.AUTOTRADER_DROPDOWN_HEIGHT + 4
                        <= VillagerTradeMenuLayout.TRADES_CONTENT_Y
                        + VillagerTradeMenuLayout.TRADES_CONTENT_HEIGHT,
                "Autotrader dropdown must remain inside the menu"
        );
        require(
                VillagerTradeScreenLayout.MANUAL_SCROLL_X
                        >= VillagerTradeScreenLayout.MANUAL_ROW_X
                        + VillagerTradeScreenLayout.MANUAL_ROW_WIDTH + 2,
                "Trader scrollbar must keep two pixels of separation from offer rows"
        );
        require(
                VillagerTradeMenuLayout.AUTOTRADER_OUTPUT_Y + SlotRenderer.FRAME_SIZE
                        <= VillagerTradeMenuLayout.AUTOTRADER_OUTPUT_PANEL_Y
                        + VillagerTradeMenuLayout.AUTOTRADER_PANEL_HEIGHT,
                "Autotrader output slots must remain inside their visual panel"
        );
        require(
                VillagerTradeScreenLayout.DROPDOWN_VISIBLE_ROWS == 8
                        && VillagerTradeScreenLayout.dropdownContentHeight(1)
                        == VillagerTradeScreenLayout.DROPDOWN_ROW_HEIGHT
                        && VillagerTradeScreenLayout.dropdownContentHeight(2)
                        == VillagerTradeScreenLayout.DROPDOWN_ROW_HEIGHT * 2
                        && VillagerTradeScreenLayout.dropdownContentHeight(8)
                        == VillagerTradeScreenLayout.AUTOTRADER_DROPDOWN_HEIGHT
                        && VillagerTradeScreenLayout.dropdownContentHeight(9)
                        == VillagerTradeScreenLayout.AUTOTRADER_DROPDOWN_HEIGHT,
                "Autotrader dropdown height must follow its rows up to the eight-row maximum"
        );
    }

    private static boolean close(double left, double right) {
        return Math.abs(left - right) < 1.0E-12D;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
