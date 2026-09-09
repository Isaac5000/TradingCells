package com.cosmocraft.trading_cells.feature.logistics.domain.model;

import java.util.Locale;

public enum LogisticsResourceType {
    ITEM,
    FLUID,
    GAS,
    ENERGY;

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static LogisticsResourceType fromSerializedName(String name) {
        for (LogisticsResourceType type : values()) {
            if (type.serializedName().equals(name)) {
                return type;
            }
        }
        return ITEM;
    }
}
