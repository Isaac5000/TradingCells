package com.cosmocraft.trading_cells.feature.infusion.adapters.output.client;

import java.util.function.IntSupplier;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

final class InfuserControlButton extends Button {
    private final boolean lock;
    private final IntSupplier state;

    InfuserControlButton(int x, int y, boolean lock, IntSupplier state, OnPress action) {
        super(x, y, 18, 18, Component.empty(), action, DEFAULT_NARRATION);
        this.lock = lock;
        this.state = state;
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        extractDefaultSprite(graphics);
        int x = getX(), y = getY(), value = state.getAsInt();
        int color = !active ? 0xFF858585 : value == 2 ? 0xFFFF7474 : 0xFFE3E5DD;
        if (lock) {
            graphics.fill(x + 5, y + 8, x + 13, y + 14, color);
            graphics.fill(x + 7, y + 3, x + 12, y + 5, color);
            graphics.fill(x + 5, y + 4, x + 7, y + 9, color);
            if (value != 0) { graphics.fill(x + 11, y + 4, x + 13, y + 9, color); }
            graphics.fill(x + 8, y + 10, x + 10, y + 12, 0xFF343A3D);
        } else {
            graphics.fill(x + 4, y + 3, x + 14, y + 15, color);
            graphics.fill(x + 5, y + 4, x + 13, y + 14, 0xFF343A3D);
            graphics.fill(x + 6, y + (value == 0 ? 10 : 5), x + 12, y + 13, 0xFF80FF20);
        }
    }
}
