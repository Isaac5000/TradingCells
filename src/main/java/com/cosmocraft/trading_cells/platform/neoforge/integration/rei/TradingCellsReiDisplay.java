package com.cosmocraft.trading_cells.platform.neoforge.integration.rei;

import java.util.List;
import java.util.Optional;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.DisplaySerializer;
import me.shedaniel.rei.api.common.display.basic.BasicDisplay;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/** Client-only REI representation of one Trading Cells machine operation. */
public final class TradingCellsReiDisplay extends BasicDisplay {
    private final CategoryIdentifier<TradingCellsReiDisplay> category;
    private final TradingCellsReiLayout layout;
    private final List<EntryIngredient> requiredEntries;
    private final int durationTicks;
    private final List<Component> notes;
    private final Optional<OutputAmount> outputAmount;

    public TradingCellsReiDisplay(
            CategoryIdentifier<TradingCellsReiDisplay> category,
            TradingCellsReiLayout layout,
            Identifier location,
            List<EntryIngredient> inputs,
            List<EntryIngredient> requiredEntries,
            List<EntryIngredient> outputs,
            int durationTicks,
            List<Component> notes,
            Optional<OutputAmount> outputAmount
    ) {
        super(List.copyOf(inputs), List.copyOf(outputs), Optional.of(location));
        this.category = category;
        this.layout = layout;
        this.requiredEntries = List.copyOf(requiredEntries);
        this.durationTicks = Math.max(0, durationTicks);
        this.notes = List.copyOf(notes);
        this.outputAmount = outputAmount.filter(OutputAmount::isVisible);
    }

    @Override
    public List<EntryIngredient> getRequiredEntries() {
        return requiredEntries;
    }

    @Override
    public CategoryIdentifier<TradingCellsReiDisplay> getCategoryIdentifier() {
        return category;
    }

    public int durationTicks() {
        return durationTicks;
    }

    public TradingCellsReiLayout layout() {
        return layout;
    }

    public List<Component> notes() {
        return notes;
    }

    public Optional<OutputAmount> outputAmount() {
        return outputAmount;
    }

    @Override
    public @Nullable DisplaySerializer<? extends TradingCellsReiDisplay> getSerializer() {
        return null;
    }

    public record OutputAmount(int minimum, int maximum) {
        public OutputAmount {
            if (minimum < 1 || maximum < minimum) {
                throw new IllegalArgumentException("Invalid REI output amount");
            }
        }

        public boolean isVisible() {
            return maximum > 1;
        }

        public String label() {
            return minimum == maximum ? "x" + maximum : minimum + "-" + maximum;
        }
    }
}
