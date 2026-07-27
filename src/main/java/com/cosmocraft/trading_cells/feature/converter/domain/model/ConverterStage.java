package com.cosmocraft.trading_cells.feature.converter.domain.model;

public enum ConverterStage {
    IDLE,
    INFECTING,
    CURING;

    public int durationTicks(int infectionTicks, int cureTicks) {
        return switch (this) {
            case IDLE -> 0;
            case INFECTING -> Math.max(1, infectionTicks);
            case CURING -> Math.max(1, cureTicks);
        };
    }

    public boolean isProcessing() {
        return this != IDLE;
    }

    public static ConverterStage fromId(int id) {
        ConverterStage[] values = values();
        return values[Math.clamp(id, 0, values.length - 1)];
    }
}
