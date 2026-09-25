package com.cosmocraft.trading_cells.gametest.feature.mobfarm;

import com.cosmocraft.trading_cells.feature.mobfarm.adapters.input.EntityEssenceData;
import com.cosmocraft.trading_cells.feature.mobfarm.adapters.input.EssenceExtractorItem;
import com.cosmocraft.trading_cells.feature.mobfarm.adapters.input.EssenceWorkbenchBlockEntity;
import com.cosmocraft.trading_cells.feature.mobfarm.adapters.input.MobFarmBlockEntity;
import com.cosmocraft.trading_cells.feature.mobfarm.adapters.input.MobFarmMenu;
import com.cosmocraft.trading_cells.feature.mobfarm.adapters.output.MobFarmRegistrationAdapter;
import com.cosmocraft.trading_cells.gametest.shared.BlockEntityStateFixtures;
import com.cosmocraft.trading_cells.gametest.shared.GameTestCase;
import com.cosmocraft.trading_cells.gametest.shared.GameTestFixtures;
import com.cosmocraft.trading_cells.platform.neoforge.mobfarm.MobFarmLootTables;
import com.cosmocraft.trading_cells.shared.machines.domain.model.MinecraftExperience;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.TagValueInput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

public final class MobSimulationGameTests {
    private MobSimulationGameTests() { }
    public static List<GameTestCase> tests() {
        return List.of(new GameTestCase("mob_simulation_actual_module_state", 40, MobSimulationGameTests::moduleState),
                new GameTestCase("mob_simulation_block_item_names", 40, MobSimulationGameTests::blockItemNames),
                new GameTestCase("mob_simulation_extractor_interaction", 40, MobSimulationGameTests::extractorInteraction),
                new GameTestCase("mob_simulation_extractor_rejections", 40, MobSimulationGameTests::extractorRejections),
                new GameTestCase("mob_simulation_extractor_last_use", 40, MobSimulationGameTests::extractorLastUse),
                new GameTestCase("mob_simulation_extractor_full_inventory", 40, MobSimulationGameTests::extractorFullInventory),
                new GameTestCase("mob_simulation_syringe_reload", 40, MobSimulationGameTests::syringeReload),
                new GameTestCase("mob_simulation_syringe_animated_extraction", 40, MobSimulationGameTests::animatedExtraction),
                new GameTestCase("mob_simulation_workbench_atomic_cost", 40, MobSimulationGameTests::workbench),
                new GameTestCase("mob_simulation_workbench_high_level_cost", 40, MobSimulationGameTests::highLevelWorkbench),
                new GameTestCase("mob_simulation_workbench_shape", 40, MobSimulationGameTests::workbenchShape),
                new GameTestCase("mob_simulation_preview_before_tick", 40, MobSimulationGameTests::previewBeforeTick),
                new GameTestCase("mob_simulation_preview_impossible_loot", 40, MobSimulationGameTests::previewImpossibleLoot),
                new GameTestCase("mob_simulation_save_without_chunk_loads", 40, MobSimulationGameTests::saveWithoutChunkLoads),
                new GameTestCase("mob_simulation_pending_loot_reload", 40, MobSimulationGameTests::pending),
                new GameTestCase("mob_simulation_filters_and_automation", 40, MobSimulationGameTests::automation));
    }

    private static void blockItemNames(GameTestHelper helper) {
        for (var item : List.of(MobFarmRegistrationAdapter.ITEM.get(), MobFarmRegistrationAdapter.WORKBENCH_ITEM.get())) {
            helper.assertValueEqual(item.getDefaultInstance().getHoverName(), item.getBlock().getName(),
                    "Simulation block items share their block's translated name");
        }
        helper.succeed();
    }

    private static void extractorInteraction(GameTestHelper helper) {
        for (String id : List.of("cow", "villager", "zombie", "warden")) {
            var player = connectedPlayer(helper);
            var tool = MobFarmRegistrationAdapter.ESSENCE_EXTRACTOR.get().getDefaultInstance();
            player.setItemInHand(InteractionHand.OFF_HAND, tool);
            player.getInventory().setItem(9, new ItemStack(MobFarmRegistrationAdapter.EMPTY_VIAL.get(), 2));
            var target = MobFarmLootTables.createTarget(helper.getLevel(), Identifier.withDefaultNamespace(id));
            reload(player, tool);
            float health = target.getHealth();
            var uuid = target.getUUID();
            helper.assertValueEqual(interact(player, target, InteractionHand.OFF_HAND), InteractionResult.CONSUME,
                    "Registered extraction handler accepts " + id);
            helper.assertValueEqual(tool.getDamageValue(), 1, "Extraction wears the held tool once");
            helper.assertValueEqual(player.getInventory().getItem(9).getCount(), 1, "One bottle consumed");
            helper.assertTrue(target.isAlive() && target.getUUID().equals(uuid) && target.getHealth() == health,
                    "Extraction preserves the living creature, identity and health");
            ItemStack essence = extractedEssence(player);
            helper.assertValueEqual(EntityEssenceData.entityTypeId(essence), Identifier.withDefaultNamespace(id),
                    "The extracted essence belongs to the interacted creature");
            helper.assertValueEqual(EntityEssenceData.isHighLevel(essence), id.equals("warden"),
                    "Classification works through the actual extraction event");
            interact(player, target, InteractionHand.OFF_HAND);
            helper.assertValueEqual(essenceCount(player), 1, "Cooldown prevents repeated extraction");
            helper.assertValueEqual(player.getInventory().getItem(9).getCount(), 1, "Cooldown preserves bottles");
            helper.assertValueEqual(tool.getDamageValue(), 1, "Cooldown preserves durability");
            helper.assertTrue(EntityEssenceData.createEntity(helper.getLevel(), EntityEssenceData.moduleOf(EntityEssenceData.coreOf(essence))) != null,
                    "Extracted state creates a usable simulation module");
            for (int tick = 0; tick < 40; tick++) { player.getCooldowns().tick(); }
            helper.assertFalse(player.getCooldowns().isOnCooldown(tool), "Extractor cooldown expires after forty ticks");
            reload(player, tool);
            interact(player, target, InteractionHand.OFF_HAND);
            helper.assertValueEqual(essenceCount(player), 2, "Extraction resumes once the cooldown expires");
        }
        helper.succeed();
    }

    private static void extractorRejections(GameTestHelper helper) {
        var player = connectedPlayer(helper);
        var tool = MobFarmRegistrationAdapter.ESSENCE_EXTRACTOR.get().getDefaultInstance();
        player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        var target = MobFarmLootTables.createTarget(helper.getLevel(), Identifier.withDefaultNamespace("cow"));
        interact(player, target, InteractionHand.MAIN_HAND);
        helper.assertValueEqual(essenceCount(player), 0, "Survival extraction requires a bottle");
        helper.assertValueEqual(tool.getDamageValue(), 0, "Failed extraction does not wear the tool");
        helper.assertFalse(player.getCooldowns().isOnCooldown(tool), "Failed extraction does not start cooldown");
        player.getInventory().setItem(9, new ItemStack(MobFarmRegistrationAdapter.EMPTY_VIAL.get(), 2));
        target.setHealth(0);
        interact(player, target, InteractionHand.MAIN_HAND);
        helper.assertTrue(interact(player, connectedPlayer(helper), InteractionHand.MAIN_HAND) == null,
                "The handler does not intercept interactions with players");
        helper.assertValueEqual(essenceCount(player), 0, "Dead entities and players cannot produce essence");
        helper.assertValueEqual(player.getInventory().getItem(9).getCount(), 2, "Rejected targets preserve bottles");
        player.getInventory().setItem(9, ItemStack.EMPTY);
        player.setGameMode(GameType.CREATIVE);
        target.setHealth(target.getMaxHealth());
        interact(player, target, InteractionHand.MAIN_HAND);
        helper.assertValueEqual(essenceCount(player), 1, "Creative extraction does not require bottles");
        helper.assertValueEqual(tool.getDamageValue(), 0, "Creative extraction preserves durability");
        helper.succeed();
    }

    private static void extractorLastUse(GameTestHelper helper) {
        var player = connectedPlayer(helper);
        var tool = MobFarmRegistrationAdapter.ESSENCE_EXTRACTOR.get().getDefaultInstance();
        tool.setDamageValue(tool.getMaxDamage() - 1);
        player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        player.getInventory().setItem(9, new ItemStack(MobFarmRegistrationAdapter.EMPTY_VIAL.get(), 2));
        reload(player, tool);
        var target = MobFarmLootTables.createTarget(helper.getLevel(), Identifier.withDefaultNamespace("cow"));
        interact(player, target, InteractionHand.MAIN_HAND);
        helper.assertTrue(tool.isEmpty(), "Last use breaks the extractor");
        helper.assertValueEqual(essenceCount(player), 1, "Last use still delivers exactly one essence");
        var replacement = MobFarmRegistrationAdapter.ESSENCE_EXTRACTOR.get().getDefaultInstance();
        helper.assertTrue(player.getCooldowns().isOnCooldown(replacement), "Tool break must not erase extractor cooldown");
        helper.succeed();
    }

    private static void extractorFullInventory(GameTestHelper helper) {
        var player = connectedPlayer(helper);
        player.setPos(net.minecraft.world.phys.Vec3.atCenterOf(helper.absolutePos(GameTestFixtures.TEST_POS)));
        for (int slot = 0; slot < 36; slot++) { player.getInventory().setItem(slot, new ItemStack(Items.STONE, 64)); }
        player.getInventory().setItem(9, new ItemStack(MobFarmRegistrationAdapter.EMPTY_VIAL.get(), 2));
        player.setItemInHand(InteractionHand.OFF_HAND, MobFarmRegistrationAdapter.ESSENCE_EXTRACTOR.get().getDefaultInstance());
        reload(player, player.getOffhandItem());
        var target = MobFarmLootTables.createTarget(helper.getLevel(), Identifier.withDefaultNamespace("cow"));
        interact(player, target, InteractionHand.OFF_HAND);
        helper.assertValueEqual(essenceCount(player), 0, "Full inventory has no room for the essence");
        var dropped = helper.getLevel().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(3),
                item -> item.getItem().is(MobFarmRegistrationAdapter.RAW_ESSENCE.get()));
        helper.assertValueEqual(dropped.size(), 1, "Overflow essence drops once instead of being lost");
        helper.assertValueEqual(dropped.getFirst().getItem().getCount(), 1, "Dropped essence count");
        dropped.forEach(ItemEntity::discard);
        helper.succeed();
    }

    private static @org.jspecify.annotations.Nullable InteractionResult interact(ServerPlayer player, LivingEntity target, InteractionHand hand) {
        if (!(target instanceof net.minecraft.world.entity.player.Player) && player.level().getEntity(target.getUUID()) == null) {
            target.setPos(player.position());
            player.level().addFreshEntity(target);
        }
        var result = CommonHooks.onInteractEntity(player, target, hand, net.minecraft.world.phys.Vec3.ZERO);
        if (EssenceExtractorItem.extractionTier(player.getItemInHand(hand)) > 0) {
            for (int tick = 0; tick < EssenceExtractorItem.EXTRACTION_TICKS; tick++) { player.doTick(); }
        }
        return result;
    }

    private static void animatedExtraction(GameTestHelper helper) {
        var player = connectedPlayer(helper);
        var tool = MobFarmRegistrationAdapter.ESSENCE_EXTRACTOR.get().getDefaultInstance();
        player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        EssenceExtractorItem.setLoaded(tool, true);
        var target = MobFarmLootTables.createTarget(helper.getLevel(), Identifier.withDefaultNamespace("cow"));
        target.setPos(player.position()); helper.getLevel().addFreshEntity(target);
        EssenceExtractorItem.beginExtraction(player, target, InteractionHand.MAIN_HAND);
        for (int tick = 0; tick < 12; tick++) { player.doTick(); }
        helper.assertValueEqual(essenceCount(player), 0, "No essence before the extraction animation completes");
        helper.assertTrue(EssenceExtractorItem.isLoaded(tool), "Part-filled vial remains loaded");
        player.stopUsingItem();
        helper.assertValueEqual(EssenceExtractorItem.extractionTier(tool), 0, "Cancelled animation clears transient target");
        helper.assertValueEqual(tool.getDamageValue(), 0, "Cancel preserves durability");
        EssenceExtractorItem.beginExtraction(player, target, InteractionHand.MAIN_HAND);
        target.setPos(player.position().add(20, 0, 0)); player.doTick();
        helper.assertFalse(player.isUsingItem(), "Moving target cancels extraction");
        helper.assertTrue(EssenceExtractorItem.isLoaded(tool), "Moving target preserves the loaded vial");
        target.setPos(player.position());
        EssenceExtractorItem.beginExtraction(player, target, InteractionHand.MAIN_HAND);
        for (int tick = 0; tick < 23; tick++) { player.doTick(); }
        helper.assertValueEqual(essenceCount(player), 0, "Final extraction tick required");
        player.doTick();
        helper.assertValueEqual(essenceCount(player), 1, "24-tick extraction delivers once");
        helper.assertValueEqual(tool.getDamageValue(), 1, "Only completed extraction spends durability");
        helper.succeed();
    }

    private static void reload(ServerPlayer player, ItemStack tool) {
        tool.getItem().finishUsingItem(tool, player.level(), player);
        player.stopUsingItem();
    }

    private static void syringeReload(GameTestHelper helper) {
        var player = connectedPlayer(helper);
        player.setPos(net.minecraft.world.phys.Vec3.atCenterOf(helper.absolutePos(GameTestFixtures.TEST_POS)));
        ItemStack tool = MobFarmRegistrationAdapter.ESSENCE_EXTRACTOR.get().getDefaultInstance();
        player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        player.getInventory().setItem(9, new ItemStack(MobFarmRegistrationAdapter.EMPTY_VIAL.get(), 2));
        var target = MobFarmLootTables.createTarget(helper.getLevel(), Identifier.withDefaultNamespace("cow"));
        interact(player, target, InteractionHand.MAIN_HAND);
        helper.assertValueEqual(essenceCount(player), 0, "Unloaded syringe cannot extract");
        helper.assertValueEqual(player.getInventory().getItem(9).getCount(), 2, "Attempt does not auto-consume a vial");
        tool.getItem().use(player.level(), player, InteractionHand.MAIN_HAND);
        for (int tick = 0; tick < 12; tick++) { player.doTick(); }
        helper.assertFalse(EssenceExtractorItem.isLoaded(tool), "Partial reload is not a loaded syringe");
        player.stopUsingItem();
        helper.assertValueEqual(player.getInventory().getItem(9).getCount(), 2, "Cancelled reload spends nothing");
        tool.getItem().use(player.level(), player, InteractionHand.MAIN_HAND);
        for (int tick = 0; tick < EssenceExtractorItem.RELOAD_TICKS - 1; tick++) { player.doTick(); }
        helper.assertFalse(EssenceExtractorItem.isLoaded(tool), "The vial is not consumed before the final tick");
        player.doTick();
        helper.assertTrue(EssenceExtractorItem.isLoaded(tool), "Completed reload locks a vial into the top socket");
        helper.assertValueEqual(player.getInventory().getItem(9).getCount(), 1, "Exactly one vial consumed on completion");
        helper.assertValueEqual(tool.getDamageValue(), 0, "Reload does not consume durability");
        tool.getItem().finishUsingItem(tool, player.level(), player);
        helper.assertValueEqual(player.getInventory().getItem(9).getCount(), 1, "Repeated finish cannot charge twice");
        var ops = helper.getLevel().registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE);
        var serialized = ItemStack.CODEC.encodeStart(ops, tool).getOrThrow();
        var restored = ItemStack.CODEC.parse(ops, serialized).getOrThrow();
        helper.assertTrue(EssenceExtractorItem.isLoaded(restored), "Loaded state survives saving and transfer");
        interact(player, target, InteractionHand.MAIN_HAND);
        helper.assertFalse(EssenceExtractorItem.isLoaded(tool), "Extraction empties the socket");
        helper.assertValueEqual(essenceCount(player), 1, "Loaded vial produces exactly one raw essence");
        helper.assertValueEqual(player.getInventory().getItem(9).getCount(), 1, "Extraction cannot consume a second inventory vial");
        helper.assertValueEqual(tool.getDamageValue(), 1, "Successful extraction wears the syringe once");
        helper.succeed();
    }

    private static ItemStack extractedEssence(ServerPlayer player) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(MobFarmRegistrationAdapter.RAW_ESSENCE.get())) { return stack; }
        }
        return ItemStack.EMPTY;
    }

    private static int essenceCount(ServerPlayer player) {
        int count = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(MobFarmRegistrationAdapter.RAW_ESSENCE.get())) { count += stack.getCount(); }
        }
        return count;
    }

    private static ItemStack essence(GameTestHelper helper) {
        var target = MobFarmLootTables.createTarget(helper.getLevel(), Identifier.withDefaultNamespace("cow"));
        CompoundTag state = new CompoundTag();
        state.putString("DeathLootTable", "trading_cells_gametest:captured_entity");
        target.load(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), state));
        return EntityEssenceData.essenceOf(target);
    }

    private static void moduleState(GameTestHelper helper) {
        ItemStack essence = essence(helper);
        ItemStack module = EntityEssenceData.moduleOf(essence);
        helper.assertTrue(module.is(MobFarmRegistrationAdapter.ENTITY_MODULE.get()), "Workbench result has the module identity");
        var recreated = EntityEssenceData.createEntity(helper.getLevel(), module);
        helper.assertTrue(recreated != null, "Snapshot restores its actual creature");
        helper.assertValueEqual(recreated.getLootTable().orElseThrow().identifier().toString(),
                "trading_cells_gametest:captured_entity", "Per-instance custom loot table survives essence and module");
        helper.assertFalse(EntityEssenceData.isHighLevel(essence), "Ordinary cow is normal tier");
        var warden = MobFarmLootTables.createTarget(helper.getLevel(), Identifier.withDefaultNamespace("warden"));
        helper.assertTrue(EntityEssenceData.isHighLevel(EntityEssenceData.essenceOf(warden)), "Health-based classification recognizes strong creatures");
        var player = helper.makeMockServerPlayer(GameType.SURVIVAL);
        helper.assertTrue(EntityEssenceData.essenceOf(player).isEmpty(), "Player data cannot become a creature module");
        helper.assertTrue(EntityEssenceData.createEntity(helper.getLevel(), ItemStack.EMPTY) == null, "Empty/malformed module has no fallback creature");
        helper.succeed();
    }

    private static void workbench(GameTestHelper helper) {
        helper.setBlock(GameTestFixtures.TEST_POS, MobFarmRegistrationAdapter.ESSENCE_WORKBENCH.get());
        var bench = helper.getBlockEntity(GameTestFixtures.TEST_POS, EssenceWorkbenchBlockEntity.class);
        var player = connectedPlayer(helper);
        player.setPos(net.minecraft.world.phys.Vec3.atCenterOf(bench.getBlockPos()));
        bench.setItem(0, essence(helper));
        bench.setItem(1, new ItemStack(MobFarmRegistrationAdapter.MODEL_BASES.getFirst().get(), 2));
        BlockEntityStateFixtures.fillIndexedSlots(helper, bench, "Slot", 2, 1, new ItemStack(Items.IRON_INGOT, 4));
        player.giveExperiencePoints(1_000);
        helper.assertFalse(bench.synthesize(player), "Player XP is not spent implicitly");
        bench.experience().transfer(player, 1, 0);
        helper.assertValueEqual(bench.experience().amount(), 1_000, "Manual transfer fills internal XP");
        BlockEntityStateFixtures.fillIndexedSlots(helper, bench, "Slot", 3, 1, Items.STONE.getDefaultInstance());
        helper.assertFalse(bench.synthesize(player), "Occupied result prevents crafting");
        helper.assertValueEqual(bench.experience().amount(), 1_000, "Blocked result preserves internal XP");
        bench.removeItem(3, 1);
        helper.assertTrue(bench.synthesize(player), "Matching core and base produce a reusable model");
        helper.assertValueEqual(bench.experience().amount(), 0, "Tier I consumes exactly 1000 XP");
        helper.assertTrue(bench.getItem(0).isEmpty(), "One core consumed");
        helper.assertValueEqual(bench.getItem(1).getCount(), 1, "Only one base consumed");
        helper.assertValueEqual(bench.getItem(2).getCount(), 4, "Legacy metal remains recoverable");
        helper.assertTrue(bench.getItem(3).is(MobFarmRegistrationAdapter.ENTITY_MODULE.get()), "Model delivered");
        helper.assertFalse(bench.synthesize(player), "Repeated button cannot duplicate a model");
        helper.succeed();
    }

    private static void highLevelWorkbench(GameTestHelper helper) {
        helper.setBlock(GameTestFixtures.TEST_POS, MobFarmRegistrationAdapter.ESSENCE_WORKBENCH.get());
        var bench = helper.getBlockEntity(GameTestFixtures.TEST_POS, EssenceWorkbenchBlockEntity.class);
        var player = connectedPlayer(helper);
        var center = net.minecraft.world.phys.Vec3.atCenterOf(bench.getBlockPos());
        player.setPos(center);
        var warden = MobFarmLootTables.createTarget(helper.getLevel(), Identifier.withDefaultNamespace("warden"));
        ItemStack core = EntityEssenceData.essenceOf(warden);
        int tier = EntityEssenceData.tier(core).id();
        int cost = EntityEssenceData.tier(core).modelExperience();
        bench.setItem(0, core);
        bench.setItem(1, new ItemStack(MobFarmRegistrationAdapter.MODEL_BASES.getFirst().get()));
        bench.experience().setRaw(cost);
        helper.assertFalse(bench.synthesize(player), "Wrong-tier base cannot substitute for the matching base");
        bench.setItem(1, new ItemStack(MobFarmRegistrationAdapter.MODEL_BASES.get(tier - 1).get(), 2));
        player.setPos(center.add(20, 0, 0));
        helper.assertFalse(bench.synthesize(player), "Distant requests cannot craft");
        player.setPos(center);
        bench.experience().setRaw(cost - 1);
        helper.assertFalse(bench.synthesize(player), "One missing XP prevents consumption");
        helper.assertValueEqual(bench.getItem(1).getCount(), 2, "Failures preserve both bases");
        bench.experience().setRaw(cost);
        helper.assertTrue(bench.synthesize(player), "Matching tier and exact internal XP succeed");
        helper.assertValueEqual(bench.getItem(1).getCount(), 1, "Only one base consumed");
        helper.assertValueEqual(bench.experience().amount(), 0, "Exact tier cost consumed");
        var simulated = EntityEssenceData.createEntity(helper.getLevel(), bench.getItem(3));
        helper.assertTrue(simulated != null && simulated.getType() == warden.getType(), "Model restores the warden");
        helper.succeed();
    }

    private static MobFarmBlockEntity farm(GameTestHelper helper) {
        helper.setBlock(GameTestFixtures.TEST_POS, MobFarmRegistrationAdapter.BLOCK.get());
        var farm = helper.getBlockEntity(GameTestFixtures.TEST_POS, MobFarmBlockEntity.class);
        farm.setItem(0, GameTestFixtures.adultVillagerCapture(helper));
        farm.setItem(1, Items.IRON_SWORD.getDefaultInstance());
        farm.setItem(2, EntityEssenceData.moduleOf(essence(helper)));
        farm.processTick();
        return farm;
    }

    private static void workbenchShape(GameTestHelper helper) {
        var state = MobFarmRegistrationAdapter.ESSENCE_WORKBENCH.get().defaultBlockState();
        var expected = net.minecraft.world.phys.shapes.Shapes.or(
                net.minecraft.world.level.block.Block.box(1, 10, 1, 15, 13, 15),
                net.minecraft.world.level.block.Block.box(3, 13, 3, 13, 14, 13),
                net.minecraft.world.level.block.Block.box(2, 0, 2, 4, 10, 4),
                net.minecraft.world.level.block.Block.box(2, 0, 12, 4, 10, 14),
                net.minecraft.world.level.block.Block.box(12, 0, 2, 14, 10, 4),
                net.minecraft.world.level.block.Block.box(12, 0, 12, 14, 10, 14));
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            var rotated = state.setValue(com.cosmocraft.trading_cells.platform.neoforge.machine.AbstractPortableMachineBlock.FACING, direction);
            var shape = rotated.getShape(helper.getLevel(), helper.absolutePos(GameTestFixtures.TEST_POS));
            helper.assertFalse(net.minecraft.world.phys.shapes.Shapes.joinIsNotEmpty(shape, expected,
                    net.minecraft.world.phys.shapes.BooleanOp.NOT_SAME), "Workbench selection follows top and legs in every orientation");
            helper.assertFalse(net.minecraft.world.phys.shapes.Shapes.joinIsNotEmpty(
                    rotated.getCollisionShape(helper.getLevel(), helper.absolutePos(GameTestFixtures.TEST_POS)), expected,
                    net.minecraft.world.phys.shapes.BooleanOp.NOT_SAME), "Workbench collision leaves space between legs");
        }
        helper.succeed();
    }

    private static void saveWithoutChunkLoads(GameTestHelper helper) {
        var pos = new net.minecraft.core.BlockPos(29_000_000, 64, 29_000_000);
        var farm = new MobFarmBlockEntity(pos, MobFarmRegistrationAdapter.BLOCK.get().defaultBlockState());
        farm.setLevel(helper.getLevel());
        var state = new CompoundTag();
        state.putBoolean("Hunting", true);
        farm.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), state));
        helper.assertFalse(helper.getLevel().getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4),
                "Detached serialization fixture starts outside loaded chunks");
        var saved = farm.saveWithFullMetadata(helper.getLevel().registryAccess());
        helper.assertTrue(saved.getBooleanOr("Hunting", false), "Last known animation state is retained");
        helper.assertFalse(helper.getLevel().getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4),
                "Saving a farm must not load chunks through redstone queries");
        helper.succeed();
    }

    private static ItemStack module(GameTestHelper helper, String type, String table) {
        var target = MobFarmLootTables.createTarget(helper.getLevel(), Identifier.withDefaultNamespace(type));
        if (table != null) {
            var state = new CompoundTag();
            state.putString("DeathLootTable", table);
            target.load(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), state));
        }
        return EntityEssenceData.moduleOf(EntityEssenceData.essenceOf(target));
    }

    private static void previewBeforeTick(GameTestHelper helper) {
        var farm = farm(helper);
        BlockEntityStateFixtures.fillIndexedSlots(helper, farm, "Slot", 5, 18, new ItemStack(Items.COBBLESTONE, 64));
        finishCycle(helper, farm);
        farm.toggleEnabled();
        var player = connectedPlayer(helper);
        player.setPos(net.minecraft.world.phys.Vec3.atCenterOf(farm.getBlockPos()));
        var menu = (MobFarmMenu) farm.createMenu(51, player.getInventory(), player);
        player.containerMenu = menu;
        farm.setItem(2, module(helper, "warden", null));
        menu.broadcastChanges();
        assertLoot(helper, menu, Items.SCULK_CATALYST, 1_000_000);
        helper.assertValueEqual(menu.lootEntries().size(), 1, "Pending cow diamonds do not contaminate warden preview");
        farm.setItem(2, module(helper, "iron_golem", null));
        menu.broadcastChanges();
        assertLoot(helper, menu, Items.IRON_INGOT, 1_000_000);
        helper.assertTrue(menu.lootEntries().stream().noneMatch(entry -> entry.stack().is(Items.SCULK_CATALYST)),
                "Switch before a tick removes all previous creature drops");
        farm.setItem(2, module(helper, "warden", "trading_cells_gametest:simulation_echo"));
        menu.broadcastChanges();
        assertLoot(helper, menu, Items.ECHO_SHARD, 1_000_000);
        helper.assertValueEqual(menu.lootEntries().size(), 1, "Custom instance table replaces vanilla loot immediately");
        int revision = menu.lootRevision();
        menu.broadcastChanges();
        helper.assertValueEqual(menu.lootRevision(), revision, "Unchanged preview reuses its snapshot");
        var saved = farm.saveWithFullMetadata(helper.getLevel().registryAccess());
        helper.assertValueEqual(saved.getIntOr("ObservedLootCount", 0), 0, "Predictions are not saved as observed drops");
        helper.assertValueEqual(saved.getIntOr("PendingLootCount", 0), 1, "Old pending output remains safely queued");
        helper.assertValueEqual(farm.storedExperience(), 5, "Opening and swapping previews never runs a cycle");
        farm.setItem(2, ItemStack.EMPTY);
        menu.broadcastChanges();
        helper.assertTrue(menu.lootEntries().isEmpty(), "Removing module clears preview despite pending output");
        helper.succeed();
    }

    private static void previewImpossibleLoot(GameTestHelper helper) {
        var farm = farm(helper);
        farm.toggleEnabled();
        var player = connectedPlayer(helper);
        player.setPos(net.minecraft.world.phys.Vec3.atCenterOf(farm.getBlockPos()));
        var menu = (MobFarmMenu) farm.createMenu(52, player.getInventory(), player);
        player.containerMenu = menu;
        farm.setItem(2, module(helper, "creeper", null));
        menu.broadcastChanges();
        helper.assertValueEqual(menu.lootEntries().size(), 1, "Creeper has gunpowder, no impossible discs or unenchanted head");
        assertLoot(helper, menu, Items.GUNPOWDER, 666667);
        var sword = Items.IRON_SWORD.getDefaultInstance();
        sword.enchant(helper.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                .getOrThrow(com.cosmocraft.trading_cells.feature.combat.adapters.api.CombatEnchantments.DECAPITATION), 1);
        farm.setItem(1, sword);
        menu.broadcastChanges();
        assertLoot(helper, menu, Items.CREEPER_HEAD, 35_000);
        farm.rememberLoot(List.of(Identifier.withDefaultNamespace("creeper_head")));
        farm.setItem(1, Items.IRON_SWORD.getDefaultInstance());
        menu.broadcastChanges();
        helper.assertValueEqual(menu.lootEntries().size(), 1, "Removing enchantment hides previously observed but now impossible head");
        farm.setItem(2, module(helper, "cow", "trading_cells_gametest:simulation_unknown"));
        menu.broadcastChanges();
        assertLoot(helper, menu, Items.ECHO_SHARD, -1);
        helper.succeed();
    }

    private static void assertLoot(GameTestHelper helper, MobFarmMenu menu, net.minecraft.world.item.Item item, int chance) {
        var drop = menu.lootEntries().stream().filter(entry -> entry.stack().is(item)).findFirst().orElse(null);
        helper.assertTrue(drop != null, "Expected preview item " + item);
        helper.assertValueEqual(drop.probability(), chance, "Probability available before first cycle for " + item);
    }

    private static void finishCycle(GameTestHelper helper, MobFarmBlockEntity farm) {
        BlockEntityStateFixtures.setInt(helper, farm, "CycleTicks", farm.cycleDurationTicks() - 1);
        farm.processTick();
    }

    private static void pending(GameTestHelper helper) {
        var farm = farm(helper);
        BlockEntityStateFixtures.fillIndexedSlots(helper, farm, "Slot", 5, 18, new ItemStack(Items.COBBLESTONE, 64));
        finishCycle(helper, farm);
        helper.assertValueEqual(farm.storedExperience(), 5, "One completed kill yields original XP amount");
        helper.assertValueEqual(farm.getItem(1).getDamageValue(), 1, "Unprotected sword wears once");
        var saved = farm.saveWithFullMetadata(helper.getLevel().registryAccess());
        helper.assertValueEqual(saved.getIntOr("PendingLootCount", 0), 1, "Full output keeps rolled result");
        farm.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), saved));
        for (int tick = 0; tick < 10; tick++) { farm.processTick(); }
        helper.assertValueEqual(farm.storedExperience(), 5, "Blocked queue never rerolls or duplicates XP");
        farm.removeItem(5, 64);
        farm.processTick();
        helper.assertTrue(farm.getItem(5).is(Items.DIAMOND), "Saved custom-table loot drains when room appears");
        helper.assertValueEqual(farm.getItem(5).getCount(), 1, "No duplicate after reload");
        helper.assertValueEqual(farm.getItem(1).getDamageValue(), 1, "Draining does not repeat durability charge");
        helper.assertTrue(farm.creature().is(MobFarmRegistrationAdapter.ENTITY_MODULE.get()), "Module is reusable");
        helper.succeed();
    }

    private static void automation(GameTestHelper helper) {
        var farm = farm(helper);
        farm.toggleLoot(Identifier.withDefaultNamespace("diamond"));
        finishCycle(helper, farm);
        helper.assertTrue(farm.getItem(5).isEmpty(), "Unchecked loot is discarded deliberately");
        helper.assertValueEqual(farm.storedExperience(), 5, "Loot filters do not disable XP");
        farm.toggleLoot(Identifier.withDefaultNamespace("diamond"));
        farm.setItem(4, new ItemStack(BuiltInRegistries.ITEM.getOptional(
                Identifier.fromNamespaceAndPath("trading_cells", "mob_farm_capacity_iron_upgrade")).orElseThrow()));
        farm.processTick();
        helper.assertValueEqual(farm.simulatedKills(), 4, "Capacity upgrade determines kills independently");
        finishCycle(helper, farm);
        helper.assertValueEqual(farm.getItem(5).getCount(), 4, "Native table runs once per simulated kill");
        var output = helper.requireCapability(Capabilities.Item.BLOCK, GameTestFixtures.TEST_POS, Direction.WEST);
        int outputIndex = 5;
        try (var transaction = Transaction.openRoot()) {
            helper.assertValueEqual(output.extract(outputIndex, ItemResource.of(Items.DIAMOND), 1, transaction), 1,
                    "Pipes can extract from a horizontal side");
        }
        helper.assertValueEqual(farm.getItem(5).getCount(), 4, "Aborted transfer restores exact output");
        var player = connectedPlayer(helper);
        player.setPos(net.minecraft.world.phys.Vec3.atCenterOf(farm.getBlockPos()));
        var menu = (MobFarmMenu) farm.createMenu(50, player.getInventory(), player);
        player.containerMenu = menu;
        menu.broadcastChanges();
        menu.toggleLoot(player, menu.lootRevision() - 1, Identifier.withDefaultNamespace("diamond"));
        helper.assertTrue(farm.lootEnabled(Identifier.withDefaultNamespace("diamond")), "Stale filter snapshot is rejected");
        menu.clickMenuButton(player, 0);
        int progress = farm.cycleTicks();
        farm.processTick();
        helper.assertValueEqual(farm.cycleTicks(), progress, "On/off pauses simulation");
        helper.succeed();
    }

    static net.minecraft.server.level.ServerPlayer connectedPlayer(GameTestHelper helper) {
        var player = new net.minecraft.server.level.ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "simulation-test"),
                net.minecraft.server.level.ClientInformation.createDefault()) {
            @Override public boolean isClientAuthoritative() { return false; }
        };
        var cookie = new net.minecraft.server.network.CommonListenerCookie(player.getGameProfile(), 0,
                net.minecraft.server.level.ClientInformation.createDefault(), false,
                net.neoforged.neoforge.network.connection.ConnectionType.OTHER);
        new net.minecraft.server.network.ServerGamePacketListenerImpl(helper.getLevel().getServer(),
                new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND), player, cookie) {
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet,
                    io.netty.channel.@org.jspecify.annotations.Nullable ChannelFutureListener listener) { }
        };
        player.setGameMode(GameType.SURVIVAL);
        player.setPos(net.minecraft.world.phys.Vec3.atCenterOf(helper.absolutePos(GameTestFixtures.TEST_POS)));
        player.setNoGravity(true);
        return player;
    }
}
