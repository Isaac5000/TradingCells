package com.cosmocraft.trading_cells.feature.mobfarm.domain.model;

public enum EssenceTier {
    I(1, 1_000, 1), II(2, 4_000, 2), III(3, 16_000, 4), IV(4, 64_000, 8);

    private final int id;
    private final int modelExperience;
    private final int amethyst;

    EssenceTier(int id, int modelExperience, int amethyst) {
        this.id = id;
        this.modelExperience = modelExperience;
        this.amethyst = amethyst;
    }

    public int id() { return id; }
    public int modelExperience() { return modelExperience; }
    public int amethyst() { return amethyst; }
    public static EssenceTier fromId(int id) { return values()[Math.clamp(id, 1, 4) - 1]; }
}
