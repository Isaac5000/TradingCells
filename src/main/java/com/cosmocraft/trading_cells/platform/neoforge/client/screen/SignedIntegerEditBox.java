package com.cosmocraft.trading_cells.platform.neoforge.client.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

public final class SignedIntegerEditBox extends EditBox {
    public SignedIntegerEditBox(Font font, int x, int y, int width, int height, Component message) {
        super(font, x, y, width, height, message);
    }

    @Override
    public void insertText(String text) {
        if (text.chars().allMatch(character -> character == '-' || character >= '0' && character <= '9')) {
            super.insertText(text);
        }
    }

    @Override
    public void setValue(String value) {
        if (validPartial(value)) {
            super.setValue(value);
        }
    }

    private static boolean validPartial(String value) {
        if (value.isEmpty() || value.equals("-")) {
            return true;
        }
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }
}
