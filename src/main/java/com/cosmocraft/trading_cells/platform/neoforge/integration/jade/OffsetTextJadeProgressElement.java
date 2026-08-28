package com.cosmocraft.trading_cells.platform.neoforge.integration.jade;

import com.cosmocraft.trading_cells.platform.neoforge.client.screen.FittedTextRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import snownee.jade.api.ui.BoxStyle;
import snownee.jade.api.ui.JadeUI;
import snownee.jade.api.ui.ResizeableElement;
import snownee.jade.api.view.ProgressView;

/** Keeps Jade's progress bar intact while allowing its centered text to use a separate offset. */
final class OffsetTextJadeProgressElement extends ResizeableElement {
    private static final int TEXT_Y_OFFSET = 1;
    private final ProgressView view;
    private final Component text;
    private ResizeableElement bar;

    OffsetTextJadeProgressElement(float completion, int color, Component text, int width, int height) {
        this.view = new ProgressView(
                ProgressView.Part.of(completion, color),
                null,
                JadeUI.progressStyle(),
                BoxStyle.nestedBox()
        );
        this.text = text;
        setFreeSpace(width, height);
    }

    @Override
    public void setFreeSpace(int width, int height) {
        this.width = width;
        this.height = height;
        this.bar = JadeUI.progress(view, width, height);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        bar.setX(getX());
        bar.setY(getY());
        bar.extractRenderState(graphics, mouseX, mouseY, partialTick);

        var font = Minecraft.getInstance().font;
        FittedTextRenderer.centered(
                graphics,
                font,
                text,
                getX() + 2,
                getX() + width - 2,
                getY() + TEXT_Y_OFFSET,
                font.lineHeight,
                0xFFFFFFFF,
                true
        );
    }

    @Override
    public Component getNarration() {
        return text;
    }
}
