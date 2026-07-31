package com.cosmocraft.trading_cells.feature.breeders.domain.model;

public enum BreederFood {
    BREAD,
    VEGETABLE,
    COOKED_PORKCHOP,
    NETHER_WART_BLOCK,
    RAW_PORKCHOP,
    CRIMSON_FUNGUS,
    NETHER_WART,
    NONE;

    public static BreederFood fromOrdinal(int ordinal) {
        BreederFood[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : NONE;
    }

    public static BreederFood fromName(String name) {
        try {
            return valueOf(name);
        } catch (IllegalArgumentException ignored) {
            return NONE;
        }
    }
}
