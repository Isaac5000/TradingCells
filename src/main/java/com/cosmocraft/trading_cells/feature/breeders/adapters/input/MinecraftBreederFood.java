package com.cosmocraft.trading_cells.feature.breeders.adapters.input;

import com.cosmocraft.trading_cells.feature.breeders.domain.model.BreederFood;
import com.cosmocraft.trading_cells.feature.breeders.domain.model.BreederKind;
import java.util.List;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class MinecraftBreederFood {
    private static final List<Option> VILLAGER_OPTIONS = List.of(
            new Option(Items.BREAD, BreederFood.BREAD),
            new Option(Items.CARROT, BreederFood.VEGETABLE),
            new Option(Items.POTATO, BreederFood.VEGETABLE),
            new Option(Items.BEETROOT, BreederFood.VEGETABLE)
    );
    private static final List<Option> PIGLIN_OPTIONS = List.of(
            new Option(Items.PORKCHOP, BreederFood.PORK),
            new Option(Items.COOKED_PORKCHOP, BreederFood.PORK),
            new Option(Items.CRIMSON_FUNGUS, BreederFood.CRIMSON_FUNGUS)
    );

    private MinecraftBreederFood() {
    }

    public static BreederFood from(ItemStack stack) {
        if (stack.is(Items.BREAD)) {
            return BreederFood.BREAD;
        }
        if (stack.is(Items.CARROT) || stack.is(Items.POTATO) || stack.is(Items.BEETROOT)) {
            return BreederFood.VEGETABLE;
        }
        if (stack.is(Items.PORKCHOP) || stack.is(Items.COOKED_PORKCHOP)) {
            return BreederFood.PORK;
        }
        if (stack.is(Items.CRIMSON_FUNGUS)) {
            return BreederFood.CRIMSON_FUNGUS;
        }
        return BreederFood.NONE;
    }

    public static List<Option> options(BreederKind kind) {
        return kind == BreederKind.VILLAGER ? VILLAGER_OPTIONS : PIGLIN_OPTIONS;
    }

    public record Option(Item item, BreederFood food) {
    }
}
