package com.cosmocraft.trading_cells.feature.silktouch.adapters.input;

import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.TradingCells;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Converts trusted spawner items or extracts the entity from a placed spawner. */
@EventBusSubscriber(modid = TradingCells.MOD_ID)
public final class PreservedSpawnerInteractionAdapter {
    private PreservedSpawnerInteractionAdapter() {
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        InteractionResult result = convert(event.getEntity(), event.getHand());
        if (result != InteractionResult.PASS) {
            event.setCancellationResult(result);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        InteractionResult result = SpawnerRedstoneControlAdapter.install(event);
        if (result == InteractionResult.PASS) {
            result = extractFromPlacedSpawner(event);
        }
        if (result != InteractionResult.PASS) {
            event.setCancellationResult(result);
            event.setCanceled(true);
        }
    }

    private static InteractionResult extractFromPlacedSpawner(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (event.getHand() != InteractionHand.MAIN_HAND
                || player.isShiftKeyDown()
                || !player.getMainHandItem().isEmpty()
                || player.level().isClientSide()
                || !(player.level() instanceof ServerLevel level)
                || !(event.getLevel().getBlockEntity(event.getPos()) instanceof SpawnerBlockEntity spawner)) {
            return InteractionResult.PASS;
        }

        return PreservedSpawnerItemAdapter.extractSpawnEgg(level, spawner)
                .<InteractionResult>map(egg -> {
                    player.getInventory().placeItemBackInInventory(egg);
                    return InteractionResult.SUCCESS;
                })
                .orElse(InteractionResult.PASS);
    }

    private static InteractionResult convert(Player player, net.minecraft.world.InteractionHand hand) {
        if (player.isShiftKeyDown() || player.level().isClientSide()) {
            return InteractionResult.PASS;
        }

        ItemStack spawner = player.getItemInHand(hand);
        if (!PreservedSpawnerItemAdapter.isTrustedSpawner(spawner)
                || !(player.level() instanceof ServerLevel level)) {
            return InteractionResult.PASS;
        }

        var converted = PreservedSpawnerItemAdapter.createSpawnEgg(level, spawner);
        if (converted.isEmpty()) {
            return InteractionResult.FAIL;
        }

        ItemStack egg = converted.orElseThrow();
        if (spawner.getCount() == 1) {
            player.setItemInHand(hand, egg);
            return InteractionResult.SUCCESS;
        }

        spawner.shrink(1);
        player.getInventory().placeItemBackInInventory(egg);
        return InteractionResult.SUCCESS;
    }
}
