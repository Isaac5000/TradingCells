package com.cosmocraft.trading_cells.feature.logistics.adapters.output.client;

import com.cosmocraft.trading_cells.feature.logistics.adapters.input.LogisticsPipeBlockEntity;
import com.cosmocraft.trading_cells.feature.logistics.adapters.input.PipeWrenchItem;
import com.cosmocraft.trading_cells.feature.logistics.adapters.input.PipeInteractionFace;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionResult;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public final class LogisticsPipeClientInteractionAdapter {
    private LogisticsPipeClientInteractionAdapter() {
    }

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getLevel().isClientSide()
                || !(event.getItemStack().getItem() instanceof PipeWrenchItem)
                || !(event.getLevel().getBlockEntity(event.getPos()) instanceof LogisticsPipeBlockEntity pipe)) {
            return;
        }
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }
}
