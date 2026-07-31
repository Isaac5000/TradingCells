package com.cosmocraft.trading_cells.feature.breeders.application.port.output;

public interface BreederSettingsPort {
    int villagerBreederTicks();

    int piglinBreederTicks();

    int villagerBreadCost();

    int villagerVegetableCost();

    int maximumPendingBabies();
}
