package com.cosmocraft.trading_cells.feature.converter.adapters.input;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;

public final class ConverterIngredientAdapter {
    private ConverterIngredientAdapter() {
    }

    public static boolean isWeaknessPotion(ItemStack stack) {
        if (!stack.is(Items.POTION)
                && !stack.is(Items.SPLASH_POTION)
                && !stack.is(Items.LINGERING_POTION)) {
            return false;
        }
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        return contents != null && (contents.is(Potions.WEAKNESS) || contents.is(Potions.LONG_WEAKNESS));
    }

    public static boolean isGoldenApple(ItemStack stack) {
        return stack.is(Items.GOLDEN_APPLE);
    }
}
