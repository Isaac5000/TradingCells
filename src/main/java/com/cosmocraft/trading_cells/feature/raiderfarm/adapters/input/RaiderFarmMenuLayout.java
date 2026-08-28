package com.cosmocraft.trading_cells.feature.raiderfarm.adapters.input;

import com.cosmocraft.trading_cells.platform.neoforge.mobfarm.MobFarmMenuLayout;

/** Stable menu geometry owned by mob farms rather than the villager trader UI. */
public final class RaiderFarmMenuLayout {
    public static final int ATLAS_WIDTH = MobFarmMenuLayout.ATLAS_WIDTH;
    public static final int ATLAS_HEIGHT = MobFarmMenuLayout.ATLAS_HEIGHT;
    public static final int WIDTH = MobFarmMenuLayout.WIDTH;
    public static final int HEIGHT = MobFarmMenuLayout.HEIGHT;

    public static final int PLAYER_INVENTORY_X = MobFarmMenuLayout.PLAYER_INVENTORY_X;
    public static final int PLAYER_INVENTORY_Y = MobFarmMenuLayout.PLAYER_INVENTORY_Y;
    public static final int PLAYER_HOTBAR_Y = MobFarmMenuLayout.PLAYER_HOTBAR_Y;

    public static final int EQUIPMENT_X = MobFarmMenuLayout.EQUIPMENT_X;
    public static final int EQUIPMENT_HEAD_Y = MobFarmMenuLayout.EQUIPMENT_HEAD_Y;
    public static final int EQUIPMENT_CHEST_Y = MobFarmMenuLayout.EQUIPMENT_CHEST_Y;
    public static final int EQUIPMENT_LEGS_Y = MobFarmMenuLayout.EQUIPMENT_LEGS_Y;
    public static final int EQUIPMENT_FEET_Y = MobFarmMenuLayout.EQUIPMENT_FEET_Y;
    public static final int EQUIPMENT_OFFHAND_Y = MobFarmMenuLayout.EQUIPMENT_OFFHAND_Y;

    private RaiderFarmMenuLayout() {
    }

    public static int itemX(int frameX) {
        return MobFarmMenuLayout.itemX(frameX);
    }

    public static int itemY(int frameY) {
        return MobFarmMenuLayout.itemY(frameY);
    }
}
