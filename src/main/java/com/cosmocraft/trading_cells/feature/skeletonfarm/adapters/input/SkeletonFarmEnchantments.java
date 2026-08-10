package com.cosmocraft.trading_cells.feature.skeletonfarm.adapters.input;

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

public final class SkeletonFarmEnchantments {
    public static final ResourceKey<Enchantment> WARRIORS_TOUCH = ResourceKey.create(
            Registries.ENCHANTMENT,
            Identifier.fromNamespaceAndPath(TradingCells.MOD_ID, "warriors_touch")
    );

    private SkeletonFarmEnchantments() {
    }

    public static boolean protectsSword(ItemStack stack, HolderLookup.Provider registries) {
        return level(stack, registries, WARRIORS_TOUCH) > 0;
    }

    public static int smiteLevel(ItemStack stack, HolderLookup.Provider registries) {
        return level(stack, registries, Enchantments.SMITE);
    }

    public static int lootingLevel(ItemStack stack, HolderLookup.Provider registries) {
        return level(stack, registries, Enchantments.LOOTING);
    }

    public static int sweepingEdgeLevel(ItemStack stack, HolderLookup.Provider registries) {
        return level(stack, registries, Enchantments.SWEEPING_EDGE);
    }

    public static boolean isStoredOnBook(ItemStack stack, HolderLookup.Provider registries) {
        if (!stack.is(Items.ENCHANTED_BOOK)) {
            return false;
        }
        ItemEnchantments stored = stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
        return registries.lookup(Registries.ENCHANTMENT)
                .flatMap(enchantments -> enchantments.get(WARRIORS_TOUCH))
                .map(enchantment -> stored.getLevel(enchantment) > 0)
                .orElse(false);
    }

    private static int level(
            ItemStack stack,
            HolderLookup.Provider registries,
            ResourceKey<Enchantment> key
    ) {
        if (stack.isEmpty()) {
            return 0;
        }
        return registries.lookup(Registries.ENCHANTMENT)
                .flatMap(enchantments -> enchantments.get(key))
                .map(stack::getEnchantmentLevel)
                .orElse(0);
    }
}
