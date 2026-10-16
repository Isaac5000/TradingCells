package com.cosmocraft.trading_cells.feature.combat.adapters.output.client;

import com.cosmocraft.trading_cells.feature.combat.adapters.minecraft.StormWeaponItem;
import com.cosmocraft.trading_cells.feature.combat.domain.model.StormReloadGesture;
import com.cosmocraft.trading_cells.platform.neoforge.network.StormReloadPayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = "trading_cells", value = Dist.CLIENT)
public final class StormReloadKey {
    private static final StormReloadGesture GESTURE = new StormReloadGesture();
    private static final KeyMapping.Category CATEGORY = new KeyMapping.Category(Identifier.fromNamespaceAndPath("trading_cells", "equipment"));
    public static final KeyMapping RELOAD = new KeyMapping("key.trading_cells.storm_reload", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, CATEGORY) {
        @Override public void setDown(boolean down) {
            boolean edge = down != isDown();
            super.setDown(down);
            if (!edge) { return; }
            var client = Minecraft.getInstance();
            if (client.player == null || client.gui.screen() != null) { reset(); return; }
            if (down) {
                hand = client.player.getMainHandItem().getItem() instanceof StormWeaponItem
                        ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
                pressed = client.player.getItemInHand(hand);
            }
            // Handle release edges here as a complete tap can happen between client ticks.
            dispatch(GESTURE.setDown(down));
            if (!down) { pressed = ItemStack.EMPTY; }
        }
    };
    private static InteractionHand hand = InteractionHand.MAIN_HAND;
    private static ItemStack pressed = ItemStack.EMPTY;
    private StormReloadKey() { }
    @SubscribeEvent public static void register(RegisterKeyMappingsEvent event) {
        event.registerCategory(CATEGORY);
        event.register(RELOAD);
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        var client = Minecraft.getInstance();
        // Only physical press edges count: keyboard autorepeat must not reload on release.
        while (RELOAD.consumeClick()) { }
        if (client.player == null || client.gui.screen() != null
                || !ItemStack.isSameItem(pressed, client.player.getItemInHand(hand))) { reset(); return; }
        dispatch(GESTURE.tick());
    }
    private static void reset() { GESTURE.reset(); pressed = ItemStack.EMPTY; }
    private static void dispatch(StormReloadGesture.Action action) {
        var player = Minecraft.getInstance().player;
        if (action != StormReloadGesture.Action.NONE && player != null && pressed.getItem() instanceof StormWeaponItem
                && ItemStack.isSameItem(pressed, player.getItemInHand(hand))) {
            send(hand, action == StormReloadGesture.Action.EJECT);
        }
    }
    public static void send(InteractionHand hand, boolean eject) {
        ClientPacketDistributor.sendToServer(new StormReloadPayload(hand, eject));
    }
}
