package com.cosmocraft.trading_cells.feature.logistics.adapters.input;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;

public final class NetworkCraftingCatalog {
    private NetworkCraftingCatalog() {
    }

    public static List<Entry> create(ServerLevel level) {
        List<Entry> entries = new ArrayList<>();
        var context = SlotDisplayContext.fromLevel(level);
        for (RecipeHolder<?> holder : level.recipeAccess().getRecipes()) {
            if (!(holder.value() instanceof CraftingRecipe)) {
                continue;
            }
            try {
                for (RecipeDisplay display : holder.value().display()) {
                    List<SlotDisplay> ingredients;
                    int width;
                    if (display instanceof ShapedCraftingRecipeDisplay shaped
                            && shaped.width() <= 3 && shaped.height() <= 3) {
                        ingredients = shaped.ingredients();
                        width = shaped.width();
                    } else if (display instanceof ShapelessCraftingRecipeDisplay shapeless
                            && shapeless.ingredients().size() <= 9) {
                        ingredients = shapeless.ingredients();
                        width = 3;
                    } else {
                        continue;
                    }
                    ItemStack output = display.result().resolveForFirstStack(context);
                    if (!output.isEmpty()) {
                        entries.add(new Entry(holder.id().identifier(), output.copy(), ingredients, width));
                    }
                    break;
                }
            } catch (RuntimeException ignored) {
                // A malformed optional recipe display does not disable the catalog.
            }
        }
        return List.copyOf(entries);
    }

    public record Entry(Identifier id, ItemStack result, List<SlotDisplay> ingredients, int width) {
        public List<ItemStack> grid(ServerLevel level, List<ItemStack> available) {
            var context = SlotDisplayContext.fromLevel(level);
            List<ItemStack> grid = new ArrayList<>(Collections.nCopies(9, ItemStack.EMPTY));
            for (int index = 0; index < ingredients.size(); index++) {
                List<ItemStack> candidates = ingredients.get(index).resolveForStacks(context);
                ItemStack selected = candidates.stream().filter(candidate -> available.stream()
                        .anyMatch(stack -> ItemStack.isSameItemSameComponents(stack, candidate)))
                        .findFirst().orElse(candidates.isEmpty() ? ItemStack.EMPTY : candidates.getFirst());
                grid.set(index % width + index / width * 3, selected.copyWithCount(selected.isEmpty() ? 0 : 1));
            }
            return List.copyOf(grid);
        }
    }
}
