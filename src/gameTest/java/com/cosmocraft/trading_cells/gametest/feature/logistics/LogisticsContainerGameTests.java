package com.cosmocraft.trading_cells.gametest.feature.logistics;

import com.cosmocraft.trading_cells.feature.logistics.adapters.input.NetworkTerminalBlockEntity;
import com.cosmocraft.trading_cells.feature.logistics.adapters.input.NetworkTerminalMenu;
import com.cosmocraft.trading_cells.feature.logistics.adapters.neoforge.FluidLogisticsAdapter;
import com.cosmocraft.trading_cells.feature.logistics.adapters.neoforge.LogisticsComponentData;
import com.cosmocraft.trading_cells.feature.logistics.adapters.output.LogisticsRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.logistics.domain.model.*;
import com.cosmocraft.trading_cells.gametest.shared.GameTestCase;
import com.cosmocraft.trading_cells.platform.neoforge.network.NetworkTerminalActionPayload;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import static com.cosmocraft.trading_cells.gametest.feature.logistics.LogisticsTestFixtures.*;

@EventBusSubscriber(modid = "trading_cells_gametest")
public final class LogisticsContainerGameTests {
    private static final Map<BlockPos, FluidStacksResourceHandler> TANKS = new ConcurrentHashMap<>();

    private LogisticsContainerGameTests() {
    }

    @SubscribeEvent
    public static void capabilities(RegisterCapabilitiesEvent event) {
        event.registerBlock(Capabilities.Fluid.BLOCK, (level, pos, state, entity, side) -> TANKS.get(pos), Blocks.DIAMOND_BLOCK);
    }

    public static List<GameTestCase> tests() {
        List<GameTestCase> tests = new ArrayList<>();
        for (LogisticsResourceType type : List.of(LogisticsResourceType.FLUID, LogisticsResourceType.GAS)) {
            tests.add(new GameTestCase("logistics_terminal_container_slot_" + type.serializedName(), 160,
                    helper -> containerSlot(helper, type)));
            for (InteractionHand hand : InteractionHand.values()) {
                tests.add(new GameTestCase("logistics_terminal_container_" + type.serializedName() + "_" + hand.ordinal(),
                        160, helper -> container(helper, type, hand)));
            }
        }
        return List.copyOf(tests);
    }

    private static void containerSlot(GameTestHelper helper, LogisticsResourceType type) {
        var tank = new FluidStacksResourceHandler(1, 4000);
        TANKS.put(helper.absolutePos(SOURCE), tank);
        helper.setBlock(SOURCE, Blocks.DIAMOND_BLOCK);
        helper.setBlock(ORIGIN.east(), LogisticsRegistrationAdapter.TERMINAL_BLOCK.get());
        var terminal = helper.getBlockEntity(ORIGIN.east(), NetworkTerminalBlockEntity.class);
        var origin = pipe(helper, ORIGIN, PipeKind.UNIVERSAL);
        origin.setMode(Direction.WEST, PipeSideMode.INSERT);
        var player = connectedPlayer(helper, GameType.SURVIVAL);
        player.setPos(Vec3.atCenterOf(terminal.getBlockPos()));
        var menu = new NetworkTerminalMenu(9, player.getInventory(), terminal, player, false);
        player.containerMenu = menu;
        var bucket = type == LogisticsResourceType.FLUID ? Items.WATER_BUCKET : Items.LAVA_BUCKET;
        var fluid = type == LogisticsResourceType.FLUID ? Fluids.WATER : Fluids.LAVA;
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND, 3));
        helper.startSequence().thenWaitUntil(() -> helper.assertTrue(
                network(helper).topologyReady(origin.getBlockPos(), type), "Slot route ready"))
                .thenExecute(() -> {
                    menu.handleAction(player, NetworkTerminalActionPayload.view(9, type, 0, ""));
                    menu.getSlot(36).set(new ItemStack(bucket));
                    menu.handleAction(player, action(menu, type, InteractionHand.MAIN_HAND,
                            NetworkTerminalActionPayload.Action.CONTAINER_DEPOSIT, Long.MAX_VALUE, fluid));
                    helper.assertValueEqual(tank.getAmountAsLong(0), 1000L, "Container slot deposits exact contents");
                    helper.assertTrue(menu.getSlot(36).getItem().is(Items.BUCKET), "Empty bucket stays in slot");
                    menu.handleAction(player, action(menu, type, InteractionHand.MAIN_HAND,
                            NetworkTerminalActionPayload.Action.CONTAINER_WITHDRAW, Long.MAX_VALUE, fluid));
                    helper.assertValueEqual(tank.getAmountAsLong(0), 0L, "Slot withdraws from visible INSERT storage");
                    helper.assertTrue(menu.getSlot(36).getItem().is(bucket), "Filled bucket stays in slot");
                    helper.assertValueEqual(player.getMainHandItem().getCount(), 3, "Container actions do not use hand");
                    menu.removed(player);
                    helper.assertValueEqual(player.getInventory().countItem(bucket), 1, "Closing returns container");
                    TANKS.remove(helper.absolutePos(SOURCE));
                    helper.getLevel().invalidateCapabilities(helper.absolutePos(SOURCE));
                }).thenSucceed();
    }

    private static void container(GameTestHelper helper, LogisticsResourceType type, InteractionHand hand) {
        var tank = new FluidStacksResourceHandler(1, 4000);
        TANKS.put(helper.absolutePos(SOURCE), tank);
        helper.setBlock(SOURCE, Blocks.DIAMOND_BLOCK);
        helper.setBlock(ORIGIN.east(), LogisticsRegistrationAdapter.TERMINAL_BLOCK.get());
        var terminal = helper.getBlockEntity(ORIGIN.east(), NetworkTerminalBlockEntity.class);
        var origin = pipe(helper, ORIGIN, PipeKind.UNIVERSAL);
        origin.setMode(Direction.WEST, PipeSideMode.INSERT);
        var player = connectedPlayer(helper, GameType.SURVIVAL);
        player.setPos(Vec3.atCenterOf(terminal.getBlockPos()));
        var menu = new NetworkTerminalMenu(7, player.getInventory(), terminal, player, false);
        player.containerMenu = menu;
        var filledBucket = type == LogisticsResourceType.FLUID ? Items.WATER_BUCKET : Items.LAVA_BUCKET;
        var fluid = type == LogisticsResourceType.FLUID ? Fluids.WATER : Fluids.LAVA;
        var adapter = new FluidLogisticsAdapter(type);
        InteractionHand other = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        player.setItemInHand(hand, new ItemStack(filledBucket));
        player.setItemInHand(other, new ItemStack(Items.DIAMOND, 3));
        var partialInsert = action(menu, type, hand, NetworkTerminalActionPayload.Action.INSERT_HAND, 999, fluid);
        var fullInsert = action(menu, type, hand, NetworkTerminalActionPayload.Action.INSERT_HAND, 1000, fluid);
        var partialWithdraw = action(menu, type, hand, NetworkTerminalActionPayload.Action.WITHDRAW, 999, fluid);
        var fullWithdraw = action(menu, type, hand, NetworkTerminalActionPayload.Action.WITHDRAW, 1000, fluid);
        helper.startSequence().thenWaitUntil(() -> helper.assertTrue(network(helper).topologyReady(origin.getBlockPos(), type), "Routes ready"))
                .thenExecute(() -> menu.handleAction(player, NetworkTerminalActionPayload.view(menu.containerId, type, 0, "")))
                .thenIdle(1).thenExecute(() -> {
                    menu.handleAction(player, partialInsert);
                    helper.assertValueEqual(tank.getAmountAsLong(0), 0L, "Indivisible bucket cannot partially deposit");
                    helper.assertTrue(player.getItemInHand(hand).is(filledBucket), "Partial deposit preserves full bucket");
                }).thenIdle(1).thenWaitUntil(() -> helper.assertTrue(
                        network(helper).topologyReady(origin.getBlockPos(), type), "Deposit route ready")).thenExecute(() -> {
                    var wrongMenu = new NetworkTerminalActionPayload(8, fullInsert.action(), type, 0, "", hand, null, null, "", 1000);
                    menu.handleAction(player, wrongMenu);
                    helper.assertValueEqual(tank.getAmountAsLong(0), 0L, "Stale menu cannot deposit");
                    menu.handleAction(player, fullInsert);
                    helper.assertValueEqual(tank.getAmountAsLong(0), 1000L, "Bucket contents inserted exactly");
                    helper.assertTrue(player.getItemInHand(hand).is(Items.BUCKET), "Empty bucket remains in interaction hand");
                    helper.assertTrue(adapter.contents(tank).getFirst().resource().getFluid() == fluid, "No resource conversion");
                    origin.setMode(Direction.WEST, PipeSideMode.EXTRACT);
                }).thenWaitUntil(() -> helper.assertTrue(network(helper).topologyReady(origin.getBlockPos(), type), "Extraction route ready"))
                .thenIdle(1).thenExecute(() -> {
                    menu.handleAction(player, partialWithdraw);
                    helper.assertValueEqual(tank.getAmountAsLong(0), 1000L, "Indivisible bucket cannot partially withdraw");
                    helper.assertTrue(player.getItemInHand(hand).is(Items.BUCKET), "Partial withdrawal preserves empty bucket");
                }).thenIdle(1).thenExecute(() -> {
                    player.setGameMode(GameType.SPECTATOR);
                    helper.assertTrue(player.isSpectator(), "Spectator fixture active");
                    menu.handleAction(player, fullWithdraw);
                    helper.assertValueEqual(tank.getAmountAsLong(0), 1000L, "Spectator cannot withdraw");
                    player.setGameMode(GameType.SURVIVAL);
                    player.containerMenu = menu;
                }).thenIdle(1).thenWaitUntil(() -> helper.assertTrue(
                        network(helper).topologyReady(origin.getBlockPos(), type), "Withdrawal route ready")).thenExecute(() -> {
                    helper.assertTrue(menu.stillValid(player), "Terminal still in reach");
                    helper.assertTrue(adapter.findCarried(player, hand) != null, "Selected hand exposes bucket capability");
                    menu.handleAction(player, fullWithdraw);
                    helper.assertValueEqual(tank.getAmountAsLong(0), 0L, "Full bucket extracted exactly");
                    helper.assertTrue(player.getItemInHand(hand).is(filledBucket), "Filled bucket returned to interaction hand");
                    helper.assertTrue(player.getItemInHand(other).is(Items.DIAMOND), "Other hand untouched");
                    helper.assertValueEqual(player.getItemInHand(other).getCount(), 3, "Other hand quantity untouched");
                    TANKS.remove(helper.absolutePos(SOURCE));
                    helper.getLevel().invalidateCapabilities(helper.absolutePos(SOURCE));
                }).thenSucceed();
    }

    private static NetworkTerminalActionPayload action(NetworkTerminalMenu menu, LogisticsResourceType type,
            InteractionHand hand, NetworkTerminalActionPayload.Action action, long amount, net.minecraft.world.level.material.Fluid fluid) {
        return new NetworkTerminalActionPayload(menu.containerId, action, type, 0, "", hand,
                new FluidLogisticsAdapter(type).id(), BuiltInRegistries.FLUID.getKey(fluid),
                LogisticsComponentData.fingerprint(FluidResource.of(fluid).getComponentsPatch()), amount);
    }
}
