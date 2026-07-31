package com.cosmocraft.trading_cells.platform.neoforge.menu;

/**
 * Shared logical coordinates for every custom machine menu.
 *
 * <p>The upper machine area is centred in the whole screen. The player
 * inventory is shifted only enough to leave a compact equipment column on its
 * left, avoiding the large empty rail used by the previous layout.</p>
 */
public final class MachineMenuLayout {
    public static final int WIDTH = 204;
    public static final int HEIGHT = 215;

    /** Width used by vanilla-style machine and inventory layouts. */
    public static final int CONTENT_WIDTH = 176;

    /** Centred origin for slots belonging to the block entity. */
    public static final int MACHINE_CONTENT_X = (WIDTH - CONTENT_WIDTH) / 2;

    /** Kept as an alias for older call sites; new code should use machineX. */
    public static final int CONTENT_X = MACHINE_CONTENT_X;

    public static final int TITLE_Y = 8;

    /** Player inventory is moved right just enough to fit the equipment rail. */
    public static final int PLAYER_INVENTORY_X = 32;
    public static final int PLAYER_INVENTORY_SLOT_Y = 135;
    public static final int PLAYER_INVENTORY_LABEL_X = PLAYER_INVENTORY_X + 6;
    public static final int PLAYER_INVENTORY_LABEL_Y = 122;

    public static final int EQUIPMENT_X = 6;
    public static final int EQUIPMENT_HEAD_Y = 121;
    public static final int EQUIPMENT_CHEST_Y = 139;
    public static final int EQUIPMENT_LEGS_Y = 157;
    public static final int EQUIPMENT_FEET_Y = 175;
    public static final int EQUIPMENT_OFFHAND_Y = 193;

    private MachineMenuLayout() {
    }

    public static int machineX(int localX) {
        return MACHINE_CONTENT_X + localX;
    }
}
