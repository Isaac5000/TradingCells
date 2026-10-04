package com.cosmocraft.trading_cells.feature.farmer.adapters.input;

import com.cosmocraft.trading_cells.platform.neoforge.machine.MachineToolEnchantments;
import com.cosmocraft.trading_cells.platform.neoforge.item.StoredEnchantmentLookup;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

public final class FarmerEnchantments {
    public static final ResourceKey<Enchantment> FARMERS_TOUCH = MachineToolEnchantments.FARMERS_TOUCH;

    private FarmerEnchantments() {
    }

    public static boolean protectsHoe(ItemStack stack, HolderLookup.Provider registries) {
        return MachineToolEnchantments.farmersTouch(stack, registries);
    }

    public static boolean isStoredOnBook(ItemStack stack) {
        return StoredEnchantmentLookup.contains(stack, FARMERS_TOUCH);
    }
}
