package com.cosmocraft.trading_cells.feature.mobfarm.adapters.output.client;

import com.cosmocraft.trading_cells.feature.mobfarm.adapters.input.EssenceWorkbenchMenu;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.FittedTextRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class EssenceWorkbenchScreen extends AbstractContainerScreen<EssenceWorkbenchMenu> {
    private Button synthesize;
    public EssenceWorkbenchScreen(EssenceWorkbenchMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, EssenceWorkbenchMenu.WIDTH, EssenceWorkbenchMenu.HEIGHT);
        inventoryLabelX = 37;
        inventoryLabelY = 118;
    }
    @Override protected void init() {
        super.init();
        synthesize = addRenderableWidget(Button.builder(Component.translatable("gui.trading_cells.simulation.synthesize"), button -> {
            if (minecraft.gameMode != null) { minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0); }
        }).bounds(leftPos + 37, topPos + 93, 162, 18).build());
        synthesize.active = menu.canSynthesize();
    }
    @Override protected void containerTick() { super.containerTick(); synthesize.active = menu.canSynthesize(); }
    @Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        SimulationScreenWidgets.background(graphics, leftPos, topPos, imageWidth, imageHeight);
        for (var slot : menu.slots) { SimulationScreenWidgets.slot(graphics, leftPos + slot.x, topPos + slot.y); }
        if (menu.getSlot(1).getItem().isEmpty()) {
            graphics.fakeItem(net.minecraft.world.item.Items.AMETHYST_SHARD.getDefaultInstance(), leftPos + 83, topPos + 47);
        }
        if (menu.getSlot(2).getItem().isEmpty()) { graphics.fakeItem(menu.requiredMaterial(), leftPos + 131, topPos + 47); }
        graphics.fill(leftPos + 157, topPos + 53, leftPos + 173, topPos + 57, SimulationScreenWidgets.MUTED);
        graphics.fill(leftPos + 170, topPos + 50, leftPos + 173, topPos + 60, SimulationScreenWidgets.MUTED);
        graphics.fill(leftPos + 173, topPos + 52, leftPos + 175, topPos + 58, SimulationScreenWidgets.MUTED);
    }
    @Override protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        FittedTextRenderer.left(graphics, font, title, 12, 224, 8, 16, SimulationScreenWidgets.TEXT, true);
        Component classification = Component.translatable("gui.trading_cells.simulation." + (menu.highLevel() ? "high_level" : "normal_level"));
        FittedTextRenderer.centered(graphics, font, classification, 12, 224, 27, 13, SimulationScreenWidgets.ACCENT, false);
        FittedTextRenderer.centered(graphics, font, Component.translatable("gui.trading_cells.simulation.requirements",
                menu.requiredShards(), menu.requiredMaterial().getCount(), menu.experienceCost()),
                12, 224, 73, 13, SimulationScreenWidgets.MUTED, false);
        graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, SimulationScreenWidgets.MUTED, false);
    }
}
