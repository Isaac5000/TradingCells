package com.cosmocraft.trading_cells.feature.silktouch.adapters.input;

import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.TradingCells;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Extracts entities only from placed spawners; held blocks retain normal placement. */
@EventBusSubscriber(modid = TradingCells.MOD_ID)
public final class PreservedSpawnerInteractionAdapter {
    private PreservedSpawnerInteractionAdapter() {
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        InteractionResult result = extractFromPlacedSpawner(event);
        if (result == InteractionResult.PASS) {
            result = SpawnerRedstoneControlAdapter.install(event);
        }
        if (result != InteractionResult.PASS) {
            event.setCancellationResult(result);
            event.setCanceled(true);
        }
    }

    private static InteractionResult extractFromPlacedSpawner(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (event.getHand() != InteractionHand.MAIN_HAND
                || !player.isShiftKeyDown()
                || player.level().isClientSide()
                || !(player.level() instanceof ServerLevel level)
                || !(event.getLevel().getBlockEntity(event.getPos()) instanceof SpawnerBlockEntity spawner)) {
            return InteractionResult.PASS;
        }

        return PreservedSpawnerItemAdapter.extractSpawnEgg(level, spawner)
                .<InteractionResult>map(egg -> {
                    var pos = spawner.getBlockPos();
                    var drop = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, egg);
                    drop.setDeltaMovement(0, 0.1, 0);
                    drop.setDefaultPickUpDelay();
                    level.addFreshEntity(drop);
                    return InteractionResult.SUCCESS;
                })
                .orElse(InteractionResult.PASS);
    }

}
