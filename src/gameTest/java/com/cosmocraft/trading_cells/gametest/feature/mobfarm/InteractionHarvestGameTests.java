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
                new GameTestCase("interaction_harvest_mooshrooms", 80, InteractionHarvestGameTests::mooshrooms),
                new GameTestCase("interaction_harvest_farmers_touch", 80, InteractionHarvestGameTests::farmersTouch),
                new GameTestCase("interaction_harvest_creaking", 80, InteractionHarvestGameTests::creaking),
                new GameTestCase("speed_upgrade_legacy_aliases", 80, InteractionHarvestGameTests::speedAliases));
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
    private static void farmersTouch(GameTestHelper h) {
        var enchantment = h.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                .getOrThrow(com.cosmocraft.trading_cells.platform.neoforge.machine.MachineToolEnchantments.FARMERS_TOUCH);
        var types = List.of("minecraft:sheep", "minecraft:bee", "minecraft:mooshroom", "minecraft:armadillo",
                "trading_cells_gametest:resource_creature", "minecraft:creaking");
        var tools = List.of(Items.SHEARS, Items.SHEARS, Items.SHEARS, Items.BRUSH, InteractionTestContent.tool, Items.IRON_AXE);
        for (int index = 0; index < types.size(); index++) {
            var tool = new ItemStack(tools.get(index));
            h.assertTrue(tool.supportsEnchantment(enchantment), "Vanilla and tagged mod tools accept Farmer's Touch");
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
