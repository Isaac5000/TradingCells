package com.cosmocraft.trading_cells.feature.converter.application.port.output;

public interface ConverterSettingsPort {
    int converterInfectionTicks();

    int converterCureTicks();

    int converterCureDiscountPerCycle();

    int converterMaximumCureDiscount();
}
