package com.cosmocraft.trading_cells.platform.neoforge.integration.jade;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import snownee.jade.api.ui.ResizeableElement;

/** Vanilla XP orb rendered as a compact Jade row icon. */
final class ExperienceOrbJadeElement extends ResizeableElement {
    static final int SIZE = 16;
    private static final Identifier ORB_TEXTURE =
            Identifier.withDefaultNamespace("textures/entity/experience/experience_orb.png");

    ExperienceOrbJadeElement() {
        setFreeSpace(SIZE, SIZE);
    }

    @Override
    public void setFreeSpace(int width, int height) {
        this.width = SIZE;
        this.height = SIZE;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                ORB_TEXTURE,
                getX(),
                getY(),
                32.0F,
                0.0F,
                SIZE,
                SIZE,
                SIZE,
                SIZE,
                64,
                64,
                0xFF9CFF45
        );
    }

    @Override
    public Component getNarration() {
        return Component.empty();
    }
}
