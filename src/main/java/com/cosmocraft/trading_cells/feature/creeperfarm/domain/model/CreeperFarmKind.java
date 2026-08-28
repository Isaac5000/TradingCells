package com.cosmocraft.trading_cells.feature.creeperfarm.domain.model;

import java.util.List;

public enum CreeperFarmKind {
    CREEPER(List.of(CreeperFarmLoot.GUNPOWDER, CreeperFarmLoot.HEADS)),
    CHARGED_CREEPER(List.of(
            CreeperFarmLoot.GUNPOWDER,
            CreeperFarmLoot.STORM_SHARDS,
            CreeperFarmLoot.HEADS
    ));

    private final List<CreeperFarmLoot> availableLoot;
    private final List<CreeperFarmLoot> availableLootWithoutDecapitation;

    CreeperFarmKind(List<CreeperFarmLoot> availableLoot) {
        this.availableLoot = availableLoot;
        this.availableLootWithoutDecapitation = availableLoot.stream()
                .filter(loot -> loot != CreeperFarmLoot.HEADS)
                .toList();
    }

    public List<CreeperFarmLoot> availableLoot() {
        return availableLoot;
    }

    public List<CreeperFarmLoot> availableLoot(boolean hasDecapitation) {
        return hasDecapitation ? availableLoot : availableLootWithoutDecapitation;
    }

    public boolean supports(CreeperFarmLoot loot) {
        return availableLoot.contains(loot);
    }

    public static CreeperFarmKind fromId(int id) {
        CreeperFarmKind[] values = values();
        return values[Math.clamp(id, 0, values.length - 1)];
    }
}
