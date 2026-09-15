package com.cosmocraft.trading_cells.feature.mobfarm.adapters.input;

import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.TradingCells;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (!(event.getEntity() instanceof ServerPlayer player) || !target.isAlive()
                || player.getCooldowns().isOnCooldown(tool)) { return; }
        int bottleSlot = -1;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            if (player.getInventory().getItem(slot).is(Items.GLASS_BOTTLE)) { bottleSlot = slot; break; }
        }
        if (bottleSlot < 0 && !player.getAbilities().instabuild) {
            player.sendOverlayMessage(Component.translatable("message.trading_cells.essence.bottle_required"));
            return;
        }
        ItemStack essence = EntityEssenceData.essenceOf(target);
        if (essence.isEmpty()) {
            player.sendOverlayMessage(Component.translatable("message.trading_cells.essence.unsupported"));
            return;
        }
        // Keep the item's cooldown group before its final durability point can break it.
        player.getCooldowns().addCooldown(tool, 40);
        if (!player.getAbilities().instabuild) {
            player.getInventory().removeItem(bottleSlot, 1);
            tool.hurtAndBreak(1, player, event.getHand());
        }
        if (!player.getInventory().add(essence)) { player.drop(essence, false); }
        player.getInventory().setChanged();
    }
}
