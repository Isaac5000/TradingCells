package com.cosmocraft.trading_cells.feature.farmer.adapters.input;

import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.TradingCells;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

public final class FarmerEnchantments {
    public static final ResourceKey<Enchantment> FARMERS_TOUCH = ResourceKey.create(
            Registries.ENCHANTMENT,
            Identifier.fromNamespaceAndPath(TradingCells.MOD_ID, "farmers_touch")
    );

    private FarmerEnchantments() {
    }

    public static boolean protectsHoe(ItemStack stack, HolderLookup.Provider registries) {
        var enchantmentLookup = registries.lookup(Registries.ENCHANTMENT);
        if (enchantmentLookup.isEmpty()) {
            return false;
        }
        var enchantment = enchantmentLookup.get().get(FARMERS_TOUCH);
        return enchantment.isPresent() && stack.getEnchantmentLevel(enchantment.get()) > 0;
    }

    public static boolean isStoredOnBook(ItemStack stack, HolderLookup.Provider registries) {
        if (!stack.is(Items.ENCHANTED_BOOK)) {
            return false;
        }

        ItemEnchantments storedEnchantments = stack.getOrDefault(
                DataComponents.STORED_ENCHANTMENTS,
                ItemEnchantments.EMPTY
        );
        if (storedEnchantments.isEmpty()) {
            return false;
        }

        var enchantmentLookup = registries.lookup(Registries.ENCHANTMENT);
        if (enchantmentLookup.isEmpty()) {
            return false;
        }
        var enchantment = enchantmentLookup.get().get(FARMERS_TOUCH);
        if (enchantment.isEmpty()) {
            return false;
        }

        return storedEnchantments.getLevel(enchantment.get()) > 0;
    }
}
