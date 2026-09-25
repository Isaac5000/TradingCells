package com.cosmocraft.trading_cells.architecture;

import com.cosmocraft.trading_cells.shared.mobfarm.domain.model.MobFarmCycleRules;

/** Observable timing and filtering contracts shared by every mob-farm implementation. */
final class MobFarmDomainVerification {
    private MobFarmDomainVerification() {
    }

    static void verify() {
        double[] positions = {-1.0D, 0.0D, 1.0D, 3.5D, 6.0D, 7.0D, 100.0D, Double.NaN};
        double[] damageLevels = {-1.0D, 0.0D, 1.2D, 5.0D, 30.0D, Double.NaN};
        for (double position : positions) {
            for (double damage : damageLevels) {
                int expected = legacyCycleTicks(position, damage);
                require(MobFarmCycleRules.effectiveCycleTicks(position, damage) == expected,
                        "Shared mob-farm timing changed the pre-refactor oracle");
            }
        }
        for (int sweeping : new int[]{-1, 0, 1, 3, 30, 255}) {
            int expected = Math.max(1, 1 + Math.max(0, sweeping));
            require(MobFarmCycleRules.simulatedKills(sweeping) == expected,
                    "Shared simulated-kill rules changed their pre-refactor result");
        }
        for (int ticks : new int[]{0, 1, 19, 100, 2_400}) {
            int expected = legacyRescaleProgress(ticks, 2_400, 100);
            require(MobFarmCycleRules.rescaleProgress(ticks, 2_400, 100) == expected,
                    "Shared mob-farm progress rescaling changed its pre-refactor result");
        }
        for (int mask : new int[]{0, 1, 3, 15, 255}) {
            for (int bit = 1; bit <= 16; bit <<= 1) {
                require(MobFarmCycleRules.isFilterEnabled(mask, bit, true) == ((mask & bit) != 0),
                        "Shared filter membership remains bit-based");
                require(!MobFarmCycleRules.isFilterEnabled(mask, bit, false), "Unsupported filter is disabled");
                require(MobFarmCycleRules.toggleFilter(mask, bit) == (mask ^ bit), "Shared filter toggle remains bit-based");
            }
        }
    }

    private static int legacyCycleTicks(double tierPosition, double effectiveDamageLevel) {
        double damage = Double.isFinite(effectiveDamageLevel)
                ? Math.clamp(effectiveDamageLevel, 0.0D, 5.0D)
                : 0.0D;
        double position = Double.isFinite(tierPosition) ? Math.max(0.0D, tierPosition) : 0.0D;
        double ratio = Math.pow(20.0D / 120.0D, 1.0D / 6.0D);
        double starting = position <= 6.0D
                ? 120.0D * Math.pow(ratio, position)
                : 1.0D + 19.0D * Math.pow(ratio, position - 6.0D);
        double maximumDamage = position <= 6.0D
                ? 30.0D * Math.pow(ratio, position)
                : 1.0D + 4.0D * Math.pow(ratio, position - 6.0D);
        double progress = damage / 5.0D;
        double startingDistance = Math.max(Double.MIN_NORMAL, starting - 1.0D);
        double endingDistance = Math.max(Double.MIN_NORMAL, Math.min(startingDistance, maximumDamage - 1.0D));
        double duration = 1.0D
                + startingDistance * Math.pow(endingDistance / startingDistance, progress);
        return Math.max(1, (int) Math.floor(duration + 1.0E-9D)) * 20;
    }

    private static int legacyRescaleProgress(int ticks, int previousMaximum, int newMaximum) {
        if (ticks <= 0) {
            return 0;
        }
        int previous = Math.max(1, previousMaximum);
        int next = Math.max(1, newMaximum);
        long scaled = ((long) ticks * next + previous - 1L) / previous;
        return (int) Math.clamp(scaled, 1L, next);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
