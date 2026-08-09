package com.cosmocraft.trading_cells.shared.machines.domain.model;

import java.util.concurrent.atomic.AtomicLong;

/** Tracks settled machine states so blocked work is retried only after an inventory wake-up. */
public final class MachineActivityController {
    private static final AtomicLong GLOBAL_WAKE_REVISION = new AtomicLong();
    private Activity activity = Activity.INACTIVE;
    private long wakeRevision;
    private long observedWakeRevision = -1L;
    private long observedGlobalWakeRevision = -1L;

    public Activity activity() {
        return activity;
    }

    public boolean remainsBlocked() {
        return remainsSettled(Activity.BLOCKED);
    }

    public boolean remainsInactive() {
        return remainsSettled(Activity.INACTIVE);
    }

    public boolean transition(Activity next) {
        boolean changed = activity != next;
        activity = next;
        observedWakeRevision = wakeRevision;
        observedGlobalWakeRevision = GLOBAL_WAKE_REVISION.get();
        return changed;
    }

    public void wake() {
        wakeRevision++;
    }

    public void reset() {
        activity = Activity.INACTIVE;
        wakeRevision = 0L;
        observedWakeRevision = -1L;
        observedGlobalWakeRevision = -1L;
    }

    public static void wakeAll() {
        GLOBAL_WAKE_REVISION.incrementAndGet();
    }

    private boolean remainsSettled(Activity expected) {
        return activity == expected
                && observedWakeRevision == wakeRevision
                && observedGlobalWakeRevision == GLOBAL_WAKE_REVISION.get();
    }

    public enum Activity {
        ACTIVE,
        BLOCKED,
        INACTIVE
    }
}
