package com.cosmocraft.trading_cells.feature.skeletonfarm.domain.model;

import java.util.List;

public enum SkeletonFarmKind {
    SKELETON(List.of(SkeletonFarmLoot.WEAPONS, SkeletonFarmLoot.BONES, SkeletonFarmLoot.ARROWS)),
    WITHER_SKELETON(List.of(
            SkeletonFarmLoot.WEAPONS,
            SkeletonFarmLoot.BONES,
            SkeletonFarmLoot.SKULLS,
            SkeletonFarmLoot.COAL
    )),
    STRAY(List.of(SkeletonFarmLoot.WEAPONS, SkeletonFarmLoot.BONES, SkeletonFarmLoot.ARROWS)),
    BOGGED(List.of(SkeletonFarmLoot.WEAPONS, SkeletonFarmLoot.BONES, SkeletonFarmLoot.ARROWS)),
    PARCHED(List.of(SkeletonFarmLoot.WEAPONS, SkeletonFarmLoot.BONES, SkeletonFarmLoot.ARROWS));

    private final List<SkeletonFarmLoot> availableLoot;

    SkeletonFarmKind(List<SkeletonFarmLoot> availableLoot) {
        this.availableLoot = availableLoot;
    }

    public List<SkeletonFarmLoot> availableLoot() {
        return availableLoot;
    }

    public boolean supports(SkeletonFarmLoot loot) {
        return availableLoot.contains(loot);
    }

    public static SkeletonFarmKind fromId(int id) {
        SkeletonFarmKind[] values = values();
        return values[Math.clamp(id, 0, values.length - 1)];
    }
}
