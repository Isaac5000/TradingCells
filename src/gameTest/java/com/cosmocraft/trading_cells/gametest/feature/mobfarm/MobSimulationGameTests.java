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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.TagValueInput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

public final class MobSimulationGameTests {
    private MobSimulationGameTests() { }
    public static List<GameTestCase> tests() {
        return List.of(new GameTestCase("mob_simulation_actual_module_state", 40, MobSimulationGameTests::moduleState),
                new GameTestCase("mob_simulation_workbench_atomic_cost", 40, MobSimulationGameTests::workbench),
                new GameTestCase("mob_simulation_pending_loot_reload", 40, MobSimulationGameTests::pending),
                new GameTestCase("mob_simulation_filters_and_automation", 40, MobSimulationGameTests::automation),
                new GameTestCase("mob_simulation_legacy_farm_migration", 40, MobSimulationGameTests::legacyMigration),
                new GameTestCase("mob_simulation_legacy_pending_migration", 40, MobSimulationGameTests::legacyPending),
                new GameTestCase("mob_simulation_paused_migration_tick", 40, MobSimulationGameTests::pausedMigration));
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
