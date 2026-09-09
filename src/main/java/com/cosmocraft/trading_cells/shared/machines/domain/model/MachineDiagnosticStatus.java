package com.cosmocraft.trading_cells.shared.machines.domain.model;

/** Stable coarse-grained state shared by machine diagnostics and filters. */
public enum MachineDiagnosticStatus {
    RUNNING,
    INACTIVE,
    PAUSED,
    BLOCKED
}
