package com.cosmocraft.trading_cells.feature.mobfarm.adapters.input;

import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.TradingCells;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = TradingCells.MOD_ID)
public final class EssenceExtractionEvents {
    private EssenceExtractionEvents() { }

    @SubscribeEvent
    public static void extract(PlayerInteractEvent.EntityInteract event) {
        ItemStack tool = event.getItemStack();
        if (!(tool.getItem() instanceof EssenceExtractorItem)
                || !(event.getTarget() instanceof LivingEntity target) || target instanceof Player) { return; }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.CONSUME);
        EssenceExtractorItem.beginExtraction(event.getEntity(), target, event.getHand());
    }
}
