package com.cosmocraft.trading_cells.feature.ironfarm.application.port.input;

import com.cosmocraft.trading_cells.feature.ironfarm.domain.model.IronFarmCycle;
import com.cosmocraft.trading_cells.shared.machines.domain.model.TimedProcess;

public interface IronFarmUseCase {
    IronFarmCycle cycle();

    TimedProcess.Step advance(int ticks, int villagerCount, boolean outputAvailable);

    int baseIron();

    int maximumPoppies();
}
