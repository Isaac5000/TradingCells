package com.cosmocraft.trading_cells.feature.skeletonfarm.domain.model;

import com.cosmocraft.trading_cells.shared.machines.domain.model.TimedProcess;

/** Pure timing, filtering and simulated-kill rules for the Skeleton Farm. */
public final class SkeletonFarmCycle {
    public static final int MAX_EFFECTIVE_SMITE_LEVEL = 5;
    public static final int TICKS_PER_SECOND = 20;
    private static final double WOODEN_BASE_SECONDS = 120.0D;
    private static final double WOODEN_MAX_SMITE_SECONDS = 30.0D;
    private static final double NETHERITE_BASE_SECONDS = 20.0D;
    private static final double NETHERITE_MAX_SMITE_SECONDS = 5.0D;
    private static final double MINIMUM_DURATION_SECONDS = 1.0D;
    private static final double NETHERITE_POSITION = VanillaSwordTier.NETHERITE.timingPosition();
    private static final double TIER_RATIO = Math.pow(
            NETHERITE_BASE_SECONDS / WOODEN_BASE_SECONDS,
            1.0D / NETHERITE_POSITION
    );
    private static final double ROUNDING_EPSILON = 1.0E-9D;

    private SkeletonFarmCycle() {
    }

    public static int effectiveCycleTicks(double tierPosition, int smiteLevel) {
        int smite = Math.clamp(smiteLevel, 0, MAX_EFFECTIVE_SMITE_LEVEL);
        double position = Double.isFinite(tierPosition) ? Math.max(0.0D, tierPosition) : 0.0D;
        double startingDuration = startingDuration(position);
        double maximumSmiteDuration = maximumSmiteDuration(position);
        double progress = smite / (double) MAX_EFFECTIVE_SMITE_LEVEL;
        double startingDistance = Math.max(Double.MIN_NORMAL, startingDuration - MINIMUM_DURATION_SECONDS);
        double endingDistance = Math.max(
                Double.MIN_NORMAL,
                Math.min(startingDistance, maximumSmiteDuration - MINIMUM_DURATION_SECONDS)
        );
        double duration = MINIMUM_DURATION_SECONDS
                + startingDistance * Math.pow(endingDistance / startingDistance, progress);
        int seconds = Math.max(1, (int) Math.floor(duration + ROUNDING_EPSILON));
        return seconds * TICKS_PER_SECOND;
    }

    public static int simulatedKills(int sweepingEdgeLevel) {
        return Math.max(1, 1 + Math.max(0, sweepingEdgeLevel));
    }

    public static boolean isEnabled(int mask, SkeletonFarmKind kind, SkeletonFarmLoot loot) {
        return kind.supports(loot) && (mask & loot.bit()) != 0;
    }

    public static int toggle(int mask, SkeletonFarmLoot loot) {
        return mask ^ loot.bit();
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
            boolean canHunt,
            boolean outputAvailable
    ) {
        return TimedProcess.advance(
                currentTicks,
                durationTicks,
                TimedProcess.availability(canHunt, outputAvailable)
        );
    }

    private static double startingDuration(double position) {
        if (position <= NETHERITE_POSITION) {
            return WOODEN_BASE_SECONDS * Math.pow(TIER_RATIO, position);
        }
        return MINIMUM_DURATION_SECONDS
                + (NETHERITE_BASE_SECONDS - MINIMUM_DURATION_SECONDS)
                * Math.pow(TIER_RATIO, position - NETHERITE_POSITION);
    }

    private static double maximumSmiteDuration(double position) {
        if (position <= NETHERITE_POSITION) {
            return WOODEN_MAX_SMITE_SECONDS * Math.pow(TIER_RATIO, position);
        }
        return MINIMUM_DURATION_SECONDS
                + (NETHERITE_MAX_SMITE_SECONDS - MINIMUM_DURATION_SECONDS)
                * Math.pow(TIER_RATIO, position - NETHERITE_POSITION);
    }
}
