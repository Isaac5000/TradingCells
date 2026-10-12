package com.cosmocraft.trading_cells.feature.mobfarm.adapters.output.client;

import com.cosmocraft.trading_cells.platform.neoforge.client.screen.MachineSlotGhosts;
import com.cosmocraft.trading_cells.feature.mobfarm.adapters.output.MobFarmRegistrationAdapter;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.MachineSlotSprites;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.MachineToggleButton;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.ResourceFilterButton;

import com.cosmocraft.trading_cells.feature.mobfarm.adapters.input.MobFarmMenu;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.FittedTextRenderer;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.MachineScreenUtil;
import com.cosmocraft.trading_cells.platform.neoforge.network.MobSimulationFilterPayload;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.MachineContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public final class MobFarmScreen extends MachineContainerScreen<MobFarmMenu> {
    private static final int VISIBLE_ROWS = 5;
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
        addRenderableWidget(new MachineToggleButton(leftPos + 174, topPos + 8, 163,
                menu::enabled, button -> command(0)));
        addRenderableWidget(new ResourceFilterButton(leftPos + 94, topPos + 28, menu::whitelist, button -> command(2)));
        addRenderableWidget(SimulationScreenWidgets.checkButton(leftPos + 136, topPos + 28, label("probabilities"),
                () -> probabilities, button -> probabilities = !probabilities));
        var xp = addRenderableWidget(Button.builder(Component.literal("XP"), button -> command(1))
                .bounds(leftPos + 122, topPos + 182, 32, 18).build());
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
        com.cosmocraft.trading_cells.platform.neoforge.client.screen.MachineGuiSkin.MOB_FARM.draw(graphics, leftPos, topPos, imageWidth, imageHeight);
        for (var slot : menu.slots) { SimulationScreenWidgets.slot(graphics, leftPos + slot.x, topPos + slot.y); }
        MachineSlotGhosts.sprite(graphics, menu.getSlot(0), leftPos, topPos, MachineSlotSprites.VILLAGER_HEAD);
        MachineSlotGhosts.item(graphics, menu.getSlot(1), leftPos, topPos, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_SWORD));
        MachineSlotGhosts.item(graphics, menu.getSlot(2), leftPos, topPos, MobFarmRegistrationAdapter.ENTITY_MODULE.get().getDefaultInstance());
        MachineSlotGhosts.item(graphics, menu.getSlot(3), leftPos, topPos, MobFarmRegistrationAdapter.UPGRADES.get(0).get().getDefaultInstance());
        MachineSlotGhosts.item(graphics, menu.getSlot(4), leftPos, topPos, MobFarmRegistrationAdapter.UPGRADES.get(5).get().getDefaultInstance());
        graphics.fill(leftPos + 165, topPos + 28, leftPos + 166, topPos + imageHeight - 9, 0xFF566166);
        graphics.fill(leftPos + 174, topPos + 48, leftPos + 336, topPos + 54, 0xFF161D20);
        int progress = (int) Math.clamp((long) menu.cycleTicks() * 160 / Math.max(1, menu.cycleDurationTicks()), 0, 160);
        graphics.fill(leftPos + 175, topPos + 49, leftPos + 175 + progress, topPos + 53, SimulationScreenWidgets.ACCENT);
        var entries = menu.lootEntries();
        for (int row = 0; row < VISIBLE_ROWS && row + scroll < entries.size(); row++) {
            var entry = entries.get(row + scroll);
            int y = topPos + 51 + row * 22;
            graphics.fill(leftPos + 11, y, leftPos + 155, y + 21, 0xFF343C40);
            SimulationScreenWidgets.check(graphics, leftPos + 15, y + 5, menu.whitelist() == entry.enabled());
            graphics.fakeItem(entry.stack(), leftPos + 32, y + 2);
            FittedTextRenderer.left(graphics, font, entry.stack().getHoverName(), leftPos + 53, leftPos + 151,
                    y + (probabilities ? 1 : 5), 11, SimulationScreenWidgets.TEXT, false);
            if (probabilities) {
                FittedTextRenderer.left(graphics, font, chance(entry), leftPos + 53, leftPos + 151, y + 11, 9,
                        SimulationScreenWidgets.ACCENT, false);
            }
        }
        int maximum = Math.max(0, entries.size() - VISIBLE_ROWS);
        if (maximum > 0) {
            int thumb = Math.max(12, 110 * VISIBLE_ROWS / entries.size());
            int y = topPos + 51 + scroll * (110 - thumb) / maximum;
            graphics.fill(leftPos + 157, y, leftPos + 161, y + thumb, SimulationScreenWidgets.MUTED);
        }
    }

    @Override protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        FittedTextRenderer.left(graphics, font, title, 12, 164, 8, 16, SimulationScreenWidgets.TEXT, true);
        graphics.text(font, label("loot"), 12, 33, SimulationScreenWidgets.MUTED, false);
        graphics.text(font, "%", 124, 33, SimulationScreenWidgets.MUTED, false);
        graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, SimulationScreenWidgets.MUTED, false);
        FittedTextRenderer.left(graphics, font, menu.pendingOutput() ? label("output_waiting")
                : label("cycle", menu.simulatedKills(), Math.max(1, MachineScreenUtil.durationSeconds(menu.cycleDurationTicks()))),
                12, 155, 164, 12, SimulationScreenWidgets.MUTED, false);
        FittedTextRenderer.left(graphics, font, Component.literal(Integer.toString(menu.storedExperience()) + " XP"),
                12, 120, 184, 14, 0xFFC4EA83, false);
        if (menu.lootEntries().isEmpty()) {
            FittedTextRenderer.left(graphics, font, label(menu.creature().isEmpty() ? "module_required" : "no_loot"),
                    12, 152, 56, 22, SimulationScreenWidgets.MUTED, false);
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
        if (x < leftPos + 11 || x >= leftPos + 155 || y < topPos + 51 || y >= topPos + 161) { return -1; }
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
        if (x >= leftPos + 11 && x < leftPos + 162 && y >= topPos + 51 && y < topPos + 161) {
            scroll = Math.clamp(scroll - (int) Math.signum(dy), 0, Math.max(0, menu.lootEntries().size() - VISIBLE_ROWS));
            return true;
        }
        return super.mouseScrolled(x, y, dx, dy);
    }
    private static Component label(String key, Object... args) { return Component.translatable("gui.trading_cells.simulation." + key, args); }
}
