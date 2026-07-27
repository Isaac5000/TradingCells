package com.cosmocraft.trading_cells.feature.trader.domain.model;

/** Pure countdown used by the piglin bartering cell. */
public final class PiglinBarterCycle {
    private PiglinBarterCycle() {
    }

    public static Step advance(int ticksRemaining, int durationTicks, boolean canStart) {
        int remaining = Math.max(0, ticksRemaining);
        if (remaining > 1) {
            return new Step(remaining - 1, Transition.ADVANCED);
        }
        if (remaining == 1) {
            return new Step(0, Transition.COMPLETED);
        }
        if (canStart) {
            return new Step(Math.max(1, durationTicks), Transition.STARTED);
        }
        return new Step(0, Transition.IDLE);
    }

    public enum Transition {
        IDLE,
        STARTED,
        ADVANCED,
        COMPLETED
    }

    public record Step(int ticksRemaining, Transition transition) {
    }
}
