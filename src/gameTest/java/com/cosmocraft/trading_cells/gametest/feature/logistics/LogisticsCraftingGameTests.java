package com.cosmocraft.trading_cells.gametest.feature.logistics;

import com.cosmocraft.trading_cells.feature.logistics.adapters.input.NetworkCraftingService;
import com.cosmocraft.trading_cells.feature.logistics.adapters.neoforge.ItemLogisticsAdapter;
import com.cosmocraft.trading_cells.feature.logistics.adapters.neoforge.LogisticsComponentData;
import com.cosmocraft.trading_cells.feature.logistics.domain.model.*;
import com.cosmocraft.trading_cells.gametest.shared.GameTestCase;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import static com.cosmocraft.trading_cells.gametest.feature.logistics.LogisticsTestFixtures.*;

public final class LogisticsCraftingGameTests {
    private LogisticsCraftingGameTests() {
    }

    public static List<GameTestCase> tests() {
        return List.of(
                new GameTestCase("logistics_crafting_round_robin_rollback", 120, LogisticsCraftingGameTests::roundRobin),
                new GameTestCase("logistics_terminal_failed_transfer_keeps_turn", 120, LogisticsCraftingGameTests::failedTransfer),
                new GameTestCase("logistics_crafting_remainders_to_network", 120, LogisticsCraftingGameTests::remainders));
    }

    private static void roundRobin(GameTestHelper helper) {
        var left = barrel(helper, SOURCE, Items.OAK_LOG, 8);
        var right = barrel(helper, ORIGIN.east(), Items.OAK_LOG, 8);
        var origin = pipe(helper, ORIGIN, PipeKind.ITEM);
        origin.setMode(Direction.WEST, PipeSideMode.EXTRACT);
        origin.setMode(Direction.EAST, PipeSideMode.EXTRACT);
        var player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        var grid = new ArrayList<>(Collections.nCopies(9, ItemStack.EMPTY));
        grid.set(0, new ItemStack(Items.OAK_LOG));
        helper.startSequence().thenWaitUntil(() -> helper.assertTrue(
                network(helper).topologyReady(origin.getBlockPos(), LogisticsResourceType.ITEM), "Routes ready")).thenExecute(() -> {
            helper.assertTrue(NetworkCraftingService.craft(helper.getLevel(), origin.getBlockPos(), player, grid, 1), "First recipe");
            for (int slot = 0; slot < 36; slot++) {
                player.getInventory().setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
            }
            helper.assertFalse(NetworkCraftingService.craft(helper.getLevel(), origin.getBlockPos(), player, grid, 1), "Full player cancels recipe");
            helper.assertValueEqual(count(left, Items.OAK_LOG) + count(right, Items.OAK_LOG), 15, "Rollback restores ingredients");
            player.getInventory().clearContent();
            helper.assertTrue(NetworkCraftingService.craft(helper.getLevel(), origin.getBlockPos(), player, grid, 1), "Next recipe");
            helper.assertValueEqual(count(left, Items.OAK_LOG), 7, "Cancelled recipe preserves left turn");
            helper.assertValueEqual(count(right, Items.OAK_LOG), 7, "Cancelled recipe preserves right turn");
            helper.assertTrue(NetworkCraftingService.craft(helper.getLevel(), origin.getBlockPos(), player, grid, 4), "Batch recipe");
            helper.assertValueEqual(count(left, Items.OAK_LOG), 5, "Batch alternates left source");
            helper.assertValueEqual(count(right, Items.OAK_LOG), 5, "Batch alternates right source");
            helper.assertValueEqual(player.getInventory().countItem(Items.OAK_PLANKS), 20, "Exact committed output");
        }).thenSucceed();
    }

    private static void failedTransfer(GameTestHelper helper) {
        var left = barrel(helper, SOURCE, Items.DIAMOND, 8);
        var right = barrel(helper, ORIGIN.east(), Items.DIAMOND, 8);
        var origin = pipe(helper, ORIGIN, PipeKind.ITEM);
        origin.setMode(Direction.WEST, PipeSideMode.EXTRACT);
        origin.setMode(Direction.EAST, PipeSideMode.EXTRACT);
        var adapter = new ItemLogisticsAdapter();
        var target = new ItemStacksResourceHandler(1);
        var blocked = new ItemStacksResourceHandler(0);
        String fingerprint = LogisticsComponentData.fingerprint(ItemResource.of(Items.DIAMOND).getComponentsPatch());
        var id = BuiltInRegistries.ITEM.getKey(Items.DIAMOND);
        helper.startSequence().thenWaitUntil(() -> helper.assertTrue(
                network(helper).topologyReady(origin.getBlockPos(), LogisticsResourceType.ITEM), "Routes ready")).thenExecute(() -> {
            helper.assertValueEqual(network(helper).transferFromNetwork(origin.getBlockPos(), adapter.id(), id, fingerprint, target, 1), 1L, "First withdrawal");
            helper.assertValueEqual(network(helper).transferFromNetwork(origin.getBlockPos(), adapter.id(), id, fingerprint, blocked, 1), 0L, "Blocked withdrawal");
            helper.assertValueEqual(network(helper).transferFromNetwork(origin.getBlockPos(), adapter.id(), id, fingerprint, target, 1), 1L, "Next withdrawal");
            helper.assertValueEqual(count(left, Items.DIAMOND), 7, "Failure does not skip left");
            helper.assertValueEqual(count(right, Items.DIAMOND), 7, "Failure does not skip right");
        }).thenSucceed();
    }

    private static void remainders(GameTestHelper helper) {
        var source = barrel(helper, SOURCE, Items.MILK_BUCKET, 1);
        source.setItem(1, new ItemStack(Items.MILK_BUCKET));
        source.setItem(2, new ItemStack(Items.MILK_BUCKET));
        source.setItem(3, new ItemStack(Items.SUGAR, 2));
        source.setItem(4, new ItemStack(Items.EGG));
        source.setItem(5, new ItemStack(Items.WHEAT, 3));
        var north = barrel(helper, ORIGIN.north(), Items.BUCKET, 0);
        var south = barrel(helper, ORIGIN.south(), Items.BUCKET, 0);
        var origin = pipe(helper, ORIGIN, PipeKind.ITEM);
        origin.setMode(Direction.WEST, PipeSideMode.EXTRACT);
        var sourceSettings = origin.face(Direction.WEST).copy();
        origin.setUpgradeTier(Direction.WEST, PipeUpgradeTier.IMPROVED);
        sourceSettings.setProfile(LogisticsResourceType.ITEM, new PipeResourceProfile(true, "", List.of(
                new PipeFilterRule(PipeFilterRule.Action.DENY, PipeFilterRule.MatchKind.ID,
                        "minecraft:bucket", PipeFilterRule.ComponentMatch.IGNORE, "", ""))));
        origin.applyFaceConfiguration(Direction.WEST, sourceSettings);
        for (Direction face : List.of(Direction.NORTH, Direction.SOUTH)) {
            origin.setUpgradeTier(face, PipeUpgradeTier.IMPROVED);
            var settings = origin.face(face).copy();
            var denied = List.of(Items.MILK_BUCKET, Items.SUGAR, Items.EGG, Items.WHEAT).stream()
                    .map(item -> new PipeFilterRule(PipeFilterRule.Action.DENY, PipeFilterRule.MatchKind.ID,
                            BuiltInRegistries.ITEM.getKey(item).toString(), PipeFilterRule.ComponentMatch.IGNORE, "", "")).toList();
            settings.setMode(PipeSideMode.INSERT, true);
            settings.setProfile(LogisticsResourceType.ITEM, new PipeResourceProfile(true, "", denied));
            origin.applyFaceConfiguration(face, settings);
        }
        var player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        var grid = List.of(new ItemStack(Items.MILK_BUCKET), new ItemStack(Items.MILK_BUCKET),
                new ItemStack(Items.MILK_BUCKET), new ItemStack(Items.SUGAR), new ItemStack(Items.EGG),
                new ItemStack(Items.SUGAR), new ItemStack(Items.WHEAT), new ItemStack(Items.WHEAT), new ItemStack(Items.WHEAT));
        helper.startSequence().thenWaitUntil(() -> helper.assertTrue(
                network(helper).topologyReady(origin.getBlockPos(), LogisticsResourceType.ITEM), "Routes ready")).thenExecute(() -> {
            for (int slot = 0; slot < 36; slot++) {
                player.getInventory().setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
            }
            helper.assertFalse(NetworkCraftingService.craft(helper.getLevel(), origin.getBlockPos(), player, grid, 1), "Output full cancels entire network mutation");
            helper.assertValueEqual(count(north, Items.BUCKET) + count(south, Items.BUCKET), 0, "Network remainders rolled back");
            helper.assertValueEqual(count(source, Items.MILK_BUCKET), 3, "Network inputs restored");
            player.getInventory().setItem(0, ItemStack.EMPTY);
            helper.assertTrue(NetworkCraftingService.craft(helper.getLevel(), origin.getBlockPos(), player, grid, 1), "One output slot suffices with network remainders");
            helper.assertValueEqual(count(north, Items.BUCKET) + count(south, Items.BUCKET), 3, "All buckets return to network");
            helper.assertTrue(Math.abs(count(north, Items.BUCKET) - count(south, Items.BUCKET)) == 1, "Remainders alternate equivalent destinations");
            helper.assertValueEqual(player.getInventory().countItem(Items.BUCKET), 0, "Network precedes player for remainders");
            helper.assertValueEqual(player.getInventory().countItem(Items.CAKE), 1, "Output delivered once");
        }).thenSucceed();
    }
}
