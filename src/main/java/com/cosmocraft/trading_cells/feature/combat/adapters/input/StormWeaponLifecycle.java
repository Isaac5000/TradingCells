package com.cosmocraft.trading_cells.feature.combat.adapters.input;

import com.cosmocraft.trading_cells.feature.combat.adapters.minecraft.StormWeaponItem;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = "trading_cells")
public final class StormWeaponLifecycle {
    private StormWeaponLifecycle() { }
    @SubscribeEvent public static void mining(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock event) {
        if (event.getItemStack().getItem() instanceof
                com.cosmocraft.trading_cells.feature.combat.adapters.minecraft.ElectricArcWelderItem) {
            event.setCanceled(true);
        }
    }
    @SubscribeEvent public static void tossed(ItemTossEvent event) {
        var stack = event.getEntity().getItem();
        if (stack.getItem() instanceof StormWeaponItem) { StormWeaponItem.cancelReload(stack); }
    }
    @SubscribeEvent public static void disconnected(PlayerEvent.PlayerLoggedOutEvent event) {
        for (var stack : event.getEntity().getInventory().getNonEquipmentItems()) {
            if (stack.getItem() instanceof StormWeaponItem) { StormWeaponItem.cancelReload(stack); }
        }
        var offhand = event.getEntity().getOffhandItem();
        if (offhand.getItem() instanceof StormWeaponItem) { StormWeaponItem.cancelReload(offhand); }
    }
}
