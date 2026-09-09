package com.cosmocraft.trading_cells.gametest.shared;

import com.cosmocraft.trading_cells.platform.neoforge.registration.Registration;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/** Shared machine contracts exercised through the same capability used by external transports. */
public final class MachineItemCapabilityGameTests {
    private MachineItemCapabilityGameTests() {
    }

    public static List<GameTestCase> tests() {
        return List.of(new GameTestCase("logistics_all_machine_sided_item_capabilities", 80,
                MachineItemCapabilityGameTests::sidedCapabilities),
                new GameTestCase("logistics_machine_output_rollback", 80, MachineItemCapabilityGameTests::outputRollback));
    }

    private static void outputRollback(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 2, 2);
        int checked = 0;
        for (var registered : Registration.BLOCKS.getEntries()) {
            helper.setBlock(pos, registered.get());
            var entity = helper.getLevel().getBlockEntity(helper.absolutePos(pos));
            if (!(entity instanceof WorldlyContainer container)) { continue; }
            int[] outputs = container.getSlotsForFace(Direction.DOWN);
            for (int index = 0; index < outputs.length; index++) {
                int slot = outputs[index];
                ItemStack produced = new ItemStack(Items.DIAMOND, 8);
                if (container.canPlaceItem(slot, produced)
                        || !container.canTakeItemThroughFace(slot, produced, Direction.DOWN)) { continue; }
                var data = entity.saveWithFullMetadata(helper.getLevel().registryAccess());
                var encoded = ItemStack.CODEC.encodeStart(helper.getLevel().registryAccess()
                        .createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), produced).getOrThrow();
                String key = entity instanceof com.cosmocraft.trading_cells.feature.incubators.adapters.input.IncubatorBlockEntity
                        ? "Output" : "Slot" + slot;
                data.put(key, encoded);
                entity.loadWithComponents(net.minecraft.world.level.storage.TagValueInput.create(
                        net.minecraft.util.ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), data));
                if (container.getItem(slot).isEmpty()) { continue; }
                checked++;
                var before = entity.saveWithoutMetadata(helper.getLevel().registryAccess());
                var resource = ItemResource.of(produced);
                for (Direction side : java.util.Arrays.asList(null, Direction.DOWN, Direction.UP,
                        Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST)) {
                    var access = helper.requireCapability(Capabilities.Item.BLOCK, pos, side);
                    int resourceSlot = -1;
                    for (int entry = 0; entry < access.size(); entry++) {
                        if (access.getResource(entry).equals(resource)) { resourceSlot = entry; break; }
                    }
                    helper.assertTrue(resourceSlot >= 0, registered.getId() + " exposes output on " + side);
                    try (var transaction = Transaction.openRoot()) {
                        helper.assertValueEqual(access.extract(resourceSlot, resource, 8, transaction), 8,
                                registered.getId() + " can empty its output transactionally on " + side);
                        helper.assertValueEqual(access.insert(resourceSlot, resource, 1, transaction), 0,
                                "External insertion cannot use an output slot");
                    }
                    helper.assertValueEqual(entity.saveWithoutMetadata(helper.getLevel().registryAccess()), before,
                            registered.getId() + " aborted extraction preserves exact state on " + side);
                }
                var handler = helper.requireCapability(Capabilities.Item.BLOCK, pos, Direction.DOWN);
                try (var transaction = Transaction.openRoot()) {
                    helper.assertValueEqual(handler.insert(index, resource, 1, transaction), 0,
                            "Rollback support does not permit external output insertion");
                    helper.assertValueEqual(handler.extract(index, resource, 3, transaction), 3, "Partial extraction");
                    transaction.commit();
                }
                helper.assertValueEqual(container.getItem(slot).getCount(), 5, "Partial extraction conserves remainder");
                container.setItem(slot, ItemStack.EMPTY);
            }
        }
        helper.assertTrue(checked >= 30, "All registered machine families exercised");
        helper.setBlock(pos, Blocks.AIR);
        helper.succeed();
    }

    private static void sidedCapabilities(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 2, 2);
        var probes = List.of(Items.DIAMOND, Items.WHEAT, Items.EMERALD, Items.GOLD_INGOT,
                Items.NETHERITE_SWORD, Items.NETHERITE_HOE, Items.PORKCHOP);
        int checked = 0;
        for (var registered : Registration.BLOCKS.getEntries()) {
            helper.setBlock(pos, registered.get());
            var entity = helper.getLevel().getBlockEntity(helper.absolutePos(pos));
            if (!(entity instanceof WorldlyContainer container)) {
                continue;
            }
            checked++;
            for (Direction side : Direction.values()) {
                var handler = helper.requireCapability(Capabilities.Item.BLOCK, pos, side);
                int[] slots = container.getSlotsForFace(side);
                helper.assertTrue(handler.size() >= slots.length && handler.size() <= container.getContainerSize(),
                        registered.getId() + " retains sided slots without duplicates " + side);
                for (int index = 0; index < slots.length; index++) {
                    int slot = slots[index];
                    if (!container.getItem(slot).isEmpty()) {
                        continue;
                    }
                    for (var item : probes) {
                        ItemStack stack = new ItemStack(item);
                        boolean allowed = container.canPlaceItem(slot, stack)
                                && container.canPlaceItemThroughFace(slot, stack, side);
                        try (var transaction = Transaction.openRoot()) {
                            int inserted = handler.insert(index, ItemResource.of(stack), 1, transaction);
                            helper.assertValueEqual(inserted, allowed ? 1 : 0,
                                    registered.getId() + " insertion rules " + side + " slot " + slot);
                        }
                        helper.assertTrue(container.getItem(slot).isEmpty(), "Simulated insertion must roll back");
                    }
                    ItemStack probe = new ItemStack(Items.DIAMOND);
                    container.setItem(slot, probe.copy());
                    if (!container.getItem(slot).isEmpty()) {
                        boolean allowed = container.canTakeItemThroughFace(slot, probe, side)
                                || java.util.Arrays.stream(container.getSlotsForFace(Direction.DOWN))
                                .anyMatch(output -> output == slot)
                                && container.canTakeItemThroughFace(slot, probe, Direction.DOWN);
                        try (var transaction = Transaction.openRoot()) {
                            helper.assertValueEqual(handler.extract(index, ItemResource.of(probe), 1, transaction),
                                    allowed ? 1 : 0, registered.getId() + " extraction rules " + side + " slot " + slot);
                        }
                        helper.assertValueEqual(container.getItem(slot).getCount(), 1, "Simulated extraction must roll back");
                    }
                    container.setItem(slot, ItemStack.EMPTY);
                }
            }
            var unsided = helper.requireCapability(Capabilities.Item.BLOCK, pos, null);
            helper.assertValueEqual(unsided.size(), container.getSlotsForFace(Direction.DOWN).length,
                    "Unsided consumers cannot inspect private worker or upgrade slots");
        }
        helper.setBlock(pos, Blocks.AIR);
        helper.assertTrue(checked > 0, "At least one machine contract was tested");
        helper.succeed();
    }
}
