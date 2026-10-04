package com.cosmocraft.trading_cells.feature.mobfarm.adapters.output.client;

import com.cosmocraft.trading_cells.platform.neoforge.client.screen.MachineSlotGhosts;
import com.cosmocraft.trading_cells.feature.mobfarm.adapters.output.MobFarmRegistrationAdapter;

import com.cosmocraft.trading_cells.feature.mobfarm.adapters.input.EssenceStabilizerMenu;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.FittedTextRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.MachineContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class EssenceStabilizerScreen extends MachineContainerScreen<EssenceStabilizerMenu> {
    public EssenceStabilizerScreen(EssenceStabilizerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 236, 204);
        inventoryLabelX = 37;
        inventoryLabelY = 112;
    }
    @Override public void extractBackground(GuiGraphicsExtractor graphics, int x, int y, float partialTick) {
        super.extractBackground(graphics, x, y, partialTick);
        com.cosmocraft.trading_cells.platform.neoforge.client.screen.MachineGuiSkin.ESSENCE_STABILIZER.draw(graphics, leftPos, topPos, imageWidth, imageHeight);
        for (var slot : menu.slots) { SimulationScreenWidgets.slot(graphics, leftPos + slot.x, topPos + slot.y); }
        SimulationScreenWidgets.arrow(graphics, leftPos + 124, topPos + 48, (float) menu.progress() / menu.duration());
        MachineSlotGhosts.item(graphics, menu.getSlot(0), leftPos, topPos, MobFarmRegistrationAdapter.RAW_ESSENCE.get().getDefaultInstance());
        MachineSlotGhosts.item(graphics, menu.getSlot(1), leftPos, topPos, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.AMETHYST_SHARD));
        MachineSlotGhosts.item(graphics, menu.getSlot(2), leftPos, topPos, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.REDSTONE));
    }
    @Override protected void extractLabels(GuiGraphicsExtractor graphics, int x, int y) {
        FittedTextRenderer.left(graphics, font, title, 12, 224, 8, 16, SimulationScreenWidgets.TEXT, true);
        graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, SimulationScreenWidgets.MUTED, false);
    }
}
