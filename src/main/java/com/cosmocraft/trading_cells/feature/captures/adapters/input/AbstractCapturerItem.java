package com.cosmocraft.trading_cells.feature.captures.adapters.input;

import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.FeatureComposition;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

abstract class AbstractCapturerItem extends Item {
    protected AbstractCapturerItem(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(
            ItemStack stack,
            ServerLevel level,
            Entity owner,
            @Nullable EquipmentSlot slot
    ) {
        super.inventoryTick(stack, level, owner, slot);
        configureDurability(stack);
    }

    protected final void damageAfterRelease(
            ItemStack stack,
            ServerLevel level,
            @Nullable LivingEntity owner,
            InteractionHand hand
    ) {
        configureDurability(stack);
        if (owner != null) {
            stack.hurtAndBreak(1, owner, hand);
            return;
        }
        stack.hurtAndBreak(1, level, (LivingEntity) null, ignored -> {
            // ItemStack already removes the broken capturer; no owner exists for a break animation.
        });
    }

    static void configureDurability(ItemStack stack) {
        int maximum = FeatureComposition.captures().maximumDurability();
        if (stack.getOrDefault(DataComponents.MAX_DAMAGE, 0) != maximum) {
            stack.set(DataComponents.MAX_DAMAGE, maximum);
        }

        int damage = stack.getOrDefault(DataComponents.DAMAGE, 0);
        if (damage >= maximum) {
            stack.set(DataComponents.DAMAGE, maximum - 1);
        }
    }
}
