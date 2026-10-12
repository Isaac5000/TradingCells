package com.cosmocraft.trading_cells.feature.mobfarm.adapters.interaction;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;

/** Explicit renewable breath harvest, without spawning or ticking a damaging cloud. */
public final class DragonBreathHarvestHandler implements InteractionHarvestHandler {
    @Override public boolean supports(LivingEntity entity, ItemStack input) {
        return entity instanceof EnderDragon && input.is(Items.GLASS_BOTTLE);
    }

    @Override public void interact(DisposableInteractionLevel level, LivingEntity entity, Player actor) {
        actor.setItemInHand(InteractionHand.MAIN_HAND, ItemUtils.createFilledResult(
                actor.getMainHandItem(), actor, Items.DRAGON_BREATH.getDefaultInstance()));
    }
}
