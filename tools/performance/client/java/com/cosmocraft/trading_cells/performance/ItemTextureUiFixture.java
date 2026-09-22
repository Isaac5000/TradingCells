package com.cosmocraft.trading_cells.performance;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemDisplayContext;

/** Renders the actual item models, including the animated special renderer. */
final class ItemTextureUiFixture {
    private static volatile boolean prepared;
    private static boolean ready;

    private ItemTextureUiFixture() { }

    static boolean active() { return mode().equals("item-textures") || mode().equals("pipe-hands"); }
    static boolean ready() { return ready; }
    private static String mode() { return System.getProperty("trading_cells.performance.client.uiFixture", ""); }

    static void prepare(ServerPlayer player) {
        player.getInventory().clearContent();
        player.getInventory().setSelectedSlot(0);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack("item_pipe"));
        player.setItemInHand(InteractionHand.OFF_HAND, stack("fluid_pipe"));
        player.inventoryMenu.broadcastChanges();
        prepared = true;
    }

    static void inspect(Minecraft minecraft) {
        if (ready || !prepared || minecraft.player == null || minecraft.level == null
                || !minecraft.player.getInventory().getItem(0).is(stack("item_pipe").getItem())
                || minecraft.gui.screen() != null) { return; }
        minecraft.player.getInventory().setSelectedSlot(0);
        if (mode().equals("pipe-hands")) {
            minecraft.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
            minecraft.options.fov().set(45);
            ready = true;
            System.out.println("Pipe hand models ready; primary arm=" + minecraft.player.getMainArm());
        } else {
            var state = new ItemStackRenderState();
            minecraft.getItemModelResolver().updateForTopItem(state, stack("storm_shard"), ItemDisplayContext.GUI,
                    minecraft.level, minecraft.player, 0);
            if (state.isEmpty() || !state.isAnimated()) {
                throw new IllegalStateException("Storm Shard must animate in the inventory");
            }
            minecraft.gui.setScreen(new TextureSheet());
        }
    }

    private static ItemStack stack(String name) {
        var result = BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("trading_cells", name)).getDefaultInstance();
        if (result.isEmpty()) { throw new IllegalStateException("Missing item texture fixture: " + name); }
        return result;
    }

    private static final class TextureSheet extends Screen {
        private final List<ItemStack> items = new ArrayList<>();

        TextureSheet() {
            super(Component.literal("Item textures"));
            for (String name : new String[]{"villager_capturer", "piglin_capturer", "unbreakable_villager_capturer",
                    "unbreakable_piglin_capturer", "storm_shard"}) { items.add(stack(name)); }
            for (String family : new String[]{"quarry", "piglin_barter", "pipe", "mob_farm_speed", "mob_farm_capacity"}) {
                String[] materials = {"copper", "iron", "gold", "diamond", "netherite"};
                String[] pipeTiers = {"basic", "improved", "advanced", "ultimate", "infinite"};
                for (int tier = 0; tier < materials.length; tier++) {
                    items.add(stack(family.equals("pipe") ? pipeTiers[tier] + "_pipe_upgrade"
                            : family + "_" + materials[tier] + "_upgrade"));
                }
            }
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            graphics.fill(0, 0, width, height, 0xFF262C2F);
            int left = (width - 350) / 2;
            int top = (height - 300) / 2;
            for (int index = 0; index < items.size(); index++) {
                graphics.pose().pushMatrix();
                graphics.pose().translate(left + index % 5 * 70, top + index / 5 * 50);
                graphics.pose().scale(2.5F, 2.5F);
                graphics.item(items.get(index), 0, 0);
                graphics.pose().popMatrix();
            }
            if (!ready) { System.out.println("Item textures rendered: 4 capturers, charged shard and 25 upgrades"); }
            ready = true;
        }

        @Override
        public boolean isPauseScreen() { return false; }
    }
}
