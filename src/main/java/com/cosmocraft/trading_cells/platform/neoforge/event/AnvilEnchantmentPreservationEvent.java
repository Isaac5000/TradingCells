package com.cosmocraft.trading_cells.platform.neoforge.event;

import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.TradingCells;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AnvilUpdateEvent;

/** Keeps pre-existing command-level enchantments intact when an anvil adds other enchantments. */
@EventBusSubscriber(modid = TradingCells.MOD_ID)
public final class AnvilEnchantmentPreservationEvent {
    private AnvilEnchantmentPreservationEvent() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onAnvilUpdate(AnvilUpdateEvent event) {
        ItemStack output = event.getOutput();
        if (output.isEmpty()) {
            return;
        }

        ItemEnchantments original = EnchantmentHelper.getEnchantmentsForCrafting(event.getLeft());
        ItemEnchantments.Mutable merged = new ItemEnchantments.Mutable(
                EnchantmentHelper.getEnchantmentsForCrafting(output)
        );
        boolean changed = false;
        for (var entry : original.entrySet()) {
            int originalLevel = entry.getIntValue();
            if (originalLevel <= entry.getKey().value().getMaxLevel()
                    || merged.getLevel(entry.getKey()) >= originalLevel) {
                continue;
            }
            merged.set(entry.getKey(), originalLevel);
            changed = true;
        }
        if (changed) {
            EnchantmentHelper.setEnchantments(output, merged.toImmutable());
            event.setOutput(output);
        }
    }
}
