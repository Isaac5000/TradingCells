package com.cosmocraft.trading_cells.feature.mobfarm.adapters.interaction;

import java.util.List;
import net.minecraft.world.item.ItemStack;

public record InteractionHarvestResult(ItemStack remainder, List<ItemStack> products, Status status) {
    public enum Status { EMPTY, READY, NO_RESOURCE, UNSUPPORTED, INVALID_MODULE, OUTPUT_FULL, TOO_LARGE, ERROR }
    public InteractionHarvestResult {
        remainder = remainder.copy();
        products = products.stream().map(ItemStack::copy).toList();
    }
    public static InteractionHarvestResult rejected(Status status) {
        return new InteractionHarvestResult(ItemStack.EMPTY, List.of(), status);
    }
}
