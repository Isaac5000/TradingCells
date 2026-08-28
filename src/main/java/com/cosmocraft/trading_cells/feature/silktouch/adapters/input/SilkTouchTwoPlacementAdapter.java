package com.cosmocraft.trading_cells.feature.silktouch.adapters.input;

import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.TradingCells;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

/** Restores protected Block Entity data even for vanilla operator-only spawner types. */
@EventBusSubscriber(modid = TradingCells.MOD_ID)
public final class SilkTouchTwoPlacementAdapter {
    private SilkTouchTwoPlacementAdapter() {
    }

    @SubscribeEvent
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof Player player)
                || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        ItemStack source = findSourceStack(player, event);
        if (source.isEmpty() || !SilkTouchTwoDropAdapter.hasPreservedDataMarker(source)) {
            return;
        }

        TypedEntityData<BlockEntityType<?>> data = source.get(DataComponents.BLOCK_ENTITY_DATA);
        BlockEntity blockEntity = level.getBlockEntity(event.getPos());
        if (data != null && blockEntity != null && data.type() == blockEntity.getType()) {
            data.loadInto(blockEntity, level.registryAccess());
        }
    }

    private static ItemStack findSourceStack(Player player, BlockEvent.EntityPlaceEvent event) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.is(event.getPlacedBlock().getBlock().asItem())) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }
}
