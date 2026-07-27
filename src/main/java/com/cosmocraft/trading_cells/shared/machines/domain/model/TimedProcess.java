package com.cosmocraft.trading_cells.shared.machines.domain.model;

/** Pure state transition shared by machines that advance once per server tick. */
public final class TimedProcess {
    private TimedProcess() {
    }

    public static Step advance(
            int currentTicks,
            int durationTicks,
            Availability availability
    ) {
        int duration = Math.max(1, durationTicks);
        int current = Math.clamp(currentTicks, 0, duration);
        if (availability == Availability.INACTIVE) {
            return new Step(0, current == 0 ? Transition.IDLE : Transition.RESET);
        }
        if (availability == Availability.BLOCKED) {
            return new Step(current, Transition.PAUSED);
        }

        int next = current + 1;
        if (next >= duration) {
            return new Step(0, Transition.COMPLETED);
        }
        return new Step(next, Transition.ADVANCED);
    }

    public static Availability availability(boolean enabled, boolean outputAvailable) {
        if (!enabled) {
            return Availability.INACTIVE;
        }
        return outputAvailable ? Availability.ACTIVE : Availability.BLOCKED;
    }

    public enum Availability {
        INACTIVE,
        BLOCKED,
        ACTIVE
    }

    public enum Transition {
        IDLE,
        RESET,
        PAUSED,
        ADVANCED,
        COMPLETED
    }

    public record Step(int ticks, Transition transition) {
        public boolean changedFrom(int previousTicks) {
            return ticks != previousTicks;
        }
    }
}
