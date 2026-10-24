package com.cosmocraft.trading_cells.gametest.feature.logistics;

import com.cosmocraft.trading_cells.feature.logistics.adapters.input.LogisticsPipeBlockEntity;
import com.cosmocraft.trading_cells.feature.logistics.adapters.input.LogisticsNetworkManager;
import com.cosmocraft.trading_cells.feature.logistics.adapters.output.LogisticsRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.logistics.domain.model.PipeKind;
import com.cosmocraft.trading_cells.feature.logistics.domain.model.PipeSideMode;
import com.cosmocraft.trading_cells.gametest.shared.GameTestCase;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;

public final class LogisticsChunkGameTests {
    private LogisticsChunkGameTests() {
    }

    public static List<GameTestCase> tests() {
        return List.of(new GameTestCase("logistics_real_chunk_unload_reload", 1200,
                LogisticsChunkGameTests::reload),
                new GameTestCase("logistics_unrelated_chunks_keep_topology", 80, LogisticsChunkGameTests::unrelatedChunks),
                new GameTestCase("logistics_partial_chunk_split_reconnect", 3600, LogisticsChunkGameTests::split));
    }

    private static void unrelatedChunks(GameTestHelper helper) {
        var manager = LogisticsNetworkManager.get(helper.getLevel());
        // Detached registrations exercise the index without loading distant test chunks.
        BlockPos first = new BlockPos(-160_000, 64, 160_000);
        BlockPos second = first.east();
        var state = LogisticsRegistrationAdapter.pipeBlock(PipeKind.ITEM).get().defaultBlockState();
        var pipe = new LogisticsPipeBlockEntity(first, state);
        manager.register(pipe);
        manager.register(new LogisticsPipeBlockEntity(second, state));
        try {
            manager.register(new LogisticsPipeBlockEntity(first, state));
            manager.configurationChanged(pipe);
            long revision = manager.topologyRevision();
            for (int index = 0; index < 4096; index++) {
                manager.chunkTopologyChanged(new ChunkPos(-20_000 - index, 20_000));
            }
            helper.assertValueEqual(manager.topologyRevision(), revision,
                    "4096 unrelated chunk events do not restart network scans");
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    manager.chunkTopologyChanged(new ChunkPos(-10_000 + x, 10_000 + z));
                }
            }
            helper.assertValueEqual(manager.topologyRevision(), revision + 9,
                    "Current and neighbouring chunks invalidate, including diagonal terminal bridges");
            manager.unregister(first);
            revision = manager.topologyRevision();
            manager.chunkTopologyChanged(new ChunkPos(-10_000, 10_000));
            helper.assertValueEqual(manager.topologyRevision(), revision + 1,
                    "Removing one pipe preserves the other pipe in that chunk");
            manager.unregister(second);
            revision = manager.topologyRevision();
            manager.chunkTopologyChanged(new ChunkPos(-10_000, 10_000));
            helper.assertValueEqual(manager.topologyRevision(), revision,
                    "Replacing and configuring a pipe do not leak chunk reference counts");
            helper.assertTrue(helper.getLevel().getChunkSource().getChunkNow(-10_000, 10_000) == null,
                    "Topology index does not load chunks");
        } finally {
            manager.unregister(first);
            manager.unregister(second);
        }
        helper.succeed();
    }

    private static void reload(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos test = helper.absolutePos(BlockPos.ZERO);
        int chunkX = (test.getX() >> 4) + 512;
        int chunkZ = (test.getZ() >> 4) + 512;
        BlockPos sourcePos = new BlockPos(chunkX * 16 + 13, -59, chunkZ * 16 + 8);
        BlockPos targetPos = sourcePos.east(4);
        // Only the fixture loads chunks. The production network must not retain them.
        level.setChunkForced(chunkX, chunkZ, true);
        level.setChunkForced(chunkX + 1, chunkZ, true);
        level.setBlockAndUpdate(sourcePos, Blocks.BARREL.defaultBlockState());
        level.setBlockAndUpdate(targetPos, Blocks.BARREL.defaultBlockState());
        for (int offset = 1; offset <= 3; offset++) {
            level.setBlockAndUpdate(sourcePos.east(offset),
                    LogisticsRegistrationAdapter.pipeBlock(PipeKind.ITEM).get().defaultBlockState());
        }
        var source = (BarrelBlockEntity) level.getBlockEntity(sourcePos);
        var originalTarget = (BarrelBlockEntity) level.getBlockEntity(targetPos);
        var pipe = (LogisticsPipeBlockEntity) level.getBlockEntity(sourcePos.east());
        source.setItem(0, new ItemStack(Items.DIAMOND, 4));
        pipe.setMode(Direction.WEST, PipeSideMode.EXTRACT);
        var stage = new int[] {0};
        var restoredAt = new long[] {0};
        helper.runAtTickTime(40, () -> {
            var target = (BarrelBlockEntity) level.getBlockEntity(targetPos);
            helper.assertValueEqual(target.countItem(Items.DIAMOND), 4, "Cross-chunk route works while loaded");
            pipe.setMode(Direction.WEST, PipeSideMode.NONE);
            source.setItem(0, new ItemStack(Items.DIAMOND, 4));
            level.setChunkForced(chunkX, chunkZ, false);
            level.setChunkForced(chunkX + 1, chunkZ, false);
            stage[0] = 1;
        });
        helper.onEachTick(() -> {
            if (stage[0] == 1
                    && pipe.isRemoved() && originalTarget.isRemoved()
                    && level.getChunkSource().getChunkNow(chunkX, chunkZ) == null
                    && level.getChunkSource().getChunkNow(chunkX + 1, chunkZ) == null) {
                helper.assertValueEqual(source.countItem(Items.DIAMOND), 4, "Unloaded source stays untouched");
                level.setChunkForced(chunkX, chunkZ, true);
                level.setChunkForced(chunkX + 1, chunkZ, true);
                var restored = (LogisticsPipeBlockEntity) level.getBlockEntity(sourcePos.east());
                helper.assertTrue(restored != null && !restored.isRemoved(), "Completed chunk unload and reload");
                helper.assertValueEqual(restored.face(Direction.WEST).mode(), PipeSideMode.NONE, "Face mode persisted");
                restored.setMode(Direction.WEST, PipeSideMode.EXTRACT);
                restoredAt[0] = helper.getTick();
                stage[0] = 2;
            } else if (stage[0] == 2 && helper.getTick() - restoredAt[0] >= 45) {
                var target = (BarrelBlockEntity) level.getBlockEntity(targetPos);
                helper.assertValueEqual(target.countItem(Items.DIAMOND), 8, "Reloaded route resumes without loss");
                for (int offset = 0; offset <= 4; offset++) {
                    level.setBlockAndUpdate(sourcePos.east(offset), Blocks.AIR.defaultBlockState());
                }
                level.setChunkForced(chunkX, chunkZ, false);
                level.setChunkForced(chunkX + 1, chunkZ, false);
                stage[0] = 3;
                helper.succeed();
            }
        });
        helper.runBeforeTestEnd(() -> {
            level.setChunkForced(chunkX, chunkZ, false);
            level.setChunkForced(chunkX + 1, chunkZ, false);
        });
    }

    private static void split(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos test = helper.absolutePos(BlockPos.ZERO);
        int chunkX = (test.getX() >> 4) + 1024;
        int chunkZ = (test.getZ() >> 4) + 1024;
        BlockPos sourcePos = new BlockPos(chunkX * 16 + 8, -59, chunkZ * 16 + 8);
        BlockPos targetPos = sourcePos.east(128);
        for (int offset = 0; offset <= 8; offset++) {
            level.setChunkForced(chunkX + offset, chunkZ, true);
        }
        helper.runBeforeTestEnd(() -> {
            for (int offset = 0; offset <= 8; offset++) {
                level.setChunkForced(chunkX + offset, chunkZ, false);
            }
        });
        level.setBlockAndUpdate(sourcePos, Blocks.BARREL.defaultBlockState());
        level.setBlockAndUpdate(targetPos, Blocks.BARREL.defaultBlockState());
        for (int offset = 1; offset < 128; offset++) {
            level.setBlockAndUpdate(sourcePos.east(offset),
                    LogisticsRegistrationAdapter.pipeBlock(PipeKind.ITEM).get().defaultBlockState());
        }
        var source = (BarrelBlockEntity) level.getBlockEntity(sourcePos);
        var target = (BarrelBlockEntity) level.getBlockEntity(targetPos);
        var origin = (LogisticsPipeBlockEntity) level.getBlockEntity(sourcePos.east());
        var middle = (LogisticsPipeBlockEntity) level.getBlockEntity(sourcePos.east(64));
        source.setItem(0, new ItemStack(Items.DIAMOND, 4));
        origin.setMode(Direction.WEST, PipeSideMode.EXTRACT);
        int[] stage = {0};
        long[] disconnectedAt = {0};
        // A chunk force-load schedules work; neither unloading nor loading has
        // a fixed completion tick. Keep an explicit watchdog for stalled stages,
        // rather than treating 1790 as the expected time for every run.
        helper.runAtTickTime(3500, () -> helper.assertTrue(stage[0] == 4,
                "Partial chunk split/reconnect timed out at stage " + stage[0]
                        + "; source=" + source.countItem(Items.DIAMOND)
                        + ", target=" + target.countItem(Items.DIAMOND)
                        + ", sourceRemoved=" + source.isRemoved()
                        + ", originRemoved=" + origin.isRemoved()
                        + ", originFace=" + origin.face(Direction.WEST).mode()
                        + ", registeredPipes=" + LogisticsNetworkManager.get(level).registeredPipeCount()
                        + ", routeReady=" + LogisticsNetworkManager.get(level).topologyReady(origin.getBlockPos(), com.cosmocraft.trading_cells.feature.logistics.domain.model.LogisticsResourceType.ITEM)
                        + ", routeEndpoints=" + LogisticsNetworkManager.get(level).endpointsFrom(
                                origin.getBlockPos(), com.cosmocraft.trading_cells.feature.logistics.domain.model.LogisticsResourceType.ITEM,
                                PipeSideMode.INSERT, null).size()
                        + ", middleRemoved=" + middle.isRemoved()
                        + ", middleLoaded=" + (level.getChunkSource().getChunkNow(chunkX + 4, chunkZ) != null)));
        helper.onEachTick(() -> {
            if (stage[0] == 0 && target.countItem(Items.DIAMOND) == 4) {
                // A loaded chunk retains its neighbours; remove a wide enough middle strip.
                for (int offset = 2; offset <= 6; offset++) {
                    level.setChunkForced(chunkX + offset, chunkZ, false);
                }
                stage[0] = 1;
            } else if (stage[0] == 1 && level.getChunkSource().getChunkNow(chunkX + 4, chunkZ) == null) {
                helper.assertFalse(origin.isRemoved() || source.isRemoved() || target.isRemoved(), "Both ends remain loaded");
                source.setItem(0, new ItemStack(Items.DIAMOND, 4));
                disconnectedAt[0] = helper.getTick();
                stage[0] = 2;
            } else if (stage[0] == 2 && helper.getTick() - disconnectedAt[0] >= 45) {
                helper.assertValueEqual(source.countItem(Items.DIAMOND), 4, "Disconnected source retains all items");
                helper.assertValueEqual(target.countItem(Items.DIAMOND), 4, "No transport across unloaded middle");
                helper.assertTrue(level.getChunkSource().getChunkNow(chunkX + 4, chunkZ) == null, "Network never reloads missing chunk");
                for (int offset = 2; offset <= 6; offset++) {
                    level.setChunkForced(chunkX + offset, chunkZ, true);
                }
                stage[0] = 3;
            } else if (stage[0] == 3 && target.countItem(Items.DIAMOND) == 8) {
                // Verify the route actually resumes; no manual pipe refresh is
                // permitted here, so a real reconnect failure stays visible.
                helper.assertValueEqual(source.countItem(Items.DIAMOND), 0, "Reconnected network consumes source once");
                for (int offset = 0; offset <= 128; offset++) {
                    level.setBlockAndUpdate(sourcePos.east(offset), Blocks.AIR.defaultBlockState());
                }
                stage[0] = 4;
                helper.succeed();
            }
        });
    }
}