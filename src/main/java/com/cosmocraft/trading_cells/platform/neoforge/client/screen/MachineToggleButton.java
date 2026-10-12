package com.cosmocraft.trading_cells.platform.neoforge.client.screen;

import java.util.function.BooleanSupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** Common manual run/pause control; unrelated machine modes retain their own labels. */
public final class MachineToggleButton extends Button {
    private final BooleanSupplier checked;

    public MachineToggleButton(int x, int y, int width, BooleanSupplier checked, OnPress press) {
        super(x, y, width, 18, Component.translatable("gui.trading_cells.machine.enabled"), press, supplier -> supplier.get());
        this.checked = checked;
    }

    @Override protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        var font = Minecraft.getInstance().font;
        int right = getX() + width - 24;
        FittedTextRenderer.left(graphics, font, getMessage(), Math.max(getX(), right - font.width(getMessage())),
                right, getY(), 17, 0xFFB8C1C4, false);
        if (isHoveredOrFocused()) {
            graphics.fill(getX() + width - 16, getY() + 2, getX() + width - 3, getY() + 15, 0xFFF0F3F4);
        }
        drawCheck(graphics, getX() + width - 15, getY() + 3, checked.getAsBoolean());
    }

    public static void drawCheck(GuiGraphicsExtractor graphics, int x, int y, boolean checked) {
        graphics.fill(x, y, x + 11, y + 11, 0xFF899296);
        graphics.fill(x + 1, y + 1, x + 10, y + 10, 0xFF1C2427);
        if (checked) {
            graphics.fill(x + 2, y + 5, x + 4, y + 7, 0xFF62D6D3);
            graphics.fill(x + 4, y + 7, x + 6, y + 9, 0xFF62D6D3);
            graphics.fill(x + 6, y + 5, x + 8, y + 7, 0xFF62D6D3);
            graphics.fill(x + 8, y + 3, x + 10, y + 5, 0xFF62D6D3);
        }
    }
}
