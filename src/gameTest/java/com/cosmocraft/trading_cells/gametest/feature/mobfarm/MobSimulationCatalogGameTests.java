package com.cosmocraft.trading_cells.gametest.feature.mobfarm;

import com.cosmocraft.trading_cells.feature.mobfarm.adapters.output.MobFarmRegistrationAdapter;
import com.cosmocraft.trading_cells.gametest.shared.GameTestCase;
import com.cosmocraft.trading_cells.platform.neoforge.registration.CreativeTabRegistration;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;

public final class MobSimulationCatalogGameTests {
    private MobSimulationCatalogGameTests() { }

    public static List<GameTestCase> tests() {
        return List.of(new GameTestCase("mob_simulation_creative_catalog", 40, MobSimulationCatalogGameTests::creative),
                new GameTestCase("mob_simulation_replaced_recipes", 40, MobSimulationCatalogGameTests::recipes));
    }

    private static void creative(GameTestHelper helper) {
        CreativeModeTabs.tryRebuildTabContents(helper.getLevel().enabledFeatures(), true, helper.getLevel().registryAccess());
        var tab = CreativeTabRegistration.FARMS_TAB.get();
        helper.assertTrue(tab.getIconItem().is(MobFarmRegistrationAdapter.ITEM.get()), "General farm is the tab icon");
        var items = tab.getDisplayItems().stream().map(stack -> stack.getItem()).toList();
        // Require all supported farm items, not an exact tab length: future
        // machines may be added without invalidating existing availability.
        var requiredFarmItems = new java.util.ArrayList<net.minecraft.world.item.Item>(List.of(
                MobFarmRegistrationAdapter.ITEM.get(),
                MobFarmRegistrationAdapter.INTERACTION_ITEM.get(),
                MobFarmRegistrationAdapter.WORKBENCH_ITEM.get(),
                MobFarmRegistrationAdapter.CREATURE_GENERATOR_ITEM.get(),
                MobFarmRegistrationAdapter.STABILIZER_ITEM.get(),
                MobFarmRegistrationAdapter.DISPLAY_PEDESTAL_ITEM.get(),
                MobFarmRegistrationAdapter.ESSENCE_EXTRACTOR.get(),
                MobFarmRegistrationAdapter.EMPTY_VIAL.get()));
        MobFarmRegistrationAdapter.MODEL_BASES.forEach(base -> requiredFarmItems.add(base.get()));
        for (var item : requiredFarmItems) {
            helper.assertTrue(items.contains(item), "Expected farm item must be available in its creative tab");
            helper.assertValueEqual(java.util.Collections.frequency(items, item), 1,
                    "Farm item must appear once");
        }
        var utilities = CreativeTabRegistration.ITEMS_TAB.get().getDisplayItems().stream().map(stack -> stack.getItem()).toList();
        var expectedUpgrades = new java.util.ArrayList<String>();
        for (String family : List.of("%s_pipe_upgrade", "piglin_barter_%s_upgrade", "quarry_%s_upgrade",
                "speed_%s_upgrade", "quantity_%s_upgrade")) {
            for (String material : List.of("copper", "iron", "gold", "diamond", "netherite")) {
                expectedUpgrades.add("trading_cells:" + family.formatted(material));
            }
        }
        var utilityIds = utilities.stream()
                .map(item -> BuiltInRegistries.ITEM.getKey(item).toString()).toList();
        int previousIndex = -1;
        for (String id : expectedUpgrades) {
            int index = utilityIds.indexOf(id);
            helper.assertTrue(index > previousIndex,
                    "Upgrades are present in tier/family order, even if other items are added");
            helper.assertValueEqual(java.util.Collections.frequency(utilityIds, id), 1,
                    "Upgrade must appear once in the creative tab");
            previousIndex = index;
        }
        for (int tier = 0; tier < 5; tier++) {
            var speed = MobFarmRegistrationAdapter.UPGRADES.get(tier).get();
            helper.assertTrue(utilities.contains(speed) && !items.contains(speed), "Speed upgrades only in Items tab");
            var capacity = MobFarmRegistrationAdapter.UPGRADES.get(5 + tier).get();
            helper.assertTrue(utilities.contains(capacity) && !items.contains(capacity), "Quantity upgrades only in Items tab");
        }
        helper.succeed();
    }

    private static void recipes(GameTestHelper helper) {
        var manager = helper.getLevel().getServer().getRecipeManager();
        for (String name : List.of("mob_farm_infusion", "essence_workbench", "essence_extractor")) {
            helper.assertTrue(manager.byKey(ResourceKey.create(Registries.RECIPE,
                    Identifier.fromNamespaceAndPath("trading_cells", name))).isPresent(), "Replacement recipe loaded: " + name);
        }
        for (String material : List.of("copper", "iron", "gold", "diamond", "netherite")) {
            var canonical = Identifier.fromNamespaceAndPath("trading_cells", "quantity_" + material + "_upgrade");
            var previous = Identifier.fromNamespaceAndPath("trading_cells", "capacity_" + material + "_upgrade");
            helper.assertTrue(manager.byKey(ResourceKey.create(Registries.RECIPE, canonical)).isPresent(),
                    "Quantity upgrade recipe uses the canonical identifier");
            helper.assertTrue(manager.byKey(ResourceKey.create(Registries.RECIPE, previous)).isEmpty(),
                    "Renamed upgrade recipe no longer uses the previous identifier");
        }
        helper.succeed();
    }
}