package com.cosmocraft.trading_cells.feature.quarry.adapters.input;

import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.TradingCells;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;

public final class QuarryEnchantments {
    public static final ResourceKey<Enchantment> MINERS_TOUCH = ResourceKey.create(
            Registries.ENCHANTMENT,
            Identifier.fromNamespaceAndPath(TradingCells.MOD_ID, "miners_touch")
    );

    private QuarryEnchantments() {
    }

    public static boolean protectsPickaxe(ItemStack stack, HolderLookup.Provider registries) {
        return enchantmentLevel(stack, registries, MINERS_TOUCH) > 0;
    }

    public static int fortuneLevel(ItemStack stack, HolderLookup.Provider registries) {
        return enchantmentLevel(stack, registries, Enchantments.FORTUNE);
    }

    public static boolean hasSilkTouch(ItemStack stack, HolderLookup.Provider registries) {
        return enchantmentLevel(stack, registries, Enchantments.SILK_TOUCH) > 0;
    }

    public static boolean isStoredOnBook(ItemStack stack, HolderLookup.Provider registries) {
        return isStoredOnBook(stack, registries, MINERS_TOUCH);
    }

    public static boolean isFortuneStoredOnBook(ItemStack stack, HolderLookup.Provider registries) {
        return isStoredOnBook(stack, registries, Enchantments.FORTUNE);
    }

    private static int enchantmentLevel(
            ItemStack stack,
            HolderLookup.Provider registries,
            ResourceKey<Enchantment> key
    ) {
        return registries.lookup(Registries.ENCHANTMENT)
                .flatMap(enchantments -> enchantments.get(key))
                .map(stack::getEnchantmentLevel)
                .orElse(0);
    }

    private static boolean isStoredOnBook(
            ItemStack stack,
            HolderLookup.Provider registries,
            ResourceKey<Enchantment> key
    ) {
        if (!stack.is(Items.ENCHANTED_BOOK)) {
            return false;
        }
        ItemEnchantments stored = stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
        return registries.lookup(Registries.ENCHANTMENT)
                .flatMap(enchantments -> enchantments.get(key))
                .map(enchantment -> stored.getLevel(enchantment) > 0)
                .orElse(false);
    }
}
