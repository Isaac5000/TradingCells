package com.cosmocraft.trading_cells.feature.mobfarm.adapters.output.client;

import com.cosmocraft.trading_cells.feature.mobfarm.adapters.input.MobFarmMenu;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.FittedTextRenderer;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.MachineScreenUtil;
import com.cosmocraft.trading_cells.platform.neoforge.network.MobSimulationFilterPayload;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public final class MobFarmScreen extends AbstractContainerScreen<MobFarmMenu> {
    private static final int VISIBLE_ROWS = 7;
    private int scroll;
    private boolean probabilities;
    private boolean filterClick;

    public MobFarmScreen(MobFarmMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, MobFarmMenu.WIDTH, MobFarmMenu.HEIGHT);
        inventoryLabelX = MobFarmMenu.PLAYER_X;
        inventoryLabelY = MobFarmMenu.PLAYER_Y - 12;
    }

    @Override protected void init() {
        super.init();
        addRenderableWidget(SimulationScreenWidgets.checkButton(leftPos + 344, topPos + 8, label("enabled"),
                menu::enabled, button -> command(0)));
        var chance = addRenderableWidget(Button.builder(Component.literal("%"), button -> probabilities = !probabilities)
                .bounds(leftPos + 161, topPos + 28, 18, 18).build());
        chance.setTooltip(Tooltip.create(label("probabilities")));
        var xp = addRenderableWidget(Button.builder(Component.literal("XP"), button -> command(1))
                .bounds(leftPos + 147, topPos + 220, 32, 18).build());
        xp.setTooltip(Tooltip.create(Component.translatable("button.trading_cells.withdraw_xp")));
    }

    private void command(int id) {
        if (minecraft.gameMode != null) { minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id); }
    }

    @Override protected void containerTick() {
        super.containerTick();
        scroll = Math.clamp(scroll, 0, Math.max(0, menu.lootEntries().size() - VISIBLE_ROWS));
    }

    @Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        SimulationScreenWidgets.background(graphics, leftPos, topPos, imageWidth, imageHeight);
        for (var slot : menu.slots) { SimulationScreenWidgets.slot(graphics, leftPos + slot.x, topPos + slot.y); }
        graphics.fill(leftPos + 190, topPos + 28, leftPos + 191, topPos + 237, 0xFF566166);
        graphics.fill(leftPos + 199, topPos + 61, leftPos + 361, topPos + 67, 0xFF161D20);
        int progress = (int) Math.clamp((long) menu.cycleTicks() * 160 / Math.max(1, menu.cycleDurationTicks()), 0, 160);
        graphics.fill(leftPos + 200, topPos + 62, leftPos + 200 + progress, topPos + 66, SimulationScreenWidgets.ACCENT);
        var entries = menu.lootEntries();
        for (int row = 0; row < VISIBLE_ROWS && row + scroll < entries.size(); row++) {
            var entry = entries.get(row + scroll);
            int y = topPos + 51 + row * 22;
            graphics.fill(leftPos + 11, y, leftPos + 180, y + 21, 0xFF343C40);
            SimulationScreenWidgets.check(graphics, leftPos + 15, y + 5, entry.enabled());
            graphics.fakeItem(entry.stack(), leftPos + 32, y + 2);
            FittedTextRenderer.left(graphics, font, entry.stack().getHoverName(), leftPos + 53, leftPos + 176,
                    y + (probabilities ? 1 : 5), 11, SimulationScreenWidgets.TEXT, false);
            if (probabilities) {
                FittedTextRenderer.left(graphics, font, chance(entry), leftPos + 53, leftPos + 176, y + 11, 9,
                        SimulationScreenWidgets.ACCENT, false);
            }
        }
        int maximum = Math.max(0, entries.size() - VISIBLE_ROWS);
        if (maximum > 0) {
            int thumb = Math.max(12, 154 * VISIBLE_ROWS / entries.size());
            int y = topPos + 51 + scroll * (154 - thumb) / maximum;
            graphics.fill(leftPos + 182, y, leftPos + 186, y + thumb, SimulationScreenWidgets.MUTED);
        }
    }

    @Override protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        FittedTextRenderer.left(graphics, font, title, 12, 333, 8, 16, SimulationScreenWidgets.TEXT, true);
        graphics.text(font, label("loot"), 12, 33, SimulationScreenWidgets.MUTED, false);
        graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, SimulationScreenWidgets.MUTED, false);
        FittedTextRenderer.left(graphics, font, label("cycle", menu.simulatedKills(), Math.max(1, MachineScreenUtil.durationSeconds(menu.cycleDurationTicks()))),
                199, 361, 70, 12, SimulationScreenWidgets.MUTED, false);
        FittedTextRenderer.left(graphics, font, Component.literal(Integer.toString(menu.storedExperience()) + " XP"),
                12, 143, 222, 14, 0xFFC4EA83, false);
        if (menu.lootEntries().isEmpty()) {
            FittedTextRenderer.left(graphics, font, label(menu.creature().isEmpty() ? "module_required" : "no_loot"),
                    12, 177, 56, 22, SimulationScreenWidgets.MUTED, false);
        }
        if (menu.pendingOutput()) {
            FittedTextRenderer.left(graphics, font, label("output_waiting"), 199, 361, 127, 12, 0xFFFFCB82, false);
        }
    }

    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        int index = rowAt(mouseX, mouseY);
        if (index >= 0) {
            var entry = menu.lootEntries().get(index);
            graphics.setComponentTooltipForNextFrame(font, List.of(entry.stack().getHoverName(), chance(entry)),
                    mouseX, mouseY, entry.stack());
        }
        for (int slot = 0; slot < 5; slot++) {
            var input = menu.getSlot(slot);
            if (input.getItem().isEmpty() && isHovering(input.x, input.y, 16, 16, mouseX, mouseY)) {
                graphics.setComponentTooltipForNextFrame(font, List.of(label("slot." + slot)), mouseX, mouseY, ItemStack.EMPTY);
            }
        }
    }

    private Component chance(MobFarmMenu.LootEntry entry) {
        if (entry.probability() < 0) { return label("chance_unknown"); }
        String percent = String.format(Locale.ROOT, "%.2f%%", entry.probability() / 10_000.0);
        return Component.literal(percent + "  " + entry.minimum() + "-" + entry.maximum());
    }

    private int rowAt(double x, double y) {
        if (x < leftPos + 11 || x >= leftPos + 180 || y < topPos + 51 || y >= topPos + 205) { return -1; }
        int index = scroll + (int) ((y - topPos - 51) / 22);
        return index < menu.lootEntries().size() ? index : -1;
    }

    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        filterClick = false;
        int index = rowAt(event.x(), event.y());
        if (index >= 0 && event.button() == 0) {
            var entry = menu.lootEntries().get(index);
            ClientPacketDistributor.sendToServer(new MobSimulationFilterPayload(menu.containerId, menu.lootRevision(),
                    BuiltInRegistries.ITEM.getKey(entry.stack().getItem())));
            filterClick = true;
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }
    @Override public boolean mouseReleased(MouseButtonEvent event) {
        if (filterClick) { filterClick = false; return true; }
        return super.mouseReleased(event);
    }
    @Override public boolean mouseScrolled(double x, double y, double dx, double dy) {
        if (x >= leftPos + 11 && x < leftPos + 187 && y >= topPos + 51 && y < topPos + 205) {
            scroll = Math.clamp(scroll - (int) Math.signum(dy), 0, Math.max(0, menu.lootEntries().size() - VISIBLE_ROWS));
            return true;
        }
        return super.mouseScrolled(x, y, dx, dy);
    }
    private static Component label(String key, Object... args) { return Component.translatable("gui.trading_cells.simulation." + key, args); }
}
