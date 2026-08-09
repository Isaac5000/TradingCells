package com.cosmocraft.trading_cells.feature.quarry.adapters.input;

import com.cosmocraft.trading_cells.feature.quarry.adapters.output.QuarryRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.quarry.domain.model.QuarryUpgradeTier;
import net.minecraft.world.item.ItemStack;

public final class QuarryUpgradeItems {
    private QuarryUpgradeItems() {
    }

    public static boolean isUpgrade(ItemStack stack) {
        return tier(stack) != QuarryUpgradeTier.NONE;
    }

    public static QuarryUpgradeTier tier(ItemStack stack) {
        if (stack.is(QuarryRegistrationAdapter.QUARRY_COPPER_UPGRADE_ITEM.get())) {
            return QuarryUpgradeTier.COPPER;
        }
        if (stack.is(QuarryRegistrationAdapter.QUARRY_IRON_UPGRADE_ITEM.get())) {
            return QuarryUpgradeTier.IRON;
        }
        if (stack.is(QuarryRegistrationAdapter.QUARRY_GOLD_UPGRADE_ITEM.get())) {
            return QuarryUpgradeTier.GOLD;
        }
        if (stack.is(QuarryRegistrationAdapter.QUARRY_DIAMOND_UPGRADE_ITEM.get())) {
            return QuarryUpgradeTier.DIAMOND;
        }
        if (stack.is(QuarryRegistrationAdapter.QUARRY_NETHERITE_UPGRADE_ITEM.get())) {
            return QuarryUpgradeTier.NETHERITE;
        }
        return QuarryUpgradeTier.NONE;
    }
}
