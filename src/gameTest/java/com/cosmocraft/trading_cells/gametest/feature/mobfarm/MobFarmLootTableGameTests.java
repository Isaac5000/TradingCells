package com.cosmocraft.trading_cells.gametest.feature.mobfarm;

import com.cosmocraft.trading_cells.gametest.shared.GameTestCase;
import com.cosmocraft.trading_cells.platform.neoforge.mobfarm.MobFarmLootTables;
import com.cosmocraft.trading_cells.platform.neoforge.mobfarm.MobFarmSimulationLoot;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.TagValueInput;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

public final class MobFarmLootTableGameTests {
    private static final ResourceKey<LootTable> CAPTURED_TABLE = ResourceKey.create(Registries.LOOT_TABLE,
            Identifier.fromNamespaceAndPath("trading_cells_gametest", "captured_entity"));

    private MobFarmLootTableGameTests() { }

    public static List<GameTestCase> tests() {
        return List.of(
                new GameTestCase("mobfarm_actual_entity_loot_table", 40, MobFarmLootTableGameTests::actualEntity),
                new GameTestCase("mobfarm_loot_context_restored_on_failure", 40, MobFarmLootTableGameTests::failure),
                new GameTestCase("mobfarm_loot_missing_targets", 40, MobFarmLootTableGameTests::missingTargets),
                new GameTestCase("mobfarm_wither_simulation_loot", 40, MobFarmLootTableGameTests::witherSimulation));
    }

    private static Cow capturedTarget(GameTestHelper helper) {
        var target = (Cow) MobFarmLootTables.createTarget(helper.getLevel(), Identifier.withDefaultNamespace("cow"));
        var state = new CompoundTag();
        state.putString("DeathLootTable", CAPTURED_TABLE.identifier().toString());
        target.load(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), state));
        return target;
    }

    private static void actualEntity(GameTestHelper helper) {
        var level = helper.getLevel();
        var attacker = FakePlayerFactory.getMinecraft(level);
        ItemStack original = attacker.getMainHandItem().copy();
        ItemStack sentinel = Items.GOLDEN_AXE.getDefaultInstance();
        attacker.setItemSlot(EquipmentSlot.MAINHAND, sentinel);
        try {
            var batches = MobFarmLootTables.batches(level, capturedTarget(helper), Items.IRON_SWORD.getDefaultInstance(), 4);
            helper.assertValueEqual(batches.size(), 4, "One batch per kill");
            for (var batch : batches) {
                helper.assertValueEqual(batch.size(), 1, "Instance table overrides cow's default table");
                helper.assertTrue(batch.getFirst().is(Items.DIAMOND), "Custom namespace and player-kill condition respected");
                helper.assertValueEqual(batch.getFirst().getCount(), 1, "Native table amount retained");
            }
            helper.assertTrue(ItemStack.isSameItemSameComponents(attacker.getMainHandItem(), sentinel), "Attacker weapon restored");
        } finally {
            attacker.setItemSlot(EquipmentSlot.MAINHAND, original);
        }
        helper.succeed();
    }

    private static void failure(GameTestHelper helper) {
        var level = helper.getLevel();
        var attacker = FakePlayerFactory.getMinecraft(level);
        ItemStack original = attacker.getMainHandItem().copy();
        ItemStack sentinel = Items.STONE_SWORD.getDefaultInstance();
        attacker.setItemSlot(EquipmentSlot.MAINHAND, sentinel);
        try {
            var expected = new IllegalStateException("test consumer failure");
            boolean thrown = false;
            try {
                MobFarmLootTables.roll(level, capturedTarget(helper), Items.DIAMOND_SWORD.getDefaultInstance(), 1,
                        (kill, stack) -> { throw expected; });
            } catch (IllegalStateException actual) {
                thrown = actual == expected;
            }
            helper.assertTrue(thrown, "Farm chooses its own failure policy");
            helper.assertTrue(ItemStack.isSameItemSameComponents(attacker.getMainHandItem(), sentinel), "Failure cannot leak equipped weapon");
        } finally {
            attacker.setItemSlot(EquipmentSlot.MAINHAND, original);
        }
        helper.succeed();
    }

    private static void missingTargets(GameTestHelper helper) {
        var level = helper.getLevel();
        helper.assertTrue(MobFarmLootTables.createTarget(level, Identifier.withDefaultNamespace("missing_farm_entity")) == null,
                "Unknown IDs do not fall back to another entity");
        helper.assertTrue(MobFarmLootTables.createTarget(level, Identifier.withDefaultNamespace("item")) == null,
                "Non-living entities cannot be farmed");
        helper.assertTrue(MobFarmLootTables.batches(level, null, ItemStack.EMPTY, 1).isEmpty(), "Missing target has no drops");
        var empty = new ArmorStand(EntityTypes.ARMOR_STAND, level) {
            @Override
            public Optional<ResourceKey<LootTable>> getLootTable() { return Optional.empty(); }
        };
        helper.assertTrue(MobFarmLootTables.batches(level, empty, ItemStack.EMPTY, 1).isEmpty(), "No invented table when entity has none");
        helper.assertTrue(MobFarmLootTables.batches(level, capturedTarget(helper), ItemStack.EMPTY, 0).isEmpty(), "Zero kills do not roll");
        helper.succeed();
    }

    private static void witherSimulation(GameTestHelper helper) {
        var target = MobFarmLootTables.createTarget(helper.getLevel(), Identifier.withDefaultNamespace("wither"));
        helper.assertTrue(target instanceof WitherBoss, "Wither target is created");
        helper.assertValueEqual(((WitherBoss) target).getInvulnerableTicks(), 0,
                "Detached Wither is in its normal post-spawn state");
        var loot = new java.util.ArrayList<ItemStack>();
        MobFarmSimulationLoot.roll(helper.getLevel(), target, Items.IRON_SWORD.getDefaultInstance(), 1,
                (kill, stack) -> loot.add(stack.copy()));
        helper.assertTrue(loot.stream().anyMatch(stack -> stack.is(Items.NETHER_STAR)),
                "Wither simulation includes its custom death loot");
        helper.succeed();
    }
}
