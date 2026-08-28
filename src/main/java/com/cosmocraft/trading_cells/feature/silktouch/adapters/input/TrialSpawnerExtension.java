package com.cosmocraft.trading_cells.feature.silktouch.adapters.input;

/** Persistent Trading Cells state attached to each vanilla trial spawner. */
public interface TrialSpawnerExtension extends SpawnerRedstoneControl {
    boolean tradingCells$isAwaitingPlayerExit();

    void tradingCells$setAwaitingPlayerExit(boolean awaitingPlayerExit);

    boolean tradingCells$hasCompletedTrial();

    void tradingCells$setCompletedTrial(boolean completedTrial);
}
