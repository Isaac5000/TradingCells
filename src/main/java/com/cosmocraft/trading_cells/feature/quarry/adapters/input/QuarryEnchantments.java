package com.cosmocraft.trading_cells.feature.quarry.adapters.input;

import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.TradingCells;
import com.cosmocraft.trading_cells.platform.neoforge.item.StoredEnchantmentLookup;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;

public final class QuarryEnchantments {
    public static final ResourceKey<Enchantment> MINER_TOUCH = ResourceKey.create(
            Registries.ENCHANTMENT,
            Identifier.fromNamespaceAndPath(TradingCells.MOD_ID, "miner_touch")
    );

    private QuarryEnchantments() {
    }

    public static boolean protectsPickaxe(ItemStack stack, HolderLookup.Provider registries) {
        return enchantmentLevel(stack, registries, MINER_TOUCH) > 0;
    }

    public static int fortuneLevel(ItemStack stack, HolderLookup.Provider registries) {
        return enchantmentLevel(stack, registries, Enchantments.FORTUNE);
    }

    public static boolean hasSilkTouch(ItemStack stack, HolderLookup.Provider registries) {
        return enchantmentLevel(stack, registries, Enchantments.SILK_TOUCH) > 0;
    }

    public static boolean isStoredOnBook(ItemStack stack) {
        return StoredEnchantmentLookup.contains(stack, MINER_TOUCH);
    }

    public static boolean isFortuneStoredOnBook(ItemStack stack) {
        return StoredEnchantmentLookup.contains(stack, Enchantments.FORTUNE);
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

}
