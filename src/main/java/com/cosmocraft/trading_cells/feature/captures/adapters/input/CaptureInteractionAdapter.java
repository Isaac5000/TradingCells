package com.cosmocraft.trading_cells.feature.captures.adapters.input;

import com.cosmocraft.trading_cells.feature.captures.adapters.output.CaptureRegistrationAdapter;
import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.TradingCells;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = TradingCells.MOD_ID)
public final class CaptureInteractionAdapter {
    private CaptureInteractionAdapter() {
    }

    @SubscribeEvent
    public static void onVillagerInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide()
                || !(event.getTarget() instanceof Villager villager)
                || !event.getEntity().isShiftKeyDown()) {
            return;
        }

        ItemStack heldItem = event.getEntity().getItemInHand(event.getHand());
        if (!heldItem.is(CaptureRegistrationAdapter.VILLAGER_CAPTURER_ITEM.get())) {
            return;
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (VillagerCapturerItem.hasCapturedVillager(heldItem)) {
            return;
        }

        ItemStack captureTarget = createSingleCapturerTarget(heldItem);
        if (!VillagerCapturerItem.captureVillager(captureTarget, villager)) {
            event.setCancellationResult(InteractionResult.FAIL);
            return;
        }

        finishStackedCapture(event.getEntity(), heldItem, captureTarget);
        villager.discard();
    }

    @SubscribeEvent
    public static void onPiglinInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide()
                || !(event.getTarget() instanceof Piglin piglin)
                || !event.getEntity().isShiftKeyDown()) {
            return;
        }

        ItemStack heldItem = event.getEntity().getItemInHand(event.getHand());
        if (!heldItem.is(CaptureRegistrationAdapter.PIGLIN_CAPTURER_ITEM.get())) {
            return;
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (PiglinCapturerItem.hasCapturedPiglin(heldItem)) {
            return;
        }

        ItemStack captureTarget = createSingleCapturerTarget(heldItem);
        if (!PiglinCapturerItem.capturePiglin(captureTarget, piglin)) {
            event.setCancellationResult(InteractionResult.FAIL);
            return;
        }

        finishStackedCapture(event.getEntity(), heldItem, captureTarget);
        piglin.discard();
    }

    private static ItemStack createSingleCapturerTarget(ItemStack heldItem) {
        return heldItem.getCount() <= 1
                ? heldItem
                : new ItemStack(heldItem.getItem());
    }

    private static void finishStackedCapture(Player player, ItemStack heldItem, ItemStack captureTarget) {
        if (captureTarget == heldItem) {
            return;
        }

        heldItem.shrink(1);
        if (!player.getInventory().add(captureTarget)) {
            player.drop(captureTarget, false);
        }
    }
}
