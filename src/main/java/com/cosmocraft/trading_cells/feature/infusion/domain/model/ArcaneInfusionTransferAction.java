package com.cosmocraft.trading_cells.feature.infusion.domain.model;

import java.util.Arrays;
import java.util.Optional;

public enum ArcaneInfusionTransferAction {
    DEPOSIT((byte) 0),
    DEPOSIT_ALL((byte) 1),
    WITHDRAW((byte) 2),
    WITHDRAW_ALL((byte) 3);

    private final byte id;

    ArcaneInfusionTransferAction(byte id) {
        this.id = id;
    }

    public byte id() {
        return id;
    }

    public static Optional<ArcaneInfusionTransferAction> fromId(byte id) {
        return Arrays.stream(values()).filter(value -> value.id == id).findFirst();
    }
}
