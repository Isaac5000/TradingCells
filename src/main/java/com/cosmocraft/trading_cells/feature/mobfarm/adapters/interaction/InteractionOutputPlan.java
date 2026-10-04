package com.cosmocraft.trading_cells.feature.mobfarm.adapters.interaction;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/** Computes a complete inventory replacement without changing any live stack. */
public final class InteractionOutputPlan {
    private InteractionOutputPlan() { }
    public static @Nullable List<ItemStack> insert(List<ItemStack> existing, List<ItemStack> products) {
        List<ItemStack> next = new ArrayList<>(existing.stream().map(ItemStack::copy).toList());
        for (ItemStack product : products) {
            if (product.isEmpty()) { continue; }
            int remaining = product.getCount();
            for (int pass = 0; pass < 2; pass++) {
                for (int slot = 0; slot < next.size() && remaining > 0; slot++) {
                    ItemStack current = next.get(slot);
                    if (pass == 0 ? current.isEmpty() || !ItemStack.isSameItemSameComponents(current, product) : !current.isEmpty()) { continue; }
                    int amount = Math.min(remaining, Math.min(64, product.getMaxStackSize()) - current.getCount());
                    if (amount <= 0) { continue; }
                    next.set(slot, product.copyWithCount(current.getCount() + amount));
                    remaining -= amount;
                }
            }
            if (remaining != 0) { return null; }
        }
        return next;
    }
}
