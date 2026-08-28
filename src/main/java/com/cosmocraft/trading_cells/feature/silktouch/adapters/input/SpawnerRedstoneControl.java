package com.cosmocraft.trading_cells.feature.silktouch.adapters.input;

/** Persistent opt-in redstone control attached to vanilla spawners. */
public interface SpawnerRedstoneControl {
    String PERSISTENCE_TAG = "trading_cells_redstone_control";

    boolean tradingCells$isRedstoneControlInstalled();

    void tradingCells$setRedstoneControlInstalled(boolean installed);
}
