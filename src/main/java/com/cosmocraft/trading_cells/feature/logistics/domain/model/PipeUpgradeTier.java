package com.cosmocraft.trading_cells.feature.logistics.domain.model;

import java.util.Locale;

public enum PipeUpgradeTier {
    BARE(0, 4L, 20, 50L, 256L),
    BASIC(1, 8L, 15, 100L, 1_024L),
    IMPROVED(2, 16L, 10, 500L, 8_192L),
    ADVANCED(3, 32L, 5, 2_000L, 32_768L),
    ULTIMATE(4, 64L, 1, 10_000L, 131_072L),
    INFINITE(5, 512L, 1, 100_000L, 1_048_576L);

    private final int id;
    private final long itemRate;
    private final int itemInterval;
    private final long fluidRate;
    private final long energyRate;

    PipeUpgradeTier(int id, long itemRate, int itemInterval, long fluidRate, long energyRate) {
        this.id = id;
        this.itemRate = itemRate;
        this.itemInterval = itemInterval;
        this.fluidRate = fluidRate;
        this.energyRate = energyRate;
    }

    public int id() {
        return id;
    }

    public boolean allowsRouting() { return id >= BASIC.id; }
    public boolean allowsFilters() { return id >= IMPROVED.id; }
    public boolean allowsChannels() { return id >= ADVANCED.id; }
    public boolean allowsAdvancedRules() { return id >= ULTIMATE.id; }

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public int interval(LogisticsResourceType type) {
        return type == LogisticsResourceType.ITEM ? itemInterval : 1;
    }

    public long rate(LogisticsResourceType type) {
        return switch (type) {
            case ITEM -> itemRate;
            case FLUID, GAS -> fluidRate;
            case ENERGY -> energyRate;
        };
    }

    public static PipeUpgradeTier fromId(int id) {
        for (PipeUpgradeTier tier : values()) {
            if (tier.id == id) {
                return tier;
            }
        }
        return BARE;
    }
}
