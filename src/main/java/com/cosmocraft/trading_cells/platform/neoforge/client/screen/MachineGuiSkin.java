package com.cosmocraft.trading_cells.platform.neoforge.client.screen;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/** Painted PNGs are fitted to each layout offline, then drawn with one blit. */
public enum MachineGuiSkin {
    FARMER, PIGLIN_FARMER, QUARRY, PIGLIN_QUARRY,
    VILLAGER_BREEDER, PIGLIN_BREEDER, VILLAGER_INCUBATOR, PIGLIN_INCUBATOR,
    VILLAGER_TRADING_CELL, AUTOTRADER, PIGLIN_BARTERING_CELL, NETHERITE_PIGLIN_BARTERING_CELL,
    IRON_FARM, EXPERIENCE_STORAGE, CONVERTER, MOB_FARM, ENTITY_INTERACTION_FARM,
    ESSENCE_WORKBENCH, ESSENCE_STABILIZER, ARCANE_INFUSER;

    public static final int FRAME_OUTSET = 21;

    private final String texturePrefix = "textures/gui/container/" + name().toLowerCase(Locale.ROOT) + "/";
    private final Map<Long, Identifier> textures = new HashMap<>();

    public Identifier texture(int width, int height) {
        return texture(width, height, width == 78 && height == 36 ? 0 : FRAME_OUTSET);
    }

    private Identifier texture(int width, int height, int outset) {
        String kind = width == 78 && height == 36 ? "xp_summary"
                : width == 200 && height == 124 ? "recipe" : "background";
        return textures.computeIfAbsent(((long) width << 32) | ((long) height << 8) | outset, ignored -> Identifier.fromNamespaceAndPath(
                "trading_cells", texturePrefix + kind
                        + (outset == 12 ? "_compact" : outset == 6 ? "_small" : "") + ".png"));
    }

    public static int frameOutset(int x, int y, int width, int height, int screenWidth, int screenHeight) {
        if (width == 78 && height == 36) { return 0; }
        int room = Math.min(Math.min(x, screenWidth - x - width), Math.min(y, screenHeight - y - height));
        return room >= FRAME_OUTSET ? FRAME_OUTSET : room >= 12 ? 12 : 6;
    }

    public void draw(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        int outset = frameOutset(x, y, width, height, graphics.guiWidth(), graphics.guiHeight());
        int textureWidth = width + 2 * outset;
        int textureHeight = height + 2 * outset;
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture(width, height, outset), x - outset, y - outset,
                0, 0, textureWidth, textureHeight, textureWidth, textureHeight);
    }

    public static MachineGuiSkin forTheme(MachineScreenTheme theme) {
        return switch (theme) {
            case VILLAGER_BREEDER -> VILLAGER_BREEDER;
            case VILLAGER_INCUBATOR -> VILLAGER_INCUBATOR;
            case PIGLIN_BREEDER -> PIGLIN_BREEDER;
            case PIGLIN_INCUBATOR -> PIGLIN_INCUBATOR;
            case CONVERTER -> CONVERTER;
            case FARMER -> FARMER;
            case IRON_FARM -> IRON_FARM;
        };
    }
}
