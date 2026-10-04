package com.cosmocraft.trading_cells.platform.neoforge.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/** Tooltip indexing can run without a world or registry provider. Stored holders already carry their keys. */
public final class StoredEnchantmentLookup {
    private StoredEnchantmentLookup() { }

    public static boolean contains(ItemStack stack, ResourceKey<Enchantment> key) {
        return stack.is(Items.ENCHANTED_BOOK)
                && stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY).entrySet().stream()
                .anyMatch(entry -> entry.getKey().is(key) && entry.getIntValue() > 0);
    }
}
