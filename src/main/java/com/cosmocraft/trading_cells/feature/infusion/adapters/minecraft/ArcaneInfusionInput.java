package com.cosmocraft.trading_cells.feature.infusion.adapters.minecraft;

import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

public record ArcaneInfusionInput(List<ItemStack> items) implements RecipeInput {
    public static final int SIZE = 9;

    public ArcaneInfusionInput {
        if (items.size() != SIZE) {
            throw new IllegalArgumentException("Arcane infusion requires exactly nine input slots");
        }
        items = List.copyOf(items);
    }

    @Override
    public ItemStack getItem(int index) {
        return items.get(index);
    }

    @Override
    public int size() {
        return SIZE;
    }
}
