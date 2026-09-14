package com.cosmocraft.trading_cells.gametest.feature.logistics;

import com.cosmocraft.trading_cells.feature.logistics.adapters.input.PipeTargetSelectorItem;
import com.cosmocraft.trading_cells.feature.logistics.adapters.output.LogisticsRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.logistics.domain.model.PipeKind;
import com.cosmocraft.trading_cells.feature.logistics.domain.model.PipeRuleTarget;
import com.cosmocraft.trading_cells.feature.logistics.domain.model.PipeSideMode;
import com.cosmocraft.trading_cells.gametest.shared.GameTestCase;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import static com.cosmocraft.trading_cells.gametest.feature.logistics.LogisticsTestFixtures.*;

/** Test-only external block providers; no special-case support exists in production. */
@EventBusSubscriber(modid = "trading_cells_gametest")
public final class LogisticsCapabilityGameTests {
    private static final Map<BlockPos, Storage> STORES = new ConcurrentHashMap<>();

    private LogisticsCapabilityGameTests() {
    }

    @SubscribeEvent
    public static void capabilities(RegisterCapabilitiesEvent event) {
        event.registerBlock(Capabilities.Item.BLOCK, (level, pos, state, entity, side) ->
                STORES.containsKey(pos) ? STORES.get(pos).items : null, Blocks.GOLD_BLOCK);
        event.registerBlock(Capabilities.Fluid.BLOCK, (level, pos, state, entity, side) ->
                STORES.containsKey(pos) ? STORES.get(pos).fluids : null, Blocks.GOLD_BLOCK);
        event.registerBlock(Capabilities.Energy.BLOCK, (level, pos, state, entity, side) ->
                STORES.containsKey(pos) ? STORES.get(pos).energy : null, Blocks.GOLD_BLOCK);
        event.registerBlock(Capabilities.Fluid.BLOCK, (level, pos, state, entity, side) ->
                side == null && STORES.containsKey(pos) ? STORES.get(pos).fluids : null, Blocks.IRON_BLOCK);
        event.registerBlock(Capabilities.Energy.BLOCK, (level, pos, state, entity, side) ->
                side == Direction.NORTH && STORES.containsKey(pos) ? STORES.get(pos).energy : null, Blocks.DIAMOND_BLOCK);
    }

    public static List<GameTestCase> tests() {
        return List.of(
                new GameTestCase("logistics_energy_shared_across_distances", 100, helper -> sharedResource(helper, PipeKind.ENERGY)),
                new GameTestCase("logistics_fluid_shared_by_default", 100, helper -> sharedResource(helper, PipeKind.FLUID)),
                new GameTestCase("logistics_gas_shared_by_default", 100, helper -> sharedResource(helper, PipeKind.GAS)),
                new GameTestCase("logistics_universal_four_resources", 100, LogisticsCapabilityGameTests::universal),
                new GameTestCase("logistics_target_marker_resource_capabilities", 20, LogisticsCapabilityGameTests::resourceMarker),
                new GameTestCase("logistics_external_capability_invalidation", 250,
                        LogisticsCapabilityGameTests::invalidation));
    }

    private static void resourceMarker(GameTestHelper helper) {
        var player = connectedPlayer(helper, GameType.SURVIVAL);
        var stack = LogisticsRegistrationAdapter.TARGET_SELECTOR_ITEM.get().getDefaultInstance();
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var pos = helper.absolutePos(SOURCE);
        player.setPos(Vec3.atCenterOf(pos.south()));
        var storage = new Storage();
        STORES.put(pos, storage);
        var expected = new PipeRuleTarget(helper.getLevel().dimension().identifier().toString(), pos.getX(), pos.getY(), pos.getZ());
        var previous = new PipeRuleTarget(expected.dimension(), pos.getX() + 1, pos.getY(), pos.getZ());
        try {
            for (var block : List.of(Blocks.IRON_BLOCK, Blocks.DIAMOND_BLOCK)) {
                helper.setBlock(SOURCE, block);
                helper.assertTrue(helper.getLevel().getCapability(Capabilities.Item.BLOCK, pos, null) == null,
                        "External provider exposes no item inventory");
                PipeTargetSelectorItem.setTarget(stack, previous);
                var result = player.gameMode.useItemOn(player, helper.getLevel(), stack, InteractionHand.MAIN_HAND,
                        new BlockHitResult(Vec3.atCenterOf(pos), Direction.SOUTH, pos, false));
                helper.assertTrue(result.consumesAction(), "Non-item provider is selectable while empty");
                helper.assertValueEqual(PipeTargetSelectorItem.target(stack), expected,
                        "Finds unsided fluids and energy exposed only on another face");
                helper.assertValueEqual(stack.getCount(), 1, "External selection does not consume the marker");
            }
            STORES.remove(pos);
            helper.getLevel().invalidateCapabilities(pos);
            PipeTargetSelectorItem.setTarget(stack, previous);
            player.gameMode.useItemOn(player, helper.getLevel(), stack, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(pos), Direction.SOUTH, pos, false));
            helper.assertValueEqual(PipeTargetSelectorItem.target(stack), previous,
                    "A block without its capability does not overwrite the target");
        } finally {
            STORES.remove(pos);
            helper.getLevel().invalidateCapabilities(pos);
        }
        helper.succeed();
    }

    private static void universal(GameTestHelper helper) {
        var source = new Storage();
        var target = new Storage();
        STORES.put(helper.absolutePos(SOURCE), source);
        STORES.put(helper.absolutePos(TARGET), target);
        helper.setBlock(SOURCE, Blocks.GOLD_BLOCK);
        helper.setBlock(TARGET, Blocks.GOLD_BLOCK);
        try (var transaction = Transaction.openRoot()) {
            source.items.insert(ItemResource.of(Items.EMERALD), 8, transaction);
            source.fluids.insert(FluidResource.of(Fluids.WATER), 400, transaction);
            source.fluids.insert(FluidResource.of(Fluids.LAVA), 300, transaction);
            source.energy.insert(4096, transaction);
            transaction.commit();
        }
        var pipe = line(helper, PipeKind.UNIVERSAL);
        pipe.setMode(Direction.WEST, PipeSideMode.EXTRACT);
        helper.runAtTickTime(65, () -> {
            helper.assertValueEqual(source.items.getAmountAsLong(0), 0L, "Item source consumed");
            helper.assertValueEqual(target.items.getAmountAsLong(0), 8L, "Item channel conserved");
            helper.assertValueEqual(source.energy.getAmountAsLong(), 0L, "Energy source consumed");
            helper.assertValueEqual(target.energy.getAmountAsLong(), 4096L, "Energy channel conserved");
            long liquid = 0, gas = 0;
            for (int index = 0; index < target.fluids.size(); index++) {
                var resource = target.fluids.getResource(index);
                if (resource.getFluid() == Fluids.WATER) {
                    liquid += target.fluids.getAmountAsLong(index);
                } else if (resource.getFluid() == Fluids.LAVA) {
                    gas += target.fluids.getAmountAsLong(index);
                }
            }
            helper.assertValueEqual(liquid, 400L, "Fluid route preserved water");
            helper.assertValueEqual(gas, 300L, "Gas route preserved tagged resource without conversion");
            STORES.remove(helper.absolutePos(SOURCE));
            STORES.remove(helper.absolutePos(TARGET));
            helper.succeed();
        });
    }

    private static final class Storage {
        private final ItemStacksResourceHandler items = new ItemStacksResourceHandler(4);
        private final FluidStacksResourceHandler fluids = new FluidStacksResourceHandler(4, 4000);
        private final SimpleEnergyHandler energy = new SimpleEnergyHandler(10000);
    }

    private static void sharedResource(GameTestHelper helper, PipeKind kind) {
        var source = new Storage();
        STORES.put(helper.absolutePos(SOURCE), source);
        helper.setBlock(SOURCE, Blocks.GOLD_BLOCK);
        var positions = List.of(ORIGIN.north(), ORIGIN.south(), ORIGIN.east().north(), ORIGIN.east(2).south());
        var targets = positions.stream().map(pos -> {
            var storage = new Storage();
            STORES.put(helper.absolutePos(pos), storage);
            helper.setBlock(pos, Blocks.GOLD_BLOCK);
            return storage;
        }).toList();
        try (var transaction = Transaction.openRoot()) {
            if (kind == PipeKind.ENERGY) { source.energy.insert(8000, transaction); }
            else { source.fluids.insert(FluidResource.of(kind == PipeKind.FLUID ? Fluids.WATER : Fluids.LAVA), 8000, transaction); }
            transaction.commit();
        }
        var origin = line(helper, kind);
        origin.setMode(Direction.WEST, PipeSideMode.EXTRACT);
        helper.startSequence().thenWaitUntil(() -> helper.assertTrue(
                targets.stream().anyMatch(target -> amount(target, kind) > 0), "Resource starts moving"))
                .thenExecute(() -> {
                    long first = amount(targets.getFirst(), kind);
                    for (var target : targets) {
                        helper.assertTrue(amount(target, kind) > 0, "Every equal-priority consumer receives the first batch");
                        helper.assertTrue(Math.abs(amount(target, kind) - first) <= 1, "Distance cannot starve equal-priority consumers");
                    }
                    long total = amount(source, kind) + targets.stream().mapToLong(target -> amount(target, kind)).sum();
                    helper.assertValueEqual(total, 8000L, "Resource is conserved");
                    positions.forEach(pos -> STORES.remove(helper.absolutePos(pos)));
                    STORES.remove(helper.absolutePos(SOURCE));
                }).thenSucceed();
    }

    private static long amount(Storage storage, PipeKind kind) {
        if (kind == PipeKind.ENERGY) { return storage.energy.getAmountAsLong(); }
        long total = 0;
        for (int index = 0; index < storage.fluids.size(); index++) { total += storage.fluids.getAmountAsLong(index); }
        return total;
    }

    private static void invalidation(GameTestHelper helper) {
        var source = barrel(helper, SOURCE, Items.DIAMOND, 8);
        helper.setBlock(TARGET, Blocks.GOLD_BLOCK);
        var origin = line(helper, PipeKind.ITEM);
        origin.setMode(Direction.WEST, PipeSideMode.EXTRACT);
        var target = new Storage();
        helper.runAtTickTime(25, () -> {
            helper.assertValueEqual(count(source, Items.DIAMOND), 8, "Missing capability leaves source intact");
            STORES.put(helper.absolutePos(TARGET), target);
            helper.getLevel().invalidateCapabilities(helper.absolutePos(TARGET));
        });
        long[] removedAt = {-1};
        boolean[] restored = {false};
        helper.onEachTick(() -> {
            if (removedAt[0] < 0 && target.items.getAmountAsLong(0) == 8) {
                helper.assertValueEqual(count(source, Items.DIAMOND), 0, "Capability appears without block update");
                STORES.remove(helper.absolutePos(TARGET));
                helper.getLevel().invalidateCapabilities(helper.absolutePos(TARGET));
                source.setItem(0, new net.minecraft.world.item.ItemStack(Items.DIAMOND, 4));
                removedAt[0] = helper.getTick();
            } else if (removedAt[0] >= 0 && !restored[0] && helper.getTick() - removedAt[0] >= 35) {
                helper.assertValueEqual(count(source, Items.DIAMOND), 4, "Removed provider cannot receive resources");
                STORES.put(helper.absolutePos(TARGET), target);
                helper.getLevel().invalidateCapabilities(helper.absolutePos(TARGET));
                restored[0] = true;
            } else if (restored[0] && target.items.getAmountAsLong(0) == 12) {
                helper.assertValueEqual(count(source, Items.DIAMOND), 0, "Restored provider reconnects without loss");
                STORES.remove(helper.absolutePos(TARGET));
                helper.getLevel().invalidateCapabilities(helper.absolutePos(TARGET));
                helper.succeed();
            }
        });
    }
}
