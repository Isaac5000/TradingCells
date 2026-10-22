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
        helper.assertValueEqual(items.size(), 12, "Two farms, three essence machines, display pedestal, syringe, empty vial and four bases");
        helper.assertTrue(items.contains(MobFarmRegistrationAdapter.DISPLAY_PEDESTAL_ITEM.get()), "Display pedestal available in creative");
        helper.assertTrue(items.contains(MobFarmRegistrationAdapter.CREATURE_GENERATOR_ITEM.get()), "Creature generator available in creative");
        helper.assertTrue(items.contains(MobFarmRegistrationAdapter.ITEM.get()), "General farm available in creative");
        helper.assertTrue(items.contains(MobFarmRegistrationAdapter.INTERACTION_ITEM.get()), "Interaction farm available in creative");
        helper.assertTrue(items.contains(MobFarmRegistrationAdapter.WORKBENCH_ITEM.get()), "Workbench available in creative");
        helper.assertTrue(items.contains(MobFarmRegistrationAdapter.ESSENCE_EXTRACTOR.get()), "Extractor available in creative");
        var utilities = CreativeTabRegistration.ITEMS_TAB.get().getDisplayItems().stream().map(stack -> stack.getItem()).toList();
        var expectedUpgrades = new java.util.ArrayList<String>();
        for (String family : List.of("%s_pipe_upgrade", "piglin_barter_%s_upgrade", "quarry_%s_upgrade",
                "speed_%s_upgrade", "capacity_%s_upgrade")) {
            for (String material : List.of("copper", "iron", "gold", "diamond", "netherite")) {
                expectedUpgrades.add("trading_cells:" + family.formatted(material));
            }
        }
        var finalItems = utilities.subList(utilities.size() - expectedUpgrades.size(), utilities.size()).stream()
                .map(item -> BuiltInRegistries.ITEM.getKey(item).toString()).toList();
        helper.assertValueEqual(finalItems, expectedUpgrades, "Items tab ends with pipe, barter, quarry, speed and capacity upgrades in tier order");
        for (int tier = 0; tier < 5; tier++) {
            var speed = MobFarmRegistrationAdapter.UPGRADES.get(tier).get();
            helper.assertTrue(utilities.contains(speed) && !items.contains(speed), "Speed upgrades only in Items tab");
            var capacity = MobFarmRegistrationAdapter.UPGRADES.get(5 + tier).get();
            helper.assertTrue(utilities.contains(capacity) && !items.contains(capacity), "Capacity upgrades only in Items tab");
        }
        helper.succeed();
    }

    private static void recipes(GameTestHelper helper) {
        var manager = helper.getLevel().getServer().getRecipeManager();
        for (String name : List.of("mob_farm_infusion", "essence_workbench", "essence_extractor")) {
            helper.assertTrue(manager.byKey(ResourceKey.create(Registries.RECIPE,
                    Identifier.fromNamespaceAndPath("trading_cells", name))).isPresent(), "Replacement recipe loaded: " + name);
        }
        helper.succeed();
    }
}
