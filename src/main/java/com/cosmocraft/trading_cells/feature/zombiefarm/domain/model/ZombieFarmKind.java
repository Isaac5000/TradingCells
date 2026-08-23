package com.cosmocraft.trading_cells.feature.zombiefarm.domain.model;

import java.util.List;

public enum ZombieFarmKind {
    ZOMBIE(List.of(
            ZombieFarmLoot.ROTTEN_FLESH,
            ZombieFarmLoot.IRON_INGOTS,
            ZombieFarmLoot.CARROTS,
            ZombieFarmLoot.POTATOES,
            ZombieFarmLoot.WEAPONS,
            ZombieFarmLoot.HEADS
    )),
    ZOMBIE_VILLAGER(List.of(
            ZombieFarmLoot.ROTTEN_FLESH,
            ZombieFarmLoot.IRON_INGOTS,
            ZombieFarmLoot.CARROTS,
            ZombieFarmLoot.POTATOES,
            ZombieFarmLoot.WEAPONS,
            ZombieFarmLoot.HEADS
    )),
    HUSK(List.of(
            ZombieFarmLoot.ROTTEN_FLESH,
            ZombieFarmLoot.IRON_INGOTS,
            ZombieFarmLoot.CARROTS,
            ZombieFarmLoot.POTATOES,
            ZombieFarmLoot.WEAPONS,
            ZombieFarmLoot.HEADS
    )),
    DROWNED(List.of(
            ZombieFarmLoot.ROTTEN_FLESH,
            ZombieFarmLoot.WEAPONS,
            ZombieFarmLoot.COPPER_INGOTS,
            ZombieFarmLoot.NAUTILUS_SHELLS,
            ZombieFarmLoot.HEADS
    )),
    ZOMBIFIED_PIGLIN(List.of(
            ZombieFarmLoot.ROTTEN_FLESH,
            ZombieFarmLoot.GOLD_NUGGETS,
            ZombieFarmLoot.GOLD_INGOTS,
            ZombieFarmLoot.WEAPONS,
            ZombieFarmLoot.HEADS
    )),
    ZOGLIN(List.of(
            ZombieFarmLoot.ROTTEN_FLESH
    ));

    private final List<ZombieFarmLoot> availableLoot;
    private final List<ZombieFarmLoot> availableLootWithoutDecapitation;

    ZombieFarmKind(List<ZombieFarmLoot> availableLoot) {
        this.availableLoot = availableLoot;
        this.availableLootWithoutDecapitation = availableLoot.stream()
                .filter(loot -> loot != ZombieFarmLoot.HEADS)
                .toList();
    }

    public List<ZombieFarmLoot> availableLoot() {
        return availableLoot;
    }

    public boolean supports(ZombieFarmLoot loot) {
        return availableLoot.contains(loot);
    }

    public List<ZombieFarmLoot> availableLoot(boolean hasDecapitation) {
        return hasDecapitation ? availableLoot : availableLootWithoutDecapitation;
    }

    public boolean canDropHead(boolean hasDecapitation) {
        return supports(ZombieFarmLoot.HEADS) && hasDecapitation;
    }

    public static ZombieFarmKind fromId(int id) {
        ZombieFarmKind[] values = values();
        return values[Math.clamp(id, 0, values.length - 1)];
    }
}
