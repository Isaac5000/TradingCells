package com.cosmocraft.trading_cells.feature.logistics.adapters.output.client;

import com.cosmocraft.trading_cells.feature.logistics.adapters.input.PipeConfigurationMenu;
import com.cosmocraft.trading_cells.feature.logistics.domain.model.LogisticsResourceType;
import com.cosmocraft.trading_cells.feature.logistics.domain.model.PipeFilterRule;
import com.cosmocraft.trading_cells.platform.neoforge.network.PipeChannelQueryPayload;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/** A small, debounced completion window; no client-wide channel catalogue. */
final class PipeChannelCompletion {
    private final PipeConfigurationMenu menu;
    private final LogisticsResourceType type;
    private final EditBox field;
    private final String parent;
    private String pending = "", requested;
    private int delay, first;

    PipeChannelCompletion(PipeConfigurationMenu menu, LogisticsResourceType type, EditBox field, String parent) {
        this.menu = menu; this.type = type; this.field = field; this.parent = parent;
    }

    void tick() {
        if (!field.active || !field.isFocused()) { close(); return; }
        String prefix = PipeFilterRule.searchPrefix(parent.isEmpty() ? field.getValue() : parent + "/" + field.getValue());
        if (!prefix.equals(pending)) { pending = prefix; delay = 5; first = 0; }
        if (delay > 0) { delay--; return; }
        if (!prefix.equals(requested)) {
            requested = prefix;
            ClientPacketDistributor.sendToServer(new PipeChannelQueryPayload(menu.containerId, type, prefix, true));
        }
    }

    void close() {
        if (requested != null) { ClientPacketDistributor.sendToServer(new PipeChannelQueryPayload(menu.containerId, type, "", false)); requested = null; }
    }

    private List<String> choices() {
        var result = menu.channelSuggestions();
        return field.isFocused() && requested != null && result != null && result.resource() == type && result.prefix().equals(requested)
                ? result.channels() : List.of();
    }

    boolean click(double x, double y) {
        var choices = choices();
        if (!inside(x, y, choices.size())) { return false; }
        int index = first + (int) (y - field.getY() - field.getHeight() - 1) / 11;
        if (index < choices.size()) {
            String value = choices.get(index);
            if (parent.isEmpty()) { field.setValue(value); }
            else if (value.startsWith(parent + "/")) { field.setValue(value.substring(parent.length() + 1)); }
            field.setFocused(false); close();
        }
        return true;
    }

    boolean scroll(double x, double y, double delta) {
        int count = choices().size();
        if (!inside(x, y, count)) { return false; }
        first = Math.clamp(first - (int) Math.signum(delta), 0, Math.max(0, count - 5)); return true;
    }

    void draw(GuiGraphicsExtractor graphics, Font font, int mouseX, int mouseY) {
        var choices = choices();
        int count = Math.min(5, choices.size() - first);
        if (count <= 0) { return; }
        graphics.nextStratum();
        int x = field.getX(), y = field.getY() + field.getHeight() + 1, width = field.getWidth();
        graphics.fill(x, y, x + width, y + count * 11, 0xFF111719);
        for (int row = 0; row < count; row++) {
            String value = choices.get(first + row);
            boolean hover = mouseX >= x && mouseX < x + width && mouseY >= y + row * 11 && mouseY < y + (row + 1) * 11;
            if (hover) { graphics.fill(x, y + row * 11, x + width, y + (row + 1) * 11, 0xFF506663); }
            graphics.text(font, font.plainSubstrByWidth(value, width - 6), x + 3, y + row * 11 + 1, 0xFFF5F5F5, false);
            if (hover) { graphics.setComponentTooltipForNextFrame(font, List.of(Component.literal(value)), mouseX, mouseY, ItemStack.EMPTY); }
        }
    }

    private boolean inside(double x, double y, int count) {
        return x >= field.getX() && x < field.getX() + field.getWidth() && y >= field.getY() + field.getHeight() + 1
                && y < field.getY() + field.getHeight() + 1 + Math.min(5, Math.max(0, count - first)) * 11;
    }
}
