package com.cosmocraft.trading_cells.gametest.feature.logistics;

import com.cosmocraft.trading_cells.feature.logistics.adapters.input.*;
import com.cosmocraft.trading_cells.feature.logistics.adapters.output.LogisticsRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.logistics.domain.model.*;
import com.cosmocraft.trading_cells.gametest.shared.GameTestCase;
import com.cosmocraft.trading_cells.platform.neoforge.network.NetworkTerminalActionPayload;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import static com.cosmocraft.trading_cells.gametest.feature.logistics.LogisticsTestFixtures.*;

public final class LogisticsMenuGameTests {
    private LogisticsMenuGameTests() { }

    public static List<GameTestCase> tests() {
        return List.of(new GameTestCase("logistics_terminal_cursor_and_crafting", 160, LogisticsMenuGameTests::cursor),
                new GameTestCase("logistics_terminal_pending_route", 160, LogisticsMenuGameTests::pendingRoute),
                new GameTestCase("logistics_copy_partial_invalid_fields", 20, LogisticsMenuGameTests::clipboard),
                new GameTestCase("logistics_horizontal_farm_extraction", 100, LogisticsMenuGameTests::horizontalFarm));
    }

    private static void pendingRoute(GameTestHelper helper) {
        var storage = barrel(helper, SOURCE, Items.OAK_LOG, 0);
        helper.setBlock(ORIGIN.east(), LogisticsRegistrationAdapter.CRAFTING_TERMINAL_BLOCK.get());
        var terminal = helper.getBlockEntity(ORIGIN.east(), NetworkTerminalBlockEntity.class);
        var pipe = pipe(helper, ORIGIN, PipeKind.ITEM);
        pipe.setMode(Direction.WEST, PipeSideMode.INSERT);
        var player = connectedPlayer(helper, GameType.SURVIVAL);
        player.setPos(Vec3.atCenterOf(terminal.getBlockPos()));
        var menu = new NetworkTerminalMenu(14, player.getInventory(), terminal, player, true);
        player.containerMenu = menu;
        menu.setCarried(new ItemStack(Items.OAK_LOG, 64));
        menu.handleAction(player, action(menu, NetworkTerminalActionPayload.Action.CURSOR_DEPOSIT, 64));
        helper.assertValueEqual(menu.getCarried().getCount(), 64, "Unready route leaves cursor untouched");
        helper.startSequence().thenWaitUntil(() -> {
            menu.broadcastChanges();
            helper.assertTrue(menu.getCarried().isEmpty(), "Queued deposit resumes after rebuilding route");
        }).thenExecute(() -> {
            helper.assertValueEqual(count(storage, Items.OAK_LOG), 64, "Queued deposit conserves whole stack");
            pipe.setPriority(Direction.WEST, 2);
            menu.setCarried(new ItemStack(Items.IRON_INGOT, 5));
            menu.handleAction(player, action(menu, NetworkTerminalActionPayload.Action.CURSOR_DEPOSIT, 5));
            menu.setCarried(new ItemStack(Items.DIAMOND, 3));
        }).thenWaitUntil(() -> {
            menu.broadcastChanges();
            helper.assertTrue(network(helper).topologyReady(pipe.getBlockPos(), LogisticsResourceType.ITEM), "Second rebuild ready");
        }).thenExecute(() -> {
            helper.assertValueEqual(menu.getCarried().getCount(), 3, "Changing cursor cancels pending transfer");
            helper.assertValueEqual(count(storage, Items.DIAMOND), 0, "Different cursor resource never deposited");
        }).thenSucceed();
    }

    private static void cursor(GameTestHelper helper) {
        var source = barrel(helper, SOURCE, Items.OAK_LOG, 8);
        helper.setBlock(ORIGIN.east(), LogisticsRegistrationAdapter.CRAFTING_TERMINAL_BLOCK.get());
        var terminal = helper.getBlockEntity(ORIGIN.east(), NetworkTerminalBlockEntity.class);
        var pipe = pipe(helper, ORIGIN, PipeKind.ITEM);
        pipe.setMode(Direction.WEST, PipeSideMode.INSERT);
        var player = connectedPlayer(helper, GameType.SURVIVAL);
        player.setPos(Vec3.atCenterOf(terminal.getBlockPos()));
        var menu = new NetworkTerminalMenu(13, player.getInventory(), terminal, player, true);
        player.containerMenu = menu;
        helper.startSequence().thenWaitUntil(() -> helper.assertTrue(
                network(helper).topologyReady(pipe.getBlockPos(), LogisticsResourceType.ITEM), "Routes ready"))
                .thenExecute(() -> {
                    menu.handleAction(player, NetworkTerminalActionPayload.view(13, LogisticsResourceType.ITEM, 0, ""));
                    var entry = menu.entries().getFirst();
                    menu.handleAction(player, new NetworkTerminalActionPayload(13, NetworkTerminalActionPayload.Action.CURSOR_WITHDRAW,
                            LogisticsResourceType.ITEM, 0, "", InteractionHand.MAIN_HAND, entry.adapterId(), entry.resourceId(), entry.componentFingerprint(), 1));
                    helper.assertTrue(menu.getCarried().is(Items.OAK_LOG), "Network pickup uses cursor from INSERT storage");
                    helper.assertValueEqual(count(source, Items.OAK_LOG), 7, "Pickup consumes exactly one log");
                    menu.handleAction(player, action(menu, NetworkTerminalActionPayload.Action.GHOST, 1));
                    helper.assertTrue(menu.craftingResult().is(Items.OAK_PLANKS), "Ghost grid previews real recipe");
                    menu.handleAction(player, action(menu, NetworkTerminalActionPayload.Action.CURSOR_DEPOSIT, 1));
                    helper.assertTrue(menu.getCarried().isEmpty(), "Deposit clears only transferred cursor items");
                    menu.handleAction(player, action(menu, NetworkTerminalActionPayload.Action.CRAFT_CURSOR, 1));
                    helper.assertTrue(menu.getCarried().is(Items.OAK_PLANKS), "Recipe output goes to cursor");
                    helper.assertValueEqual(menu.getCarried().getCount(), 4, "Exact recipe output");
                    helper.assertValueEqual(count(source, Items.OAK_LOG), 7, "Crafting consumes INSERT storage shown by terminal");
                }).thenIdle(1).thenWaitUntil(() -> helper.assertTrue(
                        network(helper).topologyReady(pipe.getBlockPos(), LogisticsResourceType.ITEM), "Routes ready after neighboring test updates"))
                .thenExecute(() -> {
                    menu.handleAction(player, action(menu, NetworkTerminalActionPayload.Action.CRAFT_STACK, 64));
                    helper.assertValueEqual(count(source, Items.OAK_LOG), 0, "Bulk crafting stops when ingredients run out");
                    helper.assertValueEqual(player.getInventory().countItem(Items.OAK_PLANKS), 28, "Shift crafts available recipes into inventory");
                    menu.removed(player);
                    helper.assertValueEqual(player.getInventory().countItem(Items.OAK_PLANKS), 32, "Closing returns cursor without loss");
                }).thenSucceed();
    }

    private static NetworkTerminalActionPayload action(NetworkTerminalMenu menu, NetworkTerminalActionPayload.Action action, long amount) {
        return new NetworkTerminalActionPayload(menu.containerId, action, LogisticsResourceType.ITEM,
                0, "", InteractionHand.MAIN_HAND, null, null, "", amount);
    }

    private static void clipboard(GameTestHelper helper) {
        var original = new PipeFaceConfiguration();
        original.setPriority(35);
        var pasted = PipeConfigurationClipboard.paste("""
                {"schema_version":1,"priority":2147483648,"item":{"enabled":false,"channel":"IRON",
                "mode":"BLACKLIST","rules":[{"kind":"ID","id":"minecraft:iron_ingot"},
                {"kind":"ID","id":"missing:unknown"},{"kind":"TAG","id":"missing:tag"}]}}
                """, original);
        helper.assertValueEqual(pasted.priority(), 35, "Overflow leaves destination priority unchanged");
        var profile = pasted.profile(LogisticsResourceType.ITEM);
        helper.assertFalse(profile.enabled(), "Per-resource activation copied");
        helper.assertValueEqual(profile.channel(), "iron", "Channel normalized");
        helper.assertValueEqual(profile.filters().size(), 1, "Unknown IDs and tags ignored individually");
        helper.assertFalse(profile.allows(rule -> true), "Blacklist denies selected resource");
        helper.assertTrue(profile.allows(rule -> false), "Blacklist permits other resources");
        var roundTrip = PipeConfigurationClipboard.paste(PipeConfigurationClipboard.copy(pasted), original);
        helper.assertValueEqual(roundTrip.profile(LogisticsResourceType.ITEM), profile, "Clipboard roundtrip exact");
        var whitelist = new PipeResourceProfile(true, "", List.of(), PipeResourceProfile.FilterMode.WHITELIST);
        helper.assertFalse(whitelist.allows(rule -> false), "Empty whitelist accepts nothing");
        helper.assertTrue(new PipeResourceProfile(true, "", profile.filters(), PipeResourceProfile.FilterMode.OFF)
                .allows(rule -> true), "Disabled filters keep entries but allow resources");
        helper.succeed();
    }

    private static void horizontalFarm(GameTestHelper helper) {
        helper.setBlock(SOURCE, com.cosmocraft.trading_cells.feature.zombiefarm.adapters.output.ZombieFarmRegistrationAdapter.BLOCK.get());
        var farm = helper.getBlockEntity(SOURCE, com.cosmocraft.trading_cells.feature.zombiefarm.adapters.input.ZombieFarmBlockEntity.class);
        com.cosmocraft.trading_cells.gametest.shared.BlockEntityStateFixtures.fillIndexedSlots(helper, farm,
                "Slot", com.cosmocraft.trading_cells.feature.zombiefarm.adapters.input.ZombieFarmBlockEntity.FIRST_OUTPUT_SLOT,
                1, new ItemStack(Items.ROTTEN_FLESH, 8));
        helper.assertValueEqual(farm.getItem(2).getCount(), 8, "Fixture contains actual produced loot");
        var target = barrel(helper, TARGET, Items.ROTTEN_FLESH, 0);
        var origin = line(helper, PipeKind.ITEM);
        origin.setMode(Direction.WEST, PipeSideMode.EXTRACT);
        helper.startSequence().thenWaitUntil(() -> helper.assertValueEqual(count(target, Items.ROTTEN_FLESH), 8,
                "Horizontal pipe extracts farm loot through the side")).thenSucceed();
    }
}
