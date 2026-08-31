package com.cosmocraft.trading_cells.feature.configuredmobfarm.domain.model;

public enum ConfiguredMobFarmKind {
    ARTHROPOD("arthropod"),
    SLIME("slime"),
    GUARDIAN("guardian"),
    PIGLIN("piglin"),
    BLAZE("blaze"),
    GHAST("ghast"),
    ENDERMAN("enderman"),
    SHULKER("shulker"),
    BREEZE("breeze"),
    PHANTOM("phantom");

    private final String path;

    ConfiguredMobFarmKind(String path) {
        this.path = path;
    }

    public String path() {
        return path;
    }

    public String blockId() {
        return path + "_farm";
    }

    public boolean supports(ConfiguredMobFarmLoot ignoredLoot) {
        return false;
    }

    public static ConfiguredMobFarmKind fromId(int id) {
        ConfiguredMobFarmKind[] values = values();
        return id >= 0 && id < values.length ? values[id] : ARTHROPOD;
    }

    public int persistentId() {
        return ordinal();
    }
}
