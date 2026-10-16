package com.cosmocraft.trading_cells.performance;

import com.cosmocraft.trading_cells.feature.combat.adapters.minecraft.*;
import com.cosmocraft.trading_cells.feature.combat.adapters.output.CombatRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.combat.adapters.output.client.StormReloadKey;
import com.cosmocraft.trading_cells.feature.infusion.adapters.output.ArcaneInfuserRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.infusion.adapters.input.ArcaneInfuserBlockEntity;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

/** Local visual run only; never loaded in the published mod or CI. */
final class StormUiFixture {
    private static volatile boolean prepared;
    private static boolean ready;
    private static int phase;
    private static long next;
    private static BlockPos synthesizer;
    private static float viewPitch;
    private static float viewYaw;
    private StormUiFixture() { }
    static boolean active() { return System.getProperty("trading_cells.performance.client.uiFixture", "").equals("storm"); }
    static boolean prepared() { return prepared; }
    static boolean ready() { return ready; }
    static void applyView(Minecraft mc) {
        if (!active() || !prepared || ready || mc.player == null) { return; }
        mc.player.setXRot(viewPitch); mc.player.xRotO = viewPitch;
        mc.player.setYRot(viewYaw); mc.player.yRotO = viewYaw;
        mc.player.yBodyRot = 0; mc.player.yBodyRotO = 0;
        mc.player.yHeadRot = viewYaw; mc.player.yHeadRotO = viewYaw;
    }
    static void prepare(ServerPlayer player, BlockPos pos) {
        if (prepared) { return; }
        var level = player.level();
        for (var target : BlockPos.betweenClosed(pos.offset(-2, -1, -2), pos.offset(4, 3, 4))) {
            level.setBlockAndUpdate(target, target.getY() < pos.getY() ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState());
        }
        level.setBlockAndUpdate(pos, CombatRegistrationAdapter.STATION.get().defaultBlockState());
        synthesizer = pos.east(2);
        level.setBlockAndUpdate(synthesizer, ArcaneInfuserRegistrationAdapter.BLOCK.get().defaultBlockState());
        level.setBlockAndUpdate(pos.west(2), com.cosmocraft.trading_cells.feature.mobfarm.adapters.output.MobFarmRegistrationAdapter.CREATURE_GENERATOR.get().defaultBlockState());
        var machine = (ArcaneInfuserBlockEntity) level.getBlockEntity(synthesizer);
        for (int slot = 0; slot < 9; slot++) { machine.setItem(slot, net.minecraft.world.item.Items.DIAMOND.getDefaultInstance()); }
        for (var drop : level.getEntities(net.minecraft.world.entity.EntityTypes.ITEM, new net.minecraft.world.phys.AABB(pos).inflate(8), item -> true)) { drop.discard(); }
        var station = (StormStationBlockEntity) level.getBlockEntity(pos);
        station.setItem(0, new ItemStack(CombatRegistrationAdapter.STORM_SHARD_ITEM.get(), 16));
        station.setItem(1, CombatRegistrationAdapter.CAPACITOR.get().getDefaultInstance());
        player.getInventory().clearContent();
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        player.getInventory().setSelectedSlot(0);
        player.setItemInHand(InteractionHand.MAIN_HAND, CombatRegistrationAdapter.WELDER.get().getDefaultInstance());
        for (int slot = 9; slot < 18; slot++) {
            var battery = CombatRegistrationAdapter.CAPACITOR.get().getDefaultInstance();
            StormChargeData.setCharge(battery, 128); player.getInventory().setItem(slot, battery);
        }
        player.inventoryMenu.broadcastChanges();
        player.openMenu(station);
        prepared = true;
    }
    static void inspect(Minecraft mc) {
        if (!prepared || ready || mc.player == null || System.nanoTime() < next) { return; }
        mc.player.getInventory().setSelectedSlot(0);
        switch (phase) {
            case 0 -> {
                if (mc.gui.screen() == null) { return; }
                if (java.util.Arrays.stream(mc.options.keyMappings).noneMatch(key -> key == StormReloadKey.RELOAD)) {
                    throw new IllegalStateException("Reload binding missing from Controls");
                }
            }
            case 1 -> { capture(mc, "01-station"); mc.player.closeContainer(); }
            case 2 -> {
                var attack = new net.neoforged.neoforge.client.event.InputEvent.InteractionKeyMappingTriggered(
                        0, mc.options.keyAttack, InteractionHand.MAIN_HAND);
                net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(attack);
                if (!attack.isCanceled() || attack.shouldSwingHand()) {
                    throw new IllegalStateException("Welder left click must cancel attack and swing");
                }
                capture(mc, "02-welder-first-person");
            }
            case 3 -> tapReload();
            case 4 -> capture(mc, "03-welder-reload-first");
            case 5 -> {
                mc.gameMode.releaseUsingItem(mc.player);
                mc.options.keyUse.setDown(true);
                mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
            }
            case 6 -> capture(mc, "04-welder-active-first");
            case 7 -> { mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT); mc.options.fov().set(60); }
            case 8 -> capture(mc, "05-welder-active-third");
            case 9 -> {
                mc.options.keyUse.setDown(false); mc.gameMode.releaseUsingItem(mc.player);
                StormReloadKey.RELOAD.setDown(true);
            }
            case 10 -> {
                StormReloadKey.RELOAD.setDown(false);
                if (!StormWeaponItem.battery(mc.player.getMainHandItem()).isEmpty()) { throw new IllegalStateException("Held reload key failed to eject"); }
                tapReload();
            }
            case 11 -> {
                if (!StormWeaponItem.reloading(mc.player.getMainHandItem())) { throw new IllegalStateException("Tap after held eject failed to start reload"); }
                capture(mc, "06-welder-reload-third");
            }
            case 12 -> {
                mc.options.keyUse.setDown(false); mc.gameMode.releaseUsingItem(mc.player);
                server(mc, player -> { player.setItemInHand(InteractionHand.MAIN_HAND, CombatRegistrationAdapter.BATON.get().getDefaultInstance()); player.inventoryMenu.broadcastChanges(); });
            }
            case 13 -> capture(mc, "07-baton-third-person");
            case 14 -> tapReload();
            case 15 -> capture(mc, "08-baton-reload-third");
            case 16 -> { mc.options.keyUse.setDown(false); mc.gameMode.releaseUsingItem(mc.player); mc.options.setCameraType(CameraType.FIRST_PERSON); }
            case 17 -> capture(mc, "09-baton-first-person");
            case 18 -> {
                var state = new ItemStackRenderState();
                mc.getItemModelResolver().updateForTopItem(state, CombatRegistrationAdapter.STORM_SHARD_ITEM.get().getDefaultInstance(),
                        ItemDisplayContext.GUI, mc.level, mc.player, 0);
                if (state.isEmpty() || !state.isAnimated()) { throw new IllegalStateException("Charged shard inventory animation missing"); }
                mc.gui.setScreen(new Sheet());
            }
            case 19 -> capture(mc, "10-inventory-a");
            case 20 -> {
                capture(mc, "11-inventory-b"); mc.gui.setScreen(null);
                mc.player.getRecipeBook().setOpen(net.minecraft.world.inventory.RecipeBookType.CRAFTING, false);
                server(mc, player -> player.openMenu((ArcaneInfuserBlockEntity) player.level().getBlockEntity(synthesizer)));
            }
            case 21 -> {
                if (!(mc.gui.screen() instanceof com.cosmocraft.trading_cells.feature.infusion.adapters.output.client.ArcaneInfuserScreen screen)) { return; }
                if (screen.children().stream().anyMatch(net.minecraft.client.gui.screens.recipebook.RecipeBookComponent.class::isInstance)) {
                    throw new IllegalStateException("Synthesizer recipe book was not removed");
                }
                capture(mc, "12-synthesizer-menu");
            }
            case 22 -> {
                mc.player.closeContainer();
            }
            case 23 -> capture(mc, "13-synthesizer-world");
            case 24 -> {
                mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
                server(mc, player -> { player.setItemInHand(InteractionHand.MAIN_HAND, CombatRegistrationAdapter.WELDER.get().getDefaultInstance()); player.inventoryMenu.broadcastChanges(); });
                viewPitch = -65;
            }
            case 25 -> capture(mc, "14-welder-look-up");
            case 26 -> viewPitch = 65;
            case 27 -> capture(mc, "15-welder-look-down");
            case 28 -> { viewPitch = -30; viewYaw = 55; }
            case 29 -> capture(mc, "16-welder-look-right");
            case 30 -> viewYaw = -55;
            case 31 -> capture(mc, "17-welder-look-left");
            case 32 -> {
                viewPitch = 0; viewYaw = 0;
                mc.options.setCameraType(CameraType.FIRST_PERSON);
                server(mc, player -> player.openMenu((StormStationBlockEntity) player.level().getBlockEntity(synthesizer.west(2))));
            }
            case 33 -> {
                if (mc.gui.screen() == null) { return; }
                mc.player.closeContainer();
                beginUnload(mc, false, CameraType.FIRST_PERSON);
            }
            case 34 -> captureUnload(mc, "18-welder-unload-first");
            case 35 -> beginUnload(mc, false, CameraType.THIRD_PERSON_FRONT);
            case 36 -> captureUnload(mc, "19-welder-unload-third");
            case 37 -> beginUnload(mc, true, CameraType.FIRST_PERSON);
            case 38 -> captureUnload(mc, "20-baton-unload-first");
            case 39 -> beginUnload(mc, true, CameraType.THIRD_PERSON_FRONT);
            case 40 -> captureUnload(mc, "21-baton-unload-third");
            case 41 -> mc.gui.setScreen(new EggSheet());
            case 42 -> { capture(mc, "22-tier-eggs"); mc.gui.setScreen(null); }
            case 43 -> mc.gui.setScreen(new ChargeSheet());
            case 44 -> { capture(mc, "23-weapon-charge-bars"); mc.gui.setScreen(null); }
            case 45 -> {
                mc.options.setCameraType(CameraType.FIRST_PERSON);
                server(mc, player -> player.openMenu((StormStationBlockEntity) player.level().getBlockEntity(synthesizer.west(2))));
            }
            case 46 -> { if (mc.gui.screen() == null) { return; } ready = true; }
            default -> { return; }
        }
        phase++; next = System.nanoTime() + 800_000_000L;
    }
    private static void server(Minecraft mc, java.util.function.Consumer<ServerPlayer> action) {
        var id = mc.player.getUUID();
        mc.getSingleplayerServer().execute(() -> action.accept(mc.getSingleplayerServer().getPlayerList().getPlayer(id)));
    }
    private static void tapReload() {
        StormReloadKey.RELOAD.setDown(true);
        net.minecraft.client.KeyMapping.click(StormReloadKey.RELOAD.getKey());
        StormReloadKey.tick(new net.neoforged.neoforge.client.event.ClientTickEvent.Post());
        StormReloadKey.RELOAD.setDown(false);
    }
    private static void beginUnload(Minecraft mc, boolean baton, CameraType camera) {
        mc.options.setCameraType(camera);
        server(mc, player -> {
            var weapon = (baton ? CombatRegistrationAdapter.BATON : CombatRegistrationAdapter.WELDER).get().getDefaultInstance();
            var cell = CombatRegistrationAdapter.CAPACITOR.get().getDefaultInstance();
            StormChargeData.setCharge(cell, 17);
            StormWeaponItem.setBattery(weapon, cell);
            player.setItemInHand(InteractionHand.MAIN_HAND, weapon);
            StormWeaponItem.requestReload(player, InteractionHand.MAIN_HAND, true);
            player.inventoryMenu.broadcastChanges();
        });
    }
    private static void captureUnload(Minecraft mc, String name) {
        var weapon = mc.player.getMainHandItem();
        if (!StormWeaponItem.unloading(weapon) || !StormWeaponItem.battery(weapon).isEmpty()) {
            throw new IllegalStateException("Reverse animation must not retain the real battery: " + name);
        }
        float p = StormWeaponItem.animationProgress(weapon, mc.level, 0);
        float elapsed = StormWeaponItem.reloadProgress(weapon, mc.level, 0);
        if (Math.abs(p + elapsed - 1) > 0.001F || p <= 0 || p >= 1) {
            throw new IllegalStateException("Animation does not reverse the loading timeline: " + name);
        }
        capture(mc, name);
    }
    private static final class EggSheet extends Screen {
        private final java.util.List<ItemStack> eggs = new java.util.ArrayList<>();
        EggSheet() {
            super(Component.literal("Tier eggs"));
            for (var tier : com.cosmocraft.trading_cells.feature.mobfarm.domain.model.EssenceTier.values()) {
                var egg = com.cosmocraft.trading_cells.feature.mobfarm.adapters.input.CreatureGeneratorEggs.preview(tier);
                if (net.neoforged.fml.ModList.get().isLoaded("roughlyenoughitems")) {
                    var display = com.cosmocraft.trading_cells.platform.neoforge.integration.rei.EssenceReiDisplay.generation(tier);
                    egg = display.getOutputEntries().getFirst().getFirst().<ItemStack>castValue();
                }
                if (com.cosmocraft.trading_cells.feature.mobfarm.adapters.input.EntityEssenceData.tier(egg) != tier) {
                    throw new IllegalStateException("REI output egg lost its tier");
                }
                eggs.add(egg);
            }
        }
        @Override public void extractRenderState(GuiGraphicsExtractor g, int x, int y, float partial) {
            g.fill(0, 0, width, height, 0xFF282C30);
            for (int i = 0; i < eggs.size(); i++) {
                g.pose().pushMatrix();
                g.pose().translate(width / 2F - 117 + i * 62, height / 2F - 24);
                g.pose().scale(3, 3);
                g.item(eggs.get(i), 0, 0);
                g.pose().popMatrix();
            }
        }
        @Override public boolean isPauseScreen() { return false; }
    }
    private static void capture(Minecraft mc, String name) {
        var model = mc.getEntityRenderDispatcher().getPlayerRenderer(mc.player).getModel();
        if (mc.options.getCameraType() != CameraType.FIRST_PERSON && mc.player.getMainHandItem().getItem() instanceof StormWeaponItem) {
            for (var arm : java.util.List.of(model.leftArm, model.rightArm)) {
                var original = arm.getInitialPose();
                if (Math.abs(arm.x-original.x()) + Math.abs(arm.y-original.y()) + Math.abs(arm.z-original.z()) > 0.001F) {
                    throw new IllegalStateException("Storm pose detached a shoulder: " + name);
                }
            }
        }
        Screenshot.grab(mc.gameDirectory, name + ".png", mc.gameRenderer.mainRenderTarget(), 1, message -> { });
    }
    private static final class Sheet extends Screen {
        private final java.util.List<ItemStack> items = java.util.List.of(
                CombatRegistrationAdapter.STORM_SHARD_ITEM.get().getDefaultInstance(),
                CombatRegistrationAdapter.CAPACITOR.get().getDefaultInstance(), CombatRegistrationAdapter.STATION_ITEM.get().getDefaultInstance(),
                CombatRegistrationAdapter.BATON.get().getDefaultInstance(), CombatRegistrationAdapter.WELDER.get().getDefaultInstance());
        Sheet() { super(Component.literal("Storm models")); }
        @Override public void extractRenderState(GuiGraphicsExtractor g, int x, int y, float partial) {
            g.fill(0, 0, width, height, 0xFF282C30);
            for (int i = 0; i < items.size(); i++) {
                g.pose().pushMatrix(); g.pose().translate(width / 2F - 120 + i % 3 * 85, height / 2F - 75 + i / 3 * 85);
                g.pose().scale(4, 4); g.fill(0, 0, 16, 16, 0xFFAAAAAA); g.item(items.get(i), 0, 0); g.pose().popMatrix();
            }
        }
        @Override public boolean isPauseScreen() { return false; }
    }

    private static final class ChargeSheet extends Screen {
        private final java.util.List<ItemStack> items = new java.util.ArrayList<>();
        ChargeSheet() {
            super(Component.literal("Weapon charge bars"));
            for (var item : java.util.List.of(CombatRegistrationAdapter.BATON.get(), CombatRegistrationAdapter.WELDER.get(), CombatRegistrationAdapter.CAPACITOR.get())) {
                for (int charge : new int[] {256, 128, 16, 0}) {
                    var stack = item.getDefaultInstance();
                    if (item instanceof VoltaicCapacitorItem) {
                        StormChargeData.setCharge(stack, charge);
                    } else {
                        var cell = CombatRegistrationAdapter.CAPACITOR.get().getDefaultInstance();
                        StormChargeData.setCharge(cell, charge);
                        StormWeaponItem.setBattery(stack, cell);
                    }
                    items.add(stack);
                }
            }
        }
        @Override public void extractRenderState(GuiGraphicsExtractor g, int x, int y, float partial) {
            g.fill(0, 0, width, height, 0xFF282C30);
            for (int i = 0; i < items.size(); i++) {
                g.pose().pushMatrix();
                g.pose().translate(width / 2F - 145 + i % 4 * 75, height / 2F - 110 + i / 4 * 75);
                g.pose().scale(4, 4);
                g.fill(0, 0, 16, 16, 0xFFAAAAAA);
                g.item(items.get(i), 0, 0);
                g.itemDecorations(Minecraft.getInstance().font, items.get(i), 0, 0);
                g.pose().popMatrix();
            }
        }
        @Override public boolean isPauseScreen() { return false; }
    }
}
