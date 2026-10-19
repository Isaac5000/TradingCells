package com.cosmocraft.trading_cells.gametest.feature.logistics;

import com.cosmocraft.trading_cells.feature.logistics.adapters.api.LogisticsListingEntry;
import com.cosmocraft.trading_cells.feature.logistics.adapters.api.LogisticsResourceSnapshot;
import com.cosmocraft.trading_cells.feature.logistics.adapters.neoforge.ItemLogisticsAdapter;
import com.cosmocraft.trading_cells.feature.logistics.adapters.neoforge.LogisticsComponentData;
import com.cosmocraft.trading_cells.gametest.shared.GameTestCase;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/** Compares observable listing results; optional local timing is not a CI acceptance assertion. */
public final class LogisticsListingGameTests {
    private static volatile Object sink;

    private LogisticsListingGameTests() { }

    public static List<GameTestCase> tests() {
        return List.of(new GameTestCase("logistics_listing_metadata_equivalence", 100, LogisticsListingGameTests::listing));
    }

    private static void listing(GameTestHelper h) {
        var adapter = new ItemLogisticsAdapter();
        BlockPos pos = h.absolutePos(new BlockPos(2, 3, 2));
        h.assertTrue(h.getLevel().getBlockEntity(pos) == null, "Generic external-handler fixture has no block entity");
        for (int size : new int[] {27, 216, 4096}) {
            for (int variant = 0; variant < 3; variant++) {
                var handler = fixture(size, variant);
                var expected = baseline(handler, h, pos);
                var actual = adapter.listingContents(handler, h.getLevel(), pos, Direction.UP);
                compare(h, expected, actual);
                if (actual.size() > 1) {
                    actual.getFirst().snapshot().icon().setCount(42);
                    h.assertValueEqual(actual.get(1).snapshot().icon().getCount(), 1, "Listing icons are independent copies");
                }
                try (var tx = Transaction.openRoot()) {
                    handler.extract(0, handler.getResource(0), Integer.MAX_VALUE, tx);
                    tx.commit();
                }
                compare(h, baseline(handler, h, pos), adapter.listingContents(handler, h.getLevel(), pos, Direction.UP));
                if (Boolean.getBoolean("trading_cells.benchmarkListings")) {
                    benchmark(h, adapter, pos, size, variant);
                }
            }
        }
        var variants = new ItemStacksResourceHandler(3);
        var named = new ItemStack(Items.DIAMOND);
        named.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Named resource"));
        try (var tx = Transaction.openRoot()) {
            variants.insert(0, ItemResource.of(Items.DIAMOND), 4, tx);
            variants.insert(1, ItemResource.of(named), 7, tx);
            variants.insert(2, ItemResource.of(named), 11, tx);
            tx.commit();
        }
        compare(h, baseline(variants, h, pos), adapter.listingContents(variants, h.getLevel(), pos, Direction.UP));
        h.succeed();
    }

    private static ItemStacksResourceHandler fixture(int size, int variant) {
        var handler = new ItemStacksResourceHandler(size);
        var repeated = resource(variant == 0 ? -1 : 0);
        try (var tx = Transaction.openRoot()) {
            for (int slot = 0; slot < size; slot++) {
                if (slot % 17 == 16) { continue; }
                var resource = variant == 2 ? resource(slot) : repeated;
                handler.insert(slot, resource, slot % 64 + 1, tx);
            }
            tx.commit();
        }
        return handler;
    }

    private static ItemResource resource(int variant) {
        var stack = new ItemStack(Items.DIAMOND);
        if (variant >= 0) {
            var tag = new CompoundTag();
            tag.putInt("Variant", variant);
            tag.putString("Payload", "Entity inventory data ".repeat(128));
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
        return ItemResource.of(stack);
    }

    // Previous implementation, including world lookups and sided storage identity mapping.
    private static List<LogisticsListingEntry<ItemResource>> baseline(ItemStacksResourceHandler handler,
            GameTestHelper h, BlockPos pos) {
        var result = new ArrayList<LogisticsListingEntry<ItemResource>>();
        var level = h.getLevel();
        var entity = level.getBlockEntity(pos);
        var state = level.getBlockState(pos);
        net.neoforged.neoforge.transfer.ResourceHandler<ItemResource> view = handler;
        for (int slot = 0; slot < handler.size(); slot++) {
            var resource = handler.getResource(slot);
            long amount = handler.getAmountAsLong(slot);
            if (resource.isEmpty() || amount <= 0) { continue; }
            Object inventory = handler;
            int physicalSlot = slot;
            if (view instanceof com.cosmocraft.trading_cells.platform.neoforge.machine.PortableMachineItemHandler machine) {
                inventory = entity;
                physicalSlot = machine.containerSlot(slot);
            } else if (view instanceof net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper
                    && entity instanceof net.minecraft.world.WorldlyContainer container) {
                inventory = entity;
                physicalSlot = container.getSlotsForFace(Direction.UP)[slot];
            } else if ((view instanceof net.neoforged.neoforge.transfer.item.VanillaContainerWrapper
                    || view instanceof net.neoforged.neoforge.transfer.CombinedResourceHandler<?>
                    && state.getBlock() instanceof net.minecraft.world.level.block.ChestBlock)
                    && entity instanceof net.minecraft.world.Container container) {
                inventory = entity;
                if (state.getBlock() instanceof net.minecraft.world.level.block.ChestBlock
                        && handler.size() == 2 * container.getContainerSize()) {
                    var type = state.getValue(net.minecraft.world.level.block.ChestBlock.TYPE);
                    boolean first = type == net.minecraft.world.level.block.state.properties.ChestType.RIGHT;
                    boolean firstSlot = slot < container.getContainerSize();
                    if (first != firstSlot) {
                        var other = pos.relative(net.minecraft.world.level.block.ChestBlock.getConnectedDirection(state));
                        if (level.getChunkSource().getChunkNow(other.getX() >> 4, other.getZ() >> 4) != null) {
                            inventory = level.getBlockEntity(other);
                        }
                    }
                    physicalSlot = slot % container.getContainerSize();
                }
            }
            result.add(new LogisticsListingEntry<>(new LogisticsListingEntry.StorageKey(inventory == null ? handler : inventory, physicalSlot),
                    new LogisticsResourceSnapshot<>(resource, BuiltInRegistries.ITEM.getKey(resource.getItem()),
                            LogisticsComponentData.fingerprint(resource.getComponentsPatch()), resource.getHoverName(),
                            resource.toStack(), amount)));
        }
        return result;
    }

    private static void compare(GameTestHelper h, List<LogisticsListingEntry<ItemResource>> expected,
            List<LogisticsListingEntry<ItemResource>> actual) {
        h.assertValueEqual(actual.size(), expected.size(), "Same occupied slots");
        for (int index = 0; index < expected.size(); index++) {
            var a = actual.get(index);
            var e = expected.get(index);
            h.assertValueEqual(a.storageKey(), e.storageKey(), "Physical storage identity unchanged");
            h.assertValueEqual(a.snapshot().resource(), e.snapshot().resource(), "Components distinguish resource variants");
            h.assertValueEqual(a.snapshot().resourceId(), e.snapshot().resourceId(), "Registry ID unchanged");
            h.assertValueEqual(a.snapshot().componentFingerprint(), e.snapshot().componentFingerprint(), "Exact fingerprint, including SHA-256");
            h.assertValueEqual(a.snapshot().displayName(), e.snapshot().displayName(), "Display name unchanged");
            h.assertValueEqual(a.snapshot().amount(), e.snapshot().amount(), "Current slot quantity, not cached quantity");
            h.assertTrue(ItemStack.matches(a.snapshot().icon(), e.snapshot().icon()), "Same independent icon");
        }
    }

    private static void benchmark(GameTestHelper h, ItemLogisticsAdapter adapter, BlockPos pos, int size, int variant) {
        var handler = fixture(size, variant);
        int repeats = Math.max(2, 4096 / size);
        Runnable oldRead = () -> sink = baseline(handler, h, pos);
        Runnable newRead = () -> sink = adapter.listingContents(handler, h.getLevel(), pos, Direction.UP);
        for (int warmup = 0; warmup < 12; warmup++) { measure(oldRead, repeats); measure(newRead, repeats); }
        long[] oldTimes = new long[31], newTimes = new long[31];
        for (int round = 0; round < oldTimes.length; round++) {
            if ((round & 1) == 0) {
                oldTimes[round] = measure(oldRead, repeats);
                newTimes[round] = measure(newRead, repeats);
            } else {
                newTimes[round] = measure(newRead, repeats);
                oldTimes[round] = measure(oldRead, repeats);
            }
        }
        Arrays.sort(oldTimes); Arrays.sort(newTimes);
        int median = oldTimes.length / 2;
        System.out.printf(java.util.Locale.ROOT,
                "LISTING_PROBE slots=%d variant=%d repeats=%d median_ms=%.3f->%.3f improvement=%.2f%%%n",
                size, variant, repeats, oldTimes[median] / 1e6, newTimes[median] / 1e6,
                100.0 * (oldTimes[median] - newTimes[median]) / oldTimes[median]);
    }

    private static long measure(Runnable action, int repeats) {
        long start = System.nanoTime();
        for (int index = 0; index < repeats; index++) { action.run(); }
        return System.nanoTime() - start;
    }
}
