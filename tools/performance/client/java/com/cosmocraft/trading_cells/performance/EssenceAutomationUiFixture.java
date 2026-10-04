package com.cosmocraft.trading_cells.performance;

import com.cosmocraft.trading_cells.feature.infusion.adapters.input.ArcaneInfuserBlockEntity;
import com.cosmocraft.trading_cells.feature.infusion.adapters.output.ArcaneInfuserRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.infusion.adapters.output.client.ArcaneInfuserScreen;
import com.cosmocraft.trading_cells.feature.mobfarm.adapters.input.*;
import com.cosmocraft.trading_cells.feature.mobfarm.adapters.output.MobFarmRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.mobfarm.adapters.output.client.EssenceStabilizerScreen;
import com.cosmocraft.trading_cells.feature.mobfarm.adapters.output.client.EssenceWorkbenchScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

/** Real packets and clicks; screenshots include the intermediate reload poses. */
final class EssenceAutomationUiFixture {
    private static int phase;
    private static long nextAction;
    private static long started;
    private static BlockPos origin;
    private static volatile boolean prepared;
    private static boolean rendered;
    private static volatile int extractionTarget = -1;
    private static Screen stabilizerScreen;
    private EssenceAutomationUiFixture() { }
    static boolean active() {
        String fixture = System.getProperty("trading_cells.performance.client.uiFixture", "");
        return fixture.equals("essence-automation") || fixture.equals("syringe");
    }
    static boolean ready() { return rendered; }
    static boolean prepared() { return prepared; }

    static void prepare(ServerPlayer player, BlockPos pos) {
        if (prepared) { return; }
        origin = pos.immutable();
        var level = player.level();
        for (BlockPos target : BlockPos.betweenClosed(pos.offset(-2, -1, -2), pos.offset(4, 3, 4))) {
            level.setBlockAndUpdate(target, target.getY() == pos.getY() - 1 ? Blocks.SMOOTH_STONE.defaultBlockState() : Blocks.AIR.defaultBlockState());
        }
        player.getInventory().clearContent();
        player.getInventory().setSelectedSlot(0);
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        player.setExperiencePoints(0);
        player.setExperienceLevels(0);
        player.setItemInHand(InteractionHand.MAIN_HAND, MobFarmRegistrationAdapter.ESSENCE_EXTRACTOR.get().getDefaultInstance());
        player.getInventory().setItem(9, new ItemStack(MobFarmRegistrationAdapter.EMPTY_VIAL.get(), 3));
        if (System.getProperty("trading_cells.performance.client.uiFixture", "").equals("syringe")) {
            player.inventoryMenu.broadcastChanges();
            phase = 11;
            prepared = true;
            return;
        }
        var cow = EntityTypes.COW.create(level, EntitySpawnReason.LOAD);
        level.setBlockAndUpdate(pos, MobFarmRegistrationAdapter.ESSENCE_WORKBENCH.get().defaultBlockState());
        var bench = (EssenceWorkbenchBlockEntity) level.getBlockEntity(pos);
        bench.setItem(0, EntityEssenceData.essenceOf(cow));
        bench.setItem(1, MobFarmRegistrationAdapter.MODEL_BASES.getFirst().get().getDefaultInstance());
        bench.experience().setRaw(1_000);
        level.setBlockAndUpdate(pos.east(), MobFarmRegistrationAdapter.STABILIZER.get().defaultBlockState());
        var stabilizer = (EssenceStabilizerBlockEntity) level.getBlockEntity(pos.east());
        stabilizer.setItem(0, EntityEssenceData.rawEssenceOf(cow));
        stabilizer.setItem(1, Items.AMETHYST_SHARD.getDefaultInstance());
        stabilizer.setItem(2, new ItemStack(Items.REDSTONE, 2));
        level.setBlockAndUpdate(pos.east(2), ArcaneInfuserRegistrationAdapter.BLOCK.get().defaultBlockState());
        var infuser = (ArcaneInfuserBlockEntity) level.getBlockEntity(pos.east(2));
        player.inventoryMenu.broadcastChanges();
        player.openMenu(bench);
        prepared = true;
    }

    static void inspect(Minecraft minecraft) {
        if (rendered || !prepared || minecraft.player == null || minecraft.level == null || System.nanoTime() < nextAction) { return; }
        if (phase >= 26 && phase <= 29 && minecraft.player.isUsingItem()) { assertExtractionArm(minecraft); }
        if (started == 0) { started = System.nanoTime(); }
        if (System.nanoTime() - started > 45_000_000_000L) { throw new IllegalStateException("Essence UI phase timed out: " + phase); }
        var screen = minecraft.gui.screen();
        if (phase <= 5) {
            if (!(screen instanceof EssenceWorkbenchScreen bench)) { return; }
            var menu = bench.getMenu();
            int x = bench.getLeftPos(), y = bench.getTopPos();
            switch (phase) {
                case 0 -> { if (menu.getSlot(3).getItem().isEmpty()) { return; } click(screen, x + 191, y + 133); }
                case 1 -> { if (!menu.fillStorage()) { return; } click(screen, x + 191, y + 133); }
                case 2 -> { if (menu.fillStorage()) { return; } click(screen, x + 278, y + 107); }
                case 3 -> { if (menu.storedExperience() != 0) { return; } click(screen, x + 198, y + 107); }
                case 4 -> {
                    if (menu.getSlot(3).getItem().isEmpty() || menu.storedExperience() != 1_000) { return; }
                    capture(minecraft, "01-workbench");
                    click(screen, x + 126, y + 58);
                }
                case 5 -> {
                    if (!menu.getCarried().is(MobFarmRegistrationAdapter.ENTITY_MODULE.get()) || menu.storedExperience() != 0) { return; }
                    capture(minecraft, "02-synthesis");
                    server(minecraft, player -> player.openMenu((EssenceStabilizerBlockEntity) player.level().getBlockEntity(origin.east())));
                }
                default -> { }
            }
        } else if (phase == 6) {
            if (stabilizerScreen == null && (!(screen instanceof EssenceStabilizerScreen stabilizer)
                    || stabilizer.getMenu().getSlot(3).getItem().isEmpty())) { return; }
            if (net.neoforged.fml.ModList.get().isLoaded("roughlyenoughitems")) {
                if (stabilizerScreen == null) {
                    var category = com.cosmocraft.trading_cells.platform.neoforge.integration.rei.EssenceReiDisplay.STABILIZATION;
                    if (me.shedaniel.rei.api.client.registry.display.DisplayRegistry.getInstance().get(category).size() != 4) { return; }
                    stabilizerScreen = screen;
                    capture(minecraft, "03-stabilizer");
                    if (!me.shedaniel.rei.api.client.view.ViewSearchBuilder.builder().addCategory(category).open()) {
                        throw new IllegalStateException("Cannot open stabilizer recipes");
                    }
                    nextAction = System.nanoTime() + 500_000_000L;
                    return;
                }
                assertReiOrder(screen);
                capture(minecraft, "03b-stabilizer-recipes");
                minecraft.gui.setScreen(stabilizerScreen);
            } else {
                capture(minecraft, "03-stabilizer");
            }
            server(minecraft, player -> {
                player.openMenu((ArcaneInfuserBlockEntity) player.level().getBlockEntity(origin.east(2)));
                var recipe = player.level().recipeAccess().getRecipes().stream()
                        .filter(holder -> holder.id().identifier().getPath().equals("experience_bottle_infusion")).findFirst().orElseThrow();
                ((com.cosmocraft.trading_cells.feature.infusion.adapters.input.ArcaneInfuserMenu) player.containerMenu)
                        .handlePlacement(false, false, recipe, player.level(), player.getInventory());
            });
        } else if (phase <= 10) {
            if (!(screen instanceof ArcaneInfuserScreen infuser)) { return; }
            var menu = infuser.getMenu();
            // Slot coordinates remain correct even if the recipe book moves the screen.
            int x = infuser.getGuiLeft(), y = infuser.getGuiTop();
            switch (phase) {
                case 7 -> { if (menu.automationDisplay() == null) { return; } click(screen, x + 98, y + 84); }
                case 8 -> {
                    if (menu.lockState() != 1 || !menu.lockedRecipeName().endsWith("experience_bottle_infusion")) { return; }
                    click(screen, x + 78, y + 84);
                }
                case 9 -> {
                    if (!menu.fillStorage() || menu.lockState() != 1) { return; }
                    capture(minecraft, "04-infuser-locked");
                    server(minecraft, player -> {
                        var machine = (ArcaneInfuserBlockEntity) player.level().getBlockEntity(origin.east(2));
                        machine.setItem(0, Items.GLASS_BOTTLE.getDefaultInstance());
                        machine.experience().setRaw(11);
                        machine.setChanged();
                    });
                }
                case 10 -> {
                    if (!menu.getSlot(9).getItem().is(Items.EXPERIENCE_BOTTLE) || menu.storedExperience() != 0) { return; }
                    capture(minecraft, "05-infuser-output");
                    minecraft.player.closeContainer();
                    minecraft.player.getInventory().setSelectedSlot(0);
                }
                default -> { }
            }
        } else {
            switch (phase) {
                case 11 -> {
                    if (screen != null) { return; }
                    if (!minecraft.player.getInventory().getItem(0).is(MobFarmRegistrationAdapter.ESSENCE_EXTRACTOR.get())
                            || minecraft.player.getInventory().getItem(9).getCount() != 3) { return; }
                    if (minecraft.player.getInventory().getSelectedSlot() != 0) {
                        minecraft.player.getInventory().setSelectedSlot(0);
                        nextAction = System.nanoTime() + 500_000_000L;
                        return;
                    }
                    capture(minecraft, "06-syringe-empty");
                    minecraft.options.keyUse.setDown(true);
                    minecraft.gameMode.useItem(minecraft.player, InteractionHand.MAIN_HAND);
                }
                case 12 -> {
                    if (!minecraft.player.isUsingItem() || minecraft.player.getUseItemRemainingTicks() > 17) { return; }
                    capture(minecraft, "07-syringe-lift");
                }
                case 13 -> {
                    if (!minecraft.player.isUsingItem() || minecraft.player.getUseItemRemainingTicks() > 5) { return; }
                    capture(minecraft, "08-syringe-seat");
                }
                case 14 -> {
                    if (!EssenceExtractorItem.isLoaded(minecraft.player.getMainHandItem())) { return; }
                    minecraft.options.keyUse.setDown(false);
                    if (minecraft.player.getInventory().getItem(9).getCount() != 2) { throw new IllegalStateException("Reload did not consume exactly one vial"); }
                    capture(minecraft, "09-syringe-loaded");
                }
                case 15 -> {
                    minecraft.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT);
                    minecraft.options.fov().set(45);
                }
                case 16 -> {
                    capture(minecraft, "10-syringe-third-person");
                    minecraft.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
                    minecraft.options.fov().set(70);
                    server(minecraft, player -> {
                        var cow = EntityTypes.COW.create(player.level(), EntitySpawnReason.LOAD);
                        var offset = player.getLookAngle().multiply(2, 0, 2);
                        cow.setPos(player.position().add(offset));
                        cow.setNoAi(true); cow.setNoGravity(true);
                        cow.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(500);
                        cow.setHealth(500);
                        player.level().addFreshEntity(cow);
                        extractionTarget = cow.getId();
                    });
                }
                case 17 -> {
                    var target = minecraft.level.getEntity(extractionTarget);
                    if (target == null) { return; }
                    minecraft.options.keyUse.setDown(true);
                    minecraft.gameMode.interact(minecraft.player, target, new net.minecraft.world.phys.EntityHitResult(target), InteractionHand.MAIN_HAND);
                }
                case 18 -> {
                    if (!minecraft.player.isUsingItem() || minecraft.player.getUseItemRemainingTicks() > 60) { return; }
                    assertExtractionModel(minecraft);
                    capture(minecraft, "11-extraction-half");
                }
                case 19 -> {
                    if (!minecraft.player.isUsingItem() || minecraft.player.getUseItemRemainingTicks() > 10) { return; }
                    assertExtractionModel(minecraft);
                    capture(minecraft, "12-extraction-filled");
                }
                case 20 -> {
                    if (EssenceExtractorItem.isLoaded(minecraft.player.getMainHandItem())) { return; }
                    minecraft.options.keyUse.setDown(false);
                    if (minecraft.player.getInventory().getNonEquipmentItems().stream().noneMatch(stack -> stack.is(MobFarmRegistrationAdapter.RAW_ESSENCE.get()))) { return; }
                    capture(minecraft, "13-extraction-complete");
                    minecraft.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT);
                    minecraft.options.fov().set(45);
                    server(minecraft, player -> player.level().getEntity(extractionTarget).setInvisible(true));
                }
                case 21 -> {
                    if (minecraft.player.getCooldowns().isOnCooldown(minecraft.player.getMainHandItem())) { return; }
                    minecraft.options.keyUse.setDown(true);
                    minecraft.gameMode.useItem(minecraft.player, InteractionHand.MAIN_HAND);
                }
                case 22 -> {
                    if (!minecraft.player.isUsingItem() || minecraft.player.getUseItemRemainingTicks() > 17) { return; }
                    capture(minecraft, "14-third-person-reload-approach");
                }
                case 23 -> {
                    if (!minecraft.player.isUsingItem() || minecraft.player.getUseItemRemainingTicks() > 7) { return; }
                    capture(minecraft, "15-third-person-reload-seat");
                }
                case 24 -> {
                    if (!EssenceExtractorItem.isLoaded(minecraft.player.getMainHandItem())) { return; }
                    minecraft.options.keyUse.setDown(false);
                    capture(minecraft, "16-third-person-reload-finished");
                }
                case 25 -> {
                    minecraft.options.keyUse.setDown(true);
                    var target = minecraft.level.getEntity(extractionTarget);
                    minecraft.gameMode.interact(minecraft.player, target, new net.minecraft.world.phys.EntityHitResult(target), InteractionHand.MAIN_HAND);
                }
                case 26 -> {
                    if (!minecraft.player.isUsingItem() || minecraft.player.getUseItemRemainingTicks() > 100) { return; }
                    assertExtractionModel(minecraft);
                    capture(minecraft, "17-third-person-extraction-start");
                }
                case 27 -> {
                    if (!minecraft.player.isUsingItem() || minecraft.player.getUseItemRemainingTicks() > 60) { return; }
                    assertExtractionModel(minecraft);
                    capture(minecraft, "18-third-person-extraction-half");
                }
                case 28 -> {
                    if (!minecraft.player.isUsingItem() || minecraft.player.getUseItemRemainingTicks() > 2) { return; }
                    capture(minecraft, "19-third-person-extraction-filled");
                }
                case 29 -> {
                    if (EssenceExtractorItem.isLoaded(minecraft.player.getMainHandItem())) { return; }
                    minecraft.options.keyUse.setDown(false);
                    minecraft.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
                    minecraft.options.fov().set(70);
                    server(minecraft, player -> {
                        var target = player.level().getEntity(extractionTarget);
                        if (target != null) { target.discard(); }
                        BlockPos pos = origin.east();
                        player.level().setBlockAndUpdate(pos, MobFarmRegistrationAdapter.STABILIZER.get().defaultBlockState());
                        var machine = (EssenceStabilizerBlockEntity) player.level().getBlockEntity(pos);
                        var cow = EntityTypes.COW.create(player.level(), EntitySpawnReason.LOAD);
                        machine.setItem(0, EntityEssenceData.rawEssenceOf(cow));
                        machine.setItem(1, Items.AMETHYST_SHARD.getDefaultInstance());
                        machine.setItem(2, new ItemStack(Items.REDSTONE, 2));
                    });
                }
                case 30 -> capture(minecraft, "20-stabilizer-jade");
                case 31 -> {
                    if (!net.neoforged.fml.ModList.get().isLoaded("roughlyenoughitems")) { phase = 32; break; }
                    var category = com.cosmocraft.trading_cells.platform.neoforge.integration.rei.EssenceReiDisplay.STABILIZATION;
                    if (!me.shedaniel.rei.api.client.view.ViewSearchBuilder.builder().addCategory(category).filterCategory(category).open()) {
                        throw new IllegalStateException("Missing stabilizer REI display");
                    }
                }
                case 32 -> { assertReiOrder(screen); capture(minecraft, "21-stabilizer-rei"); }
                case 33 -> {
                    minecraft.gui.setScreen(null);
                    server(minecraft, player -> {
                        player.setItemInHand(InteractionHand.MAIN_HAND, net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(
                                net.minecraft.resources.Identifier.fromNamespaceAndPath("trading_cells", "pipe_wrench")).orElseThrow().getDefaultInstance());
                        player.inventoryMenu.broadcastChanges();
                    });
                }
                case 34 -> capture(minecraft, "22-wrench-first-person");
                case 35 -> {
                    minecraft.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT);
                    minecraft.options.fov().set(60);
                }
                case 36 -> capture(minecraft, "23-wrench-third-person");
                case 37 -> {
                    minecraft.gui.setScreen(new EssenceSheet());
                }
                default -> { return; }
            }
        }
        System.out.println("Essence automation UI phase " + phase + " passed");
        phase++;
        nextAction = System.nanoTime() + (phase >= 30 ? 1_000_000_000L
                : phase >= 12 && phase <= 14 || phase >= 18 ? 20_000_000L : 500_000_000L);
    }

    private static void server(Minecraft minecraft, java.util.function.Consumer<ServerPlayer> action) {
        var id = minecraft.player.getUUID();
        minecraft.getSingleplayerServer().execute(() -> action.accept(minecraft.getSingleplayerServer().getPlayerList().getPlayer(id)));
    }
    private static void assertReiOrder(Screen screen) {
        try {
            var owner = Class.forName("me.shedaniel.rei.impl.client.gui.screen.AbstractDisplayViewingScreen");
            var field = owner.getDeclaredField("categoryMap");
            field.setAccessible(true);
            var categories = (java.util.Map<?, ?>) field.get(screen);
            if (categories.size() != 1) { throw new IllegalStateException("Unexpected REI categories"); }
            var displays = (java.util.List<?>) categories.values().iterator().next();
            if (displays.size() != 4) { throw new IllegalStateException("Missing stabilizer recipes"); }
            for (int i = 0; i < displays.size(); i++) {
                var spec = (me.shedaniel.rei.impl.display.DisplaySpec) displays.get(i);
                var display = (com.cosmocraft.trading_cells.platform.neoforge.integration.rei.EssenceReiDisplay) spec.provideInternalDisplay();
                if (display.tier() != i + 1) { throw new IllegalStateException("Stabilizer REI tier order is not I-IV"); }
            }
        } catch (ReflectiveOperationException exception) { throw new IllegalStateException("Cannot inspect REI view", exception); }
    }
    private static void click(Screen screen, int x, int y) {
        var event = new MouseButtonEvent(x, y, new MouseButtonInfo(0, 0));
        screen.mouseClicked(event, false); screen.mouseReleased(event);
    }
    private static void assertExtractionModel(Minecraft minecraft) {
        var stack = minecraft.player.getMainHandItem();
        float pose = new com.cosmocraft.trading_cells.feature.mobfarm.adapters.output.client.SyringeExtractionProperty()
                .get(stack, minecraft.level, minecraft.player, 0);
        if (pose < 4 || pose > 5) { throw new IllegalStateException("Tier III extraction model missing: " + pose); }
        System.out.println("Syringe extraction model pose: " + pose);
    }
    private static void assertExtractionArm(Minecraft minecraft) {
        if (minecraft.player.getTicksUsingItem() < 6) { return; }
        var renderer = minecraft.getEntityRenderDispatcher().getRenderer(minecraft.player);
        var state = (net.minecraft.client.renderer.entity.state.AvatarRenderState) renderer.createRenderState(minecraft.player, 0);
        var model = new net.minecraft.client.model.player.PlayerModel(minecraft.getEntityModels().bakeLayer(
                net.minecraft.client.model.geom.ModelLayers.PLAYER), false);
        model.setupAnim(state);
        var arm = model.getArm(minecraft.player.getMainArm());
        float expected = -net.minecraft.util.Mth.HALF_PI + model.head.xRot * 0.5F;
        if (Math.abs(arm.xRot - expected) > 0.001F) {
            throw new IllegalStateException("Extraction arm lowered before filling ended: " + minecraft.player.getUseItemRemainingTicks());
        }
    }
    private static void capture(Minecraft minecraft, String name) {
        Screenshot.grab(minecraft.gameDirectory, name + ".png", minecraft.gameRenderer.mainRenderTarget(), 1, message -> { });
    }
    private static final class EssenceSheet extends Screen {
        private final java.util.List<ItemStack> items = new java.util.ArrayList<>();
        EssenceSheet() {
            super(Component.literal("Essence assets"));
            items.add(MobFarmRegistrationAdapter.ESSENCE_EXTRACTOR.get().getDefaultInstance());
            ItemStack loaded = items.getFirst().copy(); EssenceExtractorItem.setLoaded(loaded, true); items.add(loaded);
            items.add(MobFarmRegistrationAdapter.EMPTY_VIAL.get().getDefaultInstance());
            items.add(MobFarmRegistrationAdapter.STABILIZER_ITEM.get().getDefaultInstance());
            items.add(net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(
                    net.minecraft.resources.Identifier.fromNamespaceAndPath("trading_cells", "pipe_wrench")).orElseThrow().getDefaultInstance());
            for (var item : java.util.List.of(MobFarmRegistrationAdapter.RAW_ESSENCE, MobFarmRegistrationAdapter.ENTITY_ESSENCE)) {
                for (int tier = 1; tier <= 4; tier++) {
                    ItemStack stack = item.get().getDefaultInstance();
                    var data = new net.minecraft.nbt.CompoundTag();
                    var essence = new net.minecraft.nbt.CompoundTag();
                    essence.putString("Type", "minecraft:cow");
                    essence.putInt("Tier", tier);
                    essence.putInt("ClassificationVersion", 1);
                    data.put("TradingCellsEssence", essence);
                    stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                            net.minecraft.world.item.component.CustomData.of(data));
                    float selected = new com.cosmocraft.trading_cells.feature.mobfarm.adapters.output.client.EssenceTierProperty()
                            .get(stack, null, null, 0);
                    if (selected != tier) { throw new IllegalStateException("Incorrect essence tier model: " + selected); }
                    items.add(stack);
                }
            }
            for (var base : MobFarmRegistrationAdapter.MODEL_BASES) { items.add(base.get().getDefaultInstance()); }
        }
        @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            graphics.fill(0, 0, width, height, 0xFF262C2F);
            int left = (width - 280) / 2, top = (height - 300) / 2;
            for (int i = 0; i < items.size(); i++) {
                graphics.pose().pushMatrix();
                graphics.pose().translate(left + i % 4 * 70, top + i / 4 * 75);
                graphics.pose().scale(3.5F, 3.5F);
                graphics.item(items.get(i), 0, 0);
                graphics.pose().popMatrix();
            }
            rendered = true;
        }
        @Override public boolean isPauseScreen() { return false; }
    }
}
