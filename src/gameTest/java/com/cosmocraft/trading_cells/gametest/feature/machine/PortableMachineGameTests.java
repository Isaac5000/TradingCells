package com.cosmocraft.trading_cells.gametest.feature.machine;

import com.cosmocraft.trading_cells.gametest.shared.GameTestCase;
import com.cosmocraft.trading_cells.gametest.shared.GameTestFixtures;
import java.util.List;
import com.cosmocraft.trading_cells.feature.experience.adapters.input.ExperienceStorageBlockEntity;
import com.cosmocraft.trading_cells.feature.infusion.adapters.input.ArcaneInfuserBlockEntity;
import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.TradingCells;
import com.cosmocraft.trading_cells.platform.neoforge.registration.ExperienceFluidRegistration;
import com.cosmocraft.trading_cells.platform.neoforge.machine.PortableMachineBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/** Behaviour-oriented GameTests for PortableMachine. */
public final class PortableMachineGameTests {
    private PortableMachineGameTests() {
    }

    public static List<GameTestCase> tests() {
        return List.of(
            new GameTestCase("portable_machine_drop_round_trip", 40,
                    PortableMachineGameTests::portableMachineDropRoundTrip)
        );
    }

    private static void portableMachineDropRoundTrip(GameTestHelper helper) {
        int tested = 0;
        int stateful = 0;
        FluidResource experience = FluidResource.of(ExperienceFluidRegistration.SOURCE.get());
        for (Block block : BuiltInRegistries.BLOCK) {
            Identifier blockId = BuiltInRegistries.BLOCK.getKey(block);
            BlockState state = block.defaultBlockState();
            if (!TradingCells.MOD_ID.equals(blockId.getNamespace())
                    || !(block instanceof EntityBlock entityBlock)
                    || !state.hasBlockEntity()) {
                continue;
            }

            helper.setBlock(GameTestFixtures.TEST_POS, state);
            BlockEntity original = helper.getLevel().getBlockEntity(helper.absolutePos(GameTestFixtures.TEST_POS));
            if (!(original instanceof PortableMachineBlockEntity machine)) {
                helper.setBlock(GameTestFixtures.TEST_POS, Blocks.AIR);
                continue;
            }

            int markerSlot = -1;
            ItemStack markerStack = ItemStack.EMPTY;
            if (machine instanceof Container container && container.getContainerSize() > 0) {
                markerSlot = insertValidMarker(container);
                if (markerSlot >= 0) {
                    markerStack = container.getItem(markerSlot).copy();
                    stateful++;
                }
            }
            int expectedExperience = addPortableExperience(machine, experience);
            if (expectedExperience > 0) {
                stateful++;
            }

            machine.prepareForBlockDrop(helper.getLevel().registryAccess());
            CompoundTag prepared = machine.getPreparedBlockDropData(helper.getLevel().registryAccess());
            if (machine instanceof Container cleared) {
                helper.assertTrue(
                        cleared.isEmpty(),
                        "Portable machine retained live inventory after snapshot for " + blockId
                );
            }

            BlockEntity restored = entityBlock.newBlockEntity(helper.absolutePos(GameTestFixtures.TEST_POS), state);
            helper.assertTrue(restored != null, "Could not recreate portable machine " + blockId);
            restored.loadCustomOnly(TagValueInput.create(
                    ProblemReporter.DISCARDING,
                    helper.getLevel().registryAccess(),
                    prepared.copy()
            ));
            if (markerSlot >= 0) {
                helper.assertTrue(
                        restored instanceof Container restoredContainer
                                && ItemStack.isSameItemSameComponents(
                                        restoredContainer.getItem(markerSlot),
                                        markerStack
                                )
                                && restoredContainer.getItem(markerSlot).getCount() == markerStack.getCount(),
                        "Portable inventory did not survive the block item for " + blockId
                );
            }
            if (expectedExperience > 0) {
                helper.assertValueEqual(
                        portableExperience(restored),
                        expectedExperience,
                        "Portable XP did not survive the block item for " + blockId
                );
            }
            machine.discardContentsAfterBlockDrop();
            tested++;
            helper.setBlock(GameTestFixtures.TEST_POS, Blocks.AIR);
        }

        helper.assertTrue(tested >= 10, "Expected at least ten portable machines, got " + tested);
        helper.assertTrue(stateful >= 10, "Expected stateful portable-machine coverage, got " + stateful);
        helper.succeed();
    }

    private static int insertValidMarker(Container container) {
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            for (Item item : BuiltInRegistries.ITEM) {
                ItemStack candidate = new ItemStack(item);
                if (candidate.isEmpty() || !container.canPlaceItem(slot, candidate)) {
                    continue;
                }
                container.setItem(slot, candidate);
                if (!container.getItem(slot).isEmpty()) {
                    return slot;
                }
            }
        }
        return -1;
    }

    private static int addPortableExperience(
            PortableMachineBlockEntity machine,
            FluidResource experience
    ) {
        ResourceHandler<FluidResource> handler;
        if (machine instanceof ExperienceStorageBlockEntity storage) {
            handler = storage.fluidHandler();
        } else if (machine instanceof ArcaneInfuserBlockEntity infuser) {
            handler = infuser.fluidHandler();
        } else {
            return 0;
        }
        try (Transaction transaction = Transaction.openRoot()) {
            long inserted = handler.insert(0, experience, 321, transaction);
            transaction.commit();
            return (int) inserted;
        }
    }

    private static int portableExperience(BlockEntity blockEntity) {
        if (blockEntity instanceof ExperienceStorageBlockEntity storage) {
            return storage.storedExperience();
        }
        return blockEntity instanceof ArcaneInfuserBlockEntity infuser ? infuser.storedExperience() : 0;
    }
}
