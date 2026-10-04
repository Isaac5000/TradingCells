package com.cosmocraft.trading_cells.feature.mobfarm.domain.model;

public enum InteractionSource {
    ITEM(2), TOOL(3);

    private final int slot;
    InteractionSource(int slot) { this.slot = slot; }
    public int slot() { return slot; }
    public InteractionSource other() { return this == ITEM ? TOOL : ITEM; }
}
