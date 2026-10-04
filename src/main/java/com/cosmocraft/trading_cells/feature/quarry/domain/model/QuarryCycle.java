package com.cosmocraft.trading_cells.feature.quarry.domain.model;

import com.cosmocraft.trading_cells.shared.machines.domain.model.TimedProcess;
import com.cosmocraft.trading_cells.shared.machines.domain.model.ToolMachineSpeed;

public final class QuarryCycle {
    public static final int BASE_DURATION_TICKS = 2_400;
    public static final int MINIMUM_DURATION_TICKS = 20;

    private QuarryCycle() {
    }

    public static int durationTicks(double tierPosition, int speedTier) {
        return ToolMachineSpeed.apply(ToolMachineSpeed.baseTicks(tierPosition,
                VanillaPickaxeTier.NETHERITE.timingPosition()), speedTier);
    }

    public static int durationSeconds(double tierPosition, int speedTier) {
        return (durationTicks(tierPosition, speedTier) + 19) / 20;
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
