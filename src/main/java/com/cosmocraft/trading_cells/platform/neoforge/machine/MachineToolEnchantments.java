package com.cosmocraft.trading_cells.platform.neoforge.machine;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

/** Tool protection shared by crop and entity-interaction machines. */
public final class MachineToolEnchantments {
    public static final ResourceKey<Enchantment> FARMERS_TOUCH = ResourceKey.create(Registries.ENCHANTMENT,
            Identifier.fromNamespaceAndPath("trading_cells", "farmers_touch"));

    private MachineToolEnchantments() { }

    public static boolean farmersTouch(ItemStack stack, HolderLookup.Provider registries) {
        return registries.lookup(Registries.ENCHANTMENT).flatMap(lookup -> lookup.get(FARMERS_TOUCH))
                .map(enchantment -> stack.getEnchantmentLevel(enchantment) > 0).orElse(false);
    }
}
