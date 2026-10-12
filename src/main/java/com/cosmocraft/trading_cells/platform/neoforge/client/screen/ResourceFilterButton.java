package com.cosmocraft.trading_cells.platform.neoforge.client.screen;

import java.util.function.BooleanSupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class ResourceFilterButton extends Button {
    private final BooleanSupplier whitelist;

    public ResourceFilterButton(int x, int y, BooleanSupplier whitelist, OnPress press) {
        super(x, y, 18, 18, label(whitelist.getAsBoolean()), press, supplier -> supplier.get());
        this.whitelist = whitelist;
    }

    public static Component label(boolean whitelist) {
        return Component.translatable("gui.trading_cells.pipe.filter.mode." + (whitelist ? "whitelist" : "blacklist"));
    }

    public static void draw(GuiGraphicsExtractor graphics, int x, int y, boolean whitelist, boolean hovered) {
        graphics.fill(x, y, x + 18, y + 18, hovered ? 0xFFBCC6CA : 0xFF727D82);
        graphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFF343C40);
        graphics.fakeItem(new ItemStack(whitelist ? Items.PAPER : Items.COAL), x + 1, y + 1);
    }

    @Override protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        boolean mode = whitelist.getAsBoolean();
        setMessage(label(mode));
        draw(graphics, getX(), getY(), mode, isHoveredOrFocused());
        if (isHoveredOrFocused()) {
            graphics.setTooltipForNextFrame(Minecraft.getInstance().font, getMessage(), mouseX, mouseY);
        }
    }
}
