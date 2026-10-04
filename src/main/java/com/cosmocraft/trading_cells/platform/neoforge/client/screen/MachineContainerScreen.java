package com.cosmocraft.trading_cells.platform.neoforge.client.screen;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** Reserves recipe-viewer space without changing Minecraft's GUI scale. */
public abstract class MachineContainerScreen<M extends AbstractContainerMenu> extends AbstractContainerScreen<M> {
    public static final int RECIPE_VIEWER_FOOTER = 24;
    private static final int RECIPE_VIEWER_SIDEBAR = 66;

    protected MachineContainerScreen(M menu, Inventory inventory, Component title, int width, int height) {
        super(menu, inventory, title, width, height);
    }

    @Override protected void init() {
        super.init();
        topPos = Math.max(6, (height - RECIPE_VIEWER_FOOTER - imageHeight) / 2);
        int frame = topPos >= MachineGuiSkin.FRAME_OUTSET ? MachineGuiSkin.FRAME_OUTSET : topPos >= 12 ? 12 : 6;
        leftPos = Math.max(6, Math.min(leftPos, width - imageWidth - RECIPE_VIEWER_SIDEBAR - frame));
    }
}
