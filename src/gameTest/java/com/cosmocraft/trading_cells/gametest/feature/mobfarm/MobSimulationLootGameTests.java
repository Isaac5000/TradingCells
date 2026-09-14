package com.cosmocraft.trading_cells.gametest.feature.mobfarm;

import com.cosmocraft.trading_cells.feature.combat.adapters.api.CombatEnchantments;
import com.cosmocraft.trading_cells.feature.combat.adapters.api.CombatItems;
import com.cosmocraft.trading_cells.feature.mobfarm.adapters.input.EntityEssenceData;
import com.cosmocraft.trading_cells.feature.mobfarm.adapters.input.MobFarmBlockEntity;
import com.cosmocraft.trading_cells.feature.mobfarm.adapters.output.MobFarmRegistrationAdapter;
import com.cosmocraft.trading_cells.gametest.shared.BlockEntityStateFixtures;
import com.cosmocraft.trading_cells.gametest.shared.GameTestCase;
import com.cosmocraft.trading_cells.gametest.shared.GameTestFixtures;
import com.cosmocraft.trading_cells.platform.neoforge.mobfarm.MobFarmLootTables;
import com.cosmocraft.trading_cells.platform.neoforge.mobfarm.MobFarmSimulationLoot;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.storage.TagValueInput;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

public final class MobSimulationLootGameTests {
    private MobSimulationLootGameTests() { }

    public static List<GameTestCase> tests() {
        return List.of(
                new GameTestCase("mob_simulation_charged_rewards", 40, MobSimulationLootGameTests::chargedRewards),
                new GameTestCase("mob_simulation_heads_and_linked_tables", 40, MobSimulationLootGameTests::heads),
                new GameTestCase("mob_simulation_reward_failure_context", 40, MobSimulationLootGameTests::failure),
                new GameTestCase("mob_simulation_equipment_profiles", 40, MobSimulationLootGameTests::equipment),
                new GameTestCase("mob_simulation_ominous_rewards", 40, MobSimulationLootGameTests::ominous),
                new GameTestCase("mob_simulation_observed_filter_persistence", 40, MobSimulationLootGameTests::filters));
    }

    private static LivingEntity target(GameTestHelper helper, String type, String table, boolean charged) {
        var target = MobFarmLootTables.createTarget(helper.getLevel(), Identifier.withDefaultNamespace(type));
        var state = new CompoundTag();
        state.putString("DeathLootTable", "trading_cells_gametest:" + table);
        state.putBoolean("powered", charged);
        target.load(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), state));
        return target;
    }

    private static ItemStack sword(GameTestHelper helper, int decapitation, int looting) {
        var sword = Items.IRON_SWORD.getDefaultInstance();
        var enchantments = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        var registry = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        enchantments.set(registry.getOrThrow(CombatEnchantments.DECAPITATION), decapitation);
        enchantments.set(registry.getOrThrow(Enchantments.LOOTING), looting);
        EnchantmentHelper.setEnchantments(sword, enchantments.toImmutable());
        return sword;
    }

    private static List<ItemStack> roll(GameTestHelper helper, LivingEntity target, ItemStack sword, int kills) {
        var result = new ArrayList<ItemStack>();
        MobFarmSimulationLoot.roll(helper.getLevel(), target, sword, kills, (kill, stack) -> result.add(stack.copy()));
        return result;
    }

    private static int count(List<ItemStack> loot, Item item) {
        return loot.stream().filter(stack -> stack.is(item)).mapToInt(ItemStack::getCount).sum();
    }

    private static void chargedRewards(GameTestHelper helper) {
        var target = target(helper, "creeper", "captured_entity", true);
        var sword = sword(helper, 100, 0);
        var loot = roll(helper, target, sword, 4);
        helper.assertValueEqual(count(loot, Items.DIAMOND), 4, "Actual custom table remains authoritative");
        helper.assertValueEqual(count(loot, Items.CREEPER_HEAD), 4, "Decapitation applies once per kill");
        helper.assertValueEqual(count(loot, CombatItems.stormShard()), 4, "Charged instance retains storm shards");
        helper.assertValueEqual(count(loot, Items.GUNPOWDER), 0, "Default creeper table is not rolled again");
        var preview = MobFarmSimulationLoot.preview(helper.getLevel(), target, sword(helper, 1, 3), 4);
        var storm = preview.get(BuiltInRegistries.ITEM.getKey(CombatItems.stormShard()));
        helper.assertValueEqual(storm.probability(), 1_000_000, "Charged reward is guaranteed");
        helper.assertValueEqual(storm.minimum(), 4, "One shard minimum per kill");
        helper.assertValueEqual(storm.maximum(), 16, "Looting controls shard quantity");
        var head = MobFarmSimulationLoot.preview(helper.getLevel(), target, sword(helper, 1, 0), 1)
                .get(Identifier.withDefaultNamespace("creeper_head"));
        helper.assertValueEqual(head.probability(), 35_000, "Ordinary Decapitation I keeps 3.5 percent chance");
        var normal = target(helper, "creeper", "captured_entity", false);
        loot = roll(helper, normal, Items.IRON_SWORD.getDefaultInstance(), 4);
        helper.assertValueEqual(count(loot, CombatItems.stormShard()), 0, "Normal creepers never gain charged rewards");
        helper.assertValueEqual(count(loot, Items.CREEPER_HEAD), 0, "Unenchanted sword cannot decapitate normal creeper");
        var legacyNative = MobFarmLootTables.batches(helper.getLevel(), target, sword, 1);
        helper.assertValueEqual(legacyNative.getFirst().size(), 1, "Shared legacy native runner gains no duplicate supplements");
        helper.succeed();
    }

    private static void heads(GameTestHelper helper) {
        for (String type : List.of("creeper", "wither_skeleton")) {
            var target = target(helper, type, "simulation_linked", false);
            var sword = sword(helper, 100, 0);
            var loot = roll(helper, target, sword, 4);
            helper.assertValueEqual(count(loot, Items.CREEPER_HEAD), 4, "Native head not duplicated for " + type);
            helper.assertValueEqual(count(loot, Items.WITHER_SKELETON_SKULL), 4, "Native skull not duplicated for " + type);
            var ids = MobFarmSimulationLoot.filterItems(target, sword);
            helper.assertTrue(ids.contains(Identifier.withDefaultNamespace("wither_skeleton_skull")), "Linked items filterable before first roll");
            var preview = MobFarmSimulationLoot.preview(helper.getLevel(), target, sword, 4);
            var skull = preview.get(Identifier.withDefaultNamespace("wither_skeleton_skull"));
            helper.assertValueEqual(skull.probability(), 1_000_000, "Linked table probability retained");
            helper.assertValueEqual(skull.maximum(), 4, "Preview never adds a second native head");
        }
        var wither = MobFarmLootTables.createTarget(helper.getLevel(), Identifier.withDefaultNamespace("wither_skeleton"));
        var preview = MobFarmSimulationLoot.preview(helper.getLevel(), wither, sword(helper, 1, 0), 1);
        helper.assertValueEqual(preview.get(Identifier.withDefaultNamespace("wither_skeleton_skull")).probability(),
                35_000, "Conditional supplement raises vanilla 2.5 percent to 3.5, not 5.9");
        helper.assertValueEqual(preview.get(Identifier.withDefaultNamespace("coal")).probability(),
                333333, "Negative and zero coal counts are both empty outcomes");
        preview = MobFarmSimulationLoot.preview(helper.getLevel(), wither, sword(helper, 1, 3), 1);
        helper.assertValueEqual(preview.get(Identifier.withDefaultNamespace("coal")).probability(),
                888889, "Looting increases zero-clamped negative coal counts");
        helper.assertValueEqual(preview.get(Identifier.withDefaultNamespace("coal")).maximum(), 4, "Coal looting bound");
        helper.assertValueEqual(preview.get(Identifier.withDefaultNamespace("wither_skeleton_skull")).probability(),
                65_000, "Looting and Decapitation both supplement native skull chance");
        helper.succeed();
    }

    private static void failure(GameTestHelper helper) {
        var attacker = FakePlayerFactory.getMinecraft(helper.getLevel());
        var original = attacker.getMainHandItem().copy();
        var sentinel = Items.GOLDEN_AXE.getDefaultInstance();
        attacker.setItemSlot(EquipmentSlot.MAINHAND, sentinel);
        try {
            var expected = new IllegalStateException("supplement consumer failure");
            boolean thrown = false;
            try {
                MobFarmSimulationLoot.roll(helper.getLevel(), target(helper, "creeper", "captured_entity", true),
                        Items.IRON_SWORD.getDefaultInstance(), 1, (kill, stack) -> {
                            if (stack.is(CombatItems.stormShard())) { throw expected; }
                        });
            } catch (IllegalStateException actual) { thrown = actual == expected; }
            helper.assertTrue(thrown, "Farm retains its atomic failure policy for additional rewards");
            helper.assertTrue(ItemStack.matches(attacker.getMainHandItem(), sentinel), "Failure restores shared fake player sword");
        } finally { attacker.setItemSlot(EquipmentSlot.MAINHAND, original); }
        helper.succeed();
    }

    private static void equipment(GameTestHelper helper) {
        var sword = sword(helper, 0, 100);
        for (String type : List.of("skeleton", "stray", "bogged", "parched", "wither_skeleton", "piglin_brute")) {
            var target = target(helper, type, "captured_entity", false);
            Item expected = type.equals("wither_skeleton") ? Items.STONE_SWORD
                    : type.equals("piglin_brute") ? Items.GOLDEN_AXE : Items.BOW;
            var loot = roll(helper, target, sword, 4);
            helper.assertValueEqual(count(loot, expected), 4, "Inherited equipment and Looting retained for " + type);
            for (ItemStack stack : loot) {
                if (stack.is(expected)) {
                    helper.assertTrue(stack.getDamageValue() >= stack.getMaxDamage() / 2
                            && stack.getDamageValue() < stack.getMaxDamage(), "Equipment is worn but not broken");
                }
            }
            var preview = MobFarmSimulationLoot.preview(helper.getLevel(), target, sword, 4).get(BuiltInRegistries.ITEM.getKey(expected));
            helper.assertValueEqual(preview.minimum(), 4, "Guaranteed weapon minimum shown for " + type);
            helper.assertValueEqual(preview.maximum(), 4, "Weapon maximum shown for " + type);
        }
        for (String type : List.of("piglin", "zombified_piglin")) {
            var target = target(helper, type, "captured_entity", false);
            target.getRandom().setSeed(12345);
            int[] weapons = new int[256];
            MobFarmSimulationLoot.roll(helper.getLevel(), target, sword, 256, (kill, stack) -> {
                if (stack.is(Items.CROSSBOW) || stack.is(Items.GOLDEN_SWORD) || stack.is(Items.GOLDEN_SPEAR)) { weapons[kill]++; }
            });
            for (int count : weapons) { helper.assertValueEqual(count, 1, "Mutually exclusive weapon variants for " + type); }
        }
        var zombie = target(helper, "zombie", "captured_entity", false);
        var preview = MobFarmSimulationLoot.preview(helper.getLevel(), zombie, sword(helper, 0, 0), 1);
        double chance = com.cosmocraft.trading_cells.feature.zombiefarm.domain.model.ZombieFarmDropRules.zombieSwordChance(
                0, helper.getLevel().getDifficulty() == net.minecraft.world.Difficulty.HARD);
        helper.assertValueEqual(preview.get(Identifier.withDefaultNamespace("iron_sword")).probability(),
                (int) Math.round(chance * 1_000_000), "Zombie spawn chance includes current difficulty");
        var drowned = target(helper, "drowned", "captured_entity", false);
        preview = MobFarmSimulationLoot.preview(helper.getLevel(), drowned, sword(helper, 0, 0), 1);
        helper.assertValueEqual(preview.get(Identifier.withDefaultNamespace("nautilus_shell")).probability(), 30_000,
                "Drowned shell supplement keeps its 3 percent chance");
        var piglin = target(helper, "piglin", "captured_entity", false);
        preview = MobFarmSimulationLoot.preview(helper.getLevel(), piglin, sword(helper, 0, 0), 1);
        helper.assertValueEqual(preview.get(Identifier.withDefaultNamespace("golden_helmet")).probability(), 8_500,
                "Piglin armor includes natural equipment spawn chance");
        var brute = target(helper, "piglin_brute", "simulation_equipment", false);
        helper.assertValueEqual(count(roll(helper, brute, sword, 4), Items.GOLDEN_AXE), 4, "Native equipment is not duplicated");
        helper.succeed();
    }

    private static void ominous(GameTestHelper helper) {
        var banner = BuiltInRegistries.ITEM.getOptional(Identifier.withDefaultNamespace("white_banner")).orElseThrow();
        var pillager = target(helper, "pillager", "captured_entity", false);
        var sword = sword(helper, 0, 100);
        var loot = roll(helper, pillager, sword, 4);
        helper.assertValueEqual(count(loot, Items.DIAMOND), 4, "Pillager instance table retained");
        helper.assertValueEqual(count(loot, Items.CROSSBOW), 4, "Pillager weapon profile retained");
        helper.assertValueEqual(count(loot, Items.OMINOUS_BOTTLE), 4, "One ominous bottle per pillager");
        helper.assertValueEqual(count(loot, banner), 4, "One ominous banner per pillager");
        var expectedBanner = com.cosmocraft.trading_cells.feature.raiderfarm.adapters.input.RaiderFarmLootAdapter
                .ominousBanner(helper.getLevel().registryAccess());
        for (ItemStack stack : loot) {
            if (stack.is(banner)) {
                helper.assertTrue(ItemStack.isSameItemSameComponents(stack, expectedBanner), "Ominous banner retains patterns and name");
            } else if (stack.is(Items.OMINOUS_BOTTLE)) {
                var amplifier = stack.get(DataComponents.OMINOUS_BOTTLE_AMPLIFIER);
                helper.assertTrue(amplifier != null && amplifier.value() >= 0 && amplifier.value() <= 4, "Bottle amplifier is I to V");
            }
        }
        pillager = target(helper, "pillager", "simulation_equipment", false);
        loot = roll(helper, pillager, sword, 4);
        helper.assertValueEqual(count(loot, Items.OMINOUS_BOTTLE), 4, "Native bottle not duplicated");
        helper.assertValueEqual(count(loot, banner), 4, "Native banner not duplicated");
        var preview = MobFarmSimulationLoot.preview(helper.getLevel(), pillager, sword, 4);
        helper.assertValueEqual(preview.get(Identifier.withDefaultNamespace("ominous_bottle")).maximum(), 4,
                "Preview does not double native ominous drops");
        helper.succeed();
    }

    private static void filters(GameTestHelper helper) {
        helper.setBlock(GameTestFixtures.TEST_POS, MobFarmRegistrationAdapter.BLOCK.get());
        var farm = helper.getBlockEntity(GameTestFixtures.TEST_POS, MobFarmBlockEntity.class);
        farm.setItem(0, GameTestFixtures.adultVillagerCapture(helper));
        farm.setItem(1, Items.IRON_SWORD.getDefaultInstance());
        farm.setItem(2, EntityEssenceData.moduleOf(EntityEssenceData.essenceOf(target(helper, "creeper", "captured_entity", true))));
        farm.processTick();
        Identifier storm = BuiltInRegistries.ITEM.getKey(CombatItems.stormShard());
        helper.assertTrue(farm.lootItems().contains(storm), "Supplement can be disabled before first cycle");
        farm.toggleLoot(storm);
        BlockEntityStateFixtures.setInt(helper, farm, "CycleTicks", farm.cycleDurationTicks() - 1);
        farm.processTick();
        helper.assertValueEqual(farm.storedExperience(), 5, "Filtering rewards preserves XP");
        for (int slot = 5; slot < 23; slot++) {
            helper.assertFalse(farm.getItem(slot).is(CombatItems.stormShard()), "Disabled supplement is discarded");
        }
        Identifier discovered = Identifier.withDefaultNamespace("emerald");
        farm.rememberLoot(List.of(discovered));
        farm.toggleLoot(discovered);
        farm.setItem(1, Items.DIAMOND_SWORD.getDefaultInstance());
        farm.processTick();
        helper.assertTrue(farm.lootItems().contains(discovered), "Weapon refresh retains discovered external loot");
        var state = farm.saveWithFullMetadata(helper.getLevel().registryAccess());
        farm.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), state));
        farm.processTick();
        helper.assertTrue(farm.lootItems().contains(discovered), "Observed filter survives save/reload");
        helper.assertFalse(farm.lootEnabled(discovered), "Disabled state survives refresh/reload");
        farm.setItem(2, EntityEssenceData.moduleOf(EntityEssenceData.essenceOf(target(helper, "cow", "captured_entity", false))));
        farm.processTick();
        helper.assertFalse(farm.lootItems().contains(discovered), "Another module does not inherit irrelevant discovered items");
        helper.succeed();
    }
}
