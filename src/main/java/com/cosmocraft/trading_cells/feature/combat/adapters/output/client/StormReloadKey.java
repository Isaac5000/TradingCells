package com.cosmocraft.trading_cells.feature.combat.adapters.output.client;

import com.cosmocraft.trading_cells.feature.combat.adapters.minecraft.StormWeaponItem;
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
    private static final KeyMapping.Category CATEGORY = new KeyMapping.Category(Identifier.fromNamespaceAndPath("trading_cells", "equipment"));
    public static final KeyMapping RELOAD = new KeyMapping("key.trading_cells.storm_reload", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, CATEGORY) {
        @Override public void setDown(boolean down) {
            if (down && !isDown()) { pendingPress = true; }
            super.setDown(down);
            // A release/repress can happen between client ticks. It is still a new press.
            if (!down) { held = 0; pressed = ItemStack.EMPTY; }
        }
    };
    private static boolean pendingPress;
    private static int held;
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
        boolean press = pendingPress;
        pendingPress = false;
        if (client.player == null || client.gui.screen() != null) { held = 0; pressed = ItemStack.EMPTY; return; }
        if (press) {
            hand = client.player.getMainHandItem().getItem() instanceof StormWeaponItem ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
            pressed = client.player.getItemInHand(hand);
            if (pressed.getItem() instanceof StormWeaponItem) { send(hand, false); }
        }
        if (!RELOAD.isDown()) { held = 0; pressed = ItemStack.EMPTY; return; }
        if (++held == 12 && pressed.getItem() instanceof StormWeaponItem
                && ItemStack.isSameItem(pressed, client.player.getItemInHand(hand))) { send(hand, true); }
    }
    public static void send(InteractionHand hand, boolean eject) {
        ClientPacketDistributor.sendToServer(new StormReloadPayload(hand, eject));
    }
}
