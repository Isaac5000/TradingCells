package com.cosmocraft.trading_cells.feature.logistics.adapters.api;

/** A visible resource and the physical storage it represents, shared across sided views. */
public record LogisticsListingEntry<R>(Object storageKey, LogisticsResourceSnapshot<R> snapshot) {
    public record StorageKey(Object inventory, Object slot) { }
}
