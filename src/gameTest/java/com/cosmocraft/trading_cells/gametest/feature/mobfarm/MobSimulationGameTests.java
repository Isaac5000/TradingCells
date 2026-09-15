package com.cosmocraft.trading_cells.gametest.feature.mobfarm;

import com.cosmocraft.trading_cells.feature.mobfarm.adapters.input.EntityEssenceData;
import com.cosmocraft.trading_cells.feature.mobfarm.adapters.input.EssenceWorkbenchBlockEntity;
import com.cosmocraft.trading_cells.feature.mobfarm.adapters.input.MobFarmBlockEntity;
import com.cosmocraft.trading_cells.feature.mobfarm.adapters.input.MobFarmMenu;
import com.cosmocraft.trading_cells.feature.mobfarm.adapters.output.MobFarmRegistrationAdapter;
import com.cosmocraft.trading_cells.gametest.shared.BlockEntityStateFixtures;
import com.cosmocraft.trading_cells.gametest.shared.GameTestCase;
import com.cosmocraft.trading_cells.gametest.shared.GameTestFixtures;
import com.cosmocraft.trading_cells.platform.neoforge.mobfarm.MobFarmLootTables;
import com.cosmocraft.trading_cells.platform.neoforge.mobfarm.LegacyMobFarmMigration;
import com.cosmocraft.trading_cells.platform.neoforge.machine.PortableMachineBlockEntity;
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
                new GameTestCase("mob_simulation_extractor_interaction", 40, MobSimulationGameTests::extractorInteraction),
                new GameTestCase("mob_simulation_extractor_rejections", 40, MobSimulationGameTests::extractorRejections),
                new GameTestCase("mob_simulation_extractor_last_use", 40, MobSimulationGameTests::extractorLastUse),
                new GameTestCase("mob_simulation_extractor_full_inventory", 40, MobSimulationGameTests::extractorFullInventory),
                new GameTestCase("mob_simulation_workbench_atomic_cost", 40, MobSimulationGameTests::workbench),
                new GameTestCase("mob_simulation_workbench_high_level_cost", 40, MobSimulationGameTests::highLevelWorkbench),
                new GameTestCase("mob_simulation_pending_loot_reload", 40, MobSimulationGameTests::pending),
                new GameTestCase("mob_simulation_filters_and_automation", 40, MobSimulationGameTests::automation),
                new GameTestCase("mob_simulation_legacy_farm_migration", 40, MobSimulationGameTests::legacyMigration),
                new GameTestCase("mob_simulation_legacy_pending_migration", 40, MobSimulationGameTests::legacyPending),
                new GameTestCase("mob_simulation_paused_migration_tick", 40, MobSimulationGameTests::pausedMigration));
    }

    private static void extractorInteraction(GameTestHelper helper) {
        for (String id : List.of("cow", "villager", "zombie", "warden")) {
            var player = connectedPlayer(helper);
            var tool = MobFarmRegistrationAdapter.ESSENCE_EXTRACTOR.get().getDefaultInstance();
            player.setItemInHand(InteractionHand.OFF_HAND, tool);
            player.getInventory().setItem(9, new ItemStack(Items.GLASS_BOTTLE, 2));
            var target = MobFarmLootTables.createTarget(helper.getLevel(), Identifier.withDefaultNamespace(id));
            float health = target.getHealth();
            var uuid = target.getUUID();
            helper.assertValueEqual(interact(player, target, InteractionHand.OFF_HAND), InteractionResult.SUCCESS,
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
            helper.assertTrue(EntityEssenceData.createEntity(helper.getLevel(), EntityEssenceData.moduleOf(essence)) != null,
                    "Extracted state creates a usable simulation module");
            for (int tick = 0; tick < 40; tick++) { player.getCooldowns().tick(); }
            helper.assertFalse(player.getCooldowns().isOnCooldown(tool), "Extractor cooldown expires after forty ticks");
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
        player.getInventory().setItem(9, new ItemStack(Items.GLASS_BOTTLE, 2));
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
        player.getInventory().setItem(9, new ItemStack(Items.GLASS_BOTTLE, 2));
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
        player.getInventory().setItem(9, new ItemStack(Items.GLASS_BOTTLE, 2));
        player.setItemInHand(InteractionHand.OFF_HAND, MobFarmRegistrationAdapter.ESSENCE_EXTRACTOR.get().getDefaultInstance());
        var target = MobFarmLootTables.createTarget(helper.getLevel(), Identifier.withDefaultNamespace("cow"));
        interact(player, target, InteractionHand.OFF_HAND);
        helper.assertValueEqual(essenceCount(player), 0, "Full inventory has no room for the essence");
        var dropped = helper.getLevel().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(3),
                item -> item.getItem().is(MobFarmRegistrationAdapter.ENTITY_ESSENCE.get()));
        helper.assertValueEqual(dropped.size(), 1, "Overflow essence drops once instead of being lost");
        helper.assertValueEqual(dropped.getFirst().getItem().getCount(), 1, "Dropped essence count");
        dropped.forEach(ItemEntity::discard);
        helper.succeed();
    }

    private static @org.jspecify.annotations.Nullable InteractionResult interact(ServerPlayer player, LivingEntity target, InteractionHand hand) {
        return CommonHooks.onInteractEntity(player, target, hand, net.minecraft.world.phys.Vec3.ZERO);
    }

    private static ItemStack extractedEssence(ServerPlayer player) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(MobFarmRegistrationAdapter.ENTITY_ESSENCE.get())) { return stack; }
        }
        return ItemStack.EMPTY;
    }

    private static int essenceCount(ServerPlayer player) {
        int count = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(MobFarmRegistrationAdapter.ENTITY_ESSENCE.get())) { count += stack.getCount(); }
        }
        return count;
    }

    private static PortableMachineBlockEntity legacyFarm(GameTestHelper helper, String id) {
        var block = BuiltInRegistries.BLOCK.getOptional(Identifier.fromNamespaceAndPath("trading_cells", id)).orElseThrow();
        helper.setBlock(GameTestFixtures.TEST_POS, block);
        return helper.getBlockEntity(GameTestFixtures.TEST_POS, PortableMachineBlockEntity.class);
    }

    private static void legacyMigration(GameTestHelper helper) {
        var ids = new java.util.ArrayList<>(List.of("skeleton_farm", "zombie_farm", "raider_farm", "creeper_farm"));
        for (var kind : com.cosmocraft.trading_cells.feature.configuredmobfarm.domain.model.ConfiguredMobFarmKind.values()) {
            ids.add(kind.blockId());
        }
        helper.assertValueEqual(ids.size(), 21, "All replaced farm IDs covered");
        for (String id : ids) {
            var old = legacyFarm(helper, id);
            var container = (net.minecraft.world.Container) old;
            ItemStack worker = GameTestFixtures.adultVillagerCapture(helper);
            container.setItem(0, worker.copy());
            container.setItem(1, Items.IRON_SWORD.getDefaultInstance());
            BlockEntityStateFixtures.fillIndexedSlots(helper, old, "Slot", 2, 18, new ItemStack(Items.COBBLESTONE, 37));
            CompoundTag data = old.saveWithFullMetadata(helper.getLevel().registryAccess());
            data.putInt("CycleTicks", 123);
            data.putInt("CycleDurationTicks", 1200);
            data.putInt("StoredExperience", 7654);
            data.putBoolean("Enabled", false);
            data.putString("MachineRedstoneMode", "low_signal_pauses");
            data.putInt("EnabledLootMask", 0);
            data.putInt("DisabledDynamicLootCount", 1);
            data.putString("DisabledDynamicLoot0", "minecraft:redstone");
            old.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), data));
            var redstone = old.redstoneMode();
            helper.assertTrue(LegacyMobFarmMigration.convert(old), "Migration succeeds for " + id);
            var farm = helper.getBlockEntity(GameTestFixtures.TEST_POS, MobFarmBlockEntity.class);
            helper.assertTrue(ItemStack.matches(farm.worker(), worker), "Worker data retained for " + id);
            helper.assertTrue(farm.getItem(1).is(Items.IRON_SWORD), "Sword retained for " + id);
            helper.assertTrue(EntityEssenceData.createEntity(helper.getLevel(), farm.creature()) != null, "Creature module restored for " + id);
            for (int slot = 5; slot < 23; slot++) {
                helper.assertTrue(farm.getItem(slot).is(Items.COBBLESTONE) && farm.getItem(slot).getCount() == 37,
                        "Output slot offset preserved for " + id);
            }
            helper.assertValueEqual(farm.storedExperience(), 7654, "XP unchanged during conversion");
            helper.assertValueEqual(farm.cycleTicks(), 123, "Exact unfinished progress retained");
            helper.assertFalse(farm.enabled(), "Manual pause retained");
            helper.assertValueEqual(farm.redstoneMode(), redstone, "Redstone mode retained");
            helper.assertFalse(farm.lootEnabled(Identifier.withDefaultNamespace("redstone")), "Dynamic filter retained");
            helper.assertFalse(LegacyMobFarmMigration.convert(farm), "Conversion is one-time");
        }
        helper.succeed();
    }

    private static void legacyPending(GameTestHelper helper) {
        var old = legacyFarm(helper, "skeleton_farm");
        var inventory = (net.minecraft.world.Container) old;
        inventory.setItem(0, GameTestFixtures.adultVillagerCapture(helper));
        inventory.setItem(1, Items.IRON_SWORD.getDefaultInstance());
        BlockEntityStateFixtures.fillIndexedSlots(helper, old, "Slot", 2, 18, new ItemStack(Items.COBBLESTONE, 64));
        var data = old.saveWithFullMetadata(helper.getLevel().registryAccess());
        var output = net.minecraft.world.level.storage.TagValueOutput.createWithContext(ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
        output.store("PendingLoot0", ItemStack.CODEC, new ItemStack(Items.DIAMOND, 3));
        data.merge(output.buildResult());
        data.putInt("PendingLootCount", 1);
        data.putBoolean("PendingLootReady", true);
        data.putInt("CycleTicks", 999);
        data.putInt("CycleDurationTicks", 1000);
        data.putInt("StoredExperience", 17);
        old.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), data));
        helper.assertTrue(LegacyMobFarmMigration.convert(old), "Legacy pending batch can migrate");
        var farm = helper.getBlockEntity(GameTestFixtures.TEST_POS, MobFarmBlockEntity.class);
        helper.assertValueEqual(farm.storedExperience(), 17, "Conversion itself does not pay unfinished work");
        farm.processTick();
        helper.assertValueEqual(farm.storedExperience(), 22, "Old unpaid batch settles once");
        helper.assertValueEqual(farm.getItem(1).getDamageValue(), 1, "Old batch wears sword once");
        data = farm.saveWithFullMetadata(helper.getLevel().registryAccess());
        helper.assertValueEqual(data.getIntOr("PendingLootCount", 0), 1, "Overflow is preserved, not discarded");
        farm.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), data));
        farm.removeItem(5, 64);
        farm.processTick();
        helper.assertTrue(farm.getItem(5).is(Items.DIAMOND) && farm.getItem(5).getCount() == 3, "Saved roll survives reload without reroll");
        helper.assertValueEqual(farm.storedExperience(), 22, "Draining migrated batch cannot pay twice");
        helper.assertValueEqual(farm.getItem(1).getDamageValue(), 1, "Draining migrated batch cannot charge twice");
        helper.succeed();
    }

    private static void pausedMigration(GameTestHelper helper) {
        var old = legacyFarm(helper, "creeper_farm");
        var data = old.saveWithFullMetadata(helper.getLevel().registryAccess());
        data.putBoolean("Enabled", false);
        data.putInt("CreeperKind", 1);
        data.putString("CreeperTarget", "trading_cells:charged_creeper");
        old.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), data));
        old.setRedstoneMode(com.cosmocraft.trading_cells.shared.machines.domain.model.MachineRedstoneMode.LOW_SIGNAL_PAUSES);
        helper.runAfterDelay(2, () -> {
            var farm = helper.getBlockEntity(GameTestFixtures.TEST_POS, MobFarmBlockEntity.class);
            helper.assertFalse(farm.enabled(), "Paused farm is converted by its actual block ticker");
            var target = EntityEssenceData.createEntity(helper.getLevel(), farm.creature());
            helper.assertTrue(target instanceof net.minecraft.world.entity.monster.Creeper creeper && creeper.isPowered(),
                    "Synthetic charged target becomes actual captured entity state");
            helper.succeed();
        });
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
        bench.setItem(1, new ItemStack(Items.AMETHYST_SHARD, 4));
        bench.setItem(2, new ItemStack(Items.IRON_INGOT, 4));
        helper.assertFalse(bench.synthesize(player), "Insufficient XP does not consume any ingredients");
        helper.assertValueEqual(bench.getItem(1).getCount(), 4, "Shards preserved on failure");
        player.giveExperiencePoints(1_000);
        BlockEntityStateFixtures.fillIndexedSlots(helper, bench, "Slot", 3, 1, Items.STONE.getDefaultInstance());
        helper.assertFalse(bench.synthesize(player), "Occupied result prevents crafting");
        helper.assertValueEqual(MinecraftExperience.totalPoints(player.experienceLevel, player.experienceProgress), 1_000,
                "Occupied output does not spend XP");
        bench.removeItem(3, 1);
        helper.assertTrue(bench.synthesize(player), "Valid requirements produce a reusable module");
        helper.assertValueEqual(MinecraftExperience.totalPoints(player.experienceLevel, player.experienceProgress), 700,
                "Normal synthesis charges exactly 300 points");
        helper.assertTrue(bench.getItem(0).isEmpty() && bench.getItem(1).isEmpty() && bench.getItem(2).isEmpty(), "One complete set consumed");
        helper.assertTrue(bench.getItem(3).is(MobFarmRegistrationAdapter.ENTITY_MODULE.get()), "Module delivered to output");
        helper.assertFalse(bench.synthesize(player), "Repeated button cannot duplicate module");
        helper.succeed();
    }

    private static void highLevelWorkbench(GameTestHelper helper) {
        helper.setBlock(GameTestFixtures.TEST_POS, MobFarmRegistrationAdapter.ESSENCE_WORKBENCH.get());
        var bench = helper.getBlockEntity(GameTestFixtures.TEST_POS, EssenceWorkbenchBlockEntity.class);
        var player = connectedPlayer(helper);
        var center = net.minecraft.world.phys.Vec3.atCenterOf(bench.getBlockPos());
        player.setPos(center);
        var warden = MobFarmLootTables.createTarget(helper.getLevel(), Identifier.withDefaultNamespace("warden"));
        bench.setItem(0, EntityEssenceData.essenceOf(warden));
        bench.setItem(1, new ItemStack(Items.AMETHYST_SHARD, 16));
        bench.setItem(2, new ItemStack(Items.IRON_INGOT, 64));
        player.giveExperiencePoints(3_000);
        helper.assertFalse(bench.synthesize(player), "High-level synthesis cannot substitute iron for netherite");
        helper.assertValueEqual(bench.getItem(2).getCount(), 64, "Wrong material is never consumed");
        bench.setItem(2, new ItemStack(Items.NETHERITE_INGOT, 2));
        bench.setItem(1, new ItemStack(Items.AMETHYST_SHARD, 15));
        helper.assertFalse(bench.synthesize(player), "High-level synthesis requires all sixteen shards");
        bench.setItem(1, new ItemStack(Items.AMETHYST_SHARD, 17));
        player.setPos(center.add(20, 0, 0));
        helper.assertFalse(bench.synthesize(player), "Distant requests cannot craft at the workbench");
        player.setPos(center);
        com.cosmocraft.trading_cells.platform.neoforge.experience.PlayerExperienceTransfer.removePoints(player, 1);
        helper.assertFalse(bench.synthesize(player), "2999 XP is insufficient for high-level synthesis");
        helper.assertValueEqual(bench.getItem(1).getCount(), 17, "Every failed request preserves shards");
        helper.assertValueEqual(bench.getItem(2).getCount(), 2, "Every failed request preserves netherite");
        helper.assertValueEqual(MinecraftExperience.totalPoints(player.experienceLevel, player.experienceProgress), 2_999,
                "Failed requests preserve XP");
        player.giveExperiencePoints(1);
        helper.assertTrue(bench.synthesize(player), "Exact high-level requirements produce the module");
        helper.assertTrue(bench.getItem(0).isEmpty(), "One essence consumed");
        helper.assertValueEqual(bench.getItem(1).getCount(), 1, "Exactly sixteen shards consumed");
        helper.assertValueEqual(bench.getItem(2).getCount(), 1, "Exactly one netherite ingot consumed");
        helper.assertValueEqual(MinecraftExperience.totalPoints(player.experienceLevel, player.experienceProgress), 0,
                "Exactly three thousand XP consumed");
        var module = bench.getItem(3);
        helper.assertTrue(EntityEssenceData.isHighLevel(module), "Output retains high-level classification");
        var simulated = EntityEssenceData.createEntity(helper.getLevel(), module);
        helper.assertTrue(simulated != null && simulated.getType() == warden.getType(), "Output restores the warden");
        helper.assertFalse(bench.synthesize(player), "Repeated requests cannot duplicate high-level modules");
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

    private static net.minecraft.server.level.ServerPlayer connectedPlayer(GameTestHelper helper) {
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
        return player;
    }
}
