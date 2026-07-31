package com.cosmocraft.trading_cells.platform.neoforge.integration.rei;

import com.cosmocraft.trading_cells.platform.neoforge.client.screen.MachineScreenTheme;
import net.minecraft.resources.Identifier;

enum TradingCellsReiLayout {
    VILLAGER_BREEDING(
            Kind.BREEDING,
            MachineScreenTheme.VILLAGER_BREEDER,
            blockTexture("oak_planks"),
            0xFFE2B23F
    ),
    PIGLIN_BREEDING(
            Kind.BREEDING,
            MachineScreenTheme.PIGLIN_BREEDER,
            blockTexture("crimson_planks"),
            0xFFE04A4A
    ),
    VILLAGER_INCUBATION(
            Kind.INCUBATION,
            MachineScreenTheme.VILLAGER_INCUBATOR,
            blockTexture("yellow_wool"),
            0xFFF0C934
    ),
    PIGLIN_INCUBATION(
            Kind.INCUBATION,
            MachineScreenTheme.PIGLIN_INCUBATOR,
            blockTexture("red_wool"),
            0xFFD84B45
    ),
    FARMING(
            Kind.FARMING,
            MachineScreenTheme.FARMER,
            blockTexture("dirt"),
            0xFF55A630
    ),
    CONVERSION(
            Kind.CONVERSION,
            MachineScreenTheme.CONVERTER,
            blockTexture("mossy_cobblestone"),
            0xFF8E5AA7
    ),
    IRON_FARM(
            Kind.IRON_FARM,
            MachineScreenTheme.IRON_FARM,
            blockTexture("cobblestone"),
            0xFFD7D7D7
    ),
    PIGLIN_BARTERING(
            Kind.PIGLIN_BARTERING,
            MachineScreenTheme.PIGLIN_BREEDER,
            blockTexture("polished_blackstone"),
            0xFFE1A11A
    ),
    NETHERITE_PIGLIN_BARTERING(
            Kind.NETHERITE_PIGLIN_BARTERING,
            MachineScreenTheme.IRON_FARM,
            blockTexture("netherite_block"),
            0xFFE1A11A
    );

    private final Kind kind;
    private final MachineScreenTheme theme;
    private final Identifier surface;
    private final int progressColor;

    TradingCellsReiLayout(
            Kind kind,
            MachineScreenTheme theme,
            Identifier surface,
            int progressColor
    ) {
        this.kind = kind;
        this.theme = theme;
        this.surface = surface;
        this.progressColor = progressColor;
    }

    public Kind kind() {
        return kind;
    }

    public MachineScreenTheme theme() {
        return theme;
    }

    public Identifier surface() {
        return surface;
    }

    public int progressColor() {
        return progressColor;
    }

    private static Identifier blockTexture(String path) {
        return Identifier.fromNamespaceAndPath("minecraft", "textures/block/" + path + ".png");
    }

    enum Kind {
        BREEDING,
        INCUBATION,
        FARMING,
        CONVERSION,
        IRON_FARM,
        PIGLIN_BARTERING,
        NETHERITE_PIGLIN_BARTERING
    }
}
