package com.cosmocraft.trading_cells.platform.neoforge.config;

import com.cosmocraft.trading_cells.feature.breeders.application.port.output.BreederSettingsPort;
import com.cosmocraft.trading_cells.feature.converter.application.port.output.ConverterSettingsPort;
import com.cosmocraft.trading_cells.feature.farmer.application.port.output.FarmerSettingsPort;
import com.cosmocraft.trading_cells.feature.incubators.application.port.output.IncubatorSettingsPort;
import com.cosmocraft.trading_cells.feature.ironfarm.application.port.output.IronFarmSettingsPort;
import com.cosmocraft.trading_cells.feature.trader.application.port.output.TraderSettingsPort;

public interface FeatureSettings extends
        BreederSettingsPort,
        ConverterSettingsPort,
        FarmerSettingsPort,
        IncubatorSettingsPort,
        IronFarmSettingsPort,
        TraderSettingsPort {
}
