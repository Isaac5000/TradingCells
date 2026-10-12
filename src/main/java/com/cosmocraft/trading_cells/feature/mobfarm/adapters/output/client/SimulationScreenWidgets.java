package com.cosmocraft.trading_cells.feature.mobfarm.adapters.output.client;

import java.util.function.BooleanSupplier;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.trader.VillagerTradeScreenCommon;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

final class SimulationScreenWidgets {
    static final int TEXT = 0xFFF0F3F4;
    static final int MUTED = 0xFFB8C1C4;
    static final int ACCENT = 0xFF62D6D3;
    private SimulationScreenWidgets() { }

    static void slot(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xFF727D82);
        graphics.fill(x, y, x + 16, y + 16, 0xFF343C40);
    }
    static void arrow(GuiGraphicsExtractor graphics, int x, int y, float progress) {
        drawArrow(graphics, x, y, MUTED);
        int width = (int) (24 * Math.clamp(progress, 0, 1));
        if (width > 0) {
            graphics.enableScissor(x, y, x + width, y + 16);
            drawArrow(graphics, x, y, ACCENT);
            graphics.disableScissor();
        }
    }
    private static void drawArrow(GuiGraphicsExtractor graphics, int x, int y, int color) {
        VillagerTradeScreenCommon.drawEssenceArrow(graphics, x, y, color);
    }
    static void check(GuiGraphicsExtractor graphics, int x, int y, boolean checked) {
        com.cosmocraft.trading_cells.platform.neoforge.client.screen.MachineToggleButton.drawCheck(graphics, x, y, checked);
    }
    static Button checkButton(int x, int y, Component message, BooleanSupplier checked, Button.OnPress press) {
        Button button = new Button(x, y, 18, 18, message, press, supplier -> supplier.get()) {
            @Override protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
                if (isHoveredOrFocused()) {
                    graphics.fill(getX() + 2, getY() + 2, getX() + 15, getY() + 15, TEXT);
                }
                check(graphics, getX() + 3, getY() + 3, checked.getAsBoolean());
            }
        };
        button.setTooltip(Tooltip.create(message));
        return button;
    }
}
