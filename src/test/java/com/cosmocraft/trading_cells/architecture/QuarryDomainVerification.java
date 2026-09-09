package com.cosmocraft.trading_cells.architecture;

import com.cosmocraft.trading_cells.feature.quarry.adapters.input.QuarryBlockEntity;
import com.cosmocraft.trading_cells.feature.quarry.domain.model.QuarryCycle;
import com.cosmocraft.trading_cells.feature.quarry.domain.model.QuarryFortune;
import com.cosmocraft.trading_cells.feature.quarry.domain.model.QuarryUpgradeTier;
import com.cosmocraft.trading_cells.feature.quarry.domain.model.VanillaPickaxeTier;
import com.cosmocraft.trading_cells.shared.machines.domain.model.TimedProcess;

final class QuarryDomainVerification {
    private QuarryDomainVerification() {
    }

    static void verify() {
        verifyQuarryRules();
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

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
