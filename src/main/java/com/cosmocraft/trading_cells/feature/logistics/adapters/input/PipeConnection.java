package com.cosmocraft.trading_cells.feature.logistics.adapters.input;

import net.minecraft.util.StringRepresentable;
import org.jspecify.annotations.NonNull;

public enum PipeConnection implements StringRepresentable {
    NONE("none"),
    PIPE("pipe"),
    INSERT("insert"),
    EXTRACT("extract");

    private final String serializedName;

    PipeConnection(String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public @NonNull String getSerializedName() {
        return serializedName;
    }
}
