package com.cosmocraft.trading_cells.feature.skeletonfarm.domain.model;

import java.util.List;

public enum SkeletonFarmKind {
    SKELETON(false, List.of(
            SkeletonFarmLoot.WEAPONS,
            SkeletonFarmLoot.BONES,
            SkeletonFarmLoot.ARROWS,
            SkeletonFarmLoot.SKULLS
    )),
    WITHER_SKELETON(true, List.of(
            SkeletonFarmLoot.WEAPONS,
            SkeletonFarmLoot.BONES,
            SkeletonFarmLoot.SKULLS,
            SkeletonFarmLoot.COAL
    )),
    STRAY(false, List.of(
            SkeletonFarmLoot.WEAPONS,
            SkeletonFarmLoot.BONES,
            SkeletonFarmLoot.ARROWS,
            SkeletonFarmLoot.SKULLS
    )),
    BOGGED(false, List.of(
            SkeletonFarmLoot.WEAPONS,
            SkeletonFarmLoot.BONES,
            SkeletonFarmLoot.ARROWS,
            SkeletonFarmLoot.SKULLS
    )),
    PARCHED(false, List.of(
            SkeletonFarmLoot.WEAPONS,
            SkeletonFarmLoot.BONES,
            SkeletonFarmLoot.ARROWS,
            SkeletonFarmLoot.SKULLS
    )),
    SKELETON_HORSE(false, List.of(
            SkeletonFarmLoot.BONES
    ));

    private final boolean nativeHeadDrop;
    private final List<SkeletonFarmLoot> availableLoot;
    private final List<SkeletonFarmLoot> availableLootWithoutDecapitation;

    SkeletonFarmKind(boolean nativeHeadDrop, List<SkeletonFarmLoot> availableLoot) {
        this.nativeHeadDrop = nativeHeadDrop;
        this.availableLoot = availableLoot;
        this.availableLootWithoutDecapitation = nativeHeadDrop
                ? availableLoot
                : availableLoot.stream()
                        .filter(loot -> loot != SkeletonFarmLoot.SKULLS)
                        .toList();
    }

    public List<SkeletonFarmLoot> availableLoot() {
        return availableLoot;
    }

    public boolean supports(SkeletonFarmLoot loot) {
        return availableLoot.contains(loot);
    }

    public List<SkeletonFarmLoot> availableLoot(boolean hasDecapitation) {
        return hasDecapitation ? availableLoot : availableLootWithoutDecapitation;
    }

    public boolean canDropHead(boolean hasDecapitation) {
        return supports(SkeletonFarmLoot.SKULLS) && (nativeHeadDrop || hasDecapitation);
    }

    public static SkeletonFarmKind fromId(int id) {
        SkeletonFarmKind[] values = values();
        return values[Math.clamp(id, 0, values.length - 1)];
    }
}
