package com.cosmocraft.trading_cells.platform.neoforge.integration.rei;

import java.util.List;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ItemLike;

/** Renders every process with the cropped work area of its real machine screen. */
public final class TradingCellsReiCategory implements DisplayCategory<TradingCellsReiDisplay> {
    public static final int DISPLAY_WIDTH = TradingCellsReiMachineDisplay.WIDTH;

    private final CategoryIdentifier<TradingCellsReiDisplay> identifier;
    private final Component title;
    private final Renderer icon;

    public TradingCellsReiCategory(
            CategoryIdentifier<TradingCellsReiDisplay> identifier,
            String titleKey,
            ItemLike icon
    ) {
        this.identifier = identifier;
        this.title = Component.translatable(titleKey);
        this.icon = EntryStacks.of(icon);
    }

    @Override
    public CategoryIdentifier<TradingCellsReiDisplay> getCategoryIdentifier() {
        return identifier;
    }

    @Override
    public Component getTitle() {
        return title;
    }

    @Override
    public Renderer getIcon() {
        return icon;
    }

    @Override
    public List<Widget> setupDisplay(TradingCellsReiDisplay display, Rectangle bounds) {
        return TradingCellsReiMachineDisplay.setup(display, bounds);
    }

    @Override
    public int getDisplayHeight() {
        return TradingCellsReiMachineDisplay.HEIGHT;
    }

    @Override
    public int getDisplayWidth(TradingCellsReiDisplay display) {
        return DISPLAY_WIDTH;
    }

    @Override
    public int getMaximumDisplaysPerPage() {
        return 2;
    }
}
