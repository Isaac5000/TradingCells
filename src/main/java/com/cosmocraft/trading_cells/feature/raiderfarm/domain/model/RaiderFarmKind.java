package com.cosmocraft.trading_cells.feature.raiderfarm.domain.model;

import java.util.List;

public enum RaiderFarmKind {
    PILLAGER(List.of(RaiderFarmLoot.WEAPONS, RaiderFarmLoot.OMINOUS_BANNER)),
    EVOKER(List.of()),
    RAVAGER(List.of()),
    WITCH(List.of());

    private final List<RaiderFarmLoot> availableLoot;

    RaiderFarmKind(List<RaiderFarmLoot> availableLoot) {
        this.availableLoot = availableLoot;
    }

    public List<RaiderFarmLoot> availableLoot() {
        return availableLoot;
    }

    public List<RaiderFarmLoot> availableLoot(boolean ignoredHasDecapitation) {
        return availableLoot;
    }

    public boolean supports(RaiderFarmLoot loot) {
        return availableLoot.contains(loot);
    }

    public static RaiderFarmKind fromId(int id) {
        if (id <= 1) {
            // Value 1 belonged to the removed ominous Pillager variant.
            return PILLAGER;
        }
        return switch (id) {
            case 2 -> EVOKER;
            case 3 -> RAVAGER;
            default -> WITCH;
        };
    }

    public int persistentId() {
        return switch (this) {
            case PILLAGER -> 0;
            case EVOKER -> 2;
            case RAVAGER -> 3;
            case WITCH -> 4;
        };
    }

    public static List<RaiderFarmKind> selectableValues() {
        return List.of(values());
    }
}
