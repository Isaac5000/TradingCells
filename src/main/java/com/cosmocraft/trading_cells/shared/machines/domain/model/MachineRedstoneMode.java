package com.cosmocraft.trading_cells.shared.machines.domain.model;

/** Optional pause policy for automatic machines. IDs are persisted, so append only. */
public enum MachineRedstoneMode {
    IGNORE("ignore"),
    HIGH_SIGNAL_PAUSES("high_signal_pauses"),
    LOW_SIGNAL_PAUSES("low_signal_pauses");

    private final String serializedName;

    MachineRedstoneMode(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }

    public boolean pauses(boolean powered) {
        return switch (this) {
            case IGNORE -> false;
            case HIGH_SIGNAL_PAUSES -> powered;
            case LOW_SIGNAL_PAUSES -> !powered;
        };
    }

    public MachineRedstoneMode next() {
        MachineRedstoneMode[] modes = values();
        return modes[(ordinal() + 1) % modes.length];
    }

    public static MachineRedstoneMode fromSerializedName(String name) {
        for (MachineRedstoneMode mode : values()) {
            if (mode.serializedName.equals(name)) {
                return mode;
            }
        }
        return IGNORE;
    }
}
