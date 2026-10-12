package com.cosmocraft.trading_cells.feature.combat.adapters.output.client;

import com.cosmocraft.trading_cells.feature.combat.adapters.minecraft.StormStationMenu;
import com.cosmocraft.trading_cells.feature.combat.domain.model.StormChargeRules;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.FittedTextRenderer;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.MachineContainerScreen;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.trader.VillagerTradeScreenCommon;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public final class StormStationScreen extends MachineContainerScreen<StormStationMenu> {
    private static final Identifier BACKGROUND = Identifier.fromNamespaceAndPath("trading_cells", "textures/gui/container/storm_station/background.png");
    public StormStationScreen(StormStationMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 216, 180);
        inventoryLabelX = 28;
        inventoryLabelY = 84;
    }
    @Override public void extractBackground(GuiGraphicsExtractor graphics, int x, int y, float partial) {
        super.extractBackground(graphics, x, y, partial);
        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);
        for (var slot : menu.slots) {
            if (!slot.isActive()) { continue; }
            int sx = leftPos + slot.x, sy = topPos + slot.y;
            graphics.fill(sx - 1, sy - 1, sx + 17, sy + 17, 0xFF71818C);
            graphics.fill(sx, sy, sx + 16, sy + 16, 0xFF172229);
        }
        int ax = leftPos + 98, ay = topPos + 48;
        VillagerTradeScreenCommon.drawEssenceArrow(graphics, ax, ay, 0xFF74838A);
        int width = 24 * menu.progress() / StormChargeRules.TRANSFER_TICKS;
        if (width > 0) {
            graphics.enableScissor(ax, ay, ax + width, ay + 16);
            VillagerTradeScreenCommon.drawEssenceArrow(graphics, ax, ay, 0xFF35CFF2);
            graphics.disableScissor();
        }
    }
    @Override protected void extractLabels(GuiGraphicsExtractor graphics, int x, int y) {
        FittedTextRenderer.left(graphics, font, title, 12, 204, 10, 16, 0xFFF0F3F4, true);
        FittedTextRenderer.centered(graphics, font, Component.translatable("tooltip.trading_cells.storm.charge", menu.charge(), menu.capacity()),
                12, 204, 30, 12, 0xFF35CFF2, false);
        graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFFB8C1C4, false);
    }
}
