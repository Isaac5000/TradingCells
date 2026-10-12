package com.cosmocraft.trading_cells.feature.combat.domain.model;

public final class StormChargeRules {
    public static final int CAPACITY = 256;
    public static final int SHARD_CHARGE = 16;
    public static final int TRANSFER_TICKS = 25;

    private StormChargeRules() { }

    public static int clamp(int charge, int capacity) { return Math.clamp(charge, 0, capacity); }
}
