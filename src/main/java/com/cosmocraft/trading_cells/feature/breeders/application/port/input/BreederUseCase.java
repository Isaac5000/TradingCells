package com.cosmocraft.trading_cells.feature.breeders.application.port.input;

import com.cosmocraft.trading_cells.feature.breeders.domain.model.BreederFood;
import com.cosmocraft.trading_cells.feature.breeders.domain.model.BreederKind;
import com.cosmocraft.trading_cells.shared.machines.domain.model.TimedProcess;

public interface BreederUseCase {
    int durationTicks(BreederKind kind);

    int foodCost(BreederKind kind, BreederFood food);

    int maximumPendingBabies();

    TimedProcess.Step advance(int ticks, BreederKind kind, boolean canGenerateBaby);
}
