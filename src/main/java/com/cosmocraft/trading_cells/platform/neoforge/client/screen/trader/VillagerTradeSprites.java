package com.cosmocraft.trading_cells.platform.neoforge.client.screen.trader;

import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.TradingCells;
import net.minecraft.resources.Identifier;

/** Shared textures and visual states for every villager trade presentation. */
public final class VillagerTradeSprites {
    public static final int ROW_WIDTH = 100;
    public static final int ROW_HEIGHT = 24;
    public static final int DROPDOWN_ROW_WIDTH = 94;
    public static final int DROPDOWN_ROW_HEIGHT = 18;
    public static final int ARROW_WIDTH = 14;
    public static final int ARROW_HEIGHT = 10;

    public static final Identifier DROPDOWN = texture("selector/trade_dropdown");
    public static final Identifier DISABLED_SLOT_OVERLAY = texture("slots/disabled_slot_overlay");

    private static final Identifier ROW_NORMAL = texture("rows/trade_row");
    private static final Identifier ROW_HOVERED = texture("rows/trade_row_hovered");
    private static final Identifier ROW_SELECTED = texture("rows/trade_row_selected");
    private static final Identifier ROW_DISABLED = texture("rows/trade_row_disabled");

    private static final Identifier DROPDOWN_ROW_NORMAL = texture("selector/trade_dropdown_row");
    private static final Identifier DROPDOWN_ROW_HOVERED = texture("selector/trade_dropdown_row_hovered");
    private static final Identifier DROPDOWN_ROW_SELECTED = texture("selector/trade_dropdown_row_selected");
    private static final Identifier DROPDOWN_ROW_DISABLED = texture("selector/trade_dropdown_row_disabled");

    private static final Identifier ARROW_NORMAL = texture("arrows/trade_arrow");
    private static final Identifier ARROW_HOVERED = texture("arrows/trade_arrow_hovered");
    private static final Identifier ARROW_SELECTED = texture("arrows/trade_arrow_selected");
    private static final Identifier ARROW_DISABLED = texture("arrows/trade_arrow_disabled");

    private VillagerTradeSprites() {
    }

    public static State state(boolean disabled, boolean selected, boolean hovered) {
        if (disabled) {
            return State.DISABLED;
        }
        if (selected) {
            return State.SELECTED;
        }
        return hovered ? State.HOVERED : State.NORMAL;
    }

    public static Identifier row(State state) {
        return switch (state) {
            case NORMAL -> ROW_NORMAL;
            case HOVERED -> ROW_HOVERED;
            case SELECTED -> ROW_SELECTED;
            case DISABLED -> ROW_DISABLED;
        };
    }

    public static Identifier dropdownRow(State state) {
        return switch (state) {
            case NORMAL -> DROPDOWN_ROW_NORMAL;
            case HOVERED -> DROPDOWN_ROW_HOVERED;
            case SELECTED -> DROPDOWN_ROW_SELECTED;
            case DISABLED -> DROPDOWN_ROW_DISABLED;
        };
    }

    public static Identifier arrow(State state) {
        return switch (state) {
            case NORMAL -> ARROW_NORMAL;
            case HOVERED -> ARROW_HOVERED;
            case SELECTED -> ARROW_SELECTED;
            case DISABLED -> ARROW_DISABLED;
        };
    }

    private static Identifier texture(String name) {
        return Identifier.fromNamespaceAndPath(
                TradingCells.MOD_ID,
                "textures/gui/trader/widgets/" + name + ".png"
        );
    }

    public enum State {
        NORMAL,
        HOVERED,
        SELECTED,
        DISABLED
    }
}
