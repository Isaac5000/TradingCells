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
        return List.of(new GameTestCase("enchantment_tooltips_without_world", 40, TooltipGameTests::withoutWorld),
                new GameTestCase("enchantment_touch_legacy_ids", 40, TooltipGameTests::legacyIds),
                new GameTestCase("entity_item_preview_payload", 40, TooltipGameTests::previewPayload));
    }

    private static void withoutWorld(GameTestHelper h) {
        var registry = h.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        for (String id : List.of("trading_cells:farmer_touch", "trading_cells:miner_touch",
                "trading_cells:warrior_touch", "trading_cells:decapitation", "minecraft:fortune")) {
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

    private static void legacyIds(GameTestHelper h) {
        var registry = h.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        var ops = h.getLevel().registryAccess().createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE);
        for (String role : List.of("farmer", "miner", "warrior")) {
            var current = ResourceKey.create(Registries.ENCHANTMENT, Identifier.parse("trading_cells:" + role + "_touch"));
            var legacy = ResourceKey.create(Registries.ENCHANTMENT, Identifier.parse("trading_cells:" + role + "s_touch"));
            h.assertTrue(registry.getOrThrow(current) == registry.getOrThrow(legacy), "Old ID aliases the same holder");
            var book = EnchantmentHelper.createBook(new EnchantmentInstance(registry.getOrThrow(current), 1));
            var saved = ItemStack.CODEC.encodeStart(ops, book).getOrThrow().toString().replace(role + "_touch", role + "s_touch");
            var restored = ItemStack.CODEC.parse(ops, com.google.gson.JsonParser.parseString(saved)).getOrThrow();
            h.assertTrue(StoredEnchantmentLookup.contains(restored, current), "Existing enchanted book survives ID migration");
            h.assertFalse(ItemStack.CODEC.encodeStart(ops, restored).getOrThrow().toString().contains(role + "s_touch"),
                    "Next save writes the canonical ID");
            var machine = new com.cosmocraft.trading_cells.feature.infusion.adapters.input.ArcaneInfuserBlockEntity(
                    net.minecraft.core.BlockPos.ZERO,
                    com.cosmocraft.trading_cells.feature.infusion.adapters.output.ArcaneInfuserRegistrationAdapter.BLOCK.get().defaultBlockState());
            machine.setLevel(h.getLevel());
            var savedMachine = new net.minecraft.nbt.CompoundTag();
            savedMachine.putString("LockedRecipe", "trading_cells:" + role + "s_touch_infusion");
            machine.loadWithComponents(net.minecraft.world.level.storage.TagValueInput.create(
                    net.minecraft.util.ProblemReporter.DISCARDING, h.getLevel().registryAccess(), savedMachine));
            h.assertValueEqual(machine.lockedRecipeId(), Identifier.parse("trading_cells:" + role + "_touch_infusion"),
                    "Persisted recipe lock follows renamed infusion");
        }
        h.succeed();
    }

    private static void previewPayload(GameTestHelper h) {
        var kind = com.cosmocraft.trading_cells.feature.captures.domain.model.CapturedMobKind.VILLAGER;
        var capture = new ItemStack(com.cosmocraft.trading_cells.feature.captures.adapters.api.CapturedMobStackAdapter.capturerItem(kind));
        var tag = new net.minecraft.nbt.CompoundTag();
        tag.putInt("Age", -24_000);
        tag.putString("Brain", "x".repeat(35_554));
        var data = new net.minecraft.nbt.CompoundTag();
        data.putString("type", "minecraft:plains");
        data.putString("profession", "minecraft:farmer");
        data.putInt("level", 5);
        tag.put("VillagerData", data);
        capture.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
        var preview = com.cosmocraft.trading_cells.platform.neoforge.item.EntityItemPreviewData.compact(capture);
        h.assertTrue(preview != capture && capture.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA).copyTag().equals(tag),
                "Preview cannot mutate saved entity data");
        h.assertTrue(com.cosmocraft.trading_cells.feature.captures.adapters.api.CapturedMobStackAdapter.isBaby(kind, preview), "Preview retains baby appearance");
        h.assertValueEqual(preview.getCount(), capture.getCount(), "Preview retains stack quantity");
        var bytes = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), h.getLevel().registryAccess(),
                net.neoforged.neoforge.network.connection.ConnectionType.OTHER);
        try {
            for (int i = 0; i < 54; i++) { ItemStack.OPTIONAL_STREAM_CODEC.encode(bytes, preview); }
            h.assertTrue(bytes.readableBytes() < 14_000, "Full double chest of captured villagers fits Jade's 16 KiB packet");
        } finally { bytes.release(); }
        var ordinary = new ItemStack(Items.DIAMOND);
        h.assertTrue(com.cosmocraft.trading_cells.platform.neoforge.item.EntityItemPreviewData.compact(ordinary) == ordinary,
                "Unrelated items unchanged");
        h.succeed();
    }
}
