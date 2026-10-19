package com.cosmocraft.trading_cells.gametest.feature.mobfarm;

import com.cosmocraft.trading_cells.feature.mobfarm.adapters.input.EntityEssenceData;
import com.cosmocraft.trading_cells.feature.mobfarm.adapters.interaction.DisposableInteractionLevel;
import com.cosmocraft.trading_cells.feature.mobfarm.adapters.interaction.InteractionHarvestDispatcher;
import com.cosmocraft.trading_cells.feature.mobfarm.adapters.interaction.InteractionHarvestResult;
import com.cosmocraft.trading_cells.feature.mobfarm.adapters.interaction.InteractionIsolation;
import com.cosmocraft.trading_cells.feature.mobfarm.domain.model.InteractionSource;
import com.cosmocraft.trading_cells.gametest.shared.GameTestCase;
import java.util.List;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

public final class InteractionHarvestGameTests {
    private InteractionHarvestGameTests() { }
    public static List<GameTestCase> tests() {
        return List.of(new GameTestCase("interaction_harvest_vanilla", 80, InteractionHarvestGameTests::vanilla),
                new GameTestCase("interaction_harvest_isolation", 80, InteractionHarvestGameTests::isolation),
                new GameTestCase("interaction_storage_cleanup_failures", 80, InteractionHarvestGameTests::storageCleanup),
                new GameTestCase("interaction_attempt_cleanup", 80, InteractionHarvestGameTests::attemptCleanup),
                new GameTestCase("interaction_harvest_mooshrooms", 80, InteractionHarvestGameTests::mooshrooms),
                new GameTestCase("interaction_harvest_farmer_touch", 80, InteractionHarvestGameTests::farmerTouch),
                new GameTestCase("interaction_harvest_creaking", 80, InteractionHarvestGameTests::creaking),
                new GameTestCase("speed_upgrade_legacy_aliases", 80, InteractionHarvestGameTests::speedAliases));
    }
    private static void attemptCleanup(GameTestHelper h) {
        var source = h.getLevel();
        var rule = net.minecraft.world.level.gamerules.GameRules.MOB_DROPS;
        boolean original = source.getGameRules().get(rule);
        try (var level = DisposableInteractionLevel.create(source)) {
            for (int attempt = 0; attempt < 2; attempt++) {
                level.reset(source);
                var rules = level.getGameRules();
                h.assertTrue(rules != source.getGameRules(), "Attempt owns independent game rules");
                h.assertValueEqual(rules.get(rule), original, "Attempt observes current source rules");
                rules.set(rule, !original, null);
                var chunk = level.getChunk(0, 0, net.minecraft.world.level.chunk.status.ChunkStatus.FULL, true);
                level.placeTemporaryBlock(DisposableInteractionLevel.ORIGIN, Blocks.HONEY_BLOCK.defaultBlockState());
                var drop = new net.minecraft.world.entity.item.ItemEntity(level, 0, 64, 0, new ItemStack(Items.HONEYCOMB));
                try {
                    level.addFreshEntity(drop);
                    if (attempt == 1) { throw new UnsupportedOperationException("Injected interaction failure"); }
                } catch (UnsupportedOperationException expected) {
                    h.assertValueEqual(attempt, 1, "Only injected failure is expected");
                } finally {
                    level.finishAttempt();
                }
                h.assertTrue(drop.isRemoved(), "Captured drop never enters an entity manager");
                h.assertTrue(level.capturedDrops().isEmpty(), "Cleanup releases drops");
                h.assertTrue(level.getBlockState(DisposableInteractionLevel.ORIGIN).isAir(), "Cleanup releases blocks");
                h.assertValueEqual(source.getGameRules().get(rule), original, "Source rules remain unchanged");
                level.reset(source);
                h.assertTrue(level.getGameRules() != rules, "Mutated rules never survive into another attempt");
                h.assertValueEqual(level.getGameRules().get(rule), original, "Next attempt restores source value");
                h.assertTrue(level.getChunk(0, 0, net.minecraft.world.level.chunk.status.ChunkStatus.FULL, true) != chunk,
                        "Temporary chunks are not retained between attempts");
            }
            if (Boolean.getBoolean("trading_cells.benchmarkInteractionCleanup")) { benchmarkCleanup(level, source); }
            level.finishAttempt();
        } catch (java.io.IOException failure) { throw new java.io.UncheckedIOException(failure); }
        h.succeed();
    }
    private static void benchmarkCleanup(DisposableInteractionLevel level, net.minecraft.server.level.ServerLevel source) {
        Runnable baseline = () -> { level.reset(source); level.reset(source); };
        Runnable candidate = () -> { level.reset(source); level.finishAttempt(); };
        for (int warmup = 0; warmup < 12; warmup++) { measureCleanup(baseline); measureCleanup(candidate); }
        long[] before = new long[31], after = new long[31];
        for (int round = 0; round < before.length; round++) {
            if ((round & 1) == 0) {
                before[round] = measureCleanup(baseline); after[round] = measureCleanup(candidate);
            } else {
                after[round] = measureCleanup(candidate); before[round] = measureCleanup(baseline);
            }
        }
        java.util.Arrays.sort(before); java.util.Arrays.sort(after);
        System.out.printf(java.util.Locale.ROOT, "INTERACTION_CLEANUP attempts=4096 baseline_ms=%.3f candidate_ms=%.3f improvement=%.2f%%%n",
                before[15] / 1_000_000.0, after[15] / 1_000_000.0, 100.0 * (before[15] - after[15]) / before[15]);
    }
    private static long measureCleanup(Runnable action) {
        long start = System.nanoTime();
        for (int attempt = 0; attempt < 4096; attempt++) { action.run(); }
        return System.nanoTime() - start;
    }
    private static void storageCleanup(GameTestHelper h) {
        try {
            var cleanup = DisposableInteractionLevel.class.getDeclaredMethod("closeAndDelete",
                    java.io.Closeable.class, java.nio.file.Path.class);
            cleanup.setAccessible(true);
            var root = java.nio.file.Files.createTempDirectory("trading-cells-cleanup-test-");
            java.nio.file.Files.createFile(root.resolve("owned.tmp"));
            var closeFailure = new java.io.IOException("Injected close failure");
            java.io.Closeable failingStorage = () -> { throw closeFailure; };
            try {
                cleanup.invoke(null, failingStorage, root);
                throw new AssertionError("Expected close failure");
            } catch (java.lang.reflect.InvocationTargetException expected) {
                h.assertTrue(expected.getCause() == closeFailure, "Original close failure is preserved");
            }
            h.assertFalse(java.nio.file.Files.exists(root), "Close failure still deletes all owned files");
            try {
                cleanup.invoke(null, failingStorage, root);
                throw new AssertionError("Expected close and missing-directory failure");
            } catch (java.lang.reflect.InvocationTargetException expected) {
                h.assertTrue(expected.getCause() == closeFailure, "Cleanup never replaces the primary failure");
                h.assertTrue(closeFailure.getSuppressed().length == 1
                        && closeFailure.getSuppressed()[0] instanceof java.nio.file.NoSuchFileException,
                        "Deletion failure is retained as suppressed diagnostic");
            }
            var field = DisposableInteractionLevel.class.getDeclaredField("temporaryRoot");
            field.setAccessible(true);
            java.nio.file.Path actualRoot;
            try (var disposable = DisposableInteractionLevel.create(h.getLevel())) {
                actualRoot = (java.nio.file.Path) field.get(disposable);
                h.assertTrue(java.nio.file.Files.isDirectory(actualRoot), "Sandbox owns temporary storage");
            }
            h.assertFalse(java.nio.file.Files.exists(actualRoot), "Normal sandbox shutdown deletes temporary storage");
        } catch (ReflectiveOperationException | java.io.IOException failure) {
            throw new AssertionError("Temporary storage cleanup", failure);
        }
        h.succeed();
    }
    private static void speedAliases(GameTestHelper h) {
        for (String material : List.of("copper", "iron", "gold", "diamond", "netherite")) {
            var id = Identifier.fromNamespaceAndPath("trading_cells", "speed_" + material + "_upgrade");
            var legacy = Identifier.fromNamespaceAndPath("trading_cells", "mob_farm_speed_" + material + "_upgrade");
            var registry = net.minecraft.core.registries.BuiltInRegistries.ITEM;
            var item = registry.getValue(id);
            h.assertTrue(item != Items.AIR && registry.getValue(legacy) == item, "Old registry names resolve to general upgrades");
            var tag = new net.minecraft.nbt.CompoundTag(); tag.putString("id", legacy.toString()); tag.putInt("count", 1);
            var ops = h.getLevel().registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE);
            var restored = ItemStack.CODEC.parse(ops, tag).getOrThrow();
            h.assertTrue(restored.is(item), "Stored inventory migrates the legacy upgrade");
            var saved = ItemStack.CODEC.encodeStart(ops, restored).getOrThrow();
            h.assertValueEqual(((net.minecraft.nbt.CompoundTag) saved).getStringOr("id", ""), id.toString(), "Saves use the new ID");
        }
        h.succeed();
    }
    private static void creaking(GameTestHelper h) {
        var module = EntityEssenceData.moduleFor(h.getLevel(), Identifier.withDefaultNamespace("creaking"));
        var original = module.copy();
        var axe = new ItemStack(Items.IRON_AXE);
        for (int cycle = 0; cycle < 2; cycle++) {
            var result = InteractionHarvestDispatcher.harvest(h.getLevel(), module, axe, InteractionSource.TOOL);
            h.assertValueEqual(result.status(), InteractionHarvestResult.Status.READY, "Resin needs no real heart or delayed tick");
            int resin = result.products().stream().filter(stack -> stack.is(Items.RESIN_CLUMP)).mapToInt(ItemStack::getCount).sum();
            h.assertTrue(resin >= 2 && resin <= 3, "Vanilla resin pulse quantity");
            h.assertValueEqual(result.remainder().getDamageValue(), cycle + 1, "One axe wear per harvest");
            h.assertTrue(ItemStack.matches(module, original), "Stored creaking stays unchanged");
            axe = result.remainder();
        }
        var wrong = InteractionHarvestDispatcher.harvest(h.getLevel(), module, new ItemStack(Items.STICK), InteractionSource.TOOL);
        h.assertTrue(wrong.products().isEmpty(), "Resin requires axe ability");
        axe.setDamageValue(axe.getMaxDamage() - 1);
        var broken = InteractionHarvestDispatcher.harvest(h.getLevel(), module, axe, InteractionSource.TOOL);
        h.assertTrue(broken.remainder().isEmpty() && !broken.products().isEmpty(), "Final axe use produces resin and breaks");
        h.assertFalse(InteractionIsolation.active(), "Resin handler cleans isolation");
        h.succeed();
    }
    private static void vanilla(GameTestHelper h) {
        assertHarvest(h, "cow", Items.BUCKET, InteractionSource.ITEM, Items.MILK_BUCKET);
        assertHarvest(h, "mooshroom", Items.BOWL, InteractionSource.ITEM, Items.MUSHROOM_STEW);
        assertHarvest(h, "sheep", Items.SHEARS, InteractionSource.TOOL, Blocks.WOOL.white().asItem());
        assertHarvest(h, "armadillo", Items.BRUSH, InteractionSource.TOOL, Items.ARMADILLO_SCUTE);
        assertHarvest(h, "bee", Items.GLASS_BOTTLE, InteractionSource.ITEM, Items.HONEY_BOTTLE);
        assertHarvest(h, "bee", Items.SHEARS, InteractionSource.TOOL, Items.HONEYCOMB);
        assertHarvest(h, "ender_dragon", Items.GLASS_BOTTLE, InteractionSource.ITEM, Items.DRAGON_BREATH);
        var dragon = EntityEssenceData.moduleFor(h.getLevel(), Identifier.withDefaultNamespace("ender_dragon"));
        var bottles = new ItemStack(Items.GLASS_BOTTLE, 64);
        var breath = InteractionHarvestDispatcher.harvest(h.getLevel(), dragon, bottles, InteractionSource.ITEM);
        h.assertValueEqual(breath.products().stream().filter(s -> s.is(Items.DRAGON_BREATH)).mapToInt(ItemStack::getCount).sum(), 1,
                "One bottle produces exactly one breath before farm commit");
        h.assertValueEqual(bottles.getCount(), 64, "Harvest preparation never consumes real bottles");
        h.assertTrue(breath.remainder().isEmpty(), "The transformed bottle is a product, not a reusable input");
        var module = EntityEssenceData.moduleFor(h.getLevel(), Identifier.withDefaultNamespace("cow"));
        var food = InteractionHarvestDispatcher.harvest(h.getLevel(), module, new ItemStack(Items.WHEAT), InteractionSource.ITEM);
        h.assertTrue(food.products().isEmpty(), "Feeding is not resource production");
        h.assertFalse(InteractionIsolation.active(), "Scope cleaned after attempts");
        h.succeed();
    }
    private static void assertHarvest(GameTestHelper h, String type, Item input, InteractionSource channel, Item expected) {
        var module = EntityEssenceData.moduleFor(h.getLevel(), Identifier.withDefaultNamespace(type));
        var original = module.copy();
        var result = InteractionHarvestDispatcher.harvest(h.getLevel(), module, new ItemStack(input), channel);
        h.assertValueEqual(result.status(), InteractionHarvestResult.Status.READY, type + " interaction status");
        h.assertTrue(result.products().stream().anyMatch(stack -> stack.is(expected)), type + " produces real resource");
        h.assertTrue(ItemStack.matches(module, original), "Saved entity remains untouched");
        if (channel == InteractionSource.TOOL) { h.assertTrue(result.remainder().getDamageValue() > 0, "Real tool wear"); }
    }
    private static void mooshrooms(GameTestHelper h) {
        for (String variant : List.of("red", "brown")) {
            var module = EntityEssenceData.moduleFor(h.getLevel(), Identifier.withDefaultNamespace("mooshroom"));
            var root = module.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA).copyTag();
            var essence = root.getCompoundOrEmpty("TradingCellsEssence");
            var state = essence.getCompoundOrEmpty("State"); state.putString("Type", variant);
            essence.put("State", state); root.put("TradingCellsEssence", essence);
            module.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(root));
            var original = module.copy();
            var tool = new ItemStack(Items.SHEARS);
            tool.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Reusable shears"));
            for (int cycle = 0; cycle < 2; cycle++) {
                var result = InteractionHarvestDispatcher.harvest(h.getLevel(), module, tool, InteractionSource.TOOL);
                h.assertValueEqual(result.status(), InteractionHarvestResult.Status.READY, variant + " renewable shearing");
                var expected = variant.equals("red") ? Items.RED_MUSHROOM : Items.BROWN_MUSHROOM;
                h.assertValueEqual(result.products().stream().filter(stack -> stack.is(expected)).mapToInt(ItemStack::getCount).sum(), 5, "Vanilla quantity and variant");
                h.assertValueEqual(result.remainder().getDamageValue(), cycle + 1, "Only one tool wear per operation");
                h.assertTrue(ItemStack.matches(module, original), "Mushrooms and entity data preserved");
                tool = result.remainder();
            }
            state.putInt("Age", -24000); essence.put("State", state); root.put("TradingCellsEssence", essence);
            module.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(root));
            var baby = InteractionHarvestDispatcher.harvest(h.getLevel(), module, tool, InteractionSource.TOOL);
            h.assertTrue(baby.products().isEmpty(), "Baby mooshrooms are not sheared");
        }
        h.succeed();
    }
    private static void farmerTouch(GameTestHelper h) {
        var enchantment = h.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                .getOrThrow(com.cosmocraft.trading_cells.platform.neoforge.machine.MachineToolEnchantments.FARMER_TOUCH);
        var types = List.of("minecraft:sheep", "minecraft:bee", "minecraft:mooshroom", "minecraft:armadillo",
                "trading_cells_gametest:resource_creature", "minecraft:creaking");
        var tools = List.of(Items.SHEARS, Items.SHEARS, Items.SHEARS, Items.BRUSH, InteractionTestContent.tool, Items.IRON_AXE);
        for (int index = 0; index < types.size(); index++) {
            var tool = new ItemStack(tools.get(index));
            h.assertTrue(tool.supportsEnchantment(enchantment), "Vanilla and tagged mod tools accept Farmer Touch");
            tool.enchant(enchantment, 1);
            tool.setDamageValue(tool.getMaxDamage() - 1);
            tool.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Protected tool"));
            var original = tool.copy();
            var module = EntityEssenceData.moduleFor(h.getLevel(), Identifier.parse(types.get(index)));
            for (int cycle = 0; cycle < 3; cycle++) {
                var result = InteractionHarvestDispatcher.harvest(h.getLevel(), module, tool, InteractionSource.TOOL);
                h.assertValueEqual(result.status(), InteractionHarvestResult.Status.READY, "Protected last-use tool still produces");
                h.assertTrue(ItemStack.matches(result.remainder(), original), "No wear, breakage or component mutation");
                h.assertTrue(ItemStack.matches(tool, original), "Real tool untouched during preparation");
                tool = result.remainder();
            }
        }
        var ordinary = new ItemStack(Items.SHEARS); ordinary.enchant(enchantment, 1);
        ordinary.hurtAndBreak(1, h.getLevel(), (net.minecraft.server.level.ServerPlayer) null, item -> { });
        h.assertValueEqual(ordinary.getDamageValue(), 1, "Protection does not leak into ordinary world use");
        h.succeed();
    }
    private static void isolation(GameTestHelper h) {
        try (var disposable = DisposableInteractionLevel.create(h.getLevel())) {
            var pos = h.absolutePos(new net.minecraft.core.BlockPos(1, 2, 1));
            var before = h.getLevel().getBlockState(pos);
            var player = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
            var spawned = net.minecraft.world.entity.EntityTypes.COW.create(disposable, net.minecraft.world.entity.EntitySpawnReason.LOAD);
            var escapedDrop = new net.minecraft.world.entity.item.ItemEntity(h.getLevel(), pos.getX(), pos.getY(), pos.getZ(), new ItemStack(Items.DIAMOND));
            try (var scope = InteractionIsolation.enter(disposable)) {
                expectDenied(h, () -> h.getLevel().setBlockAndUpdate(pos, Blocks.DIAMOND_BLOCK.defaultBlockState()));
                expectDenied(h, () -> h.getLevel().getServer().execute(() -> { throw new AssertionError("Escaped task"); }));
                expectDenied(h, () -> h.getLevel().getServer().getPlayerList());
                expectDenied(h, () -> disposable.getScoreboard());
                expectDenied(h, () -> disposable.getServer().clockManager());
                expectDenied(h, () -> disposable.getServer().getWeatherData());
                expectDenied(h, () -> disposable.getServer().getScheduledEvents());
                expectDenied(h, () -> disposable.getBlockTicks());
                expectDenied(h, () -> disposable.setBlockAndUpdate(pos, Blocks.DIAMOND_BLOCK.defaultBlockState()));
                expectDenied(h, () -> disposable.addFreshEntity(spawned));
                expectDenied(h, () -> h.getLevel().addFreshEntity(escapedDrop));
                expectDenied(h, () -> player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.POISON, 200)));
                h.assertTrue(scope.rejected(), "Forbidden calls invalidate attempt even when caught by a mod");
            }
            h.assertValueEqual(h.getLevel().getBlockState(pos), before, "Main world unchanged");
            h.assertFalse(player.hasEffect(net.minecraft.world.effect.MobEffects.POISON), "Effects never reach outside player");
            h.assertTrue(h.getLevel().getEntity(escapedDrop.getUUID()) == null, "Rejected drop never enters real world");
            spawned.discard(); escapedDrop.discard(); player.discard();
            h.assertFalse(InteractionIsolation.active(), "Scope cleanup");
        } catch (java.io.IOException exception) { throw new java.io.UncheckedIOException(exception); }
        h.succeed();
    }
    private static void expectDenied(GameTestHelper h, Runnable action) {
        boolean denied = false;
        try { action.run(); } catch (UnsupportedOperationException expected) { denied = true; }
        h.assertTrue(denied, "Unavailable operation rejected");
    }
}
