package com.cosmocraft.trading_cells.gametest.feature.logistics;

import com.cosmocraft.trading_cells.feature.experience.adapters.input.ExperienceStorageBlockEntity;
import com.cosmocraft.trading_cells.feature.experience.adapters.output.ExperienceStorageRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.logistics.adapters.input.LogisticsPipeBlockEntity;
import com.cosmocraft.trading_cells.feature.logistics.domain.model.*;
import com.cosmocraft.trading_cells.gametest.shared.GameTestCase;
import com.cosmocraft.trading_cells.platform.neoforge.registration.ExperienceFluidRegistration;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import static com.cosmocraft.trading_cells.gametest.feature.logistics.LogisticsTestFixtures.*;

public final class LogisticsTransportGameTests {
    private LogisticsTransportGameTests() {
    }

    public static List<GameTestCase> tests() {
        return List.of(
                new GameTestCase("logistics_partial_output_conservation", 80, LogisticsTransportGameTests::partialOutput),
                new GameTestCase("logistics_priority_before_distance", 80, LogisticsTransportGameTests::priority),
                new GameTestCase("logistics_nearest_equal_priority", 80, LogisticsTransportGameTests::nearest),
                new GameTestCase("logistics_copper_farthest", 100, helper -> distribution(helper, PipeRoutingMode.FARTHEST)),
                new GameTestCase("logistics_copper_equal", 100, helper -> distribution(helper, PipeRoutingMode.EQUAL)),
                new GameTestCase("logistics_copper_random", 100, helper -> distribution(helper, PipeRoutingMode.RANDOM)),
                new GameTestCase("logistics_equal_routes_round_robin", 200, LogisticsTransportGameTests::roundRobin),
                new GameTestCase("logistics_channels_first_match_filters", 80, LogisticsTransportGameTests::filters),
                new GameTestCase("logistics_disconnect_reconnect", 100, LogisticsTransportGameTests::disconnect),
                new GameTestCase("logistics_xp_uses_fluid_route", 80, LogisticsTransportGameTests::experience),
                new GameTestCase("logistics_xp_all_storage_faces", 600, LogisticsTransportGameTests::experienceFaces));
    }

    private static void experienceFaces(GameTestHelper helper) {
        var center = new net.minecraft.core.BlockPos(3, 3, 3);
        var sequence = helper.startSequence();
        for (var kind : List.of(PipeKind.FLUID, PipeKind.UNIVERSAL)) {
            for (Direction side : Direction.values()) {
                var connection = center.relative(side);
                var destination = center.relative(side, 2);
                sequence.thenExecute(() -> {
                    helper.setBlock(center, ExperienceStorageRegistrationAdapter.BLOCK.get());
                    helper.setBlock(destination, ExperienceStorageRegistrationAdapter.BLOCK.get());
                    var storage = helper.getBlockEntity(center, ExperienceStorageBlockEntity.class);
                    try (var transaction = Transaction.openRoot()) {
                        storage.fluidHandler().insert(FluidResource.of(ExperienceFluidRegistration.SOURCE.get()), 500, transaction);
                        transaction.commit();
                    }
                    var node = pipe(helper, connection, kind);
                    node.setMode(side.getOpposite(), PipeSideMode.EXTRACT);
                    node.setMode(side, PipeSideMode.INSERT);
                }).thenWaitUntil(() -> helper.assertValueEqual(
                        helper.getBlockEntity(destination, ExperienceStorageBlockEntity.class).storedExperience(), 500,
                        kind + " extracts via " + side)).thenExecute(() -> {
                    helper.assertValueEqual(helper.getBlockEntity(center, ExperienceStorageBlockEntity.class).storedExperience(), 0, "No duplicated XP");
                    var node = helper.getBlockEntity(connection, com.cosmocraft.trading_cells.feature.logistics.adapters.input.LogisticsPipeBlockEntity.class);
                    node.setMode(side.getOpposite(), PipeSideMode.INSERT);
                    node.setMode(side, PipeSideMode.EXTRACT);
                }).thenWaitUntil(() -> helper.assertValueEqual(
                        helper.getBlockEntity(center, ExperienceStorageBlockEntity.class).storedExperience(), 500,
                        kind + " inserts via " + side)).thenExecute(() -> {
                    helper.assertValueEqual(helper.getBlockEntity(destination, ExperienceStorageBlockEntity.class).storedExperience(), 0, "No XP lost on return");
                    helper.setBlock(connection, Blocks.AIR);
                    helper.setBlock(destination, Blocks.AIR);
                    helper.setBlock(center, Blocks.AIR);
                });
            }
        }
        sequence.thenSucceed();
    }

    private static void partialOutput(GameTestHelper helper) {
        var source = barrel(helper, SOURCE, Items.DIAMOND, 20);
        var target = barrel(helper, TARGET, Items.DIAMOND, 63);
        for (int slot = 1; slot < target.getContainerSize(); slot++) {
            target.setItem(slot, new net.minecraft.world.item.ItemStack(Items.COBBLESTONE, 64));
        }
        var origin = line(helper, PipeKind.ITEM);
        origin.setMode(Direction.WEST, PipeSideMode.EXTRACT);
        helper.runAtTickTime(35, () -> {
            helper.assertValueEqual(count(target, Items.DIAMOND), 64, "Partial output accepts exactly one item");
            helper.assertValueEqual(count(source, Items.DIAMOND), 19, "Remainder stays in source");
            helper.succeed();
        });
    }

    private static void priority(GameTestHelper helper) {
        var source = barrel(helper, SOURCE, Items.DIAMOND, 8);
        var near = barrel(helper, ORIGIN.above(), Items.DIAMOND, 0);
        var far = barrel(helper, TARGET, Items.DIAMOND, 0);
        var origin = line(helper, PipeKind.UNIVERSAL);
        origin.setPriority(Direction.UP, Integer.MIN_VALUE);
        helper.getBlockEntity(ORIGIN.east(2), LogisticsPipeBlockEntity.class)
                .setPriority(Direction.EAST, Integer.MAX_VALUE);
        origin.setMode(Direction.WEST, PipeSideMode.EXTRACT);
        helper.runAtTickTime(50, () -> {
            helper.assertValueEqual(count(far, Items.DIAMOND), 8, "Signed priority outranks distance");
            helper.assertValueEqual(count(near, Items.DIAMOND), 0, "Lower priority untouched");
            helper.assertValueEqual(count(source, Items.DIAMOND), 0, "Source depleted exactly");
            helper.succeed();
        });
    }

    private static void filters(GameTestHelper helper) {
        var source = barrel(helper, SOURCE, Items.DIAMOND, 8);
        source.setItem(1, new net.minecraft.world.item.ItemStack(Items.IRON_INGOT, 8));
        var target = barrel(helper, TARGET, Items.DIAMOND, 0);
        var wrongChannel = barrel(helper, ORIGIN.above(), Items.DIAMOND, 0);
        var origin = line(helper, PipeKind.ITEM);
        origin.setUpgradeTier(Direction.WEST, PipeUpgradeTier.ULTIMATE);
        origin.setUpgradeTier(Direction.UP, PipeUpgradeTier.ADVANCED);
        var sourceFace = origin.face(Direction.WEST).copy();
        sourceFace.setMode(PipeSideMode.EXTRACT, true);
        sourceFace.setProfile(LogisticsResourceType.ITEM, new PipeResourceProfile(true, "alpha", List.of(
                new PipeFilterRule(PipeFilterRule.Action.DENY, PipeFilterRule.MatchKind.ID,
                        "trading_cells:item|minecraft:iron_ingot", PipeFilterRule.ComponentMatch.IGNORE, "", ""))));
        origin.applyFaceConfiguration(Direction.WEST, sourceFace);
        var upper = origin.face(Direction.UP).copy();
        upper.setProfile(LogisticsResourceType.ITEM, new PipeResourceProfile(true, "BETA", List.of()));
        origin.applyFaceConfiguration(Direction.UP, upper);
        var end = helper.getBlockEntity(ORIGIN.east(2), LogisticsPipeBlockEntity.class);
        end.setUpgradeTier(Direction.EAST, PipeUpgradeTier.ADVANCED);
        var destination = end.face(Direction.EAST).copy();
        destination.setProfile(LogisticsResourceType.ITEM, new PipeResourceProfile(true, "ALPHA", List.of()));
        end.applyFaceConfiguration(Direction.EAST, destination);
        helper.runAtTickTime(50, () -> {
            helper.assertValueEqual(count(target, Items.DIAMOND), 8, "Case-insensitive channel match");
            helper.assertValueEqual(count(wrongChannel, Items.DIAMOND), 0, "Other channel isolated");
            helper.assertValueEqual(count(source, Items.IRON_INGOT), 8, "Adapter-qualified deny rule");
            helper.succeed();
        });
    }

    private static void nearest(GameTestHelper helper) {
        barrel(helper, SOURCE, Items.DIAMOND, 8);
        var near = barrel(helper, ORIGIN.above(), Items.DIAMOND, 0);
        var far = barrel(helper, TARGET, Items.DIAMOND, 0);
        var origin = line(helper, PipeKind.ITEM);
        origin.setMode(Direction.WEST, PipeSideMode.EXTRACT);
        helper.runAtTickTime(50, () -> {
            helper.assertValueEqual(count(near, Items.DIAMOND), 8, "Shortest route wins equal priorities");
            helper.assertValueEqual(count(far, Items.DIAMOND), 0, "Longer route remains unused");
            helper.succeed();
        });
    }

    private static void distribution(GameTestHelper helper, PipeRoutingMode mode) {
        var source = barrel(helper, SOURCE, Items.DIAMOND, 8);
        var near = barrel(helper, ORIGIN.above(), Items.DIAMOND, 0);
        var far = barrel(helper, TARGET, Items.DIAMOND, 0);
        var origin = line(helper, PipeKind.ITEM);
        origin.setUpgradeTier(Direction.WEST, PipeUpgradeTier.BASIC);
        var settings = origin.face(Direction.WEST).copy();
        settings.setMode(PipeSideMode.EXTRACT, true);
        settings.setProfile(LogisticsResourceType.ITEM, new PipeResourceProfile(true, "", List.of(),
                PipeResourceProfile.FilterMode.OFF, mode));
        origin.applyFaceConfiguration(Direction.WEST, settings);
        helper.succeedWhen(() -> {
            helper.assertValueEqual(count(source, Items.DIAMOND), 0, "Copper batch completes");
            helper.assertValueEqual(count(near, Items.DIAMOND) + count(far, Items.DIAMOND), 8, "Resources conserved");
            if (mode == PipeRoutingMode.FARTHEST) {
                helper.assertValueEqual(count(far, Items.DIAMOND), 8, "Farthest path chosen");
            } else if (mode == PipeRoutingMode.EQUAL) {
                helper.assertValueEqual(count(near, Items.DIAMOND), 4, "Nearby inventory shares the batch");
                helper.assertValueEqual(count(far, Items.DIAMOND), 4, "Distant inventory shares the same batch");
            }
        });
    }

    private static void roundRobin(GameTestHelper helper) {
        var source = barrel(helper, SOURCE, Items.DIAMOND, 16);
        var upper = barrel(helper, ORIGIN.above(), Items.DIAMOND, 0);
        var lower = barrel(helper, ORIGIN.below(), Items.DIAMOND, 0);
        var origin = line(helper, PipeKind.ITEM);
        origin.setMode(Direction.WEST, PipeSideMode.EXTRACT);
        helper.runAtTickTime(30, () -> network(helper).invalidateTopology());
        helper.succeedWhen(() -> {
            helper.assertValueEqual(count(source, Items.DIAMOND), 0, "All batches have completed");
            helper.assertValueEqual(count(upper, Items.DIAMOND), 8, "First equal route gets half the batches");
            helper.assertValueEqual(count(lower, Items.DIAMOND), 8, "Second equal route is not starved");
        });
    }

    private static void disconnect(GameTestHelper helper) {
        var source = barrel(helper, SOURCE, Items.DIAMOND, 4);
        var target = barrel(helper, TARGET, Items.DIAMOND, 0);
        var origin = line(helper, PipeKind.ITEM);
        origin.cycleSide(Direction.EAST);
        origin.setMode(Direction.WEST, PipeSideMode.EXTRACT);
        helper.runAtTickTime(30, () -> {
            helper.assertValueEqual(count(source, Items.DIAMOND), 4, "Disabled physical link blocks transfer");
            origin.cycleSide(Direction.EAST);
        });
        helper.runAtTickTime(65, () -> {
            helper.assertValueEqual(count(target, Items.DIAMOND), 4, "Reconnected route resumes");
            helper.setBlock(ORIGIN.east(), Blocks.AIR);
            source.setItem(0, new net.minecraft.world.item.ItemStack(Items.DIAMOND, 4));
        });
        helper.runAtTickTime(90, () -> {
            helper.assertValueEqual(count(source, Items.DIAMOND), 4, "Removed pipe invalidates cached route");
            helper.succeed();
        });
    }

    private static void experience(GameTestHelper helper) {
        helper.setBlock(SOURCE, ExperienceStorageRegistrationAdapter.BLOCK.get());
        helper.setBlock(TARGET, ExperienceStorageRegistrationAdapter.BLOCK.get());
        var source = helper.getBlockEntity(SOURCE, ExperienceStorageBlockEntity.class);
        var target = helper.getBlockEntity(TARGET, ExperienceStorageBlockEntity.class);
        var handler = helper.requireCapability(Capabilities.Fluid.BLOCK, SOURCE, Direction.EAST);
        try (var transaction = Transaction.openRoot()) {
            handler.insert(FluidResource.of(ExperienceFluidRegistration.SOURCE.get()), 500, transaction);
            transaction.commit();
        }
        var origin = line(helper, PipeKind.FLUID);
        origin.setMode(Direction.WEST, PipeSideMode.EXTRACT);
        helper.runAtTickTime(35, () -> {
            helper.assertValueEqual(source.storedExperience(), 0, "XP extracted through standard fluid capability");
            helper.assertValueEqual(target.storedExperience(), 500, "XP conservation");
            helper.succeed();
        });
    }
}
