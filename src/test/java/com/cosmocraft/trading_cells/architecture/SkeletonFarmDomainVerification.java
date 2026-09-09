package com.cosmocraft.trading_cells.architecture;

import com.cosmocraft.trading_cells.feature.skeletonfarm.adapters.input.SkeletonFarmBlockEntity;
import com.cosmocraft.trading_cells.feature.skeletonfarm.adapters.input.SkeletonFarmMenu;
import com.cosmocraft.trading_cells.feature.skeletonfarm.adapters.input.SkeletonFarmMenuLayout;
import com.cosmocraft.trading_cells.feature.combat.domain.model.DecapitationRules;
import com.cosmocraft.trading_cells.feature.skeletonfarm.domain.model.SkeletonFarmDropRules;
import com.cosmocraft.trading_cells.feature.skeletonfarm.domain.model.SkeletonFarmCycle;
import com.cosmocraft.trading_cells.feature.skeletonfarm.domain.model.SkeletonFarmKind;
import com.cosmocraft.trading_cells.feature.skeletonfarm.domain.model.SkeletonFarmLoot;
import com.cosmocraft.trading_cells.feature.combat.domain.model.StormShardDropRules;
import com.cosmocraft.trading_cells.shared.mobfarm.domain.model.VanillaSwordTier;

final class SkeletonFarmDomainVerification {
    private SkeletonFarmDomainVerification() {
    }

    static void verify() {
        verifySkeletonFarmRules();
    }

    private static void verifySkeletonFarmRules() {
        require(SkeletonFarmMenu.WIDTH == 348 && SkeletonFarmMenu.HEIGHT == 210,
                "The Skeleton Farm must use the Trader's 348x210 menu geometry");
        int inventoryGroupLeft = SkeletonFarmMenuLayout.EQUIPMENT_X;
        int inventoryGroupRight = SkeletonFarmMenuLayout.PLAYER_INVENTORY_X + 9 * 18;
        int inventoryPanelLeft = 123;
        int inventoryPanelRight = 343;
        require(inventoryGroupLeft - inventoryPanelLeft == inventoryPanelRight - inventoryGroupRight,
                "The Skeleton Farm inventory and equipment slots must be horizontally centered");
        require(SkeletonFarmBlockEntity.OUTPUT_SLOT_COUNT == 18,
                "The Skeleton Farm must expose eighteen output slots");
        require(SkeletonFarmCycle.effectiveCycleTicks(VanillaSwordTier.WOODEN.timingPosition(), 0) == 2_400,
                "A wooden sword without Smite must take 120 seconds");
        require(SkeletonFarmCycle.effectiveCycleTicks(VanillaSwordTier.NETHERITE.timingPosition(), 0) == 400,
                "A netherite sword without Smite must take 20 seconds");
        require(SkeletonFarmCycle.effectiveCycleTicks(VanillaSwordTier.NETHERITE.timingPosition(), 5) == 100,
                "A netherite Smite V sword must take five seconds");
        int sharpnessFiveEquivalent = SkeletonFarmCycle.effectiveCycleTicks(
                VanillaSwordTier.NETHERITE.timingPosition(),
                1.2D
        );
        require(sharpnessFiveEquivalent < 400 && sharpnessFiveEquivalent > 100,
                "Sharpness V must improve speed without matching Smite V against undead targets");
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
        require(!SkeletonFarmCycle.hasEnabledLoot(0, SkeletonFarmKind.SKELETON),
                "An empty Skeleton Farm filter must be recognized as an XP-only cycle");
        require(SkeletonFarmKind.SKELETON.supports(SkeletonFarmLoot.SKULLS),
                "Regular Skeletons must expose their Decapitation head filter");
        require(!SkeletonFarmKind.SKELETON.availableLoot(false).contains(SkeletonFarmLoot.SKULLS)
                        && SkeletonFarmKind.SKELETON.availableLoot(true).contains(SkeletonFarmLoot.SKULLS)
                        && SkeletonFarmKind.WITHER_SKELETON.availableLoot(false).contains(SkeletonFarmLoot.SKULLS),
                "Head filters must require Decapitation except for native Wither Skeleton skulls");
        require(SkeletonFarmCycle.hasEnabledLoot(SkeletonFarmLoot.SKULLS.bit(), SkeletonFarmKind.WITHER_SKELETON),
                "Supported selected loot must activate the Skeleton Farm");
        verifySkeletonFarmPreview(
                SkeletonFarmKind.SKELETON,
                SkeletonFarmLoot.WEAPONS,
                85_000,
                1,
                1
        );
        verifySkeletonFarmPreview(
                SkeletonFarmKind.SKELETON,
                SkeletonFarmLoot.BONES,
                666_667,
                1,
                2
        );
        verifySkeletonFarmPreview(
                SkeletonFarmKind.SKELETON,
                SkeletonFarmLoot.ARROWS,
                666_667,
                1,
                2
        );
        verifySkeletonFarmPreview(
                SkeletonFarmKind.SKELETON,
                SkeletonFarmLoot.SKULLS,
                35_000,
                1,
                1
        );
        verifySkeletonFarmPreview(
                SkeletonFarmKind.WITHER_SKELETON,
                SkeletonFarmLoot.SKULLS,
                25_000,
                1,
                1
        );
        verifySkeletonFarmPreview(
                SkeletonFarmKind.WITHER_SKELETON,
                SkeletonFarmLoot.COAL,
                500_000,
                1,
                1
        );
        SkeletonFarmDropRules.BaseDrop lootingCycle = SkeletonFarmDropRules.cycleDrop(
                SkeletonFarmLoot.BONES,
                3,
                4
        );
        require(lootingCycle.probabilityPartsPerMillion() == 999_228
                        && lootingCycle.minimumAmount() == 1
                        && lootingCycle.maximumAmount() == 20,
                "Skeleton Farm help must combine Looting III and four simulated kills without changing generation");
        SkeletonFarmDropRules.BaseDrop lootingThreeWitherSkull = SkeletonFarmDropRules.headCycleDrop(
                SkeletonFarmKind.WITHER_SKELETON,
                3,
                4,
                0
        );
        require(lootingThreeWitherSkull.probabilityPartsPerMillion() == 202_506,
                "Skeleton Farm help must show the accumulated 20.25 percent Wither skull chance for four Looting III kills");
        require(DecapitationRules.vanillaHeadChance(0) == 0.025D
                        && DecapitationRules.vanillaHeadChance(3) == 0.055D,
                "Wither Skeleton skulls must retain the vanilla Looting curve");
        require(Math.abs(DecapitationRules.decapitationHeadChance(1) - 0.035D) < 1.0E-12D
                        && Math.abs(DecapitationRules.decapitationHeadChance(6) - 0.085D) < 1.0E-12D
                        && Math.abs(DecapitationRules.decapitationHeadChance(30) - 0.325D) < 1.0E-12D
                        && DecapitationRules.decapitationHeadChance(255) == 1.0D,
                "Decapitation must preserve command levels while normal acquisition stops at level six");
        require(DecapitationRules.farmHeadChance(30, false, 0) == 0.0D
                        && Math.abs(DecapitationRules.farmHeadChance(0, false, 3) - 0.055D) < 1.0E-12D
                        && Math.abs(DecapitationRules.farmHeadChance(30, false, 3) - 0.055D) < 1.0E-12D
                        && DecapitationRules.farmHeadChance(0, false, 255) == 1.0D,
                "Non-native heads must depend only on Decapitation, never Looting");
        require(Math.abs(DecapitationRules.farmHeadChance(3, true, 6) - 0.115D) < 1.0E-12D,
                "Wither skull chance must add Looting III and Decapitation VI once");
        double vanillaFailure = 1.0D - DecapitationRules.vanillaHeadChance(3);
        double combinedChance = 1.0D - vanillaFailure
                * (1.0D - DecapitationRules.supplementalNativeHeadChance(3, 6));
        require(Math.abs(combinedChance - 0.115D) < 1.0E-12D,
                "The supplemental world roll must reach 11.5 percent without duplicate heads");
        require(StormShardDropRules.maximumAmount(0) == 1
                        && StormShardDropRules.maximumAmount(3) == 4,
                "Charged Creepers must drop one Storm Shard plus up to one bonus per Looting level");
        require(SkeletonFarmCycle.effectiveCycleTicks(100.0D, 5) >= 20,
                "Modded sword tiers must never make a cycle faster than one second");
    }

    private static void verifySkeletonFarmPreview(
            SkeletonFarmKind kind,
            SkeletonFarmLoot loot,
            int probabilityPartsPerMillion,
            int minimumAmount,
            int maximumAmount
    ) {
        require(kind.supports(loot), "Skeleton Farm preview references an unavailable filter for " + kind + '/' + loot);
        SkeletonFarmDropRules.BaseDrop preview = loot == SkeletonFarmLoot.SKULLS
                ? SkeletonFarmDropRules.headCycleDrop(
                        kind,
                        0,
                        1,
                        kind == SkeletonFarmKind.WITHER_SKELETON ? 0 : 1
                )
                : SkeletonFarmDropRules.baseDrop(loot);
        require(preview.probabilityPartsPerMillion() == probabilityPartsPerMillion,
                "Skeleton Farm REI probability changed for " + kind + '/' + loot);
        require(preview.minimumAmount() == minimumAmount && preview.maximumAmount() == maximumAmount,
                "Skeleton Farm REI amount changed for " + kind + '/' + loot);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
