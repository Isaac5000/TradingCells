package com.cosmocraft.trading_cells.feature.mobfarm.adapters.interaction;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.creaking.Creaking;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.ItemAbilities;

/** Renewable resin without a real heart, delayed ticks or damage to the stored creature. */
public final class CreakingHarvestHandler implements InteractionHarvestHandler {
    @Override public boolean supports(LivingEntity entity, ItemStack input) {
        return entity instanceof Creaking && input.canPerformAction(ItemAbilities.AXE_STRIP);
    }

    @Override public void interact(DisposableInteractionLevel level, LivingEntity entity, Player actor) {
        // Match the heart's vanilla 2-3 clumps per production pulse.
        entity.spawnAtLocation(level, new ItemStack(Items.RESIN_CLUMP, level.getRandom().nextIntBetweenInclusive(2, 3)));
        actor.getMainHandItem().hurtAndBreak(1, actor, EquipmentSlot.MAINHAND);
    }
}
