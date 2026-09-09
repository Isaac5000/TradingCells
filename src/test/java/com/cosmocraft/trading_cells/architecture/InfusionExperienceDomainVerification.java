package com.cosmocraft.trading_cells.architecture;

import com.cosmocraft.trading_cells.feature.experience.application.service.ExperienceStorageService;
import com.cosmocraft.trading_cells.feature.experience.domain.model.ExperienceMath;
import com.cosmocraft.trading_cells.feature.infusion.application.service.ArcaneInfusionService;
import com.cosmocraft.trading_cells.feature.infusion.domain.model.ArcaneInfusionAttempt;
import com.cosmocraft.trading_cells.feature.infusion.domain.model.ArcaneInfusionDecision;
import com.cosmocraft.trading_cells.shared.machines.domain.model.MinecraftExperience;
import com.cosmocraft.trading_cells.platform.neoforge.event.HighLevelEnchantmentPalette;

final class InfusionExperienceDomainVerification {
    private InfusionExperienceDomainVerification() {
    }

    static void verify() {
        verifyArcaneInfusion();
        verifyExperienceStorage();
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
        require(ExperienceMath.levelForTotalPointsRoundedUp(160) == 10,
                "An exact level boundary must not be rounded to the following level");
        require(ExperienceMath.levelForTotalPointsRoundedUp(161) == 11,
                "A partial level must round up for the XP conversion display");
        require(ExperienceMath.levelForTotalPointsRoundedUp(Integer.MAX_VALUE) == 21_863,
                "The rounded conversion must remain inside the representable int XP range");
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

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
