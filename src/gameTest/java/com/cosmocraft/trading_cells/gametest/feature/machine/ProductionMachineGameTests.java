package com.cosmocraft.trading_cells.gametest.feature.machine;

import com.cosmocraft.trading_cells.feature.farmer.adapters.input.FarmerBlockEntity;
import com.cosmocraft.trading_cells.feature.farmer.adapters.output.FarmerRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.quarry.adapters.input.QuarryBlockEntity;
import com.cosmocraft.trading_cells.feature.quarry.adapters.output.QuarryRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.mobfarm.adapters.input.EntityInteractionFarmBlockEntity;
import com.cosmocraft.trading_cells.feature.mobfarm.adapters.input.MobFarmBlockEntity;
import com.cosmocraft.trading_cells.feature.mobfarm.adapters.output.MobFarmRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.trader.adapters.input.NetheritePiglinBarteringCellBlockEntity;
import com.cosmocraft.trading_cells.feature.trader.adapters.output.TraderRegistrationAdapter;
import com.cosmocraft.trading_cells.gametest.shared.BlockEntityStateFixtures;
import com.cosmocraft.trading_cells.gametest.shared.GameTestCase;
import com.cosmocraft.trading_cells.gametest.shared.GameTestFixtures;
import com.cosmocraft.trading_cells.platform.neoforge.machine.PortableMachineBlockEntity;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.TagValueInput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

public final class ProductionMachineGameTests {
    private ProductionMachineGameTests() { }

    public static List<GameTestCase> tests() {
        return List.of(new GameTestCase("production_speed_slots", 60, ProductionMachineGameTests::speed),
                new GameTestCase("production_expanded_output_storage", 60, ProductionMachineGameTests::storage));
    }

    private static List<Block> toolMachines() {
        return List.of(FarmerRegistrationAdapter.FARMER_BLOCK.get(), FarmerRegistrationAdapter.PIGLIN_FARMER_BLOCK.get(),
                QuarryRegistrationAdapter.QUARRY_BLOCK.get(), QuarryRegistrationAdapter.PIGLIN_QUARRY_BLOCK.get());
    }

    private static void speed(GameTestHelper h) {
        int variant = 0;
        for (Block block : toolMachines()) {
            h.setBlock(GameTestFixtures.TEST_POS, block);
            var machine = h.getBlockEntity(GameTestFixtures.TEST_POS, PortableMachineBlockEntity.class);
            var inventory = (WorldlyContainer) machine;
            boolean farmer = machine instanceof FarmerBlockEntity;
            boolean piglin = (variant++ % 2) != 0;
            int toolSlot = farmer ? FarmerBlockEntity.HOE_SLOT : QuarryBlockEntity.PICKAXE_SLOT;
            int speedSlot = farmer ? FarmerBlockEntity.SPEED_SLOT : QuarryBlockEntity.SPEED_SLOT;
            ContainerData data = farmer ? ((FarmerBlockEntity) machine).dataAccess() : ((QuarryBlockEntity) machine).dataAccess();
            inventory.setItem(0, piglin ? GameTestFixtures.adultPiglinCapture(h) : GameTestFixtures.adultVillagerCapture(h));
            if (farmer) { inventory.setItem(1, new ItemStack(piglin ? Items.CRIMSON_FUNGUS : Items.WHEAT_SEEDS)); }
            var tool = new ItemStack(farmer ? Items.WOODEN_HOE : Items.WOODEN_PICKAXE);
            inventory.setItem(toolSlot, tool.copy());
            machine.processServerTick();
            int base = data.get(1);
            tool.enchant(h.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.EFFICIENCY), 5);
            inventory.setItem(toolSlot, tool);
            machine.processServerTick();
            h.assertValueEqual(data.get(1), base, "Efficiency no longer affects machine timing");
            for (int tier = 1; tier <= 5; tier++) {
                inventory.setItem(speedSlot, MobFarmRegistrationAdapter.UPGRADES.get(tier - 1).get().getDefaultInstance());
                h.assertValueEqual(data.get(1), Math.max(20, (base + (1 << tier) - 1) / (1 << tier)), "Dedicated speed tier " + tier);
            }
            inventory.removeItem(speedSlot, 1);
            h.assertValueEqual(data.get(1), base, "Removing speed restores tool-based timing");
            inventory.setItem(toolSlot, new ItemStack(farmer ? Items.NETHERITE_HOE : Items.NETHERITE_PICKAXE));
            h.assertTrue(data.get(1) < base, "Tool tier still improves timing");
            inventory.setItem(toolSlot, tool.copy());
            data.set(0, 117);
            var upgrade = MobFarmRegistrationAdapter.UPGRADES.getFirst().get().getDefaultInstance();
            h.assertFalse(inventory.canPlaceItem(speedSlot, MobFarmRegistrationAdapter.UPGRADES.get(5).get().getDefaultInstance()),
                    "Capacity upgrade cannot replace speed");
            var handler = h.requireCapability(Capabilities.Item.BLOCK, GameTestFixtures.TEST_POS, Direction.EAST);
            int handlerSlot = -1;
            for (int index = 0; index < handler.size(); index++) {
                if (handler.isValid(index, ItemResource.of(upgrade))) { handlerSlot = index; break; }
            }
            h.assertTrue(handlerSlot >= 0, "Horizontal capability exposes speed input");
            try (var transaction = Transaction.openRoot()) {
                h.assertValueEqual(handler.insert(handlerSlot, ItemResource.of(upgrade), 3, transaction), 1, "Speed limited to one unit");
            }
            h.assertTrue(inventory.getItem(speedSlot).isEmpty(), "Cancelled insertion restores speed input");
            h.assertValueEqual(data.get(0), 117, "Rollback preserves exact progress");
            h.assertValueEqual(data.get(1), base, "Rollback preserves duration");
            try (var transaction = Transaction.openRoot()) {
                h.assertValueEqual(handler.insert(handlerSlot, ItemResource.of(upgrade), 1, transaction), 1, "Committed speed insertion");
                transaction.commit();
            }
            machine.processServerTick();
            h.assertValueEqual(data.get(1), (base + 1) / 2, "Committed automation activates speed next tick");
            h.assertValueEqual(data.get(0), 60, "Progress rescales upward to preserve partial work before advancing");
            h.assertFalse(inventory.canTakeItemThroughFace(speedSlot, upgrade, Direction.DOWN), "Speed is not an output");
            var saved = machine.saveCustomOnly(h.getLevel().registryAccess());
            inventory.removeItem(speedSlot, 1);
            machine.loadCustomOnly(TagValueInput.create(ProblemReporter.DISCARDING, h.getLevel().registryAccess(), saved));
            h.assertTrue(ItemStack.matches(inventory.getItem(speedSlot), upgrade), "Speed survives saving");
        }
        h.succeed();
    }

    private static void storage(GameTestHelper h) {
        var blocks = new java.util.ArrayList<>(toolMachines());
        blocks.addAll(List.of(MobFarmRegistrationAdapter.BLOCK.get(), MobFarmRegistrationAdapter.INTERACTION_BLOCK.get(),
                TraderRegistrationAdapter.NETHERITE_PIGLIN_BARTERING_CELL_BLOCK.get()));
        for (Block block : blocks) {
            h.setBlock(GameTestFixtures.TEST_POS, block);
            var machine = h.getBlockEntity(GameTestFixtures.TEST_POS, PortableMachineBlockEntity.class);
            var inventory = (WorldlyContainer) machine;
            int first = machine instanceof FarmerBlockEntity || machine instanceof QuarryBlockEntity ? 3
                    : machine instanceof NetheritePiglinBarteringCellBlockEntity ? 6 : 5;
            int oldCount = machine instanceof NetheritePiglinBarteringCellBlockEntity ? 8 : 18;
            BlockEntityStateFixtures.fillIndexedSlots(h, machine, "Slot", first, oldCount, new ItemStack(Items.STONE, 63));
            h.assertTrue(inventory.getItem(first + oldCount - 1).is(Items.STONE), "Old last output stays at its original index");
            h.assertTrue(inventory.getItem(first + oldCount).isEmpty(), "New outputs start empty");
            BlockEntityStateFixtures.fillIndexedSlots(h, machine, "Slot", first + 26, 1, new ItemStack(Items.DIAMOND, 11));
            machine.prepareForBlockDrop(h.getLevel().registryAccess());
            var data = machine.getPreparedBlockDropData(h.getLevel().registryAccess());
            machine.loadCustomOnly(TagValueInput.create(ProblemReporter.DISCARDING, h.getLevel().registryAccess(), data));
            h.assertValueEqual(inventory.getItem(first).getCount(), 63, "First old output survives block drop");
            h.assertValueEqual(inventory.getItem(first + 26).getCount(), 11, "Twenty-seventh output survives block drop");
            var player = h.makeMockServerPlayer(net.minecraft.world.level.GameType.SURVIVAL);
            player.setPos(net.minecraft.world.phys.Vec3.atCenterOf(machine.getBlockPos()));
            var menu = ((MenuProvider) machine).createMenu(1, player.getInventory(), player);
            h.assertTrue(menu != null, "Expanded menu exists");
            for (int index = 0; index < 27; index++) {
                var slot = menu.getSlot(first + index);
                h.assertValueEqual(slot.x, menu.getSlot(first).x + index % 9 * 18, "Nine output columns");
                h.assertValueEqual(slot.y, menu.getSlot(first).y + index / 9 * 18, "Three output rows");
                h.assertFalse(slot.mayPlace(new ItemStack(Items.STONE)), "Output stays extraction-only");
            }
            for (int a = 0; a < menu.slots.size(); a++) {
                for (int b = a + 1; b < menu.slots.size(); b++) {
                    var left = menu.getSlot(a); var right = menu.getSlot(b);
                    h.assertTrue(Math.abs(left.x - right.x) >= 16 || Math.abs(left.y - right.y) >= 16, "Slots never overlap");
                }
            }
            menu.quickMoveStack(player, first + 26);
            h.assertTrue(inventory.getItem(first + 26).isEmpty(), "Shift extracts the last new output");
            h.assertValueEqual(player.getInventory().countItem(Items.DIAMOND), 11, "Shift preserves output quantity");
        }
        h.succeed();
    }
}
