package com.cosmocraft.trading_cells.gametest.feature.mobfarm;

import com.cosmocraft.trading_cells.feature.mobfarm.adapters.output.MobFarmRegistrationAdapter;
import com.cosmocraft.trading_cells.gametest.shared.GameTestCase;
import com.cosmocraft.trading_cells.platform.neoforge.mobfarm.LegacyMobFarmBlock;
import com.cosmocraft.trading_cells.platform.neoforge.registration.CreativeTabRegistration;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
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
        helper.assertValueEqual(items.size(), 13, "One farm, workbench, extractor and ten upgrades");
        helper.assertTrue(items.contains(MobFarmRegistrationAdapter.ITEM.get()), "General farm available in creative");
        helper.assertTrue(items.contains(MobFarmRegistrationAdapter.WORKBENCH_ITEM.get()), "Workbench available in creative");
        helper.assertTrue(items.contains(MobFarmRegistrationAdapter.ESSENCE_EXTRACTOR.get()), "Extractor available in creative");
        MobFarmRegistrationAdapter.UPGRADES.forEach(upgrade ->
                helper.assertTrue(items.contains(upgrade.get()), "Every speed/capacity tier available in creative"));
        for (var creativeTab : BuiltInRegistries.CREATIVE_MODE_TAB) {
            helper.assertFalse(creativeTab.getSearchTabDisplayItems().stream().anyMatch(stack ->
                    stack.getItem() instanceof BlockItem item && item.getBlock() instanceof LegacyMobFarmBlock),
                    "Legacy farms absent from every creative/search tab");
        }
        helper.succeed();
    }

    private static void recipes(GameTestHelper helper) {
        int retained = 0;
        var manager = helper.getLevel().getServer().getRecipeManager();
        for (var block : BuiltInRegistries.BLOCK) {
            if (!(block instanceof LegacyMobFarmBlock)) { continue; }
            retained++;
            var id = BuiltInRegistries.BLOCK.getKey(block);
            helper.assertTrue(BuiltInRegistries.ITEM.getOptional(id).isPresent(), "Old item ID retained for world compatibility");
            var recipe = ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath(id.getNamespace(), id.getPath() + "_infusion"));
            helper.assertTrue(manager.byKey(recipe).isEmpty(), "Old farm cannot still be crafted: " + id);
        }
        helper.assertValueEqual(retained, 21, "All legacy block registrations retained");
        for (String name : List.of("mob_farm_infusion", "essence_workbench", "essence_extractor")) {
            helper.assertTrue(manager.byKey(ResourceKey.create(Registries.RECIPE,
                    Identifier.fromNamespaceAndPath("trading_cells", name))).isPresent(), "Replacement recipe loaded: " + name);
        }
        helper.succeed();
    }
}
