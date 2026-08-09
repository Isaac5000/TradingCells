package com.cosmocraft.trading_cells.feature.quarry.adapters.input;

public enum QuarryStatus {
    STOPPED("stopped"),
    WORKER_REQUIRED("worker_required"),
    PICKAXE_REQUIRED("pickaxe_required"),
    PICKAXE_BROKEN("pickaxe_broken"),
    NO_COMPATIBLE_MATERIALS("no_compatible_materials"),
    INVENTORY_FULL("inventory_full"),
    MINING("mining");

    private final String translationPath;

    QuarryStatus(String translationPath) {
        this.translationPath = translationPath;
    }

    public String translationKey() {
        return "status.trading_cells.quarry." + translationPath;
    }

    public static QuarryStatus fromIndex(int index) {
        return values()[Math.clamp(index, 0, values().length - 1)];
    }
}
