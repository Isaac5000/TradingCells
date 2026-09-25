package com.cosmocraft.trading_cells.platform.neoforge.integration.rei;

import com.cosmocraft.trading_cells.feature.mobfarm.adapters.output.MobFarmRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.mobfarm.domain.model.EssenceTier;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.FittedTextRenderer;
import java.util.ArrayList;
import java.util.List;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public record EssenceReiCategory(boolean synthesis) implements DisplayCategory<EssenceReiDisplay> {
    @Override public CategoryIdentifier<EssenceReiDisplay> getCategoryIdentifier() {
        return synthesis ? EssenceReiDisplay.SYNTHESIS : EssenceReiDisplay.STABILIZATION;
    }
    @Override public Component getTitle() {
        return Component.translatable("block.trading_cells." + (synthesis ? "essence_workbench" : "essence_stabilizer"));
    }
    @Override public Renderer getIcon() {
        return EntryStacks.of(synthesis ? MobFarmRegistrationAdapter.WORKBENCH_ITEM.get() : MobFarmRegistrationAdapter.STABILIZER_ITEM.get());
    }
    @Override public List<Widget> setupDisplay(EssenceReiDisplay display, Rectangle bounds) {
        List<Widget> widgets = new ArrayList<>();
        widgets.add(Widgets.createDrawableWidget((graphics, mouseX, mouseY, delta) -> {
            graphics.fill(bounds.x, bounds.y, bounds.getMaxX(), bounds.getMaxY(), 0xFF151B1D);
            graphics.fill(bounds.x + 1, bounds.y + 1, bounds.getMaxX() - 1, bounds.getMaxY() - 1, 0xFF2C3438);
            var font = Minecraft.getInstance().font;
            FittedTextRenderer.centered(graphics, font, Component.translatable("gui.trading_cells.essence.tier", EssenceTier.fromId(display.tier()).name()),
                    bounds.x + 8, bounds.getMaxX() - 8, bounds.y + 8, 12, 0xFF6DDBEF, false);
            graphics.fill(bounds.x + 87, bounds.y + 36, bounds.x + 107, bounds.y + 40, 0xFFADB9BC);
            for (int i = 0; i < 4; i++) { graphics.fill(bounds.x + 107 + i, bounds.y + 32 + i, bounds.x + 108 + i, bounds.y + 44 - i, 0xFFADB9BC); }
            Component cost = Component.translatable(synthesis ? "rei.trading_cells.arcane_experience" : "rei.trading_cells.essence.duration", display.cost());
            FittedTextRenderer.centered(graphics, font, cost, bounds.x + 6, bounds.getMaxX() - 6, bounds.y + 62, 12, 0xFFB5ED92, false);
        }));
        for (int i = 0; i < display.getInputEntries().size(); i++) {
            widgets.add(Widgets.createSlot(new Rectangle(bounds.x + 12 + i * 23, bounds.y + 29, 18, 18))
                    .entries(display.getInputEntries().get(i)).markInput());
        }
        for (int i = 0; i < display.getOutputEntries().size(); i++) {
            widgets.add(Widgets.createSlot(new Rectangle(bounds.x + 123 + i * 23, bounds.y + 29, 18, 18))
                    .entries(display.getOutputEntries().get(i)).markOutput());
        }
        return widgets;
    }
    @Override public int getDisplayHeight() { return 82; }
    @Override public int getDisplayWidth(EssenceReiDisplay display) { return 200; }
}
