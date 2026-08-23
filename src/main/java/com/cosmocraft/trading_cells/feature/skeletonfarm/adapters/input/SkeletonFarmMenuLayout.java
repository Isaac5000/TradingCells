package com.cosmocraft.trading_cells.feature.skeletonfarm.adapters.input;

/** Stable menu geometry owned by mob farms rather than the villager trader UI. */
public final class SkeletonFarmMenuLayout {
    public static final int ATLAS_WIDTH = 512;
    public static final int ATLAS_HEIGHT = 256;
    public static final int WIDTH = 348;
    public static final int HEIGHT = 210;

    public static final int PLAYER_INVENTORY_X = 162;
    public static final int PLAYER_INVENTORY_Y = 116;
    public static final int PLAYER_HOTBAR_Y = 174;

    public static final int EQUIPMENT_X = 142;
    public static final int EQUIPMENT_HEAD_Y = 108;
    public static final int EQUIPMENT_CHEST_Y = 126;
    public static final int EQUIPMENT_LEGS_Y = 144;
    public static final int EQUIPMENT_FEET_Y = 162;
    public static final int EQUIPMENT_OFFHAND_Y = 180;

    private SkeletonFarmMenuLayout() {
    }

    public static int itemX(int frameX) {
        return frameX + 1;
    }

    public static int itemY(int frameY) {
        return frameY + 1;
    }
}
