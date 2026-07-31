package com.cosmocraft.trading_cells.feature.captures.adapters.input;

import com.cosmocraft.trading_cells.feature.captures.adapters.api.CapturedMobStackAdapter;
import com.cosmocraft.trading_cells.feature.captures.domain.model.CapturedMobKind;
import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.TradingCells;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.npc.villager.Villager;
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
        if (!CapturedMobStackAdapter.isCapturer(CapturedMobKind.VILLAGER, heldItem)) {
            return;
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (VillagerCapturerItem.hasCapturedVillager(heldItem)) {
            return;
        }

        if (!VillagerCapturerItem.captureVillager(heldItem, villager)) {
            event.setCancellationResult(InteractionResult.FAIL);
            return;
        }

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
        if (!CapturedMobStackAdapter.isCapturer(CapturedMobKind.PIGLIN, heldItem)) {
            return;
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (PiglinCapturerItem.hasCapturedPiglin(heldItem)) {
            return;
        }

        if (!PiglinCapturerItem.capturePiglin(heldItem, piglin)) {
            event.setCancellationResult(InteractionResult.FAIL);
            return;
        }

        piglin.discard();
    }
}
