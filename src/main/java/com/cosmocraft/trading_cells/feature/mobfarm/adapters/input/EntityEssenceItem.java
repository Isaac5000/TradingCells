package com.cosmocraft.trading_cells.feature.mobfarm.adapters.input;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class EntityEssenceItem extends Item {
    private final boolean module;
    public EntityEssenceItem(Properties properties, boolean module) { super(properties); this.module = module; }

    @Override public Component getName(ItemStack stack) {
        if (EntityEssenceData.entityTypeId(stack) == null) { return super.getName(stack); }
        return Component.translatable(module ? "item.trading_cells.entity_module.named"
                : "item.trading_cells.entity_essence.named", EntityEssenceData.displayName(stack));
    }
}
