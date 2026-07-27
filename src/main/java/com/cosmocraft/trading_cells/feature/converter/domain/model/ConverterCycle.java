package com.cosmocraft.trading_cells.feature.converter.domain.model;

/** Pure state machine for infection and curing. */
public final class ConverterCycle {
    private ConverterCycle() {
    }

    public static Step advance(
            ConverterStage stage,
            int ticks,
            boolean hasValidVillager,
            boolean canStart,
            boolean curedReady,
            int infectionTicks,
            int cureTicks
    ) {
        if (!hasValidVillager) {
            boolean changed = stage != ConverterStage.IDLE || ticks != 0;
            return new Step(
                    ConverterStage.IDLE,
                    0,
                    changed ? Transition.CANCELLED : Transition.IDLE
            );
        }
        if (stage == ConverterStage.IDLE) {
            if (!curedReady && canStart) {
                return new Step(ConverterStage.INFECTING, 0, Transition.STARTED);
            }
            return new Step(ConverterStage.IDLE, 0, Transition.IDLE);
        }

        int duration = stage.durationTicks(infectionTicks, cureTicks);
        int next = Math.max(0, ticks) + 1;
        if (next < duration) {
            return new Step(stage, next, Transition.ADVANCED);
        }
        if (stage == ConverterStage.INFECTING) {
            return new Step(ConverterStage.CURING, 0, Transition.INFECTED);
        }
        return new Step(ConverterStage.IDLE, 0, Transition.CURED);
    }

    public enum Transition {
        IDLE,
        STARTED,
        ADVANCED,
        INFECTED,
        CURED,
        CANCELLED
    }

    public record Step(
            ConverterStage stage,
            int ticks,
            Transition transition
    ) {
    }
}
