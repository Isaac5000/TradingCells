package com.cosmocraft.trading_cells.feature.mobfarm.adapters.output.client;

import com.cosmocraft.trading_cells.feature.mobfarm.adapters.input.EssenceWorkbenchMenu;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.FittedTextRenderer;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.NonNegativeIntegerEditBox;
import com.cosmocraft.trading_cells.platform.neoforge.network.ArcaneInfuserTransferPayload;
import com.cosmocraft.trading_cells.shared.machines.domain.model.MinecraftExperience;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

public final class EssenceWorkbenchScreen extends AbstractContainerScreen<EssenceWorkbenchMenu> {
    private Button mode, deposit, withdraw;
    private EditBox levels;
    public EssenceWorkbenchScreen(EssenceWorkbenchMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, EssenceWorkbenchMenu.WIDTH, EssenceWorkbenchMenu.HEIGHT);
        inventoryLabelX = 84;
        inventoryLabelY = 146;
    }
    private void action(int action) {
        if (minecraft.gameMode != null) { minecraft.gameMode.handleInventoryButtonClick(menu.containerId, action); }
    }
    @Override protected void init() {
        super.init();
        levels = addRenderableWidget(new NonNegativeIntegerEditBox(font, leftPos + 160, topPos + 80, 160, 14,
                Component.translatable("gui.trading_cells.experience_amount")));
        levels.setMaxLength(10);
        levels.setHint(Component.translatable("gui.trading_cells.experience_amount_hint_short"));
        levels.setTooltip(Tooltip.create(Component.translatable("gui.trading_cells.experience_amount_hint")));
        deposit = addRenderableWidget(Button.builder(Component.translatable("button.trading_cells.deposit_xp"), b -> transfer(false))
                .bounds(leftPos + 160, topPos + 99, 78, 18).build());
        withdraw = addRenderableWidget(Button.builder(Component.translatable("button.trading_cells.withdraw_xp"), b -> transfer(true))
                .bounds(leftPos + 242, topPos + 99, 78, 18).build());
        mode = addRenderableWidget(Button.builder(modeLabel(), button -> action(5))
                .bounds(leftPos + 160, topPos + 120, 160, 18).build());
        addRenderableWidget(SimulationScreenWidgets.checkButton(leftPos + 12, topPos + 112,
                Component.translatable("gui.trading_cells.essence.automatic"), menu::automatic, button -> action(6)));
        updateButtons();
    }
    private void transfer(boolean extracting) {
        String text = levels.getValue();
        int amount;
        try { amount = text.isEmpty() ? 0 : Integer.parseInt(text); }
        catch (NumberFormatException ignored) { return; }
        byte action = (byte) ((extracting ? 2 : 0) + (text.isEmpty() ? 1 : 0));
        if (text.isEmpty() || amount > 0) {
            ClientPacketDistributor.sendToServer(new ArcaneInfuserTransferPayload(menu.containerId, action, amount));
        }
    }
    private Component modeLabel() {
        return Component.translatable("gui.trading_cells.xp." + (menu.fillStorage() ? "fill_storage" : "active_recipe"));
    }
    private void updateButtons() {
        mode.setMessage(modeLabel());
        deposit.active = minecraft.player != null && MinecraftExperience.totalPoints(minecraft.player.experienceLevel,
                minecraft.player.experienceProgress) > 0 && menu.storedExperience() < Integer.MAX_VALUE;
        withdraw.active = menu.storedExperience() > 0;
    }
    @Override protected void containerTick() { super.containerTick(); updateButtons(); }
    @Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        SimulationScreenWidgets.background(graphics, leftPos, topPos, imageWidth, imageHeight);
        for (var slot : menu.slots) {
            if (slot.isActive()) { SimulationScreenWidgets.slot(graphics, leftPos + slot.x, topPos + slot.y); }
        }
        SimulationScreenWidgets.arrow(graphics, leftPos + 86, topPos + 50, 0);
        summary(graphics, 160, "gui.trading_cells.storage_summary", menu.storedExperience(),
                MinecraftExperience.levelForTotalPoints(menu.storedExperience()));
        if (minecraft.player != null) {
            summary(graphics, 242, "gui.trading_cells.player_summary",
                    MinecraftExperience.totalPoints(minecraft.player.experienceLevel, minecraft.player.experienceProgress),
                    minecraft.player.experienceLevel);
        }
    }
    private void summary(GuiGraphicsExtractor graphics, int x, String title, int xp, int level) {
        SimulationScreenWidgets.background(graphics, leftPos + x, topPos + 28, 78, 36);
        FittedTextRenderer.centered(graphics, font, Component.translatable(title),
                leftPos + x + 3, leftPos + x + 75, topPos + 31, 10, SimulationScreenWidgets.TEXT, false);
        FittedTextRenderer.centered(graphics, font, Component.translatable("gui.trading_cells.arcane_experience", xp),
                leftPos + x + 3, leftPos + x + 75, topPos + 42, 9, 0xFF94F42B, false);
        FittedTextRenderer.centered(graphics, font, Component.translatable("gui.trading_cells.arcane_levels", level),
                leftPos + x + 3, leftPos + x + 75, topPos + 52, 9, 0xFF94F42B, false);
    }
    @Override protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        FittedTextRenderer.left(graphics, font, title, 12, 320, 8, 16, SimulationScreenWidgets.TEXT, true);
        graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, SimulationScreenWidgets.MUTED, false);
        FittedTextRenderer.left(graphics, font, Component.translatable("gui.trading_cells.essence.automatic"),
                32, 145, 117, 12, SimulationScreenWidgets.TEXT, false);
        FittedTextRenderer.centered(graphics, font, Component.translatable("gui.trading_cells.arcane_amount_label"),
                160, 320, 68, 10, SimulationScreenWidgets.TEXT, false);
        if (menu.experienceCost() > 0) {
            FittedTextRenderer.centered(graphics, font, Component.literal(menu.experienceCost() + " XP"),
                    12, 144, 76, 12, SimulationScreenWidgets.ACCENT, false);
        }
    }
}
