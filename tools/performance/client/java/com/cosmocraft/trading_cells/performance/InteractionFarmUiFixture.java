package com.cosmocraft.trading_cells.performance;

import com.mojang.blaze3d.platform.InputConstants;

import com.cosmocraft.trading_cells.feature.captures.adapters.api.CapturedMobStackAdapter;
import com.cosmocraft.trading_cells.feature.captures.domain.model.CapturedMobKind;
import com.cosmocraft.trading_cells.feature.mobfarm.adapters.input.EntityEssenceData;
import com.cosmocraft.trading_cells.feature.mobfarm.adapters.input.EntityInteractionFarmBlockEntity;
import com.cosmocraft.trading_cells.feature.mobfarm.adapters.output.MobFarmRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.mobfarm.adapters.output.client.EntityInteractionFarmScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

/** Real menu packets, both held-item render states and the actual machine in a disposable world. */
final class InteractionFarmUiFixture {
    private static volatile boolean prepared;
    private static BlockPos origin;
    private static long nextAction;
    private static int phase;
    private InteractionFarmUiFixture() { }
    static boolean active() { return System.getProperty("trading_cells.performance.client.uiFixture", "").equals("interaction-farm"); }
    static boolean ready() { return phase >= 6; }
    static void prepare(ServerPlayer player, BlockPos pos) {
        if (prepared) { return; }
        origin = pos.immutable();
        var level = player.level();
        for (var target : BlockPos.betweenClosed(pos.offset(-2, -1, -2), pos.offset(4, 3, 3))) {
            level.setBlockAndUpdate(target, target.getY() == pos.getY() - 1 ? Blocks.SMOOTH_STONE.defaultBlockState() : Blocks.AIR.defaultBlockState());
        }
        var villager = EntityTypes.VILLAGER.create(level, EntitySpawnReason.LOAD);
        var worker = new ItemStack(CapturedMobStackAdapter.capturerItem(CapturedMobKind.VILLAGER));
        CapturedMobStackAdapter.setData(CapturedMobKind.VILLAGER, worker, CapturedMobStackAdapter.createVillagerData(villager));
        for (int i = 0; i < 2; i++) {
            var target = pos.east(i * 2);
            level.setBlockAndUpdate(target, MobFarmRegistrationAdapter.INTERACTION_BLOCK.get().defaultBlockState());
            var farm = (EntityInteractionFarmBlockEntity) level.getBlockEntity(target);
            farm.setItem(0, worker.copy()); farm.setItem(1, EntityEssenceData.moduleFor(level, Identifier.withDefaultNamespace(i == 0 ? "bee" : "cow")));
            farm.setItem(2, new ItemStack(i == 0 ? Items.GLASS_BOTTLE : Items.BUCKET, 8));
            if (i == 0) { farm.setItem(3, new ItemStack(Items.SHEARS)); }
            farm.setItem(4, MobFarmRegistrationAdapter.UPGRADES.get(2).get().getDefaultInstance());
            for (int tick = 0; tick < 150; tick++) { farm.processServerTick(); }
        }
        level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                new net.minecraft.world.phys.AABB(pos).inflate(6)).forEach(net.minecraft.world.entity.Entity::discard);
        player.getInventory().clearContent();
        player.getInventory().setItem(0, MobFarmRegistrationAdapter.INTERACTION_ITEM.get().getDefaultInstance());
        player.getInventory().setItem(9, new ItemStack(Items.GLASS_BOTTLE, 32));
        player.getInventory().setSelectedSlot(0); player.inventoryMenu.broadcastChanges();
        player.openMenu((EntityInteractionFarmBlockEntity) level.getBlockEntity(pos));
        prepared = true;
    }
    static void inspect(Minecraft minecraft) {
        if (!prepared || ready() || minecraft.player == null || minecraft.level == null || System.nanoTime() < nextAction) { return; }
        if (phase < 3) {
            if (!(minecraft.gui.screen() instanceof EntityInteractionFarmScreen screen)) { return; }
            var menu = screen.getMenu();
            if (menu.getSlot(0).getItem().isEmpty()) { return; }
            if (phase == 0) {
                if (menu.slots.size() != 69 || menu.getSlot(31).x != 191 || menu.getSlot(31).y != 92
                        || menu.getSlot(32).x != 187 || menu.getSlot(32).y != 32) {
                    throw new IllegalStateException("Interaction farm slot grid changed");
                }
                if (screen.children().stream().anyMatch(child -> child instanceof net.minecraft.client.gui.components.AbstractWidget widget
                        && widget.getX() == screen.getLeftPos() + 232 && widget.getY() == screen.getTopPos() + 7)) {
                    throw new IllegalStateException("Retired redstone control still present");
                }
                if (net.neoforged.fml.ModList.get().isLoaded("roughlyenoughitems")) {
                    boolean found = me.shedaniel.rei.api.client.registry.entry.EntryRegistry.getInstance().getEntryStacks()
                            .anyMatch(entry -> entry.getValue() instanceof ItemStack stack && stack.is(MobFarmRegistrationAdapter.INTERACTION_ITEM.get()));
                    if (!found) { throw new IllegalStateException("Interaction farm missing from REI"); }
                }
                click(screen, screen.getLeftPos() + 234, screen.getTopPos() + 15);
            } else if (phase == 1) {
                if (menu.enabled() || !menu.paused()) { return; }
                click(screen, screen.getLeftPos() + 234, screen.getTopPos() + 15);
            } else {
                if (!menu.enabled() || menu.paused()) { return; }
                Screenshot.grab(minecraft.gameDirectory, "interaction-farm-menu.png", minecraft.gameRenderer.mainRenderTarget(), 1, message -> { });
                minecraft.player.closeContainer();
                System.out.println("Interaction farm menu: 9x3 outputs, manual pause and resume packets verified");
            }
        } else if (phase == 3) {
            var farm = minecraft.level.getBlockEntity(origin);
            if (farm == null || minecraft.getBlockEntityRenderDispatcher().getRenderer(farm) == null) {
                throw new IllegalStateException("Missing interaction farm renderer");
            }
            Screenshot.grab(minecraft.gameDirectory, "interaction-farm-models.png", minecraft.gameRenderer.mainRenderTarget(), 1, message -> { });
            System.out.println("Interaction farm render: two-item worker and single-item fallback captured for visual review");
            if (!net.neoforged.fml.ModList.get().isLoaded("roughlyenoughitems")) { phase = 5; }
        } else if (phase == 4) {
            var category = com.cosmocraft.trading_cells.platform.neoforge.integration.rei.ArcaneInfusionReiDisplay.CATEGORY;
            var display = me.shedaniel.rei.api.client.registry.display.DisplayRegistry.getInstance().get(category).stream()
                    .filter(value -> value.getDisplayLocation().map(id -> id.getPath().equals("entity_interaction_farm_infusion")).orElse(false))
                    .findFirst().orElseThrow(() -> new IllegalStateException("Missing interaction farm infusion in REI"));
            if (display.experience() != 50_000) { throw new IllegalStateException("Wrong interaction farm REI XP cost"); }
            if (!me.shedaniel.rei.api.client.view.ViewSearchBuilder.builder()
                    .addRecipesFor(me.shedaniel.rei.api.common.util.EntryStacks.of(MobFarmRegistrationAdapter.INTERACTION_ITEM.get())).open()) {
                throw new IllegalStateException("Cannot open interaction farm REI recipe");
            }
        } else {
            if (minecraft.gui.screen() == null || !minecraft.gui.screen().getClass().getName().contains("DisplayViewingScreen")) {
                throw new IllegalStateException("Interaction farm REI recipe screen missing");
            }
            Screenshot.grab(minecraft.gameDirectory, "interaction-farm-recipe.png", minecraft.gameRenderer.mainRenderTarget(), 1, message -> { });
            minecraft.gui.setScreen(null);
            System.out.println("Interaction farm REI recipe: 50000 XP verified and displayed");
        }
        phase++; nextAction = System.nanoTime() + 1_000_000_000L;
    }
    private static void click(net.minecraft.client.gui.screens.Screen screen, int x, int y) {
        var event = new MouseButtonEvent(x, y, new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0));
        screen.mouseClicked(event, false); screen.mouseReleased(event);
    }
}
