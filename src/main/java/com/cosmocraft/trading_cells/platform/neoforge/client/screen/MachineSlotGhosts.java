package com.cosmocraft.trading_cells.platform.neoforge.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class MachineSlotGhosts {
    private MachineSlotGhosts() { }

    public static void upgrade(GuiGraphicsExtractor graphics, Slot slot, int left, int top, boolean capacity) {
        var id = Identifier.fromNamespaceAndPath("trading_cells", (capacity ? "capacity" : "speed") + "_copper_upgrade");
        net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(id)
                .ifPresent(upgrade -> item(graphics, slot, left, top, upgrade.getDefaultInstance()));
    }

    public static void item(GuiGraphicsExtractor graphics, Slot slot, int left, int top, ItemStack icon) {
        if (slot.hasItem()) { return; }
        int x = left + slot.x, y = top + slot.y;
        graphics.fakeItem(icon, x, y);
        graphics.fill(x, y, x + 16, y + 16, 0xB0343D41);
    }

    public static void sprite(GuiGraphicsExtractor graphics, Slot slot, int left, int top, Identifier icon) {
        if (!slot.hasItem()) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, icon, left + slot.x, top + slot.y, 16, 16);
        }
    }
}
