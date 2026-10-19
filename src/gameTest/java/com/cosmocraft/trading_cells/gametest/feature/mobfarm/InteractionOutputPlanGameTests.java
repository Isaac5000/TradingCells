package com.cosmocraft.trading_cells.gametest.feature.mobfarm;

import com.cosmocraft.trading_cells.feature.mobfarm.adapters.interaction.InteractionOutputPlan;
import com.cosmocraft.trading_cells.gametest.shared.GameTestCase;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class InteractionOutputPlanGameTests {
    private static volatile Object sink;
    private InteractionOutputPlanGameTests() { }

    public static List<GameTestCase> tests() {
        return List.of(new GameTestCase("interaction_output_plan_equivalence", 80, InteractionOutputPlanGameTests::equivalence));
    }

    private static void equivalence(GameTestHelper h) {
        var random = new Random(104729);
        for (int sample = 0; sample < 500; sample++) {
            List<ItemStack> existing = new ArrayList<>(), products = new ArrayList<>();
            for (int slot = 0; slot < 27; slot++) { existing.add(stack(random.nextInt(6), 1 + random.nextInt(64))); }
            for (int product = 0; product < sample % 9; product++) { products.add(stack(random.nextInt(6), 1 + random.nextInt(128))); }
            compare(h, existing, products);
        }
        for (int variant = 0; variant < 4; variant++) {
            var existing = fixture(variant);
            var products = List.of(stack(1, 5));
            compare(h, existing, products);
            if (Boolean.getBoolean("trading_cells.benchmarkOutputPlan")) { benchmark(existing, products, variant); }
        }
        var shared = stack(1, 5);
        compare(h, List.of(shared, shared), List.of());
        compare(h, List.of(shared), List.of(shared));
        compare(h, List.of(), List.of(stack(1, 1)));
        compare(h, List.of(ItemStack.EMPTY), List.of(stack(1, 64), stack(2, 1)));
        h.succeed();
    }

    private static ItemStack stack(int variant, int count) {
        if (variant == 0) { return ItemStack.EMPTY; }
        ItemStack result = new ItemStack(switch (variant) {
            case 1, 2 -> Items.HONEYCOMB;
            case 3 -> Items.HONEY_BOTTLE;
            case 4 -> Items.SHEARS;
            default -> Items.STONE;
        }, count);
        if (variant == 2) { result.set(DataComponents.CUSTOM_NAME, Component.literal("Distinct component")); }
        if (variant == 4) { result.setDamageValue(count); }
        return result;
    }

    private static List<ItemStack> fixture(int variant) {
        List<ItemStack> result = new ArrayList<>();
        for (int slot = 0; slot < 27; slot++) {
            result.add(switch (variant) {
                case 0 -> ItemStack.EMPTY;
                case 1 -> stack(5, 64);
                case 2 -> stack(slot == 0 ? 1 : 5, slot == 0 ? 60 : 64);
                default -> stack(1, 60);
            });
        }
        return result;
    }

    private static void compare(GameTestHelper h, List<ItemStack> existing, List<ItemStack> products) {
        var original = existing.stream().map(ItemStack::copy).toList();
        var originalProducts = products.stream().map(ItemStack::copy).toList();
        var expected = baseline(existing, products);
        var actual = InteractionOutputPlan.insert(existing, products);
        h.assertTrue((expected == null) == (actual == null), "Same complete-fit decision");
        if (actual != null) {
            for (int slot = 0; slot < actual.size(); slot++) {
                h.assertTrue(ItemStack.matches(expected.get(slot), actual.get(slot)), "Same count, components and slot order");
                if (!actual.get(slot).isEmpty()) {
                    for (var input : existing) { h.assertTrue(actual.get(slot) != input, "Result owns its stacks"); }
                    for (var product : products) { h.assertTrue(actual.get(slot) != product, "Result never shares product stacks"); }
                    for (int other = 0; other < slot; other++) {
                        h.assertTrue(actual.get(slot) != actual.get(other), "Result slots remain independent");
                    }
                    actual.get(slot).setCount(1);
                }
            }
        }
        for (int slot = 0; slot < existing.size(); slot++) {
            h.assertTrue(ItemStack.matches(existing.get(slot), original.get(slot)), "Live inventory unchanged on success or failure");
        }
        for (int slot = 0; slot < products.size(); slot++) {
            h.assertTrue(ItemStack.matches(products.get(slot), originalProducts.get(slot)), "Products unchanged");
        }
    }

    // Original algorithm retained only as a differential-test and timing reference.
    private static List<ItemStack> baseline(List<ItemStack> existing, List<ItemStack> products) {
        List<ItemStack> next = new ArrayList<>(existing.stream().map(ItemStack::copy).toList());
        for (ItemStack product : products) {
            if (product.isEmpty()) { continue; }
            int remaining = product.getCount();
            for (int pass = 0; pass < 2; pass++) {
                for (int slot = 0; slot < next.size() && remaining > 0; slot++) {
                    ItemStack current = next.get(slot);
                    if (pass == 0 ? current.isEmpty() || !ItemStack.isSameItemSameComponents(current, product) : !current.isEmpty()) { continue; }
                    int amount = Math.min(remaining, Math.min(64, product.getMaxStackSize()) - current.getCount());
                    if (amount <= 0) { continue; }
                    next.set(slot, product.copyWithCount(current.getCount() + amount));
                    remaining -= amount;
                }
            }
            if (remaining != 0) { return null; }
        }
        return next;
    }

    private static void benchmark(List<ItemStack> existing, List<ItemStack> products, int variant) {
        Runnable oldPlan = () -> sink = baseline(existing, products);
        Runnable newPlan = () -> sink = InteractionOutputPlan.insert(existing, products);
        for (int warmup = 0; warmup < 12; warmup++) { measure(oldPlan); measure(newPlan); }
        long[] before = new long[31], after = new long[31];
        for (int round = 0; round < before.length; round++) {
            if ((round & 1) == 0) { before[round] = measure(oldPlan); after[round] = measure(newPlan); }
            else { after[round] = measure(newPlan); before[round] = measure(oldPlan); }
        }
        Arrays.sort(before); Arrays.sort(after);
        System.out.printf(Locale.ROOT, "OUTPUT_PLAN variant=%d calls=4096 baseline_ms=%.3f candidate_ms=%.3f improvement=%.2f%%%n",
                variant, before[15] / 1_000_000.0, after[15] / 1_000_000.0, 100.0 * (before[15] - after[15]) / before[15]);
    }

    private static long measure(Runnable action) {
        long start = System.nanoTime();
        for (int call = 0; call < 4096; call++) { action.run(); }
        return System.nanoTime() - start;
    }
}
