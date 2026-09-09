package com.cosmocraft.trading_cells.gametest.feature.configuredmobfarm;

import com.cosmocraft.trading_cells.feature.configuredmobfarm.adapters.input.ConfiguredMobFarmLootPreview;
import com.cosmocraft.trading_cells.gametest.shared.GameTestCase;
import java.util.List;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;

public final class ConfiguredMobFarmPreviewGameTests {
    private ConfiguredMobFarmPreviewGameTests() { }

    public static List<GameTestCase> tests() {
        return List.of(new GameTestCase("configured_farm_loaded_loot_preview", 40, ConfiguredMobFarmPreviewGameTests::preview));
    }

    private static void preview(GameTestHelper helper) {
        var sword = Items.IRON_SWORD.getDefaultInstance();
        var cow = ConfiguredMobFarmLootPreview.calculate(helper.getLevel(), id("cow"), sword, 0, 1);
        helper.assertTrue(cow.containsKey(id("leather")), "Loaded cow table has a numeric preview");
        helper.assertValueEqual(cow.get(id("leather")).probabilityPartsPerMillion(), 666667, "Zero-count rolls included in probability");
        helper.assertValueEqual(cow.get(id("beef")).maximumAmount(), 3, "Cow meat maximum from table");
        var fish = ConfiguredMobFarmLootPreview.calculate(helper.getLevel(), id("cod"), sword, 0, 2);
        helper.assertValueEqual(fish.get(id("cod")).minimumAmount(), 2, "Two guaranteed fish per cycle");
        helper.assertValueEqual(fish.get(id("bone_meal")).probabilityPartsPerMillion(), 97500, "Independent rare-drop cycle probability");
        var looting = ConfiguredMobFarmLootPreview.calculate(helper.getLevel(), id("cow"), sword, 3, 1);
        helper.assertValueEqual(looting.get(id("leather")).probabilityPartsPerMillion(), 944444, "Looting uses rounded continuous count");
        helper.assertValueEqual(looting.get(id("leather")).maximumAmount(), 5, "Looting count added");
        for (String target : List.of("pig", "chicken", "rabbit", "mooshroom", "sheep", "salmon", "tropical_fish", "pufferfish",
                "squid", "glow_squid", "dolphin", "horse", "donkey", "mule", "llama", "trader_llama",
                "turtle", "blaze", "enderman", "shulker", "breeze", "phantom", "ghast", "guardian", "elder_guardian")) {
            helper.assertFalse(ConfiguredMobFarmLootPreview.calculate(helper.getLevel(), id(target), sword, 0, 1).isEmpty(),
                    target + " loaded table yields numeric cycle information");
        }
        helper.assertTrue(ConfiguredMobFarmLootPreview.calculate(helper.getLevel(), id("camel"), sword, 0, 1).isEmpty(),
                "An empty death table does not invent drops");
        helper.succeed();
    }

    private static Identifier id(String name) { return Identifier.withDefaultNamespace(name); }
}
