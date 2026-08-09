package com.cosmocraft.trading_cells.feature.experience.domain.model;

import java.util.Optional;

public enum ExperienceTransferAction {
    DEPOSIT((byte) 0),
    DEPOSIT_ALL((byte) 1),
    WITHDRAW((byte) 2),
    WITHDRAW_ALL((byte) 3);

    private final byte id;

    ExperienceTransferAction(byte id) {
        this.id = id;
    }

    public byte id() {
        return id;
    }

    public static Optional<ExperienceTransferAction> fromId(byte id) {
        for (ExperienceTransferAction action : values()) {
            if (action.id == id) {
                return Optional.of(action);
            }
        }
        return Optional.empty();
    }
}
