package com.cosmocraft.trading_cells.platform.neoforge.menu;

/**
 * Logical visible bounds for the shared villager trader screens.
 *
 * <p>The PNG atlas remains 512x256, while the rendered/interactive menu uses
 * 348x210 pixels. This preserves the real vanilla-style visible footprint and
 * only extends the original barter layout moderately to the right and by the
 * allowed thirty pixels downwards.</p>
 */
public final class VillagerTradeMenuLayout {
    public static final int ATLAS_WIDTH = 512;
    public static final int ATLAS_HEIGHT = 256;
    public static final int WIDTH = 348;
    public static final int HEIGHT = 210;

    public static final int TRADES_PANEL_X = 5;
    public static final int TRADES_PANEL_Y = 5;
    public static final int TRADES_PANEL_WIDTH = 114;
    public static final int TRADES_PANEL_HEIGHT = 200;
    public static final int TRADES_CONTENT_X = 9;
    public static final int TRADES_CONTENT_Y = 28;
    public static final int TRADES_CONTENT_WIDTH = 106;
    public static final int TRADES_CONTENT_HEIGHT = 172;

    public static final int RIGHT_PANEL_X = 123;
    public static final int RIGHT_PANEL_Y = 5;
    public static final int RIGHT_PANEL_WIDTH = 220;
    public static final int PROFESSION_PANEL_HEIGHT = 66;

    public static final int XP_PANEL_X = 123;
    public static final int XP_PANEL_Y = 74;
    public static final int XP_PANEL_WIDTH = 220;
    public static final int XP_PANEL_HEIGHT = 28;

    public static final int RESET_BUTTON_X = 301;
    public static final int RESET_BUTTON_Y = 74;
    public static final int RESET_BUTTON_WIDTH = 42;
    public static final int RESET_BUTTON_HEIGHT = 28;

    public static final int INVENTORY_PANEL_X = 123;
    public static final int INVENTORY_PANEL_Y = 104;
    public static final int INVENTORY_PANEL_WIDTH = 220;
    public static final int INVENTORY_PANEL_HEIGHT = 102;

    public static final int PLAYER_INVENTORY_X = 150;
    public static final int PLAYER_INVENTORY_Y = 116;
    public static final int PLAYER_HOTBAR_Y = 174;

    /** One compact vertical column on the left of the player inventory: head/chest/legs/feet and offhand last. */
    public static final int EQUIPMENT_X = 130;
    public static final int EQUIPMENT_HEAD_Y = 108;
    public static final int EQUIPMENT_CHEST_Y = 126;
    public static final int EQUIPMENT_LEGS_Y = 144;
    public static final int EQUIPMENT_FEET_Y = 162;
    public static final int EQUIPMENT_OFFHAND_Y = 180;


    public static final int MANUAL_PAYMENT_A_X = 165;
    public static final int MANUAL_PAYMENT_B_X = 191;
    public static final int MANUAL_RESULT_X = 249;
    public static final int MANUAL_TRADER_SLOT_Y = 52;

    public static final int AUTOTRADER_SELECTOR_X = 11;
    public static final int AUTOTRADER_SELECTOR_Y = 29;
    public static final int AUTOTRADER_SELECTOR_WIDTH = 102;
    public static final int AUTOTRADER_SELECTOR_HEIGHT = 18;

    public static final int AUTOTRADER_PANEL_X = 12;
    public static final int AUTOTRADER_PANEL_WIDTH = 100;
    public static final int AUTOTRADER_PANEL_HEIGHT = 39;
    public static final int AUTOTRADER_INPUT_A_PANEL_Y = 60;
    public static final int AUTOTRADER_INPUT_B_PANEL_Y = 107;
    public static final int AUTOTRADER_OUTPUT_PANEL_Y = 154;
    public static final int AUTOTRADER_INPUT_A_TITLE_Y = 65;
    public static final int AUTOTRADER_INPUT_B_TITLE_Y = 112;
    public static final int AUTOTRADER_OUTPUT_TITLE_Y = 159;

    public static final int AUTOTRADER_ROW_X = 26;
    public static final int AUTOTRADER_INPUT_A_Y = 78;
    public static final int AUTOTRADER_INPUT_B_Y = 125;
    public static final int AUTOTRADER_OUTPUT_Y = 172;

    private VillagerTradeMenuLayout() {
    }

    /** Menu slot coordinates point to the 16x16 item area inside an 18x18 frame. */
    public static int itemX(int frameX) {
        return frameX + 1;
    }

    public static int itemY(int frameY) {
        return frameY + 1;
    }
}
