package com.cosmocraft.trading_cells.gametest.feature.configuredmobfarm;

import com.cosmocraft.trading_cells.feature.configuredmobfarm.adapters.input.ConfiguredMobFarmBlockEntity;
import com.cosmocraft.trading_cells.feature.configuredmobfarm.adapters.input.ConfiguredMobFarmMenu;
import com.cosmocraft.trading_cells.feature.configuredmobfarm.adapters.input.ConfiguredMobFarmTargetCatalog;
import com.cosmocraft.trading_cells.feature.configuredmobfarm.adapters.output.ConfiguredMobFarmRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.configuredmobfarm.domain.model.ConfiguredMobFarmKind;
import com.cosmocraft.trading_cells.feature.configuredmobfarm.domain.model.ConfiguredMobFarmLoot;
import com.cosmocraft.trading_cells.gametest.shared.GameTestCase;
import com.cosmocraft.trading_cells.gametest.shared.GameTestFixtures;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

/** Cross-family contracts for the shared configurable mob-farm implementation. */
public final class ConfiguredMobFarmGameTests {
    private ConfiguredMobFarmGameTests() {
    }

    public static List<GameTestCase> tests() {
        return List.of(
                new GameTestCase("configured_mob_farm_family_contracts", 20,
                        ConfiguredMobFarmGameTests::familyContracts),
                new GameTestCase("configured_mob_farm_menu_loot_contracts", 20,
                        ConfiguredMobFarmGameTests::menuLootContracts),
                new GameTestCase("configured_mob_farm_cycles_and_persistence", 20,
                        ConfiguredMobFarmGameTests::cyclesAndPersistence),
                new GameTestCase("configured_mob_farm_piglin_equipment", 20,
                        ConfiguredMobFarmGameTests::piglinEquipment)
        );
    }

    private static void familyContracts(GameTestHelper helper) {
        for (ConfiguredMobFarmKind kind : ConfiguredMobFarmKind.values()) {
            helper.setBlock(GameTestFixtures.TEST_POS, ConfiguredMobFarmRegistrationAdapter.block(kind).get());
            ConfiguredMobFarmBlockEntity farm = farm(helper);
            helper.assertValueEqual(farm.selectedKind(), kind, kind + " block family");
            helper.assertTrue(!ConfiguredMobFarmTargetCatalog.targets(kind).isEmpty(),
                    kind + " must expose at least one target");
            helper.assertTrue(
                    ConfiguredMobFarmTargetCatalog.isKnownTarget(kind, farm.selectedTargetId()),
                    kind + " default target must belong to its own family"
            );

            ResourceHandler<ItemResource> input = helper.requireCapability(
                    Capabilities.Item.BLOCK,
                    GameTestFixtures.TEST_POS,
                    Direction.UP
            );
            ResourceHandler<ItemResource> output = helper.requireCapability(
                    Capabilities.Item.BLOCK,
                    GameTestFixtures.TEST_POS,
                    Direction.DOWN
            );
            helper.assertValueEqual(input.size(), 2, kind + " automated input slots");
            helper.assertValueEqual(output.size(), ConfiguredMobFarmBlockEntity.OUTPUT_SLOT_COUNT,
                    kind + " automated output slots");
        }

        assertTarget(helper, ConfiguredMobFarmKind.SLIME, "minecraft:sulfur_cube", true);
        assertTarget(helper, ConfiguredMobFarmKind.GHAST, "minecraft:happy_ghast", true);
        assertTarget(helper, ConfiguredMobFarmKind.PIGLIN, "minecraft:hoglin", false);
        helper.succeed();
    }

    private static void menuLootContracts(GameTestHelper helper) {
        var player = helper.makeMockServerPlayer(GameType.SURVIVAL);
        int containerId = 1;
        for (ConfiguredMobFarmKind kind : ConfiguredMobFarmKind.values()) {
            helper.setBlock(GameTestFixtures.TEST_POS, ConfiguredMobFarmRegistrationAdapter.block(kind).get());
            ConfiguredMobFarmBlockEntity farm = farm(helper);
            for (var target : ConfiguredMobFarmTargetCatalog.targets(kind)) {
                farm.selectTarget(target.entityTypeId());
                ConfiguredMobFarmMenu menu = (ConfiguredMobFarmMenu) farm.createMenu(
                        containerId++,
                        player.getInventory(),
                        player
                );
                helper.assertValueEqual(
                        menu.selectedTargetId(),
                        target.entityTypeId(),
                        kind + " menu target"
                );
                helper.assertTrue(!menu.isLootAvailable(null),
                        kind + " must reject a missing loot category");
                helper.assertTrue(!menu.isLootEnabled(null),
                        kind + " must not enable a missing loot category");
                for (ConfiguredMobFarmLoot loot : ConfiguredMobFarmLoot.values()) {
                    menu.isLootAvailable(loot);
                    menu.isLootEnabled(loot);
                }
                for (ItemStack stack : menu.dynamicLootOptions()) {
                    helper.assertTrue(!stack.isEmpty(), kind + " dynamic loot must not be empty");
                    menu.isDynamicLootEnabled(stack);
                }
            }
        }

        helper.setBlock(
                GameTestFixtures.TEST_POS,
                ConfiguredMobFarmRegistrationAdapter.block(ConfiguredMobFarmKind.PIGLIN).get()
        );
        ConfiguredMobFarmBlockEntity piglinFarm = farm(helper);
        piglinFarm.selectTarget(Identifier.withDefaultNamespace("piglin"));
        ConfiguredMobFarmMenu piglinMenu = (ConfiguredMobFarmMenu) piglinFarm.createMenu(
                containerId,
                player.getInventory(),
                player
        );
        helper.assertTrue(contains(piglinMenu.dynamicLootOptions(), Items.GOLDEN_SWORD),
                "Piglin equipment must use the dynamic loot path");
        helper.assertTrue(contains(piglinMenu.dynamicLootOptions(), Items.CROSSBOW),
                "Piglin ranged equipment must use the dynamic loot path");
        helper.assertTrue(contains(piglinMenu.dynamicLootOptions(), Items.GOLDEN_SPEAR),
                "Piglin spear equipment must use the dynamic loot path");
        helper.assertTrue(contains(piglinMenu.dynamicLootOptions(), Items.GOLDEN_HELMET),
                "Piglin helmet must use the dynamic loot path");
        helper.assertTrue(contains(piglinMenu.dynamicLootOptions(), Items.GOLDEN_CHESTPLATE),
                "Piglin chestplate must use the dynamic loot path");
        helper.assertTrue(contains(piglinMenu.dynamicLootOptions(), Items.GOLDEN_LEGGINGS),
                "Piglin leggings must use the dynamic loot path");
        helper.assertTrue(contains(piglinMenu.dynamicLootOptions(), Items.GOLDEN_BOOTS),
                "Piglin boots must use the dynamic loot path");

        helper.setBlock(
                GameTestFixtures.TEST_POS,
                ConfiguredMobFarmRegistrationAdapter.block(ConfiguredMobFarmKind.GHAST).get()
        );
        ConfiguredMobFarmBlockEntity ghastFarm = farm(helper);
        ghastFarm.selectTarget(Identifier.withDefaultNamespace("ghast"));
        ConfiguredMobFarmMenu ghastMenu = (ConfiguredMobFarmMenu) ghastFarm.createMenu(
                containerId + 1,
                player.getInventory(),
                player
        );
        helper.assertTrue(!contains(ghastMenu.dynamicLootOptions(), Items.MUSIC_DISC_TEARS),
                "The reflected-fireball Ghast music disc must not be offered by the farm");
        helper.succeed();
    }

    private static void cyclesAndPersistence(GameTestHelper helper) {
        ItemStack worker = GameTestFixtures.adultVillagerCapture(helper);
        for (ConfiguredMobFarmKind kind : ConfiguredMobFarmKind.values()) {
            helper.setBlock(GameTestFixtures.TEST_POS, ConfiguredMobFarmRegistrationAdapter.block(kind).get());
            ConfiguredMobFarmBlockEntity farm = farm(helper);
            Identifier selected = ConfiguredMobFarmTargetCatalog.targets(kind).getLast().entityTypeId();
            farm.selectTarget(selected);
            farm.setItem(ConfiguredMobFarmBlockEntity.WORKER_SLOT, worker);
            farm.setItem(ConfiguredMobFarmBlockEntity.SWORD_SLOT, new ItemStack(Items.WOODEN_SWORD));
            farm.dataAccess().set(3, 0);
            farm.dataAccess().set(0, farm.cycleDurationTicks() - 1);
            farm.processTick();
            helper.assertTrue(farm.dataAccess().get(4) > 0,
                    kind + " must complete an XP-only cycle");

            ConfiguredMobFarmBlockEntity restored = reload(helper, farm);
            helper.assertValueEqual(restored.selectedKind(), kind, kind + " persisted family");
            helper.assertValueEqual(restored.selectedTargetId(), selected, kind + " persisted target");
            helper.assertValueEqual(restored.dataAccess().get(3), 0, kind + " persisted loot filters");
            helper.assertTrue(ItemStack.isSameItemSameComponents(
                    restored.getItem(ConfiguredMobFarmBlockEntity.WORKER_SLOT),
                    worker
            ), kind + " persisted worker");
        }
        helper.succeed();
    }

    private static void piglinEquipment(GameTestHelper helper) {
        helper.setBlock(
                GameTestFixtures.TEST_POS,
                ConfiguredMobFarmRegistrationAdapter.block(ConfiguredMobFarmKind.PIGLIN).get()
        );
        ConfiguredMobFarmBlockEntity farm = farm(helper);
        farm.selectTarget(Identifier.withDefaultNamespace("piglin"));
        farm.setItem(ConfiguredMobFarmBlockEntity.WORKER_SLOT, GameTestFixtures.adultVillagerCapture(helper));
        farm.setItem(ConfiguredMobFarmBlockEntity.SWORD_SLOT, new ItemStack(Items.NETHERITE_SWORD));

        boolean foundWeapon = false;
        boolean foundArmor = false;
        for (int cycle = 0; cycle < 1_000 && (!foundWeapon || !foundArmor); cycle++) {
            farm.dataAccess().set(0, farm.cycleDurationTicks() - 1);
            farm.processTick();
            for (int slot = ConfiguredMobFarmBlockEntity.FIRST_OUTPUT_SLOT;
                    slot < ConfiguredMobFarmBlockEntity.CONTAINER_SIZE;
                    slot++) {
                ItemStack stack = farm.removeItemNoUpdate(slot);
                if (isPiglinWeapon(stack)) {
                    helper.assertTrue(stack.getDamageValue() > 0,
                            "Piglin weapons must retain natural wear");
                    foundWeapon = true;
                } else if (isPiglinArmor(stack)) {
                    helper.assertTrue(stack.getDamageValue() > 0,
                            "Piglin armor must retain natural wear");
                    foundArmor = true;
                }
            }
        }
        helper.assertTrue(foundWeapon, "Piglin farm did not produce equipment during the sample");
        helper.assertTrue(foundArmor, "Piglin farm did not produce natural gold armor during the sample");

        farm.selectTarget(Identifier.withDefaultNamespace("piglin_brute"));
        boolean foundBruteAxe = false;
        for (int cycle = 0; cycle < 500 && !foundBruteAxe; cycle++) {
            farm.dataAccess().set(0, farm.cycleDurationTicks() - 1);
            farm.processTick();
            for (int slot = ConfiguredMobFarmBlockEntity.FIRST_OUTPUT_SLOT;
                    slot < ConfiguredMobFarmBlockEntity.CONTAINER_SIZE;
                    slot++) {
                ItemStack stack = farm.removeItemNoUpdate(slot);
                if (stack.is(Items.GOLDEN_AXE)) {
                    helper.assertTrue(stack.getDamageValue() > 0,
                            "Piglin Brute axes must retain natural wear");
                    foundBruteAxe = true;
                }
            }
        }
        helper.assertTrue(foundBruteAxe, "Piglin Brute farm did not produce its axe during the sample");
        helper.succeed();
    }

    private static ConfiguredMobFarmBlockEntity farm(GameTestHelper helper) {
        return helper.getBlockEntity(
                GameTestFixtures.TEST_POS,
                ConfiguredMobFarmBlockEntity.class
        );
    }

    private static ConfiguredMobFarmBlockEntity reload(
            GameTestHelper helper,
            ConfiguredMobFarmBlockEntity original
    ) {
        CompoundTag data = original.saveWithFullMetadata(helper.getLevel().registryAccess());
        BlockEntity restored = BlockEntity.loadStatic(
                original.getBlockPos(),
                original.getBlockState(),
                data,
                helper.getLevel().registryAccess()
        );
        helper.assertTrue(restored instanceof ConfiguredMobFarmBlockEntity,
                "Could not reload configurable mob farm");
        return (ConfiguredMobFarmBlockEntity) restored;
    }

    private static void assertTarget(
            GameTestHelper helper,
            ConfiguredMobFarmKind kind,
            String id,
            boolean expected
    ) {
        helper.assertValueEqual(
                ConfiguredMobFarmTargetCatalog.isKnownTarget(kind, Identifier.parse(id)),
                expected,
                kind + " membership for " + id
        );
    }

    private static boolean contains(List<ItemStack> stacks, net.minecraft.world.item.Item item) {
        return stacks.stream().anyMatch(stack -> stack.is(item));
    }

    private static boolean isPiglinWeapon(ItemStack stack) {
        return stack.is(Items.CROSSBOW)
                || stack.is(Items.GOLDEN_SPEAR)
                || stack.is(Items.GOLDEN_SWORD);
    }

    private static boolean isPiglinArmor(ItemStack stack) {
        return stack.is(Items.GOLDEN_HELMET)
                || stack.is(Items.GOLDEN_CHESTPLATE)
                || stack.is(Items.GOLDEN_LEGGINGS)
                || stack.is(Items.GOLDEN_BOOTS);
    }
}
