package com.cosmocraft.trading_cells.gametest.feature.mobfarm;

import com.cosmocraft.trading_cells.feature.mobfarm.adapters.input.EntityEssenceData;

import com.cosmocraft.trading_cells.feature.mobfarm.adapters.input.CreatureGeneratorBlockEntity;
import com.cosmocraft.trading_cells.feature.mobfarm.adapters.input.CreatureGeneratorEggs;
import com.cosmocraft.trading_cells.feature.mobfarm.adapters.input.EssenceWorkbenchMenu;
import com.cosmocraft.trading_cells.feature.mobfarm.adapters.output.MobFarmRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.mobfarm.domain.model.EssenceTier;
import com.cosmocraft.trading_cells.gametest.shared.BlockEntityStateFixtures;
import com.cosmocraft.trading_cells.gametest.shared.GameTestCase;
import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.Config;
import com.cosmocraft.trading_cells.platform.neoforge.registration.ExperienceFluidRegistration;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

public final class CreatureGeneratorGameTests {
    private static final BlockPos POS = new BlockPos(1, 1, 1);
    private CreatureGeneratorGameTests() { }
    public static List<GameTestCase> tests() {
        return List.of(new GameTestCase("creature_generator_tiers_atomic_output", 40, CreatureGeneratorGameTests::tiers),
                new GameTestCase("creature_generator_world_interaction", 40, CreatureGeneratorGameTests::worldInteraction),
                new GameTestCase("creature_generator_blacklist_revalidation", 40, CreatureGeneratorGameTests::blacklist),
                new GameTestCase("creature_generator_external_fallback_spawn", 40, CreatureGeneratorGameTests::fallback),
                new GameTestCase("creature_generator_persistence_xp_rollback", 40, CreatureGeneratorGameTests::persistence));
    }
    private static void worldInteraction(GameTestHelper helper) {
        var bench = machine(helper);
        var player = MobSimulationGameTests.connectedPlayer(helper);
        var pos = bench.getBlockPos();
        player.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() - 2);
        var hit = new BlockHitResult(Vec3.atLowerCornerOf(pos).add(0.5, 0.75, 0), Direction.NORTH, pos, false);
        var channel = new io.netty.channel.embedded.EmbeddedChannel(player.connection.getConnection());
        try {
            for (var held : List.of(ItemStack.EMPTY, Items.STICK.getDefaultInstance())) {
                player.closeContainer();
                player.setItemInHand(InteractionHand.MAIN_HAND, held);
                var result = player.gameMode.useItemOn(player, helper.getLevel(), held, InteractionHand.MAIN_HAND, hit);
                helper.assertTrue(result.consumesAction(), "Right-click consumes interaction");
                helper.assertTrue(player.containerMenu instanceof EssenceWorkbenchMenu menu && menu.isCreatureGenerator(),
                        "Right-click opens the creature generator with empty or occupied hand");
                helper.assertTrue(player.containerMenu.stillValid(player), "Opened menu stays usable");
            }
        } finally {
            player.closeContainer();
            channel.finishAndReleaseAll();
        }
        helper.succeed();
    }
    private static ItemStack core(String id, int tier) {
        var root = new CompoundTag();
        var essence = new CompoundTag();
        essence.putString("Type", id);
        essence.putInt("Tier", tier);
        essence.putInt("ClassificationVersion", 1);
        essence.put("State", new CompoundTag());
        root.put("TradingCellsEssence", essence);
        var stack = new ItemStack(MobFarmRegistrationAdapter.ENTITY_ESSENCE.get());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
        return stack;
    }
    private static CreatureGeneratorBlockEntity machine(GameTestHelper helper) {
        helper.setBlock(POS, MobFarmRegistrationAdapter.CREATURE_GENERATOR.get());
        return helper.getBlockEntity(POS, CreatureGeneratorBlockEntity.class);
    }
    private static void tiers(GameTestHelper helper) {
        var bench = machine(helper);
        var player = MobSimulationGameTests.connectedPlayer(helper);
        player.setPos(Vec3.atCenterOf(bench.getBlockPos()));
        for (EssenceTier tier : EssenceTier.values()) {
            bench.clearContent();
            bench.setItem(0, core("minecraft:cow", tier.id()));
            var base = MobFarmRegistrationAdapter.MODEL_BASES.get(tier.id() - 1).get().getDefaultInstance();
            helper.assertFalse(bench.canPlaceItem(1, base), "Model bases are not egg ingredients");
            bench.setItem(1, base);
            bench.experience().setRaw(tier.modelExperience());
            helper.assertFalse(bench.synthesize(player), "A base cannot replace a tagged egg");
            var egg = List.of(Items.EGG, Items.TURTLE_EGG, Items.SNIFFER_EGG, Items.EGG).get(tier.id() - 1);
            bench.setItem(1, new ItemStack(egg, 2));
            bench.experience().setRaw(tier.modelExperience() - 1);
            helper.assertTrue(bench.visibleResult().isEmpty(), "Insufficient XP cannot preview a takeable output");
            bench.experience().setRaw(tier.modelExperience());
            ItemStack preview = bench.visibleResult();
            helper.assertTrue(preview.is(Items.COW_SPAWN_EGG), "Registered vanilla egg takes precedence");
            BlockEntityStateFixtures.fillIndexedSlots(helper, bench, "Slot", 3, 1, new ItemStack(Items.STONE, 64));
            helper.assertFalse(bench.synthesize(player), "Full output rejects synthesis");
            helper.assertValueEqual(bench.experience().amount(), tier.modelExperience(), "Blocked output keeps XP");
            helper.assertValueEqual(bench.getItem(0).getCount(), 1, "Blocked output keeps core");
            bench.removeItem(3, 64);
            var menu = (EssenceWorkbenchMenu) bench.createMenu(7, player.getInventory(), player);
            helper.assertFalse(menu.getSlot(1).mayPlace(base), "Menu rejects model bases");
            helper.assertTrue(menu.getSlot(1).mayPlace(egg.getDefaultInstance()), "Menu accepts tagged eggs");
            helper.assertFalse(menu.getSlot(1).mayPlace(Items.PIG_SPAWN_EGG.getDefaultInstance()), "Spawn eggs are not ingredient eggs");
            for (int slot = 0; slot < 36; slot++) { player.getInventory().setItem(slot, new ItemStack(Items.STONE, 64)); }
            helper.assertTrue(menu.quickMoveStack(player, 3).isEmpty(), "Full player inventory rejects shift craft");
            helper.assertValueEqual(bench.experience().amount(), tier.modelExperience(), "Failed pickup keeps XP");
            player.getInventory().setItem(8, ItemStack.EMPTY);
            helper.assertTrue(menu.quickMoveStack(player, 3).is(Items.COW_SPAWN_EGG), "Shift craft commits full result");
            helper.assertValueEqual(bench.experience().amount(), 0, "Exact shared tier cost");
            helper.assertTrue(bench.getItem(0).isEmpty() && bench.getItem(1).is(egg)
                    && bench.getItem(1).getCount() == 1, "One core and exactly one tagged egg consumed");
        }
        helper.succeed();
    }
    private static void blacklist(GameTestHelper helper) {
        var bench = machine(helper);
        bench.toggleAutomatic();
        var previous = Config.CREATURE_GENERATOR_BLACKLIST.get();
        try {
            Config.CREATURE_GENERATOR_BLACKLIST.set(List.of("minecraft:wither", "minecraft:ender_dragon", "TRADING_CELLS_GAMETEST:EXTERNAL_ELITE"));
            for (String id : List.of("minecraft:wither", "minecraft:ender_dragon", "trading_cells_gametest:external_elite", "absent_mod:missing")) {
                bench.setItem(0, core(id, 4));
                bench.setItem(1, Items.EGG.getDefaultInstance());
                bench.experience().setRaw(64_000);
                bench.processTick();
                helper.assertTrue(bench.visibleResult().isEmpty() && bench.getItem(3).isEmpty(), "Forbidden/missing entity produces no egg: " + id);
                helper.assertValueEqual(bench.experience().amount(), 64_000, "Forbidden entity keeps XP");
                helper.assertValueEqual(bench.getItem(0).getCount(), 1, "Forbidden core stays recoverable");
                helper.assertValueEqual(bench.getItem(1).getCount(), 1, "Forbidden egg stays recoverable");
            }
            bench.setItem(0, core("minecraft:cow", 4));
            var preview = bench.visibleResult();
            Config.CREATURE_GENERATOR_BLACKLIST.set(List.of("minecraft:cow"));
            helper.assertFalse(bench.takeVisibleResult(preview), "Blacklist revalidated after preview before consumption");
        } finally { Config.CREATURE_GENERATOR_BLACKLIST.set(previous); }
        helper.succeed();
    }
    private static void fallback(GameTestHelper helper) {
        for (int tier = 1; tier <= 4; tier++) {
            var colored = CreatureGeneratorEggs.create(core("trading_cells_gametest:external_elite", tier));
            helper.assertValueEqual(EntityEssenceData.tier(colored).id(), tier, "Fallback preserves classified tier for its color");
            var vanilla = CreatureGeneratorEggs.create(core("minecraft:pig", tier));
            helper.assertTrue(vanilla.is(Items.PIG_SPAWN_EGG) && !vanilla.has(DataComponents.CUSTOM_DATA),
                    "Registered vanilla eggs retain their original appearance and components");
        }
        var source = core("trading_cells_gametest:external_elite", 3);
        var egg = CreatureGeneratorEggs.create(source);
        helper.assertTrue(egg.is(MobFarmRegistrationAdapter.GENERIC_SPAWN_EGG.get()), "No registered egg uses one generic fallback");
        var type = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse("trading_cells_gametest:external_elite"));
        helper.assertValueEqual(SpawnEggItem.getType(egg), type, "Fallback contains external registry identity");
        helper.assertTrue(egg.get(DataComponents.ENTITY_DATA).copyTagWithoutId().isEmpty(), "No arbitrary essence state copied to egg");
        var ops = helper.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE);
        var encoded = ItemStack.CODEC.encodeStart(ops, egg).getOrThrow();
        var restored = ItemStack.CODEC.parse(ops, encoded).getOrThrow();
        helper.assertValueEqual(EntityEssenceData.tier(restored).id(), 3, "Fallback tier survives serialization");
        helper.assertTrue(ItemStack.matches(egg, restored), "Egg component persists through save/load");
        var player = MobSimulationGameTests.connectedPlayer(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, restored);
        BlockPos ground = helper.absolutePos(new BlockPos(3, 1, 3));
        helper.getLevel().setBlockAndUpdate(ground, Blocks.STONE.defaultBlockState());
        var hit = new BlockHitResult(Vec3.atCenterOf(ground).add(0, 0.5, 0), Direction.UP, ground, false);
        var result = restored.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
        helper.assertValueEqual(result, InteractionResult.SUCCESS, "Fallback uses vanilla spawning path");
        helper.assertTrue(restored.isEmpty(), "Successful spawn consumes one egg");
        helper.assertTrue(!helper.getLevel().getEntities(type, new net.minecraft.world.phys.AABB(ground).inflate(2), entity -> true).isEmpty(),
                "External entity really spawned without importing its class");
        helper.succeed();
    }
    private static void persistence(GameTestHelper helper) {
        var bench = machine(helper);
        bench.setItem(0, core("minecraft:pig", 2));
        helper.assertTrue(bench.canPlaceItemThroughFace(1, Items.EGG.getDefaultInstance(), Direction.WEST), "Egg enters from relative right");
        helper.assertFalse(bench.canPlaceItemThroughFace(1, Items.EGG.getDefaultInstance(), Direction.EAST), "Core side rejects egg");
        var items = helper.getLevel().getCapability(Capabilities.Item.BLOCK, bench.getBlockPos(), Direction.WEST);
        helper.assertTrue(items != null, "Egg automation capability registered");
        try (var tx = Transaction.openRoot()) {
            helper.assertValueEqual(items.insert(net.neoforged.neoforge.transfer.item.ItemResource.of(Items.EGG), 16, tx), 1,
                    "Automation accepts one tagged egg per recipe");
        }
        helper.assertTrue(bench.getItem(1).isEmpty(), "Cancelled egg insertion rolls back");
        try (var tx = Transaction.openRoot()) {
            items.insert(net.neoforged.neoforge.transfer.item.ItemResource.of(Items.EGG), 16, tx);
            tx.commit();
        }
        var handler = helper.getLevel().getCapability(Capabilities.Fluid.BLOCK, bench.getBlockPos(), Direction.UP);
        helper.assertTrue(handler != null, "XP capability registered");
        try (var tx = Transaction.openRoot()) {
            helper.assertValueEqual(handler.insert(FluidResource.of(ExperienceFluidRegistration.SOURCE.get()), 8_000, tx), 4_000, "One recipe quota");
        }
        helper.assertValueEqual(bench.experience().amount(), 0, "Cancelled insertion rolls back XP");
        bench.experience().setRaw(4_010);
        bench.toggleAutomatic();
        var saved = bench.saveWithFullMetadata(helper.getLevel().registryAccess());
        var restored = new CreatureGeneratorBlockEntity(bench.getBlockPos(), bench.getBlockState());
        restored.setLevel(helper.getLevel());
        restored.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), saved));
        helper.assertTrue(restored.automatic(), "Automation persists");
        restored.processTick();
        helper.assertTrue(restored.getItem(3).is(Items.PIG_SPAWN_EGG), "Restored machine crafts correct registered egg");
        helper.assertValueEqual(restored.experience().amount(), 10, "XP surplus preserved");
        restored.processTick();
        helper.assertValueEqual(restored.getItem(3).getCount(), 1, "No repeat consumption after ingredients exhausted");
        helper.succeed();
    }
}
