package com.cosmocraft.trading_cells.platform.neoforge.machine;

import com.cosmocraft.trading_cells.shared.machines.domain.model.MachineDiagnosticSnapshot;

/** Read-only adapter consumed by Jade and the machine controller. */
public interface MachineDiagnosticSource {
    MachineDiagnosticSnapshot machineDiagnosticSnapshot();
}
