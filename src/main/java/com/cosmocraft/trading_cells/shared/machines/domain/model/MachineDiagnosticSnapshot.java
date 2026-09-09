package com.cosmocraft.trading_cells.shared.machines.domain.model;

/** Immutable, transport-neutral diagnostic values for one machine. */
public record MachineDiagnosticSnapshot(
        MachineDiagnosticStatus status,
        String reason,
        int progress,
        int progressMaximum,
        int storedExperience,
        int outputUsed,
        int outputCapacity
) {
    public static final String NONE = "none";

    public MachineDiagnosticSnapshot {
        if (status == null) {
            throw new IllegalArgumentException("status is required");
        }
        reason = reason == null || reason.isBlank() ? NONE : reason;
        progressMaximum = Math.max(0, progressMaximum);
        progress = Math.clamp(progress, 0, progressMaximum);
        storedExperience = Math.max(0, storedExperience);
        outputCapacity = Math.max(0, outputCapacity);
        outputUsed = Math.clamp(outputUsed, 0, outputCapacity);
    }

    public static MachineDiagnosticSnapshot inactive() {
        return new MachineDiagnosticSnapshot(
                MachineDiagnosticStatus.INACTIVE,
                NONE,
                0,
                0,
                0,
                0,
                0
        );
    }

    public boolean outputFull() {
        return outputCapacity > 0 && outputUsed >= outputCapacity;
    }
}
