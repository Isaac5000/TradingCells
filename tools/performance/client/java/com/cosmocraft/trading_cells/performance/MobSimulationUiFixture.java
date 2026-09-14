package com.cosmocraft.trading_cells.performance;

import com.cosmocraft.trading_cells.feature.captures.adapters.api.CapturedMobStackAdapter;
import com.cosmocraft.trading_cells.feature.captures.domain.model.CapturedMobKind;
import com.cosmocraft.trading_cells.feature.mobfarm.adapters.input.EntityEssenceData;
import com.cosmocraft.trading_cells.feature.mobfarm.adapters.input.EssenceWorkbenchBlockEntity;
import com.cosmocraft.trading_cells.feature.mobfarm.adapters.input.MobFarmBlockEntity;
import com.cosmocraft.trading_cells.feature.mobfarm.adapters.output.MobFarmRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.mobfarm.adapters.output.client.EssenceWorkbenchScreen;
import com.cosmocraft.trading_cells.feature.mobfarm.adapters.output.client.MobFarmScreen;
import com.cosmocraft.trading_cells.feature.mobfarm.adapters.output.client.EntityModuleItemRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;

/** Real server menus and ordinary clicks in the benchmark's disposable world. */
final class MobSimulationUiFixture {
    private static final String[] MODEL_TYPES = {"creeper", "cow", "warden", "ghast", "cod", "ender_dragon"};
    private static int phase;
    private static long nextAction;
    private static long started;
    private static volatile boolean modelScenePrepared;
    private MobSimulationUiFixture() { }
    private static String mode() { return System.getProperty("trading_cells.performance.client.uiFixture", ""); }
    static boolean active() { return mode().equals("simulation") || mode().equals("essences") || mode().equals("simulation-models"); }
    static boolean ready() { return phase >= 6; }

    static void prepare(ServerPlayer player, BlockPos pos) {
        var level = player.level();
        level.getServer().setDifficulty(net.minecraft.world.Difficulty.NORMAL, true);
        player.getInventory().clearContent();
        if (mode().equals("simulation-models")) { prepareModels(player, pos); return; }
        var cow = EntityTypes.COW.create(level, EntitySpawnReason.LOAD);
        ItemStack essence = EntityEssenceData.essenceOf(cow);
        if (mode().equals("essences")) {
            level.setBlockAndUpdate(pos, MobFarmRegistrationAdapter.ESSENCE_WORKBENCH.get().defaultBlockState());
            var bench = (EssenceWorkbenchBlockEntity) level.getBlockEntity(pos);
            bench.setItem(0, essence);
            bench.setItem(1, new ItemStack(Items.AMETHYST_SHARD, 12));
            bench.setItem(2, new ItemStack(Items.IRON_INGOT, 12));
            player.giveExperiencePoints(1_000);
            player.getInventory().setItem(9, EntityEssenceData.essenceOf(EntityTypes.WARDEN.create(level, EntitySpawnReason.LOAD)));
            return;
        }
        level.setBlockAndUpdate(pos, MobFarmRegistrationAdapter.BLOCK.get().defaultBlockState());
        var farm = (MobFarmBlockEntity) level.getBlockEntity(pos);
        var villager = EntityTypes.VILLAGER.create(level, EntitySpawnReason.LOAD);
        ItemStack worker = new ItemStack(CapturedMobStackAdapter.capturerItem(CapturedMobKind.VILLAGER));
        CapturedMobStackAdapter.setData(CapturedMobKind.VILLAGER, worker, CapturedMobStackAdapter.createVillagerData(villager));
        farm.setItem(0, worker);
        farm.setItem(1, Items.IRON_SWORD.getDefaultInstance());
        farm.setItem(2, EntityEssenceData.moduleOf(essence));
        farm.setItem(3, MobFarmRegistrationAdapter.UPGRADES.get(2).get().getDefaultInstance());
        farm.setItem(4, MobFarmRegistrationAdapter.UPGRADES.get(7).get().getDefaultInstance());
        farm.processTick();
        var data = farm.saveWithFullMetadata(level.registryAccess());
        data.putInt("StoredExperience", 12_345);
        farm.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), data));
        for (int index = 0; index < 10; index++) {
            player.getInventory().setItem(9 + index, MobFarmRegistrationAdapter.UPGRADES.get(index).get().getDefaultInstance());
        }
        player.getInventory().setItem(20, EntityEssenceData.moduleOf(essence));
        player.getInventory().setItem(21, EntityEssenceData.moduleFor(level, Identifier.withDefaultNamespace("warden")));
        player.getInventory().setItem(22, EntityEssenceData.moduleFor(level, Identifier.withDefaultNamespace("ghast")));
    }

    private static void prepareModels(ServerPlayer player, BlockPos pos) {
        var level = player.level();
        for (BlockPos target : BlockPos.betweenClosed(pos.offset(-2, -1, -2), pos.offset(6, 5, 5))) {
            level.setBlockAndUpdate(target, target.getY() == pos.getY() - 1
                    ? Blocks.SMOOTH_STONE.defaultBlockState() : Blocks.AIR.defaultBlockState());
        }
        var villager = EntityTypes.VILLAGER.create(level, EntitySpawnReason.LOAD);
        ItemStack worker = new ItemStack(CapturedMobStackAdapter.capturerItem(CapturedMobKind.VILLAGER));
        CapturedMobStackAdapter.setData(CapturedMobKind.VILLAGER, worker, CapturedMobStackAdapter.createVillagerData(villager));
        for (int index = 0; index < MODEL_TYPES.length; index++) {
            ItemStack module = EntityEssenceData.moduleFor(level, Identifier.withDefaultNamespace(MODEL_TYPES[index]));
            if (module.isEmpty()) { throw new IllegalStateException("Missing model fixture: " + MODEL_TYPES[index]); }
            player.getInventory().setItem(index, module.copy());
            BlockPos target = pos.offset((index % 3) * 2, 0, (index / 3) * 2);
            level.setBlockAndUpdate(target, MobFarmRegistrationAdapter.BLOCK.get().defaultBlockState());
            var farm = (MobFarmBlockEntity) level.getBlockEntity(target);
            farm.setItem(0, worker.copy());
            farm.setItem(1, Items.IRON_SWORD.getDefaultInstance());
            farm.setItem(2, module);
            farm.toggleEnabled();
        }
        ItemStack unavailable = MobFarmRegistrationAdapter.ENTITY_MODULE.get().getDefaultInstance();
        CompoundTag data = new CompoundTag();
        CompoundTag essence = new CompoundTag();
        essence.putString("Type", "trading_cells:missing_test_entity");
        data.put("TradingCellsEssence", essence);
        unavailable.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        player.getInventory().setItem(6, unavailable);
        player.getInventory().setSelectedSlot(0);
        player.inventoryMenu.broadcastChanges();
        modelScenePrepared = true;
    }

    static void inspect(Minecraft minecraft) {
        if (ready() || System.nanoTime() < nextAction) { return; }
        if (mode().equals("simulation-models")) {
            if (!modelScenePrepared || minecraft.player == null || minecraft.level == null
                    || minecraft.getEntityRenderDispatcher().camera == null) { return; }
            if (started == 0) { started = System.nanoTime(); }
            if (System.nanoTime() - started > 15_000_000_000L) {
                throw new IllegalStateException("Simulation model inventory synchronization timed out");
            }
            if (!Identifier.fromNamespaceAndPath("trading_cells", "missing_test_entity").equals(
                    EntityEssenceData.entityTypeId(minecraft.player.getInventory().getItem(6)))) { return; }
            var renderer = new EntityModuleItemRenderer();
            for (int index = 0; index < MODEL_TYPES.length; index++) {
                ItemStack module = minecraft.player.getInventory().getItem(index);
                if (!Identifier.withDefaultNamespace(MODEL_TYPES[index]).equals(EntityEssenceData.entityTypeId(module))) { return; }
                if (EntityEssenceData.createEntity(minecraft.level, module) == null) {
                    throw new IllegalStateException("Cannot load module in slot " + index + ": " + module);
                }
                if (renderer.extractArgument(module) == null) {
                    throw new IllegalStateException("Missing entity module renderer in slot " + index);
                }
            }
            if (renderer.extractArgument(minecraft.player.getInventory().getItem(6)) != null) {
                throw new IllegalStateException("Unknown module did not retain the empty-pedestal fallback");
            }
            minecraft.player.getInventory().setSelectedSlot(0);
            System.out.println("Simulation model previews checked: creeper, cow, warden, ghast, cod, dragon and missing-type fallback");
            phase = 6;
            return;
        }
        Screen screen = minecraft.gui.screen();
        if (!(screen instanceof MobFarmScreen) && !(screen instanceof EssenceWorkbenchScreen)) { return; }
        if (started == 0) { started = System.nanoTime(); }
        if (System.nanoTime() - started > 25_000_000_000L) { throw new IllegalStateException("Simulation UI timeout at " + phase); }
        if (screen instanceof MobFarmScreen farm) {
            var menu = farm.getMenu();
            int x = (screen.width - 374) / 2, y = (screen.height - 246) / 2;
            switch (phase) {
                case 0 -> { if (menu.lootEntries().isEmpty()) { return; } click(screen, x + 169, y + 36); }
                case 1 -> click(screen, x + 20, y + 60);
                case 2 -> { if (menu.lootEntries().getFirst().enabled()) { return; } click(screen, x + 351, y + 16); }
                case 3 -> { if (menu.enabled()) { return; } click(screen, x + 160, y + 228); }
                case 4 -> { if (menu.storedExperience() != 0) { return; } click(screen, x + 20, y + 60); }
                case 5 -> {
                    if (!menu.lootEntries().getFirst().enabled()) { return; }
                    if (!catalogReady()) { return; }
                    if (menu.slots.size() != 59) { throw new IllegalStateException("Unexpected simulation slot count"); }
                    System.out.println("Simulation UI checked: probability view, loot filter roundtrip, on/off, XP extraction and 59 slots");
                }
                default -> { }
            }
        } else if (screen instanceof EssenceWorkbenchScreen bench) {
            var menu = bench.getMenu();
            int x = (screen.width - 236) / 2, y = (screen.height - 222) / 2;
            switch (phase) {
                case 0 -> { if (!menu.canSynthesize()) { return; } click(screen, x + 110, y + 101); }
                case 1 -> {
                    if (menu.getSlot(3).getItem().isEmpty()) { return; }
                    var slot = menu.getSlot(3);
                    var event = new MouseButtonEvent(x + slot.x + 8, y + slot.y + 8, new MouseButtonInfo(0, 1));
                    screen.mouseClicked(event, false); screen.mouseReleased(event);
                }
                case 2 -> { if (!menu.getSlot(3).getItem().isEmpty()) { return; } click(screen, x + menu.getSlot(4).x + 8, y + menu.getSlot(4).y + 8); }
                case 3 -> { if (menu.getCarried().isEmpty()) { return; } click(screen, x + 43, y + 55); }
                case 4 -> { if (!menu.highLevel() || menu.experienceCost() != 3_000) { return; } }
                case 5 -> {
                    if (menu.canSynthesize()) { throw new IllegalStateException("High-level synthesis accepted insufficient requirements"); }
                    System.out.println("Essence UI checked: normal synthesis, output Shift-click, high-level classification and disabled insufficient recipe");
                }
                default -> { }
            }
        }
        phase++;
        nextAction = System.nanoTime() + 600_000_000L;
    }

    private static void click(Screen screen, int x, int y) {
        var event = new MouseButtonEvent(x, y, new MouseButtonInfo(0, 0));
        screen.mouseClicked(event, false); screen.mouseReleased(event);
    }

    private static boolean catalogReady() {
        if (!net.neoforged.fml.ModList.get().isLoaded("roughlyenoughitems")) {
            throw new IllegalStateException("Simulation catalog verification requires REI");
        }
        var registry = me.shedaniel.rei.api.client.registry.entry.EntryRegistry.getInstance();
        if (registry.isReloading() || registry.size() == 0) { return false; }
        var items = registry.getEntryStacks().map(entry -> entry.getValue())
                .filter(value -> value instanceof ItemStack).map(value -> ((ItemStack) value).getItem()).toList();
        if (items.stream().anyMatch(item -> item instanceof net.minecraft.world.item.BlockItem block
                && block.getBlock() instanceof com.cosmocraft.trading_cells.platform.neoforge.mobfarm.LegacyMobFarmBlock)) {
            throw new IllegalStateException("REI still lists replaced entity farms");
        }
        var creative = com.cosmocraft.trading_cells.platform.neoforge.registration.CreativeTabRegistration.FARMS_TAB.get().getDisplayItems();
        if (creative.size() != 13 || creative.stream().anyMatch(stack -> !items.contains(stack.getItem()))) {
            throw new IllegalStateException("Creative/REI missing one of the thirteen simulation entries");
        }
        System.out.println("Simulation catalog checked: 13 creative/REI entries and no legacy farm items");
        return true;
    }
}
