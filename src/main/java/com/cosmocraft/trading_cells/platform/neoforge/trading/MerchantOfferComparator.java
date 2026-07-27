package com.cosmocraft.trading_cells.platform.neoforge.trading;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import org.jspecify.annotations.Nullable;

/** Shared semantic comparison used when refreshing offers without relying on object identity. */
public final class MerchantOfferComparator {
    private MerchantOfferComparator() {
    }

    public static boolean equivalent(MerchantOffers first, MerchantOffers second) {
        if (first.size() != second.size()) {
            return false;
        }
        for (int index = 0; index < first.size(); index++) {
            if (!equivalent(first.get(index), second.get(index))) {
                return false;
            }
        }
        return true;
    }

    public static boolean equivalent(@Nullable MerchantOffer first, @Nullable MerchantOffer second) {
        if (first == null || second == null) {
            return first == second;
        }
        return ItemStack.isSameItemSameComponents(first.getBaseCostA(), second.getBaseCostA())
                && first.getBaseCostA().getCount() == second.getBaseCostA().getCount()
                && itemCostsEquivalent(first.getItemCostB().orElse(null), second.getItemCostB().orElse(null))
                && ItemStack.isSameItemSameComponents(first.getResult(), second.getResult())
                && first.getResult().getCount() == second.getResult().getCount()
                && first.getMaxUses() == second.getMaxUses()
                && first.getXp() == second.getXp();
    }

    public static int findEquivalentIndex(MerchantOffers offers, @Nullable MerchantOffer target) {
        if (target == null) {
            return 0;
        }
        for (int index = 0; index < offers.size(); index++) {
            if (equivalent(target, offers.get(index))) {
                return index;
            }
        }
        return 0;
    }

    private static boolean itemCostsEquivalent(@Nullable ItemCost first, @Nullable ItemCost second) {
        if (first == null || second == null) {
            return first == second;
        }
        return ItemStack.isSameItemSameComponents(first.itemStack(), second.itemStack())
                && first.count() == second.count();
    }
}
