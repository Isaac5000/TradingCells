package com.cosmocraft.trading_cells.feature.logistics.domain.model;

import java.util.Locale;

public enum PipeSideMode {
    NONE,
    INSERT,
    EXTRACT;

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public PipeSideMode next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public static PipeSideMode fromSerializedName(String name) {
        for (PipeSideMode mode : values()) {
            if (mode.serializedName().equals(name)) {
                return mode;
            }
        }
        return NONE;
    }
}
