package com.cosmocraft.trading_cells.gametest.shared;

import com.cosmocraft.trading_cells.gametest.shared.GameTestCase;
import com.cosmocraft.trading_cells.gametest.shared.GameTestFixtures;
import java.util.List;
import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.TradingCells;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Behaviour-oriented GameTests for BlockEntity. */
public final class BlockEntityGameTests {
    private BlockEntityGameTests() {
    }

    public static List<GameTestCase> tests() {
        return List.of(
            new GameTestCase("block_entity_round_trip", 40,
                    BlockEntityGameTests::blockEntityRoundTrip)
        );
    }

    private static void blockEntityRoundTrip(GameTestHelper helper) {
        int tested = 0;
        for (Block block : BuiltInRegistries.BLOCK) {
            Identifier blockId = BuiltInRegistries.BLOCK.getKey(block);
            BlockState state = block.defaultBlockState();
            if (!TradingCells.MOD_ID.equals(blockId.getNamespace()) || !state.hasBlockEntity()) {
                continue;
            }

            helper.setBlock(GameTestFixtures.TEST_POS, state);
            BlockPos absolutePos = helper.absolutePos(GameTestFixtures.TEST_POS);
            BlockEntity original = helper.getLevel().getBlockEntity(absolutePos);
            helper.assertTrue(original != null, "Missing block entity for " + blockId);

            CompoundTag expected = original.saveCustomOnly(helper.getLevel().registryAccess());
            CompoundTag full = original.saveWithFullMetadata(helper.getLevel().registryAccess());
            BlockEntity restored = BlockEntity.loadStatic(
                    absolutePos,
                    state,
                    full,
                    helper.getLevel().registryAccess()
            );
            helper.assertTrue(restored != null, "Could not reload block entity for " + blockId);
            helper.assertTrue(
                    restored.getClass().equals(original.getClass()),
                    "Reloaded a different block entity class for " + blockId
            );
            helper.assertTrue(
                    expected.equals(restored.saveCustomOnly(helper.getLevel().registryAccess())),
                    "Block entity NBT changed during round trip for " + blockId
            );
            tested++;
            helper.setBlock(GameTestFixtures.TEST_POS, Blocks.AIR);
        }

        helper.assertTrue(tested >= 12, "Expected at least 12 Trading Cells block entities, got " + tested);
        helper.succeed();
    }
}
