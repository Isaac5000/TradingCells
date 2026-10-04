package com.cosmocraft.trading_cells.feature.mobfarm.adapters.interaction;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Ordered extensions may handle a context before the normal entity interaction pipeline. */
public interface InteractionHarvestHandler {
    boolean supports(LivingEntity entity, ItemStack input);
    void interact(DisposableInteractionLevel level, LivingEntity entity, Player actor);
}
