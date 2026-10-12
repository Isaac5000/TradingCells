package com.cosmocraft.trading_cells.performance;

import com.cosmocraft.trading_cells.feature.farmer.adapters.output.FarmerRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.quarry.adapters.output.QuarryRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.mobfarm.adapters.input.EssenceWorkbenchMenu;
import com.cosmocraft.trading_cells.feature.mobfarm.adapters.output.MobFarmRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.trader.adapters.output.TraderRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.infusion.adapters.output.ArcaneInfuserRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.breeders.adapters.output.BreederRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.incubators.adapters.output.IncubatorRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.ironfarm.adapters.output.IronFarmRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.converter.adapters.output.ConverterRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.trader.adapters.output.AutotraderRegistrationAdapter;
import java.util.List;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.MachineGuiSkin;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.MachineSlotSprites;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.trader.VillagerTradeSprites;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** Empty-slot silhouettes, expanded grids, speed input and shared XP mode in real menus. */
final class MachineFixesUiFixture {
    private static volatile int expectedMenu = -1;
    private static volatile BlockPos origin;
    private static int index, stage;
    private static long nextAction;
    private static List<Block> machines;
    private static boolean delegated;
    private static boolean checkingRei;
    private static int recipeIndex;
    private static net.minecraft.resources.Identifier selectedMaterial;
    private static final List<String> REI_CATEGORIES = List.of("farming", "piglin_farming", "quarry", "piglin_quarry",
            "villager_breeding", "piglin_breeding", "villager_incubation", "piglin_incubation", "iron_farm",
            "conversion", "piglin_bartering", "netherite_piglin_bartering", "essence_stabilization");

    private MachineFixesUiFixture() { }
    private static void verifyGuiResourceLocations() {
        for (var skin : MachineGuiSkin.values()) {
            if (skin != MachineGuiSkin.PIGLIN_BARTERING_CELL) {
                var background = skin.texture(204, 215);
                requireGui(background.getPath().startsWith("textures/gui/container/"), "Backgrounds belong under gui/container");
                requireGuiResource(background.getPath());
                requireGuiResource(background.getPath().replace("background.png", "background_compact.png"));
                requireGuiResource(background.getPath().replace("background.png", "background_small.png"));
            }
            switch (skin) {
                case VILLAGER_TRADING_CELL, AUTOTRADER, EXPERIENCE_STORAGE, MOB_FARM,
                        ENTITY_INTERACTION_FARM, ESSENCE_WORKBENCH, ESSENCE_STABILIZER -> { }
                default -> requireGuiResource(skin.texture(200, 124).getPath());
            }
        }
        requireGuiResource(MachineGuiSkin.ESSENCE_WORKBENCH.texture(78, 36).getPath());
        for (var sprite : java.util.List.of(MachineSlotSprites.VILLAGER_HEAD, MachineSlotSprites.PIGLIN_HEAD,
                MachineSlotSprites.CAPTURER, MachineSlotSprites.HOE, MachineSlotSprites.WHEAT_SEEDS,
                MachineSlotSprites.WEAKNESS_POTION, MachineSlotSprites.GOLDEN_APPLE, MachineSlotSprites.BREAD,
                MachineSlotSprites.PORKCHOP, VillagerTradeSprites.DROPDOWN, VillagerTradeSprites.DISABLED_SLOT_OVERLAY)) {
            requireGuiResource("textures/gui/sprites/" + sprite.getPath() + ".png");
        }
        for (var state : VillagerTradeSprites.State.values()) {
            requireGuiResource("textures/gui/sprites/" + VillagerTradeSprites.row(state).getPath() + ".png");
            requireGuiResource("textures/gui/sprites/" + VillagerTradeSprites.dropdownRow(state).getPath() + ".png");
            requireGuiResource("textures/gui/sprites/" + VillagerTradeSprites.arrow(state).getPath() + ".png");
        }
    }

    private static void requireGuiResource(String path) {
        requireGui(Minecraft.getInstance().getResourceManager().getResource(net.minecraft.resources.Identifier.fromNamespaceAndPath("trading_cells", path)).isPresent(),
                "GUI resource declared by Java must exist: " + path);
    }

    private static void requireGui(boolean condition, String message) {
        if (!condition) { throw new IllegalStateException(message); }
    }

    static void verifySearchFooter(AbstractContainerScreen<?> screen, int frame) {
        int bottom = screen.getTopPos() + screen.getImageHeight() + frame;
        var minecraft = Minecraft.getInstance();
        int scale = minecraft.getWindow().calculateScale(minecraft.options.guiScale().get(), minecraft.isEnforceUnicode());
        requireGui(minecraft.getWindow().getGuiScale() == scale, "Machine must preserve the configured GUI scale");
        requireGui(bottom <= screen.height - 22, "Recipe viewer footer must fit below " + screen.getClass().getSimpleName());
        requireGui(screen.width - screen.getLeftPos() - screen.getImageWidth() - frame >= 62,
                "Recipe viewer must have room for at least three native-size columns");
        for (var slot : screen.getMenu().slots) {
            if (!slot.isActive() || slot.x < 0 || slot.y < 0) { continue; }
            requireGui(slot.x >= 4 && slot.y >= 4 && slot.x + 16 <= screen.getImageWidth() - 4
                    && slot.y + 16 <= screen.getImageHeight() - 4, "Native-size slot must stay inside the frame");
            requireGui(screen.children().stream().filter(net.minecraft.client.gui.components.AbstractWidget.class::isInstance)
                    .map(net.minecraft.client.gui.components.AbstractWidget.class::cast).noneMatch(widget -> widget.visible
                            && widget.getX() < screen.getLeftPos() + slot.x + 16 && widget.getX() + widget.getWidth() > screen.getLeftPos() + slot.x
                            && widget.getY() < screen.getTopPos() + slot.y + 16 && widget.getY() + widget.getHeight() > screen.getTopPos() + slot.y),
                    "Controls must not overlap native-size slots");
        }
        if (net.neoforged.fml.ModList.get().isLoaded("roughlyenoughitems")) {
            var field = me.shedaniel.rei.api.client.REIRuntime.getInstance().getSearchTextField();
            requireGui(field != null, "REI search field must be present");
            var bounds = ((me.shedaniel.rei.api.client.gui.widgets.WidgetWithBounds) field).getBounds();
            requireGui(bounds.width >= 80 && bounds.height >= 14 && bounds.y >= bottom
                    && bounds.y + bounds.height <= screen.height, "REI search field must be complete and below the menu: " + bounds);
        }
    }
    static boolean active() { return System.getProperty("trading_cells.performance.client.uiFixture", "").equals("machine-fixes"); }
    static boolean ready() { return delegated && InteractionFarmUiFixture.ready(); }
    static boolean prepared() { return origin != null; }

    static void prepare(ServerPlayer player, BlockPos pos) {
        origin = pos.immutable();
        machines = List.of(FarmerRegistrationAdapter.FARMER_BLOCK.get(), FarmerRegistrationAdapter.PIGLIN_FARMER_BLOCK.get(),
                QuarryRegistrationAdapter.QUARRY_BLOCK.get(), QuarryRegistrationAdapter.PIGLIN_QUARRY_BLOCK.get(),
                MobFarmRegistrationAdapter.BLOCK.get(), MobFarmRegistrationAdapter.INTERACTION_BLOCK.get(),
                TraderRegistrationAdapter.NETHERITE_PIGLIN_BARTERING_CELL_BLOCK.get(),
                MobFarmRegistrationAdapter.ESSENCE_WORKBENCH.get(), MobFarmRegistrationAdapter.STABILIZER.get(),
                ArcaneInfuserRegistrationAdapter.BLOCK.get(), BreederRegistrationAdapter.VILLAGER_BREEDER_BLOCK.get(),
                BreederRegistrationAdapter.PIGLIN_BREEDER_BLOCK.get(), IncubatorRegistrationAdapter.VILLAGER_INCUBATOR_BLOCK.get(),
                IncubatorRegistrationAdapter.PIGLIN_INCUBATOR_BLOCK.get(), IronFarmRegistrationAdapter.IRON_FARM_BLOCK.get(),
                ConverterRegistrationAdapter.CONVERTER_BLOCK.get(), TraderRegistrationAdapter.VILLAGER_TRADER_BLOCK.get(),
                AutotraderRegistrationAdapter.AUTOTRADER_BLOCK.get(),
                com.cosmocraft.trading_cells.feature.experience.adapters.output.ExperienceStorageRegistrationAdapter.BLOCK.get());
        player.getInventory().clearContent();
        for (var target : BlockPos.betweenClosed(pos.offset(-2, -1, -2), pos.offset(3, 3, 3))) {
            player.level().setBlockAndUpdate(target, target.getY() == pos.getY() - 1 ? Blocks.SMOOTH_STONE.defaultBlockState() : Blocks.AIR.defaultBlockState());
        }
        open(player, 0);
    }

    private static void open(ServerPlayer player, int machine) {
        player.closeContainer();
        player.level().setBlockAndUpdate(origin, machines.get(machine).defaultBlockState());
        var entity = player.level().getBlockEntity(origin);
        if (entity instanceof com.cosmocraft.trading_cells.feature.quarry.adapters.input.QuarryBlockEntity quarry) {
            quarry.setItem(1, net.minecraft.world.item.Items.DIAMOND_PICKAXE.getDefaultInstance());
        }
        if (entity instanceof com.cosmocraft.trading_cells.feature.mobfarm.adapters.input.MobFarmBlockEntity farm) {
            farm.setItem(2, com.cosmocraft.trading_cells.feature.mobfarm.adapters.input.EntityEssenceData.moduleFor(
                    player.level(), net.minecraft.resources.Identifier.withDefaultNamespace("cow")));
        }
        if (entity instanceof com.cosmocraft.trading_cells.feature.infusion.adapters.input.ArcaneInfuserBlockEntity infuser) {
            infuser.setItem(0, net.minecraft.world.item.Items.GLASS_BOTTLE.getDefaultInstance());
            player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, net.minecraft.world.item.Items.IRON_HELMET.getDefaultInstance());
        }
        if (entity instanceof com.cosmocraft.trading_cells.feature.trader.adapters.input.VillagerTradingCellBlockEntity
                || entity instanceof com.cosmocraft.trading_cells.feature.trader.adapters.input.AutotraderBlockEntity) {
            var villager = net.minecraft.world.entity.EntityTypes.VILLAGER.create(player.level(), net.minecraft.world.entity.EntitySpawnReason.LOAD);
            var offers = new net.minecraft.world.item.trading.MerchantOffers();
            offers.add(new net.minecraft.world.item.trading.MerchantOffer(new net.minecraft.world.item.trading.ItemCost(net.minecraft.world.item.Items.EMERALD),
                    new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BREAD, 3), 12, 2, 0.05F));
            villager.setOffers(offers); villager.setVillagerXp(10);
            var kind = com.cosmocraft.trading_cells.feature.captures.domain.model.CapturedMobKind.VILLAGER;
            var capturer = new net.minecraft.world.item.ItemStack(com.cosmocraft.trading_cells.feature.captures.adapters.api.CapturedMobStackAdapter.capturerItem(kind));
            com.cosmocraft.trading_cells.feature.captures.adapters.api.CapturedMobStackAdapter.setData(kind, capturer,
                    com.cosmocraft.trading_cells.feature.captures.adapters.api.CapturedMobStackAdapter.createVillagerData(villager));
            if (entity instanceof com.cosmocraft.trading_cells.feature.trader.adapters.input.VillagerTradingCellBlockEntity trader) {
                trader.insertVillagerFromCapturer(capturer); trader.openTrade(player);
            } else {
                ((com.cosmocraft.trading_cells.feature.trader.adapters.input.AutotraderBlockEntity) entity).insertVillagerFromCapturer(capturer);
                player.openMenu((MenuProvider) entity);
            }
            villager.discard();
        } else { player.openMenu((MenuProvider) entity); }
        expectedMenu = player.containerMenu.containerId;
    }

    static void inspect(Minecraft minecraft) {
        if (delegated) { InteractionFarmUiFixture.inspect(minecraft); return; }
        if (checkingRei) { inspectRei(minecraft); return; }
        if (expectedMenu < 0 || minecraft.player == null || System.nanoTime() < nextAction
                || !(minecraft.gui.screen() instanceof AbstractContainerScreen<?> screen)
                || screen.getMenu().containerId != expectedMenu) { return; }
        if (stage == 0) {
            if (index == 0) { verifyGuiResourceLocations(); }
            nextAction = System.nanoTime() + 700_000_000L;
            stage = 1;
            return;
        }
        var menu = screen.getMenu();
        String name = BuiltInRegistries.BLOCK.getKey(machines.get(index)).getPath();
        if (stage >= 10 && stage <= 15) {
            if (menu instanceof com.cosmocraft.trading_cells.feature.quarry.adapters.input.QuarryMenu quarry) {
                inspectQuarryFilter(minecraft, screen, quarry, name);
            } else if (menu instanceof com.cosmocraft.trading_cells.feature.mobfarm.adapters.input.MobFarmMenu farm) {
                if (stage == 10) {
                    requireGui(!farm.enabled() && farm.whitelist(), "Entity farm controls synchronize");
                    capture(minecraft, name + "-whitelist");
                    click(screen, screen.getLeftPos() + 102, screen.getTopPos() + 36);
                    stage = 11;
                } else {
                    requireGui(!farm.whitelist(), "Entity farm switches back to blacklist");
                    capture(minecraft, name + "-blacklist");
                    stage = 6;
                }
            }
            nextAction = System.nanoTime() + 700_000_000L;
            return;
        }
        if (stage == 1) {
            int outer = MachineGuiSkin.frameOutset(screen.getLeftPos(), screen.getTopPos(),
                    screen.getImageWidth(), screen.getImageHeight(), screen.width, screen.height);
            requireGui(screen.getTopPos() - outer >= 0, "Machine top frame must fit: " + name);
            requireGui(screen.getTopPos() + screen.getImageHeight() + outer <= screen.height - 22,
                    "REI search footer must remain clear: " + name);
            verifySearchFooter(screen, outer);
            if (name.equals("iron_farm")) {
                var flowers = screen.children().stream().filter(net.minecraft.client.gui.components.Checkbox.class::isInstance)
                        .map(net.minecraft.client.gui.components.Checkbox.class::cast).findFirst().orElseThrow();
                requireGui(flowers.getX() == screen.getLeftPos()
                                + com.cosmocraft.trading_cells.platform.neoforge.menu.PlayerEquipmentSlots.X,
                        "Poppy control must sit one pixel right of the armor slot frame");
                requireGui(flowers.getY() + flowers.getHeight() == screen.getTopPos()
                                + com.cosmocraft.trading_cells.platform.neoforge.menu.PlayerEquipmentSlots.HEAD_Y - 2,
                        "Poppy control must leave one clear pixel above the armor frame");
            }
            if (net.neoforged.fml.ModList.get().isLoaded("roughlyenoughitems")) {
                var bounds = me.shedaniel.rei.api.client.registry.screen.ScreenRegistry.getInstance().getScreenBounds(screen);
                int outset = com.cosmocraft.trading_cells.platform.neoforge.client.screen.MachineGuiSkin.frameOutset(
                        screen.getLeftPos(), screen.getTopPos(), screen.getImageWidth(), screen.getImageHeight(), screen.width, screen.height);
                if (bounds.x != screen.getLeftPos() - outset || bounds.width != screen.getImageWidth() + 2 * outset) {
                    throw new IllegalStateException("REI overlaps decorative frame: " + name);
                }
            }
            if (index < 7) {
                int first = index < 4 ? 3 : index == 6 ? 6 : 5;
                for (int slot = 0; slot < 27; slot++) {
                    if (menu.getSlot(first + slot).x != menu.getSlot(first).x + slot % 9 * 18
                            || menu.getSlot(first + slot).y != menu.getSlot(first).y + slot / 9 * 18) {
                        throw new IllegalStateException("Incorrect 9x3 grid: " + name);
                    }
                }
            }
            capture(minecraft, name + "-empty");
            if (menu instanceof com.cosmocraft.trading_cells.feature.quarry.adapters.input.QuarryMenu) {
                click(screen, screen.getLeftPos() + 103, screen.getTopPos() + 15);
                click(screen, screen.getLeftPos() + 140, screen.getTopPos() + 55);
                stage = 10; nextAction = System.nanoTime() + 700_000_000L; return;
            }
            if (menu instanceof com.cosmocraft.trading_cells.feature.mobfarm.adapters.input.MobFarmMenu) {
                click(screen, screen.getLeftPos() + 325, screen.getTopPos() + 16);
                click(screen, screen.getLeftPos() + 102, screen.getTopPos() + 36);
                stage = 10; nextAction = System.nanoTime() + 700_000_000L; return;
            }
            if (index < 4) {
                server(minecraft, player -> ((Container) player.level().getBlockEntity(origin)).setItem(30,
                        MobFarmRegistrationAdapter.UPGRADES.getFirst().get().getDefaultInstance()));
                stage = 2; nextAction = System.nanoTime() + 700_000_000L; return;
            }
            if (menu instanceof EssenceWorkbenchMenu) {
                var event = new MouseButtonEvent(screen.getLeftPos() + 191, screen.getTopPos() + 133, new MouseButtonInfo(0, 0));
                screen.mouseClicked(event, false); screen.mouseReleased(event);
                stage = 2; nextAction = System.nanoTime() + 700_000_000L; return;
            }
            if (screen instanceof com.cosmocraft.trading_cells.feature.infusion.adapters.output.client.ArcaneInfuserScreen) {
                requireGui(screen.children().stream().noneMatch(net.minecraft.client.gui.screens.recipebook.RecipeBookComponent.class::isInstance),
                        "Synthesizer no longer has a recipe book");
                stage = 3; nextAction = System.nanoTime() + 700_000_000L; return;
            }
        } else {
            if (stage >= 3 && stage <= 5) {
                var helmet = menu.getSlot(menu.slots.size() - 5);
                if (stage == 3) {
                    capture(minecraft, name + "-equipment");
                    click(screen, screen.getLeftPos() + helmet.x + 8, screen.getTopPos() + helmet.y + 8);
                } else if (stage == 4) {
                    requireGui(menu.getCarried().is(net.minecraft.world.item.Items.IRON_HELMET), "Armor slot remains accessible");
                    click(screen, screen.getLeftPos() + helmet.x + 8, screen.getTopPos() + helmet.y + 8);
                } else {
                    requireGui(helmet.getItem().is(net.minecraft.world.item.Items.IRON_HELMET) && menu.getCarried().isEmpty(), "Equipment is restored");
                    System.out.println("Synthesizer leaves armor visible and usable at native scale");
                }
                stage++; nextAction = System.nanoTime() + 700_000_000L; return;
            }
            if (index < 4 && menu.getSlot(30).getItem().isEmpty()) { return; }
            if (menu instanceof EssenceWorkbenchMenu workbench && !workbench.fillStorage()) { return; }
            capture(minecraft, name + "-control");
        }
        System.out.println("Machine fixes UI verified: " + name);
        stage = 0; expectedMenu = -1;
        int next = ++index;
        if (next < machines.size()) { server(minecraft, player -> open(player, next)); }
        else {
            if (net.neoforged.fml.ModList.get().isLoaded("roughlyenoughitems")) {
                minecraft.player.closeContainer(); checkingRei = true; nextAction = System.nanoTime() + 700_000_000L;
            } else { delegate(minecraft); }
        }
    }

    private static void click(AbstractContainerScreen<?> screen, int x, int y) {
        var event = new MouseButtonEvent(x, y, new MouseButtonInfo(0, 0));
        screen.mouseClicked(event, false); screen.mouseReleased(event);
    }

    private static void inspectQuarryFilter(Minecraft minecraft, AbstractContainerScreen<?> screen,
            com.cosmocraft.trading_cells.feature.quarry.adapters.input.QuarryMenu quarry, String name) {
        if (stage == 10) {
            requireGui(!quarry.enabled() && !quarry.whitelist(), "Quarry pause and default blacklist synchronize");
            var entries = quarry.catalogEntries().stream().filter(entry -> entry.blockedReason() == 0).toList();
            requireGui(!entries.isEmpty(), "Quarry catalogue has selectable materials");
            selectedMaterial = entries.getFirst().materialId();
            click(screen, screen.getLeftPos() + 133, screen.getTopPos() + 101);
        } else if (stage == 11) {
            requireGui(quarry.materialListed(selectedMaterial), "Quarry row selection synchronizes");
            capture(minecraft, name + "-blacklist");
            click(screen, screen.getLeftPos() + 135, screen.getTopPos() + 73);
        } else if (stage == 12) {
            requireGui(quarry.whitelist() && quarry.materialListed(selectedMaterial), "Changing mode retains selected material");
            capture(minecraft, name + "-whitelist");
            click(screen, screen.getLeftPos() + 120, screen.getTopPos() + 40);
            click(screen, screen.getLeftPos() + 103, screen.getTopPos() + 15);
        } else {
            requireGui(quarry.enabled(), "Quarry reactivates from common control");
            server(minecraft, player -> ((Container) player.level().getBlockEntity(origin)).setItem(30,
                    MobFarmRegistrationAdapter.UPGRADES.getFirst().get().getDefaultInstance()));
            stage = 2;
            return;
        }
        stage++;
    }

    private static void inspectRei(Minecraft minecraft) {
        if (System.nanoTime() < nextAction) { return; }
        String name = REI_CATEGORIES.get(recipeIndex);
        if (stage == 0) {
            var category = me.shedaniel.rei.api.common.category.CategoryIdentifier.of("trading_cells", name);
            if (!me.shedaniel.rei.api.client.view.ViewSearchBuilder.builder().addCategory(category).filterCategory(category).open()) {
                throw new IllegalStateException("Cannot open themed REI category: " + name);
            }
            stage = 1;
        } else {
            if (minecraft.gui.screen() == null || !minecraft.gui.screen().getClass().getName().contains("DisplayViewingScreen")) {
                throw new IllegalStateException("Missing REI screen: " + name);
            }
            capture(minecraft, "rei-" + name); minecraft.gui.setScreen(null); stage = 0;
            if (++recipeIndex == REI_CATEGORIES.size()) { checkingRei = false; delegate(minecraft); }
        }
        nextAction = System.nanoTime() + 700_000_000L;
    }

    private static void delegate(Minecraft minecraft) {
        delegated = true;
        server(minecraft, player -> InteractionFarmUiFixture.prepare(player, origin));
    }

    private static void server(Minecraft minecraft, java.util.function.Consumer<ServerPlayer> operation) {
        var server = minecraft.getSingleplayerServer();
        var id = minecraft.player.getUUID();
        server.execute(() -> operation.accept(server.getPlayerList().getPlayer(id)));
    }

    private static void capture(Minecraft minecraft, String name) {
        Screenshot.grab(minecraft.gameDirectory, "fixes-" + name + ".png", minecraft.gameRenderer.mainRenderTarget(), 1, message -> { });
    }
}
