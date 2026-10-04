package com.cosmocraft.trading_cells.feature.mobfarm.adapters.output.client;

import com.cosmocraft.trading_cells.platform.neoforge.client.screen.MachineSlotGhosts;
import com.cosmocraft.trading_cells.feature.mobfarm.adapters.output.MobFarmRegistrationAdapter;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.MachineSlotSprites;

import com.cosmocraft.trading_cells.feature.mobfarm.adapters.input.EntityInteractionFarmMenu;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.FittedTextRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.MachineContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class EntityInteractionFarmScreen extends MachineContainerScreen<EntityInteractionFarmMenu> {
    public EntityInteractionFarmScreen(EntityInteractionFarmMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, EntityInteractionFarmMenu.WIDTH, EntityInteractionFarmMenu.HEIGHT);
        inventoryLabelX = EntityInteractionFarmMenu.PLAYER_X;
        inventoryLabelY = EntityInteractionFarmMenu.PLAYER_Y - 12;
    }
    @Override protected void init() {
        super.init();
        addRenderableWidget(SimulationScreenWidgets.checkButton(leftPos + 226, topPos + 7,
                label("enabled"), menu::enabled, button -> command(0)));
    }
    private void command(int button) {
        if (minecraft.gameMode != null) { minecraft.gameMode.handleInventoryButtonClick(menu.containerId, button); }
    }
    @Override public void extractBackground(GuiGraphicsExtractor graphics, int x, int y, float partialTick) {
        super.extractBackground(graphics, x, y, partialTick);
        com.cosmocraft.trading_cells.platform.neoforge.client.screen.MachineGuiSkin.ENTITY_INTERACTION_FARM.draw(graphics, leftPos, topPos, imageWidth, imageHeight);
        for (var slot : menu.slots) { SimulationScreenWidgets.slot(graphics, leftPos + slot.x, topPos + slot.y); }
        MachineSlotGhosts.sprite(graphics, menu.getSlot(0), leftPos, topPos, MachineSlotSprites.VILLAGER_HEAD);
        MachineSlotGhosts.item(graphics, menu.getSlot(1), leftPos, topPos, MobFarmRegistrationAdapter.ENTITY_MODULE.get().getDefaultInstance());
        MachineSlotGhosts.item(graphics, menu.getSlot(2), leftPos, topPos, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BUCKET));
        MachineSlotGhosts.item(graphics, menu.getSlot(3), leftPos, topPos, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.SHEARS));
        MachineSlotGhosts.item(graphics, menu.getSlot(4), leftPos, topPos, MobFarmRegistrationAdapter.UPGRADES.get(0).get().getDefaultInstance());
        MachineSlotGhosts.item(graphics, menu.getSlot(32), leftPos, topPos, MobFarmRegistrationAdapter.UPGRADES.get(5).get().getDefaultInstance());
        graphics.fill(leftPos + 47, topPos + 50, leftPos + 209, topPos + 54, 0xFF161D20);
        graphics.fill(leftPos + 48, topPos + 51, leftPos + 48 + menu.progress() * 160 / menu.duration(), topPos + 53, SimulationScreenWidgets.ACCENT);
    }
    @Override protected void extractLabels(GuiGraphicsExtractor graphics, int x, int y) {
        FittedTextRenderer.left(graphics, font, title, 10, 163, 8, 15, SimulationScreenWidgets.TEXT, true);
        FittedTextRenderer.left(graphics, font, label("enabled"), 169, 222, 8, 15, SimulationScreenWidgets.MUTED, false);
        graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, SimulationScreenWidgets.MUTED, false);
    }
    private static Component label(String key) { return Component.translatable("gui.trading_cells.interaction." + key); }
}
