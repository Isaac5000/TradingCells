package com.cosmocraft.trading_cells.feature.mobfarm.adapters.output.client;

import java.util.function.BooleanSupplier;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

final class SimulationScreenWidgets {
    static final int TEXT = 0xFFF0F3F4;
    static final int MUTED = 0xFFB8C1C4;
    static final int ACCENT = 0xFF62D6D3;
    private SimulationScreenWidgets() { }

    static void background(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, 0xFF899296);
        graphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, 0xFF2B3236);
    }
    static void slot(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xFF727D82);
        graphics.fill(x, y, x + 16, y + 16, 0xFF343C40);
    }
    static void check(GuiGraphicsExtractor graphics, int x, int y, boolean checked) {
        graphics.fill(x, y, x + 11, y + 11, 0xFF899296);
        graphics.fill(x + 1, y + 1, x + 10, y + 10, 0xFF1C2427);
        if (checked) {
            graphics.fill(x + 2, y + 5, x + 4, y + 7, ACCENT);
            graphics.fill(x + 4, y + 7, x + 6, y + 9, ACCENT);
            graphics.fill(x + 6, y + 5, x + 8, y + 7, ACCENT);
            graphics.fill(x + 8, y + 3, x + 10, y + 5, ACCENT);
        }
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
