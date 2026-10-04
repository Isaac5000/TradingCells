package com.cosmocraft.trading_cells.feature.mobfarm.adapters.interaction;

import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.ItemAbilities;

public final class BeeHarvestHandler implements InteractionHarvestHandler {
    @Override public boolean supports(LivingEntity entity, ItemStack input) {
        return entity instanceof Bee && (input.is(Items.GLASS_BOTTLE) || input.canPerformAction(ItemAbilities.SHEARS_HARVEST));
    }
    @Override public void interact(DisposableInteractionLevel level, LivingEntity entity, Player actor) {
        var pos = DisposableInteractionLevel.ORIGIN;
        var hive = Blocks.BEEHIVE.defaultBlockState().setValue(BeehiveBlock.HONEY_LEVEL, 5);
        level.placeTemporaryBlock(pos, hive);
        hive.useItemOn(actor.getMainHandItem(), level, actor, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
    }
}
