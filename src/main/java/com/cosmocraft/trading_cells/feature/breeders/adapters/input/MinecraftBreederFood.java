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
            new Option(Items.COOKED_PORKCHOP, BreederFood.COOKED_PORKCHOP),
            new Option(Items.NETHER_WART_BLOCK, BreederFood.NETHER_WART_BLOCK),
            new Option(Items.PORKCHOP, BreederFood.RAW_PORKCHOP),
            new Option(Items.CRIMSON_FUNGUS, BreederFood.CRIMSON_FUNGUS),
            new Option(Items.NETHER_WART, BreederFood.NETHER_WART)
    );

    private MinecraftBreederFood() {
    }

    public static BreederFood from(BreederKind kind, ItemStack stack) {
        for (Option option : options(kind)) {
            if (stack.is(option.item())) {
                return option.food();
            }
        }
        return BreederFood.NONE;
    }

    public static List<Option> options(BreederKind kind) {
        return kind == BreederKind.VILLAGER ? VILLAGER_OPTIONS : PIGLIN_OPTIONS;
    }

    public record Option(Item item, BreederFood food) {
    }
}
