package com.cosmocraft.trading_cells.feature.logistics.adapters.api;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public record LogisticsResourceSnapshot<R>(
        R resource,
        Identifier resourceId,
        String componentFingerprint,
        Component displayName,
        ItemStack icon,
        long amount
) {
    public LogisticsResourceSnapshot {
        componentFingerprint = componentFingerprint == null ? "" : componentFingerprint;
        displayName = displayName == null ? Component.literal(resourceId.toString()) : displayName;
        icon = icon == null ? ItemStack.EMPTY : icon.copyWithCount(icon.isEmpty() ? 0 : 1);
        amount = Math.max(0L, amount);
    }
}
