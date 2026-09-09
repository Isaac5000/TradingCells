package com.cosmocraft.trading_cells.gametest.feature.configuredmobfarm;

import com.cosmocraft.trading_cells.feature.configuredmobfarm.adapters.input.ConfiguredMobFarmBlockEntity;
import com.cosmocraft.trading_cells.feature.configuredmobfarm.adapters.input.ConfiguredMobFarmMenu;
import com.cosmocraft.trading_cells.feature.configuredmobfarm.adapters.input.ConfiguredMobFarmTargetCatalog;
import com.cosmocraft.trading_cells.feature.configuredmobfarm.adapters.output.ConfiguredMobFarmRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.configuredmobfarm.domain.model.ConfiguredMobFarmKind;
import com.cosmocraft.trading_cells.feature.configuredmobfarm.domain.model.ConfiguredMobFarmLoot;
import com.cosmocraft.trading_cells.gametest.shared.GameTestCase;
import com.cosmocraft.trading_cells.gametest.shared.GameTestFixtures;
import com.cosmocraft.trading_cells.gametest.shared.BlockEntityStateFixtures;
import com.cosmocraft.trading_cells.platform.neoforge.mobfarm.MobFarmTargetReloadListener;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
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
import net.neoforged.neoforge.transfer.transaction.Transaction;

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
                new GameTestCase("configured_mob_farm_activity_states", 20,
                        ConfiguredMobFarmGameTests::activityStates),
                new GameTestCase("configured_mob_farm_output_states", 20,
                        ConfiguredMobFarmGameTests::outputStates),
                new GameTestCase("livestock_and_fish_real_loot", 20,
                        ConfiguredMobFarmGameTests::livestockAndFishRealLoot),
                new GameTestCase("new_configured_families_real_loot", 20,
                        ConfiguredMobFarmGameTests::newConfiguredFamiliesRealLoot),
                new GameTestCase("mob_farm_invalid_descriptor_isolation", 20,
                        ConfiguredMobFarmGameTests::invalidDescriptorIsolation),
                new GameTestCase("configured_mob_farm_real_sided_transfers", 20,
                        ConfiguredMobFarmGameTests::realSidedTransfers),
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
            helper.assertValueEqual(input.size(), ConfiguredMobFarmBlockEntity.CONTAINER_SIZE, kind + " exposed automation slots");
            helper.assertValueEqual(output.size(), ConfiguredMobFarmBlockEntity.OUTPUT_SLOT_COUNT,
                    kind + " automated output slots");
            for (Direction side : Direction.Plane.HORIZONTAL) {
                ResourceHandler<ItemResource> sidedInput = helper.requireCapability(
                        Capabilities.Item.BLOCK,
                        GameTestFixtures.TEST_POS,
                        side
                );
                helper.assertValueEqual(sidedInput.size(), ConfiguredMobFarmBlockEntity.CONTAINER_SIZE,
                        kind + " horizontal automation slots on " + side);
            }
        }

        assertTarget(helper, ConfiguredMobFarmKind.SLIME, "minecraft:sulfur_cube", true);
        assertTarget(helper, ConfiguredMobFarmKind.GHAST, "minecraft:happy_ghast", true);
        assertTarget(helper, ConfiguredMobFarmKind.PIGLIN, "minecraft:hoglin", false);
        for (String target : List.of("cow", "mooshroom", "sheep", "pig", "chicken", "rabbit", "goat")) {
            assertTarget(helper, ConfiguredMobFarmKind.LIVESTOCK, "minecraft:" + target, true);
        }
        assertTarget(helper, ConfiguredMobFarmKind.LIVESTOCK, "minecraft:hoglin", false);
        for (String target : List.of("cod", "salmon", "tropical_fish", "pufferfish")) {
            assertTarget(helper, ConfiguredMobFarmKind.FISH, "minecraft:" + target, true);
        }
        for (String target : List.of("squid", "glow_squid", "dolphin", "nautilus")) {
            assertTarget(helper, ConfiguredMobFarmKind.AQUATIC, "minecraft:" + target, true);
        }
        for (String target : List.of("horse", "donkey", "mule", "camel", "llama", "trader_llama")) {
            assertTarget(helper, ConfiguredMobFarmKind.MOUNT, "minecraft:" + target, true);
        }
        for (String target : List.of("axolotl", "frog", "tadpole", "turtle")) {
            assertTarget(helper, ConfiguredMobFarmKind.AMPHIBIAN, "minecraft:" + target, true);
        }
        assertTarget(helper, ConfiguredMobFarmKind.BEE, "minecraft:bee", true);
        assertTarget(helper, ConfiguredMobFarmKind.CREAKING, "minecraft:creaking", true);
        helper.succeed();
    }

    private static void invalidDescriptorIsolation(GameTestHelper helper) {
        Map<Identifier, JsonElement> resources = new LinkedHashMap<>();
        Identifier validId = Identifier.fromNamespaceAndPath("verification", "valid_fish");
        resources.put(validId, JsonParser.parseString("""
                {
                  "schema_version": 1,
                  "family": "trading_cells:fish",
                  "entity_type": "minecraft:salmon",
                  "generator_item": "minecraft:salmon_spawn_egg",
                  "order": 20
                }
                """));
        resources.put(
                Identifier.fromNamespaceAndPath("verification", "invalid_fish"),
                JsonParser.parseString("""
                        {
                          "schema_version": -1,
                          "family": "trading_cells:fish",
                          "entity_type": "minecraft:cod",
                          "generator_item": "minecraft:cod_spawn_egg",
                          "order": 10
                        }
                        """)
        );
        helper.assertValueEqual(
                MobFarmTargetReloadListener.validDescriptorIds(resources),
                List.of(validId),
                "An invalid descriptor must not discard a valid neighbor"
        );
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
            int persistedProgress = Math.max(1, farm.cycleDurationTicks() / 2);
            farm.dataAccess().set(0, persistedProgress);
            ConfiguredMobFarmBlockEntity inProgress = reload(helper, farm);
            helper.assertValueEqual(inProgress.cycleTicks(), persistedProgress,
                    kind + " persisted progress");
            helper.assertTrue(ItemStack.isSameItemSameComponents(
                    inProgress.getItem(ConfiguredMobFarmBlockEntity.SWORD_SLOT),
                    new ItemStack(Items.WOODEN_SWORD)
            ), kind + " persisted sword");

            farm.dataAccess().set(0, farm.cycleDurationTicks() - 1);
            farm.processTick();
            helper.assertTrue(farm.dataAccess().get(4) > 0,
                    kind + " must complete an XP-only cycle");

            ConfiguredMobFarmBlockEntity restored = reload(helper, farm);
            helper.assertValueEqual(restored.selectedKind(), kind, kind + " persisted family");
            helper.assertValueEqual(restored.selectedTargetId(), selected, kind + " persisted target");
            helper.assertValueEqual(restored.dataAccess().get(3), 0, kind + " persisted loot filters");
            helper.assertValueEqual(restored.dataAccess().get(4), farm.dataAccess().get(4),
                    kind + " persisted XP");
            helper.assertTrue(ItemStack.isSameItemSameComponents(
                    restored.getItem(ConfiguredMobFarmBlockEntity.WORKER_SLOT),
                    worker
            ), kind + " persisted worker");
        }
        helper.succeed();
    }

    private static void activityStates(GameTestHelper helper) {
        ItemStack worker = GameTestFixtures.adultVillagerCapture(helper);
        for (ConfiguredMobFarmKind kind : ConfiguredMobFarmKind.values()) {
            helper.setBlock(GameTestFixtures.TEST_POS, ConfiguredMobFarmRegistrationAdapter.block(kind).get());
            ConfiguredMobFarmBlockEntity farm = farm(helper);

            farm.processTick();
            helper.assertValueEqual(farm.cycleTicks(), 0, kind + " without worker or sword");

            farm.setItem(ConfiguredMobFarmBlockEntity.WORKER_SLOT, worker);
            farm.processTick();
            helper.assertValueEqual(farm.cycleTicks(), 0, kind + " without sword");

            farm.setItem(ConfiguredMobFarmBlockEntity.SWORD_SLOT, new ItemStack(Items.WOODEN_SWORD));
            farm.processTick();
            helper.assertTrue(farm.cycleTicks() > 0, kind + " active progress");

            int pausedAt = farm.cycleTicks();
            farm.toggleEnabled();
            farm.processTick();
            helper.assertValueEqual(farm.cycleTicks(), pausedAt, kind + " paused progress");

            ConfiguredMobFarmBlockEntity paused = reload(helper, farm);
            helper.assertValueEqual(paused.dataAccess().get(8), 0, kind + " persisted paused state");
        }
        helper.succeed();
    }

    private static void realSidedTransfers(GameTestHelper helper) {
        for (ConfiguredMobFarmKind kind : ConfiguredMobFarmKind.values()) {
            for (Direction side : List.of(
                    Direction.UP,
                    Direction.NORTH,
                    Direction.SOUTH,
                    Direction.WEST,
                    Direction.EAST
            )) {
                helper.setBlock(GameTestFixtures.TEST_POS, ConfiguredMobFarmRegistrationAdapter.block(kind).get());
                ConfiguredMobFarmBlockEntity farm = farm(helper);
                ItemStack worker = GameTestFixtures.adultVillagerCapture(helper);
                ResourceHandler<ItemResource> handler = helper.requireCapability(
                        Capabilities.Item.BLOCK,
                        GameTestFixtures.TEST_POS,
                        side
                );
                try (Transaction transaction = Transaction.openRoot()) {
                    helper.assertValueEqual(
                            handler.insert(0, ItemResource.of(worker), 1, transaction),
                            1,
                            kind + " worker insertion from " + side
                    );
                    helper.assertValueEqual(
                            handler.insert(1, ItemResource.of(Items.WOODEN_SWORD), 1, transaction),
                            1,
                            kind + " sword insertion from " + side
                    );
                    transaction.commit();
                }
                helper.assertTrue(!farm.getItem(ConfiguredMobFarmBlockEntity.WORKER_SLOT).isEmpty(),
                        kind + " inserted worker from " + side);
                helper.assertTrue(farm.getItem(ConfiguredMobFarmBlockEntity.SWORD_SLOT).is(Items.WOODEN_SWORD),
                        kind + " inserted sword from " + side);
                farm.clearContent();
            }

            helper.setBlock(GameTestFixtures.TEST_POS, ConfiguredMobFarmRegistrationAdapter.block(kind).get());
            ConfiguredMobFarmBlockEntity farm = farm(helper);
            ResourceHandler<ItemResource> bottom = helper.requireCapability(
                    Capabilities.Item.BLOCK,
                    GameTestFixtures.TEST_POS,
                    Direction.DOWN
            );
            try (Transaction transaction = Transaction.openRoot()) {
                helper.assertValueEqual(
                        bottom.insert(ItemResource.of(Items.WOODEN_SWORD), 1, transaction),
                        0,
                        kind + " rejects insertion from below"
                );
            }

            farm.setItem(ConfiguredMobFarmBlockEntity.WORKER_SLOT, GameTestFixtures.adultVillagerCapture(helper));
            farm.setItem(ConfiguredMobFarmBlockEntity.SWORD_SLOT, new ItemStack(Items.WOODEN_SWORD));
            Identifier physicalTarget = physicalLootTarget(kind);
            if (physicalTarget == null) {
                farm.dataAccess().set(0, farm.cycleDurationTicks() - 1);
                farm.processTick();
                helper.assertTrue(farm.dataAccess().get(4) > 0,
                        kind + " XP-only target completed without an output slot");
                helper.assertValueEqual(firstOccupiedOutput(farm), -1,
                        kind + " XP-only target invented an item output");
                continue;
            }
            farm.selectTarget(physicalTarget);
            produceOneOutput(helper, kind, farm);
            int outputSlot = firstOccupiedOutput(farm);
            ItemStack output = farm.getItem(outputSlot).copy();
            int handlerSlot = outputSlot - ConfiguredMobFarmBlockEntity.FIRST_OUTPUT_SLOT;
            try (Transaction transaction = Transaction.openRoot()) {
                helper.assertValueEqual(
                        bottom.extract(handlerSlot, ItemResource.of(output), 1, transaction),
                        1,
                        kind + " output extraction from below"
                );
                transaction.commit();
            }
        }
        helper.succeed();
    }

    private static void outputStates(GameTestHelper helper) {
        ItemStack worker = GameTestFixtures.adultVillagerCapture(helper);
        for (ConfiguredMobFarmKind kind : ConfiguredMobFarmKind.values()) {
            helper.setBlock(GameTestFixtures.TEST_POS, ConfiguredMobFarmRegistrationAdapter.block(kind).get());
            ConfiguredMobFarmBlockEntity farm = farm(helper);
            Identifier physicalTarget = physicalLootTarget(kind);
            if (physicalTarget != null) {
                farm.selectTarget(physicalTarget);
            }
            farm.setItem(ConfiguredMobFarmBlockEntity.WORKER_SLOT, worker);
            farm.setItem(ConfiguredMobFarmBlockEntity.SWORD_SLOT, new ItemStack(Items.WOODEN_SWORD));
            BlockEntityStateFixtures.fillIndexedSlots(
                    helper,
                    farm,
                    "Slot",
                    ConfiguredMobFarmBlockEntity.FIRST_OUTPUT_SLOT,
                    ConfiguredMobFarmBlockEntity.OUTPUT_SLOT_COUNT,
                    new ItemStack(Items.COBBLESTONE, 64)
            );

            if (physicalTarget == null) {
                farm.dataAccess().set(0, farm.cycleDurationTicks() - 1);
                farm.processTick();
                helper.assertTrue(farm.dataAccess().get(4) > 0,
                        kind + " XP-only cycle must continue with full item output");
                continue;
            }

            farm.processTick();
            helper.assertValueEqual(farm.cycleTicks(), 0, kind + " full output progress");
            helper.assertValueEqual(farm.dataAccess().get(4), 0, kind + " full output XP");

            farm.removeItem(ConfiguredMobFarmBlockEntity.FIRST_OUTPUT_SLOT, 64);
            farm.dataAccess().set(0, farm.cycleDurationTicks() - 1);
            farm.processTick();
            helper.assertTrue(farm.dataAccess().get(4) > 0,
                    kind + " resumes with one available output slot");
        }
        helper.succeed();
    }

    private static void livestockAndFishRealLoot(GameTestHelper helper) {
        var player = helper.makeMockServerPlayer(GameType.SURVIVAL);
        int containerId = 200;
        for (ConfiguredMobFarmKind kind : List.of(
                ConfiguredMobFarmKind.LIVESTOCK,
                ConfiguredMobFarmKind.FISH
        )) {
            for (var target : ConfiguredMobFarmTargetCatalog.targets(kind)) {
                helper.setBlock(GameTestFixtures.TEST_POS, ConfiguredMobFarmRegistrationAdapter.block(kind).get());
                ConfiguredMobFarmBlockEntity farm = farm(helper);
                farm.clearContent();
                farm.selectTarget(target.entityTypeId());
                farm.setItem(ConfiguredMobFarmBlockEntity.WORKER_SLOT,
                        GameTestFixtures.adultVillagerCapture(helper));
                farm.setItem(ConfiguredMobFarmBlockEntity.SWORD_SLOT, new ItemStack(Items.NETHERITE_SWORD));

                boolean naturallyEmpty = target.entityTypeId().equals(Identifier.withDefaultNamespace("goat"));
                if (naturallyEmpty) {
                    farm.dataAccess().set(0, farm.cycleDurationTicks() - 1);
                    farm.processTick();
                    helper.assertTrue(farm.dataAccess().get(4) > 0,
                            "Goat must complete its death-loot cycle as XP-only");
                    helper.assertValueEqual(firstOccupiedOutput(farm), -1,
                            "Goat must not invent a vanilla death drop");
                } else {
                    produceOneOutput(helper, kind, farm);
                }

                ConfiguredMobFarmMenu menu = (ConfiguredMobFarmMenu) farm.createMenu(
                        containerId++,
                        player.getInventory(),
                        player
                );
                if (!menu.dynamicLootOptions().isEmpty()) {
                    ItemStack disabled = menu.dynamicLootOptions().getFirst();
                    Identifier disabledId = BuiltInRegistries.ITEM.getKey(disabled.getItem());
                    farm.toggleDynamicLoot(disabledId);
                    ConfiguredMobFarmBlockEntity restored = reload(helper, farm);
                    helper.assertTrue(restored.disabledDynamicLoot().contains(disabledId),
                            kind + " persisted disabled loot " + disabledId);
                    ConfiguredMobFarmMenu reopened = (ConfiguredMobFarmMenu) restored.createMenu(
                            containerId++,
                            player.getInventory(),
                            player
                    );
                    helper.assertTrue(!reopened.isDynamicLootEnabled(disabled),
                            kind + " reopened menu retained disabled loot " + disabledId);
                }
            }
        }
        helper.succeed();
    }

    private static void newConfiguredFamiliesRealLoot(GameTestHelper helper) {
        for (ConfiguredMobFarmKind kind : List.of(
                ConfiguredMobFarmKind.AQUATIC,
                ConfiguredMobFarmKind.MOUNT,
                ConfiguredMobFarmKind.AMPHIBIAN,
                ConfiguredMobFarmKind.BEE,
                ConfiguredMobFarmKind.CREAKING
        )) {
            for (var target : ConfiguredMobFarmTargetCatalog.targets(kind)) {
                helper.setBlock(GameTestFixtures.TEST_POS, ConfiguredMobFarmRegistrationAdapter.block(kind).get());
                ConfiguredMobFarmBlockEntity farm = farm(helper);
                farm.selectTarget(target.entityTypeId());
                farm.setItem(ConfiguredMobFarmBlockEntity.WORKER_SLOT,
                        GameTestFixtures.adultVillagerCapture(helper));
                farm.setItem(ConfiguredMobFarmBlockEntity.SWORD_SLOT, new ItemStack(Items.NETHERITE_SWORD));

                if (isXpOnlyTarget(target.entityTypeId())) {
                    farm.dataAccess().set(0, farm.cycleDurationTicks() - 1);
                    farm.processTick();
                    helper.assertTrue(farm.dataAccess().get(4) > 0,
                            target.entityTypeId() + " must complete an XP-only cycle");
                    helper.assertValueEqual(firstOccupiedOutput(farm), -1,
                            target.entityTypeId() + " must not invent a death drop");
                } else {
                    produceOneOutput(helper, kind, farm);
                    helper.assertTrue(farm.dataAccess().get(4) > 0,
                            target.entityTypeId() + " real-loot cycle must also store XP");
                }
            }
        }
        helper.succeed();
    }

    private static void produceOneOutput(
            GameTestHelper helper,
            ConfiguredMobFarmKind kind,
            ConfiguredMobFarmBlockEntity farm
    ) {
        for (int cycle = 0; cycle < 100 && firstOccupiedOutput(farm) < 0; cycle++) {
            farm.dataAccess().set(0, farm.cycleDurationTicks() - 1);
            farm.processTick();
        }
        helper.assertTrue(firstOccupiedOutput(farm) >= 0, kind + " produced no extractable output");
    }

    private static int firstOccupiedOutput(ConfiguredMobFarmBlockEntity farm) {
        for (int slot = ConfiguredMobFarmBlockEntity.FIRST_OUTPUT_SLOT;
                slot < ConfiguredMobFarmBlockEntity.CONTAINER_SIZE;
                slot++) {
            if (!farm.getItem(slot).isEmpty()) {
                return slot;
            }
        }
        return -1;
    }

    private static Identifier physicalLootTarget(ConfiguredMobFarmKind kind) {
        return switch (kind) {
            case ARTHROPOD -> Identifier.withDefaultNamespace("spider");
            case SLIME -> Identifier.withDefaultNamespace("slime");
            case GUARDIAN -> Identifier.withDefaultNamespace("guardian");
            case PIGLIN -> Identifier.withDefaultNamespace("piglin");
            case BLAZE -> Identifier.withDefaultNamespace("blaze");
            case GHAST -> Identifier.withDefaultNamespace("ghast");
            case ENDERMAN -> Identifier.withDefaultNamespace("enderman");
            case SHULKER -> Identifier.withDefaultNamespace("shulker");
            case BREEZE -> Identifier.withDefaultNamespace("breeze");
            case PHANTOM -> Identifier.withDefaultNamespace("phantom");
            case LIVESTOCK -> Identifier.withDefaultNamespace("cow");
            case FISH -> Identifier.withDefaultNamespace("cod");
            case AQUATIC -> Identifier.withDefaultNamespace("squid");
            case MOUNT -> Identifier.withDefaultNamespace("horse");
            case AMPHIBIAN -> Identifier.withDefaultNamespace("turtle");
            case BEE, CREAKING -> null;
        };
    }

    private static boolean isXpOnlyTarget(Identifier targetId) {
        return targetId.equals(Identifier.withDefaultNamespace("axolotl"))
                || targetId.equals(Identifier.withDefaultNamespace("frog"))
                || targetId.equals(Identifier.withDefaultNamespace("tadpole"))
                || targetId.equals(Identifier.withDefaultNamespace("bee"))
                || targetId.equals(Identifier.withDefaultNamespace("creaking"));
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
