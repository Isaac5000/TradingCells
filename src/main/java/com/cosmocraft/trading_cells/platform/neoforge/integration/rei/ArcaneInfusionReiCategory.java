package com.cosmocraft.trading_cells.platform.neoforge.integration.rei;

import com.cosmocraft.trading_cells.feature.infusion.adapters.output.ArcaneInfuserRegistrationAdapter;
import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.TradingCells;
import java.util.List;
import java.util.Optional;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** Client renderer for the server-synchronized Arcane Infusion displays. */
public final class ArcaneInfusionReiCategory implements DisplayCategory<ArcaneInfusionReiDisplay> {
    private static final CategoryIdentifier<TradingCellsReiDisplay> VISUAL_CATEGORY =
            CategoryIdentifier.of(TradingCells.MOD_ID, "arcane_infusion");

    @Override
    public CategoryIdentifier<ArcaneInfusionReiDisplay> getCategoryIdentifier() {
        return ArcaneInfusionReiDisplay.CATEGORY;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("category.trading_cells.arcane_infusion");
    }

    @Override
    public Renderer getIcon() {
        return EntryStacks.of(ArcaneInfuserRegistrationAdapter.ITEM.get());
    }

    @Override
    public List<Widget> setupDisplay(ArcaneInfusionReiDisplay display, Rectangle bounds) {
        Identifier location = display.getDisplayLocation().orElseThrow();
        TradingCellsReiDisplay visual = new TradingCellsReiDisplay(
                VISUAL_CATEGORY,
                TradingCellsReiLayout.ARCANE_INFUSION,
                location,
                display.getInputEntries(),
                display.getRequiredEntries(),
                display.getOutputEntries(),
                0,
                List.of(Component.translatable("rei.trading_cells.arcane_experience", display.experience())),
                Optional.empty()
        );
        return TradingCellsReiMachineDisplay.setup(visual, bounds);
    }

    @Override
    public int getDisplayHeight() {
        return TradingCellsReiMachineDisplay.HEIGHT;
    }

    @Override
    public int getDisplayWidth(ArcaneInfusionReiDisplay display) {
        return TradingCellsReiMachineDisplay.WIDTH;
    }

    @Override
    public int getMaximumDisplaysPerPage() {
        return 2;
    }
}
