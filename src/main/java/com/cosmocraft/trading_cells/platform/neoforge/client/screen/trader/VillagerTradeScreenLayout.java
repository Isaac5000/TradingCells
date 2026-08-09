package com.cosmocraft.trading_cells.platform.neoforge.client.screen.trader;

import com.cosmocraft.trading_cells.platform.neoforge.client.screen.SlotRenderer;
import com.cosmocraft.trading_cells.platform.neoforge.menu.VillagerTradeMenuLayout;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public final class VillagerTradeScreenLayout {
    public static final int WIDTH = VillagerTradeMenuLayout.WIDTH;
    public static final int HEIGHT = VillagerTradeMenuLayout.HEIGHT;
    public static final int ATLAS_WIDTH = VillagerTradeMenuLayout.ATLAS_WIDTH;
    public static final int ATLAS_HEIGHT = VillagerTradeMenuLayout.ATLAS_HEIGHT;

    public static final int MANUAL_VISIBLE_ROWS = 7;
    public static final int MANUAL_ROW_HEIGHT = 24;
    public static final int DROPDOWN_VISIBLE_ROWS = 8;
    public static final int DROPDOWN_ROW_HEIGHT = 18;

    public static final int TRADES_TITLE_CENTER_X = 62;
    public static final int PROFESSION_TITLE_CENTER_X = 233;
    public static final int HEADER_TEXT_Y = 12;
    public static final int LEVEL_TEXT_X = 129;
    public static final int PROFESSION_XP_RIGHT_X = 328;
    public static final int PROFESSION_TEXT_Y = 30;
    public static final int PROFESSION_PROGRESS_X = 130;
    public static final int PROFESSION_PROGRESS_Y = 41;
    public static final int PROFESSION_PROGRESS_WIDTH = 202;
    public static final int INVENTORY_LABEL_X = 152;
    public static final int INVENTORY_LABEL_Y = 107;

    public static final int XP_DISPLAY_X = 126;
    public static final int XP_DISPLAY_Y = 77;
    public static final int XP_DISPLAY_WIDTH = 108;
    public static final int XP_DISPLAY_HEIGHT = 22;
    public static final int XP_ICON_X = 129;
    public static final int XP_ICON_Y = 80;
    public static final int XP_TEXT_X = 148;
    public static final int XP_TEXT_Y = 79;
    public static final int XP_LEVEL_TEXT_Y = 90;
    public static final int XP_BUTTON_X = 236;
    public static final int XP_BUTTON_Y = 79;
    public static final int XP_BUTTON_WIDTH = 64;
    public static final int XP_BUTTON_HEIGHT = 18;

    public static final int PREVIEW_ARROW_X = 222;
    public static final int PREVIEW_ARROW_Y_OFFSET = 4;
    public static final int AUTOTRADER_PREVIEW_ARROW_Y_OFFSET = 3;

    public static final int MANUAL_ROW_X = 11;
    public static final int MANUAL_ROW_Y = 29;
    public static final int MANUAL_ROW_WIDTH = 100;
    public static final int MANUAL_SCROLL_X = 113;
    public static final int MANUAL_SCROLL_Y = 29;
    public static final int MANUAL_SCROLL_HEIGHT = 168;

    public static final int AUTOTRADER_DROPDOWN_X = 12;
    public static final int AUTOTRADER_DROPDOWN_Y = 48;
    public static final int AUTOTRADER_DROPDOWN_VIEW_X = 14;
    public static final int AUTOTRADER_DROPDOWN_VIEW_Y = 50;
    public static final int AUTOTRADER_DROPDOWN_VIEW_WIDTH = 94;
    public static final int AUTOTRADER_DROPDOWN_TRACK_X = 109;
    public static final int AUTOTRADER_DROPDOWN_HEIGHT = DROPDOWN_VISIBLE_ROWS * DROPDOWN_ROW_HEIGHT;
    private static final int[] EQUIPMENT_SLOT_Y = new int[]{
            VillagerTradeMenuLayout.EQUIPMENT_HEAD_Y,
            VillagerTradeMenuLayout.EQUIPMENT_CHEST_Y,
            VillagerTradeMenuLayout.EQUIPMENT_LEGS_Y,
            VillagerTradeMenuLayout.EQUIPMENT_FEET_Y,
            VillagerTradeMenuLayout.EQUIPMENT_OFFHAND_Y
    };

    private VillagerTradeScreenLayout() {
    }

    public static int dropdownContentHeight(int offerCount) {
        int visibleRows = Math.clamp(offerCount, 1, DROPDOWN_VISIBLE_ROWS);
        return visibleRows * DROPDOWN_ROW_HEIGHT;
    }

    public static void drawBackground(GuiGraphicsExtractor graphics, Identifier texture, int left, int top) {
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                texture,
                left,
                top,
                0.0F,
                0.0F,
                WIDTH,
                HEIGHT,
                ATLAS_WIDTH,
                ATLAS_HEIGHT
        );
    }

    public static void drawCommonSlots(
            GuiGraphicsExtractor graphics,
            int left,
            int top,
            VillagerGuiThemeColors colors
    ) {
        drawSlotAtFramePosition(
                graphics,
                left,
                top,
                VillagerTradeMenuLayout.MANUAL_PAYMENT_A_X,
                VillagerTradeMenuLayout.MANUAL_TRADER_SLOT_Y,
                colors
        );
        drawSlotAtFramePosition(
                graphics,
                left,
                top,
                VillagerTradeMenuLayout.MANUAL_PAYMENT_B_X,
                VillagerTradeMenuLayout.MANUAL_TRADER_SLOT_Y,
                colors
        );
        drawSlotAtFramePosition(
                graphics,
                left,
                top,
                VillagerTradeMenuLayout.MANUAL_RESULT_X,
                VillagerTradeMenuLayout.MANUAL_TRADER_SLOT_Y,
                colors
        );

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                drawSlotAtFramePosition(
                        graphics,
                        left,
                        top,
                        VillagerTradeMenuLayout.PLAYER_INVENTORY_X + column * SlotRenderer.FRAME_SIZE,
                        VillagerTradeMenuLayout.PLAYER_INVENTORY_Y + row * SlotRenderer.FRAME_SIZE,
                        colors
                );
            }
        }
        for (int column = 0; column < 9; column++) {
            drawSlotAtFramePosition(
                    graphics,
                    left,
                    top,
                    VillagerTradeMenuLayout.PLAYER_INVENTORY_X + column * SlotRenderer.FRAME_SIZE,
                    VillagerTradeMenuLayout.PLAYER_HOTBAR_Y,
                    colors
            );
        }
        for (int equipmentY : EQUIPMENT_SLOT_Y) {
            drawSlotAtFramePosition(
                    graphics,
                    left,
                    top,
                    VillagerTradeMenuLayout.EQUIPMENT_X,
                    equipmentY,
                    colors
            );
        }
    }

    public static void drawSlotAtFramePosition(
            GuiGraphicsExtractor graphics,
            int left,
            int top,
            int frameX,
            int frameY,
            VillagerGuiThemeColors colors
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
