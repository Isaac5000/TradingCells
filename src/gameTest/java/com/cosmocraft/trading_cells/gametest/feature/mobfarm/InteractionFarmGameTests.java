package com.cosmocraft.trading_cells.gametest.feature.mobfarm;

import com.cosmocraft.trading_cells.feature.mobfarm.adapters.input.EntityEssenceData;
import com.cosmocraft.trading_cells.feature.mobfarm.adapters.input.EntityInteractionFarmBlockEntity;
import com.cosmocraft.trading_cells.feature.mobfarm.adapters.input.EntityInteractionFarmMenu;
import com.cosmocraft.trading_cells.feature.mobfarm.adapters.interaction.InteractionHarvestDispatcher;
import com.cosmocraft.trading_cells.feature.mobfarm.adapters.interaction.InteractionHarvestResult.Status;
import com.cosmocraft.trading_cells.feature.mobfarm.adapters.output.MobFarmRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.mobfarm.domain.model.InteractionFarmRules;
import com.cosmocraft.trading_cells.feature.mobfarm.domain.model.InteractionSource;
import com.cosmocraft.trading_cells.gametest.shared.BlockEntityStateFixtures;
import com.cosmocraft.trading_cells.gametest.shared.GameTestCase;
import com.cosmocraft.trading_cells.gametest.shared.GameTestFixtures;
import com.cosmocraft.trading_cells.platform.neoforge.machine.AbstractPortableMachineBlock;
import com.cosmocraft.trading_cells.shared.machines.domain.model.MachineRedstoneMode;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.TagValueInput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

public final class InteractionFarmGameTests {
    private InteractionFarmGameTests() { }
    public static List<GameTestCase> tests() {
        return List.of(new GameTestCase("interaction_farm_two_channels", 80, InteractionFarmGameTests::channels),
                new GameTestCase("interaction_farm_pending_reload", 80, InteractionFarmGameTests::pending),
                new GameTestCase("interaction_farm_sided_transactions", 80, InteractionFarmGameTests::automation),
                new GameTestCase("interaction_farm_speed_pause", 80, InteractionFarmGameTests::speed),
                new GameTestCase("interaction_farm_ignores_redstone", 80, InteractionFarmGameTests::ignoresRedstone),
                new GameTestCase("interaction_farm_external_compatibility", 80, InteractionFarmGameTests::external),
                new GameTestCase("interaction_farm_rejected_actions", 80, InteractionFarmGameTests::rejections),
                new GameTestCase("interaction_farm_priority_invalidation", 80, InteractionFarmGameTests::priority),
                new GameTestCase("interaction_farm_real_hoppers", 100, InteractionFarmGameTests::hoppers),
                new GameTestCase("interaction_farm_real_pipes", 100, InteractionFarmGameTests::pipes),
                new GameTestCase("interaction_farm_components_and_limits", 80, InteractionFarmGameTests::components),
                new GameTestCase("interaction_farm_capacity_batches", 80, InteractionFarmGameTests::capacity),
                new GameTestCase("interaction_farm_capacity_pending", 80, InteractionFarmGameTests::capacityPending),
                new GameTestCase("interaction_farm_quantity_touch_protection", 80,
                        InteractionFarmGameTests::quantityTouch));
    }
    private static EntityInteractionFarmBlockEntity farm(GameTestHelper h, String type) {
        h.setBlock(GameTestFixtures.TEST_POS, net.minecraft.world.level.block.Blocks.AIR);
        h.setBlock(GameTestFixtures.TEST_POS, MobFarmRegistrationAdapter.INTERACTION_BLOCK.get());
        var farm = h.getBlockEntity(GameTestFixtures.TEST_POS, EntityInteractionFarmBlockEntity.class);
        farm.setItem(0, GameTestFixtures.adultVillagerCapture(h));
        farm.setItem(1, EntityEssenceData.moduleFor(h.getLevel(), Identifier.parse(type)));
        return farm;
    }
    private static void cycle(EntityInteractionFarmBlockEntity farm) {
        int duration = farm.duration();
        for (int i = 0; i < duration; i++) { farm.processServerTick(); }
    }
    private static int count(EntityInteractionFarmBlockEntity farm, net.minecraft.world.item.Item item) {
        int result = 0;
        for (int i = 5; i < EntityInteractionFarmBlockEntity.CAPACITY; i++) { if (farm.getItem(i).is(item)) { result += farm.getItem(i).getCount(); } }
        return result;
    }
    private static void channels(GameTestHelper h) {
        var farm = farm(h, "minecraft:bee");
        farm.setItem(2, new ItemStack(Items.GLASS_BOTTLE, 4)); farm.setItem(3, new ItemStack(Items.SHEARS));
        cycle(farm);
        h.assertValueEqual(count(farm, Items.HONEY_BOTTLE), 1, "One ITEM operation");
        h.assertValueEqual(count(farm, Items.HONEYCOMB), 3, "TOOL uses a separate full hive");
        h.assertValueEqual(farm.getItem(2).getCount(), 3, "One real input consumed");
        h.assertValueEqual(farm.getItem(3).getDamageValue(), 1, "One vanilla wear, not doubled");
        ItemStack breaking = new ItemStack(Items.SHEARS); breaking.setDamageValue(breaking.getMaxDamage() - 1);
        farm.setItem(3, breaking); cycle(farm);
        h.assertTrue(farm.getItem(3).isEmpty(), "Last tool use breaks after producing");
        h.assertValueEqual(count(farm, Items.HONEYCOMB), 6, "Breaking tool produced final resources");
        h.succeed();
    }
    private static void pending(GameTestHelper h) {
        var farm = farm(h, "trading_cells_gametest:resource_creature");
        farm.setItem(2, new ItemStack(Items.EMERALD, 4));
        BlockEntityStateFixtures.fillIndexedSlots(h, farm, "Slot", 5, EntityInteractionFarmBlockEntity.OUTPUT_COUNT, new ItemStack(Items.STONE, 64));
        int before = InteractionTestContent.attempts;
        cycle(farm); cycle(farm);
        h.assertValueEqual(InteractionTestContent.attempts - before, 1, "Full outputs preserve one roll");
        h.assertValueEqual(farm.getItem(2).getCount(), 4, "Pending unpaid");
        var input = h.getLevel().getCapability(Capabilities.Item.BLOCK, farm.getBlockPos(), Direction.EAST);
        try (var transaction = Transaction.openRoot()) {
            h.assertValueEqual(input.insert(0, ItemResource.of(Items.EMERALD), 1, transaction), 1, "Pending input can be replenished");
        }
        cycle(farm);
        h.assertValueEqual(InteractionTestContent.attempts - before, 1, "Cancelled insertion preserves pending roll");
        h.assertValueEqual(farm.getItem(2).getCount(), 4, "Cancelled insertion restores input");
        farm.prepareForBlockDrop(h.getLevel().registryAccess());
        var data = farm.getPreparedBlockDropData(h.getLevel().registryAccess());
        farm.loadCustomOnly(TagValueInput.create(ProblemReporter.DISCARDING, h.getLevel().registryAccess(), data));
        farm.removeItem(5, 64); cycle(farm);
        h.assertValueEqual(count(farm, Items.GOLD_NUGGET), 0, "Partial free space never partially commits");
        h.assertValueEqual(farm.getItem(2).getCount(), 4, "Partial free space remains unpaid");
        farm.removeItem(6, 64); cycle(farm);
        h.assertValueEqual(InteractionTestContent.attempts - before, 1, "Reloaded pending does not reroll");
        h.assertValueEqual(farm.getItem(2).getCount(), 3, "Charge only at full commit");
        h.assertValueEqual(count(farm, Items.GOLD_NUGGET), 2, "All products committed");
        h.assertValueEqual(count(farm, Items.REDSTONE), 1, "Direct world drop captured");
        h.succeed();
    }
    private static void automation(GameTestHelper h) {
        var farm = farm(h, "minecraft:bee");
        Direction front = farm.getBlockState().getValue(AbstractPortableMachineBlock.FACING);
        h.assertTrue(farm.canPlaceItemThroughFace(2, new ItemStack(Items.GLASS_BOTTLE), front.getClockWise()), "ITEM on relative left");
        h.assertFalse(farm.canPlaceItemThroughFace(2, new ItemStack(Items.GLASS_BOTTLE), front.getCounterClockWise()), "ITEM not on right");
        h.assertTrue(farm.canPlaceItemThroughFace(3, new ItemStack(Items.SHEARS), front.getCounterClockWise()), "TOOL on relative right");
        var handler = h.getLevel().getCapability(Capabilities.Item.BLOCK, farm.getBlockPos(), front.getCounterClockWise());
        h.assertTrue(handler != null, "Capability registered");
        try (var transaction = Transaction.openRoot()) {
            h.assertValueEqual(handler.insert(0, ItemResource.of(Items.SHEARS), 10, transaction), 1, "Tool physical quota one");
        }
        h.assertTrue(farm.getItem(3).isEmpty(), "Cancelled transaction restores tool slot");
        try (var transaction = Transaction.openRoot()) { handler.insert(0, ItemResource.of(Items.SHEARS), 1, transaction); transaction.commit(); }
        cycle(farm);
        for (Direction face : Direction.values()) {
            var outputs = h.getLevel().getCapability(Capabilities.Item.BLOCK, farm.getBlockPos(), face);
            h.assertTrue(outputs != null, "Every face exposes outputs");
            h.assertTrue(farm.canTakeItemThroughFace(5, farm.getItem(5), face), "Outputs can leave every face");
            h.assertFalse(farm.canTakeItemThroughFace(3, farm.getItem(3), face), "Tool cannot leave automatically");
        }
        h.succeed();
    }
    private static void speed(GameTestHelper h) {
        int[] durations = {1200, 600, 300, 150, 75, 38};
        var farm = farm(h, "minecraft:cow"); farm.setItem(2, new ItemStack(Items.BUCKET, 8));
        for (int tier = 0; tier <= 5; tier++) {
            h.assertValueEqual(InteractionFarmRules.cycleDuration(tier), durations[tier], "Exact speed duration");
        }
        farm.processServerTick(); int progress = farm.progress();
        farm.toggleEnabled(); cycle(farm); h.assertValueEqual(farm.progress(), progress, "Pause preserves progress"); farm.toggleEnabled();
        farm.removeItem(0, 1); cycle(farm);
        h.assertValueEqual(farm.progress(), progress, "Missing worker stops processing");
        farm.setItem(0, GameTestFixtures.adultVillagerCapture(h));
        farm.setItem(4, MobFarmRegistrationAdapter.UPGRADES.get(4).get().getDefaultInstance()); farm.processServerTick();
        h.assertValueEqual(farm.duration(), 38, "Real installed netherite speed");
        cycle(farm); h.assertValueEqual(count(farm, Items.MILK_BUCKET), 1, "Speed never increases batch size");
        h.succeed();
    }
    private static void ignoresRedstone(GameTestHelper h) {
        var farm = farm(h, "minecraft:cow");
        farm.setItem(EntityInteractionFarmBlockEntity.ITEM, new ItemStack(Items.BUCKET, 8));
        h.assertFalse(farm.supportsRedstoneControl(), "Interaction farm has no redstone control");
        h.assertFalse(farm.getBlockState().hasAnalogOutputSignal(), "Block exposes no comparator output");
        int expectedProgress = farm.progress();
        for (boolean powered : new boolean[]{false, true}) {
            h.setBlock(GameTestFixtures.TEST_POS.west(), powered
                    ? net.minecraft.world.level.block.Blocks.REDSTONE_BLOCK : net.minecraft.world.level.block.Blocks.AIR);
            h.assertValueEqual(h.getLevel().hasNeighborSignal(farm.getBlockPos()), powered, "Real neighboring signal");
            for (var mode : MachineRedstoneMode.values()) {
                var legacy = farm.saveCustomOnly(h.getLevel().registryAccess());
                legacy.putString("MachineRedstoneMode", mode.serializedName());
                farm.loadCustomOnly(TagValueInput.create(ProblemReporter.DISCARDING, h.getLevel().registryAccess(), legacy));
                farm.setRedstoneMode(mode);
                h.assertValueEqual(farm.redstoneMode(), MachineRedstoneMode.IGNORE, "Saved and requested redstone modes ignored");
                h.assertFalse(farm.isPausedByRedstone(), "Signal never pauses interactions");
                farm.processServerTick();
                h.assertValueEqual(farm.progress(), ++expectedProgress, "Progress advances with and without signal");
                h.assertFalse(farm.saveCustomOnly(h.getLevel().registryAccess()).contains("MachineRedstoneMode"), "Obsolete mode not persisted");
                h.assertFalse(farm.exportMachineConfiguration().contains("MachineRedstoneMode"), "Obsolete mode not exported");
            }
        }
        var player = h.makeMockServerPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        player.setPos(net.minecraft.world.phys.Vec3.atCenterOf(farm.getBlockPos()));
        var menu = (EntityInteractionFarmMenu) farm.createMenu(1, player.getInventory(), player);
        h.assertTrue(menu.stillValid(player), "Menu validity does not mask retired button rejection");
        h.assertFalse(menu.clickMenuButton(player, 1), "Retired redstone button packet rejected");
        h.assertTrue(menu.clickMenuButton(player, 0), "Manual pause retained");
        farm.processServerTick();
        h.assertValueEqual(farm.progress(), expectedProgress, "Manual pause preserves progress under signal");
        h.assertTrue(menu.paused(), "Manual pause synchronized to menu");
        var module = farm.creature().copy();
        farm.prepareForBlockDrop(h.getLevel().registryAccess());
        var saved = farm.getPreparedBlockDropData(h.getLevel().registryAccess());
        farm.loadCustomOnly(TagValueInput.create(ProblemReporter.DISCARDING, h.getLevel().registryAccess(), saved));
        h.assertFalse(farm.enabled(), "Manual pause survives block drop");
        h.assertTrue(ItemStack.matches(farm.creature(), module), "Removing redstone preserves creature");
        h.assertValueEqual(farm.getItem(EntityInteractionFarmBlockEntity.ITEM).getCount(), 8, "Removing redstone preserves inputs");
        h.assertValueEqual(farm.progress(), expectedProgress, "Progress survives block drop");
        h.assertTrue(menu.clickMenuButton(player, 0), "Manual resume retained");
        h.assertFalse(menu.paused(), "Manual resume synchronized to menu");
        BlockEntityStateFixtures.fillIndexedSlots(h, farm, "Slot", EntityInteractionFarmBlockEntity.FIRST_OUTPUT,
                EntityInteractionFarmBlockEntity.OUTPUT_COUNT, new ItemStack(Items.STONE, 64));
        cycle(farm);
        h.assertValueEqual(farm.status(InteractionSource.ITEM), Status.OUTPUT_FULL, "Full outputs still block resource commit");
        h.assertValueEqual(farm.comparatorOutput(), 0, "Full outputs never emit a comparator signal");
        h.assertValueEqual(farm.getItem(EntityInteractionFarmBlockEntity.ITEM).getCount(), 8, "Blocked result remains unpaid");
        h.succeed();
    }
    private static void external(GameTestHelper h) {
        var module = EntityEssenceData.moduleFor(h.getLevel(), InteractionTestContent.ENTITY_ID);
        for (var input : List.of(new ItemStack(Items.EMERALD), new ItemStack(InteractionTestContent.tool), new ItemStack(Items.AMETHYST_SHARD))) {
            var result = InteractionHarvestDispatcher.harvest(h.getLevel(), module, input,
                    input.isDamageableItem() ? InteractionSource.TOOL : InteractionSource.ITEM);
            h.assertValueEqual(result.status(), Status.READY, "External entity/item/event API works: " + input);
        }
        var clean = InteractionHarvestDispatcher.harvest(h.getLevel(), module, new ItemStack(Items.EMERALD), InteractionSource.ITEM);
        h.assertValueEqual(clean.products().stream().mapToInt(ItemStack::getCount).sum(), 3, "No inventory or drop cross contamination");
        h.succeed();
    }
    private static void rejections(GameTestHelper h) {
        var module = EntityEssenceData.moduleFor(h.getLevel(), InteractionTestContent.ENTITY_ID);
        for (var item : List.of(Items.CHEST, Items.SADDLE, Items.ENDER_PEARL, Items.CLOCK, Items.TNT, Items.WATER_BUCKET, Items.DIRT)) {
            var input = new ItemStack(item);
            var result = InteractionHarvestDispatcher.harvest(h.getLevel(), module, input, InteractionSource.ITEM);
            h.assertTrue(result.products().isEmpty() && result.status() != Status.READY, "Unsafe action rejected: " + item);
            h.assertValueEqual(input.getCount(), 1, "No real cost on rejection");
        }
        var cow = EntityEssenceData.moduleFor(h.getLevel(), Identifier.withDefaultNamespace("cow"));
        var good = InteractionHarvestDispatcher.harvest(h.getLevel(), cow, new ItemStack(Items.BUCKET), InteractionSource.ITEM);
        h.assertValueEqual(good.status(), Status.READY, "Exceptions do not poison next attempt");
        var farm = farm(h, "trading_cells_gametest:resource_creature");
        farm.setItem(2, new ItemStack(Items.TNT)); farm.setItem(3, new ItemStack(InteractionTestContent.tool)); cycle(farm);
        h.assertValueEqual(count(farm, Items.LAPIS_LAZULI), 2, "ITEM failure does not block TOOL");
        h.assertValueEqual(farm.getItem(2).getCount(), 1, "Failed channel remains unpaid");
        h.succeed();
    }
    private static void priority(GameTestHelper h) {
        var farm = farm(h, "minecraft:bee");
        farm.setItem(2, new ItemStack(Items.GLASS_BOTTLE, 8)); farm.setItem(3, new ItemStack(Items.SHEARS));
        BlockEntityStateFixtures.fillIndexedSlots(h, farm, "Slot", 6, EntityInteractionFarmBlockEntity.OUTPUT_COUNT - 1, new ItemStack(Items.STONE, 64));
        cycle(farm);
        h.assertValueEqual(count(farm, Items.HONEY_BOTTLE), 1, "ITEM has first cycle priority");
        h.assertValueEqual(farm.getItem(3).getDamageValue(), 0, "Blocked TOOL has no wear");
        farm.removeItem(5, 64); cycle(farm);
        h.assertValueEqual(count(farm, Items.HONEYCOMB), 3, "TOOL has next cycle priority");
        h.assertValueEqual(farm.getItem(2).getCount(), 7, "Blocked ITEM did not consume");
        farm.setItem(1, EntityEssenceData.moduleFor(h.getLevel(), Identifier.withDefaultNamespace("cow")));
        farm.removeItem(5, 64); cycle(farm);
        h.assertValueEqual(count(farm, Items.HONEY_BOTTLE), 0, "Changing module invalidates old unpaid result");
        h.assertValueEqual(farm.getItem(2).getCount(), 7, "Invalid new combination does not consume");
        h.succeed();
    }
    private static void hoppers(GameTestHelper h) {
        var farm = farm(h, "minecraft:cow");
        var inputPos = GameTestFixtures.TEST_POS.east();
        h.setBlock(inputPos, net.minecraft.world.level.block.Blocks.HOPPER.defaultBlockState()
                .setValue(net.minecraft.world.level.block.HopperBlock.FACING, Direction.WEST));
        var hopper = h.getBlockEntity(inputPos, net.minecraft.world.level.block.entity.HopperBlockEntity.class);
        hopper.setItem(0, new ItemStack(Items.BUCKET, 2));
        h.runAfterDelay(20, () -> {
            h.assertValueEqual(farm.getItem(2).getCount(), 2, "Native hopper inserts into relative ITEM face");
            cycle(farm);
            var outputPos = GameTestFixtures.TEST_POS.below();
            h.setBlock(outputPos, net.minecraft.world.level.block.Blocks.HOPPER);
            var output = h.getBlockEntity(outputPos, net.minecraft.world.level.block.entity.HopperBlockEntity.class);
            h.runAfterDelay(20, () -> {
                h.assertTrue(java.util.stream.IntStream.range(0, 5).anyMatch(slot -> output.getItem(slot).is(Items.MILK_BUCKET)),
                        "Native hopper extracts physical outputs");
                h.assertValueEqual(farm.getItem(2).getCount(), 1, "Output hopper cannot steal input");
                h.succeed();
            });
        });
    }
    private static void pipes(GameTestHelper h) {
        var farm = farm(h, "minecraft:cow");
        BlockPos pipePos = GameTestFixtures.TEST_POS.east(), storagePos = pipePos.east();
        h.setBlock(storagePos, net.minecraft.world.level.block.Blocks.BARREL);
        var barrel = h.getBlockEntity(storagePos, net.minecraft.world.level.block.entity.BarrelBlockEntity.class);
        barrel.setItem(0, new ItemStack(Items.BUCKET, 2));
        h.setBlock(pipePos, com.cosmocraft.trading_cells.feature.logistics.adapters.output.LogisticsRegistrationAdapter
                .pipeBlock(com.cosmocraft.trading_cells.feature.logistics.domain.model.PipeKind.UNIVERSAL).get());
        var pipe = h.getBlockEntity(pipePos, com.cosmocraft.trading_cells.feature.logistics.adapters.input.LogisticsPipeBlockEntity.class);
        pipe.setMode(Direction.EAST, com.cosmocraft.trading_cells.feature.logistics.domain.model.PipeSideMode.EXTRACT);
        pipe.setMode(Direction.WEST, com.cosmocraft.trading_cells.feature.logistics.domain.model.PipeSideMode.INSERT);
        pipe.refreshConnectionsAround();
        h.runAfterDelay(30, () -> {
            h.assertValueEqual(farm.getItem(2).getCount(), 2, "Universal pipe supplies ITEM side");
            cycle(farm);
            pipe.setMode(Direction.EAST, com.cosmocraft.trading_cells.feature.logistics.domain.model.PipeSideMode.INSERT);
            pipe.setMode(Direction.WEST, com.cosmocraft.trading_cells.feature.logistics.domain.model.PipeSideMode.EXTRACT);
            h.runAfterDelay(30, () -> {
                h.assertValueEqual(barrel.countItem(Items.MILK_BUCKET), 1, "Universal pipe extracts result from same face");
                h.assertValueEqual(farm.getItem(2).getCount(), 1, "Universal pipe cannot extract remaining input");
                h.succeed();
            });
        });
    }
    private static void capacity(GameTestHelper h) {
        for (int tier = 1; tier <= 5; tier++) {
            var farm = farm(h, "minecraft:bee");
            int operations = 1 << tier;
            farm.setItem(2, new ItemStack(Items.GLASS_BOTTLE, 64));
            farm.setItem(3, new ItemStack(Items.SHEARS));
            var upgrade = MobFarmRegistrationAdapter.UPGRADES.get(4 + tier).get().getDefaultInstance();
            var top = h.getLevel().getCapability(Capabilities.Item.BLOCK, farm.getBlockPos(), Direction.UP);
            try (var tx = Transaction.openRoot()) {
                h.assertValueEqual(top.insert(3, ItemResource.of(upgrade), 2, tx), 1, "Top capacity quota one");
            }
            h.assertTrue(farm.getItem(EntityInteractionFarmBlockEntity.CAPACITY).isEmpty(), "Capacity insertion rollback");
            try (var tx = Transaction.openRoot()) { top.insert(3, ItemResource.of(upgrade), 1, tx); tx.commit(); }
            cycle(farm);
            h.assertValueEqual(count(farm, Items.HONEY_BOTTLE), operations, "ITEM batch uses capacity");
            h.assertValueEqual(count(farm, Items.HONEYCOMB), operations * 3, "TOOL batch uses capacity");
            h.assertValueEqual(farm.getItem(2).getCount(), 64 - operations, "Each operation consumes real input");
            h.assertValueEqual(farm.getItem(3).getDamageValue(), operations, "Each operation applies vanilla wear");
            for (Direction side : Direction.values()) {
                h.assertFalse(farm.canTakeItemThroughFace(EntityInteractionFarmBlockEntity.CAPACITY, upgrade, side), "Capacity is never output");
            }
            farm.prepareForBlockDrop(h.getLevel().registryAccess());
            var data = farm.getPreparedBlockDropData(h.getLevel().registryAccess());
            farm.loadCustomOnly(TagValueInput.create(ProblemReporter.DISCARDING, h.getLevel().registryAccess(), data));
            h.assertTrue(ItemStack.matches(farm.getItem(EntityInteractionFarmBlockEntity.CAPACITY), upgrade), "Capacity survives block drop");
            farm.setItem(2, new ItemStack(Items.GLASS_BOTTLE));
            var breaking = new ItemStack(Items.SHEARS); breaking.setDamageValue(breaking.getMaxDamage() - 1);
            farm.setItem(3, breaking); cycle(farm);
            h.assertValueEqual(count(farm, Items.HONEYCOMB), (operations + 1) * 3, "Stop batch on broken tool");
            h.assertTrue(farm.getItem(2).isEmpty() && farm.getItem(3).isEmpty(), "No duplicated inputs or tools");
        }
        h.succeed();
    }
    private static void quantityTouch(GameTestHelper h) {
        var farm = farm(h, "minecraft:bee");
        var enchantment = h.getLevel().registryAccess()
                .lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                .getOrThrow(com.cosmocraft.trading_cells.platform.neoforge.machine.MachineToolEnchantments.FARMER_TOUCH);
        var shears = new ItemStack(Items.SHEARS);
        shears.enchant(enchantment, 1);
        shears.setDamageValue(shears.getMaxDamage() - 1);
        farm.setItem(3, shears);
        farm.setItem(EntityInteractionFarmBlockEntity.CAPACITY,
                MobFarmRegistrationAdapter.UPGRADES.get(9).get().getDefaultInstance());
        cycle(farm);
        h.assertValueEqual(count(farm, Items.HONEYCOMB), 32 * 3,
                "32 completed harvests with the maximum quantity upgrade");
        h.assertTrue(ItemStack.matches(shears, farm.getItem(3)),
                "Farmer Touch prevents shears breaking across 32 operations");
        h.succeed();
    }

    private static void capacityPending(GameTestHelper h) {
        var farm = farm(h, "trading_cells_gametest:resource_creature");
        farm.setItem(2, new ItemStack(Items.EMERALD, 8));
        farm.setItem(EntityInteractionFarmBlockEntity.CAPACITY, MobFarmRegistrationAdapter.UPGRADES.get(9).get().getDefaultInstance());
        BlockEntityStateFixtures.fillIndexedSlots(h, farm, "Slot", 5, EntityInteractionFarmBlockEntity.OUTPUT_COUNT, new ItemStack(Items.STONE, 64));
        int attempts = InteractionTestContent.attempts;
        cycle(farm); cycle(farm);
        h.assertValueEqual(InteractionTestContent.attempts - attempts, 1, "Capacity does not reroll blocked result");
        farm.prepareForBlockDrop(h.getLevel().registryAccess());
        var saved = farm.getPreparedBlockDropData(h.getLevel().registryAccess());
        farm.loadCustomOnly(TagValueInput.create(ProblemReporter.DISCARDING, h.getLevel().registryAccess(), saved));
        farm.removeItem(5, 64); farm.removeItem(6, 64); cycle(farm);
        h.assertValueEqual(InteractionTestContent.attempts - attempts, 8, "Reloaded pending plus seven fresh attempts");
        h.assertValueEqual(count(farm, Items.GOLD_NUGGET), 16, "Only available real inputs consumed");
        h.assertTrue(farm.getItem(2).isEmpty(), "Batch stops at exhausted input");
        h.assertValueEqual(farm.getItem(31).getCount(), 64, "Legacy last output slot unchanged");
        h.succeed();
    }
    private static void components(GameTestHelper h) {
        var farm = farm(h, "trading_cells_gametest:resource_creature");
        var tool = new ItemStack(InteractionTestContent.tool);
        tool.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Test tool"));
        tool.enchant(h.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                .getOrThrow(net.minecraft.world.item.enchantment.Enchantments.MENDING), 1);
        farm.setItem(3, tool.copy()); cycle(farm);
        tool.setDamageValue(1);
        h.assertTrue(ItemStack.matches(farm.getItem(3), tool), "Real wear preserves name and enchantments");
        farm.removeItem(3, 1); farm.setItem(2, new ItemStack(Items.GOLD_BLOCK, 2)); cycle(farm);
        h.assertValueEqual(farm.status(InteractionSource.ITEM), Status.TOO_LARGE, "Impossible output never becomes pending");
        h.assertValueEqual(farm.getItem(2).getCount(), 2, "Impossible output consumes nothing");
        h.assertValueEqual(count(farm, Items.MILK_BUCKET), 0, "Impossible output commits no partial resources");
        var missing = farm.getItem(1).copy();
        var root = missing.get(DataComponents.CUSTOM_DATA).copyTag();
        var data = root.getCompoundOrEmpty("TradingCellsEssence"); data.putString("Type", "absent_mod:creature");
        root.put("TradingCellsEssence", data);
        missing.set(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(root));
        farm.setItem(1, missing.copy()); cycle(farm);
        h.assertValueEqual(farm.status(InteractionSource.ITEM), Status.INVALID_MODULE, "Absent entity is inactive");
        h.assertTrue(ItemStack.matches(farm.getItem(1), missing), "Absent entity data remain recoverable");
        h.assertValueEqual(farm.getItem(2).getCount(), 2, "Absent entity consumes nothing");
        var captured = InteractionHarvestDispatcher.harvest(h.getLevel(), missing, new ItemStack(Items.COD_BUCKET), InteractionSource.ITEM);
        h.assertValueEqual(captured.status(), Status.UNSUPPORTED, "Creature buckets are never resource inputs");
        h.succeed();
    }
}