package com.cosmocraft.trading_cells.feature.raiderfarm.adapters.output.client;

import com.cosmocraft.trading_cells.feature.raiderfarm.adapters.input.RaiderFarmMenuLayout;
import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.TradingCells;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.SlotRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/** Independent snapshot of the trader layout used as the mob-farm UI baseline. */
public final class RaiderFarmScreenLayout {
    private static final Identifier BACKGROUND = Identifier.fromNamespaceAndPath(
            TradingCells.MOD_ID,
            "textures/gui/raider_farm/background.png"
    );

    public static final int TRADES_TITLE_CENTER_X = 62;
    public static final int PROFESSION_TITLE_CENTER_X = 233;
    public static final int HEADER_TEXT_Y = 12;
    public static final int INVENTORY_LABEL_X = 164;
    public static final int INVENTORY_LABEL_Y = 108;

    private RaiderFarmScreenLayout() {
    }

    public static void drawBackground(GuiGraphicsExtractor graphics, int left, int top) {
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                BACKGROUND,
                left,
                top,
                0.0F,
                0.0F,
                RaiderFarmMenuLayout.WIDTH,
                RaiderFarmMenuLayout.HEIGHT,
                RaiderFarmMenuLayout.ATLAS_WIDTH,
                RaiderFarmMenuLayout.ATLAS_HEIGHT
        );
    }

    public static void drawSlotAtFramePosition(
            GuiGraphicsExtractor graphics,
            int left,
            int top,
            int frameX,
            int frameY,
            RaiderFarmGuiThemeColors colors
    ) {
        SlotRenderer.drawAtFramePosition(
                graphics,
                left,
                top,
                frameX,
                frameY,
                colors.slotPalette()
        );
    }
}
