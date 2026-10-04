package com.cosmocraft.trading_cells.gametest.shared;

import com.cosmocraft.trading_cells.platform.neoforge.item.StoredEnchantmentLookup;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;

public final class TooltipGameTests {
    private TooltipGameTests() { }
    public static List<GameTestCase> tests() {
        return List.of(new GameTestCase("enchantment_tooltips_without_world", 40, TooltipGameTests::withoutWorld));
    }

    private static void withoutWorld(GameTestHelper h) {
        var registry = h.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        for (String id : List.of("trading_cells:farmers_touch", "trading_cells:miners_touch",
                "trading_cells:warriors_touch", "trading_cells:decapitation", "minecraft:fortune")) {
            var key = ResourceKey.create(Registries.ENCHANTMENT, Identifier.parse(id));
            var book = EnchantmentHelper.createBook(new EnchantmentInstance(registry.getOrThrow(key), 1));
            h.assertTrue(StoredEnchantmentLookup.contains(book, key), "Stored holders supply tooltip keys without registries");
            h.assertFalse(StoredEnchantmentLookup.contains(new ItemStack(Items.ENCHANTED_BOOK), key), "Empty book has no descriptions");
            for (var flag : List.of(TooltipFlag.NORMAL, TooltipFlag.ADVANCED)) {
                var lines = book.getTooltipLines(Item.TooltipContext.EMPTY, null, flag);
                h.assertFalse(lines.isEmpty(), "Actual tooltip appenders accept the worldless REI context");
            }
        }
        h.succeed();
    }
}
