package com.cosmocraft.trading_cells.feature.quarry.domain.model;

import com.cosmocraft.trading_cells.shared.machines.domain.model.TimedProcess;

public final class QuarryCycle {
    public static final int BASE_DURATION_TICKS = 2_400;
    public static final int MINIMUM_DURATION_TICKS = 20;
    public static final int MAX_EFFECTIVE_EFFICIENCY_LEVEL = 5;

    private static final int TICKS_PER_SECOND = 20;
    private static final double WOODEN_BASE_SECONDS = 120.0D;
    private static final double WOODEN_MAX_EFFICIENCY_SECONDS = 30.0D;
    private static final double NETHERITE_BASE_SECONDS = 20.0D;
    private static final double NETHERITE_MAX_EFFICIENCY_SECONDS = 5.0D;
    private static final double MINIMUM_DURATION_SECONDS = 1.0D;
    private static final double NETHERITE_POSITION = VanillaPickaxeTier.NETHERITE.timingPosition();
    private static final double TIER_RATIO = Math.pow(
            NETHERITE_BASE_SECONDS / WOODEN_BASE_SECONDS,
            1.0D / NETHERITE_POSITION
    );
    private static final double ROUNDING_EPSILON = 1.0E-9D;

    private QuarryCycle() {
    }

    public static int durationTicks(double tierPosition, int efficiencyLevel) {
        return durationSeconds(tierPosition, efficiencyLevel) * TICKS_PER_SECOND;
    }

    public static int durationSeconds(double tierPosition, int efficiencyLevel) {
        int efficiency = Math.clamp(efficiencyLevel, 0, MAX_EFFECTIVE_EFFICIENCY_LEVEL);
        double position = finitePosition(tierPosition);
        double startingDuration = startingDuration(position);
        double maximumEfficiencyDuration = maximumEfficiencyDuration(position);
        double progress = efficiency / (double) MAX_EFFECTIVE_EFFICIENCY_LEVEL;
        // Geometric interpolation makes every Efficiency level a proportional reduction.
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

    private static double startingDuration(double position) {
        if (position <= NETHERITE_POSITION) {
            return WOODEN_BASE_SECONDS * Math.pow(TIER_RATIO, position);
        }
        return MINIMUM_DURATION_SECONDS
                + (NETHERITE_BASE_SECONDS - MINIMUM_DURATION_SECONDS)
                * Math.pow(TIER_RATIO, position - NETHERITE_POSITION);
    }

    private static double maximumEfficiencyDuration(double position) {
        if (position <= NETHERITE_POSITION) {
            return WOODEN_MAX_EFFICIENCY_SECONDS * Math.pow(TIER_RATIO, position);
        }
        // Tiers above netherite approach one second while each extra tier saves less time.
        return MINIMUM_DURATION_SECONDS
                + (NETHERITE_MAX_EFFICIENCY_SECONDS - MINIMUM_DURATION_SECONDS)
                * Math.pow(TIER_RATIO, position - NETHERITE_POSITION);
    }

    private static double finitePosition(double tierPosition) {
        return Double.isFinite(tierPosition) ? Math.max(0.0D, tierPosition) : 0.0D;
    }

    public static int rescaleProgress(int ticks, int previousMaximum, int newMaximum) {
        if (ticks <= 0) {
            return 0;
        }
        int previous = Math.max(1, previousMaximum);
        int next = Math.max(1, newMaximum);
        if (next == 1) {
            return 0;
        }
        long scaled = ((long) ticks * next + previous - 1L) / previous;
        return (int) Math.clamp(scaled, 1L, next - 1L);
    }

    public static TimedProcess.Step advance(
            int currentTicks,
            int durationTicks,
            boolean canMine,
            boolean outputAvailable
    ) {
        TimedProcess.Availability availability = canMine && outputAvailable
                ? TimedProcess.Availability.ACTIVE
                : TimedProcess.Availability.BLOCKED;
        return TimedProcess.advance(
                currentTicks,
                durationTicks,
                availability
        );
    }
}
