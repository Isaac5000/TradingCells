package com.cosmocraft.trading_cells.platform.neoforge.client.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

public final class NonNegativeIntegerEditBox extends EditBox {
    public NonNegativeIntegerEditBox(
            Font font,
            int x,
            int y,
            int width,
            int height,
            Component message
    ) {
        super(font, x, y, width, height, message);
    }

    @Override
    public void insertText(String text) {
        if (containsOnlyDigits(text)) {
            super.insertText(text);
        }
    }

    @Override
    public void setValue(String value) {
        if (containsOnlyDigits(value)) {
            super.setValue(value);
        }
    }

    private static boolean containsOnlyDigits(String value) {
        return value.chars().allMatch(Character::isDigit);
    }
}
