package com.cosmocraft.trading_cells.feature.trader.adapters.minecraft;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.jspecify.annotations.Nullable;

/** Shared catalog used by the netherite trader screen and optional recipe viewers. */
public final class PiglinBarterCatalog {
    private static final Set<Item> EXACT_FILTER_ITEMS = Set.of(
            Items.IRON_BOOTS,
            Items.IRON_NUGGET,
            Items.ENDER_PEARL,
            Items.DRIED_GHAST,
            Items.STRING,
            Items.QUARTZ,
            Items.OBSIDIAN,
            Items.CRYING_OBSIDIAN,
            Items.FIRE_CHARGE,
            Items.LEATHER,
            Items.SOUL_SAND,
            Items.NETHER_BRICK,
            Items.SPECTRAL_ARROW,
            Items.GRAVEL,
            Items.BLACKSTONE
    );

    private PiglinBarterCatalog() {
    }

    public static Set<Item> exactFilterItems() {
        return EXACT_FILTER_ITEMS;
    }

    public static List<Entry> entries(@Nullable RegistryAccess registries) {
        List<Entry> entries = new ArrayList<>();
        entries.add(new Entry(
                PotionContents.createItemStack(Items.POTION, Potions.WATER),
                List.of(
                        PotionContents.createItemStack(Items.POTION, Potions.FIRE_RESISTANCE),
                        PotionContents.createItemStack(Items.SPLASH_POTION, Potions.FIRE_RESISTANCE),
                        PotionContents.createItemStack(Items.POTION, Potions.WATER)
                ),
                1,
                1
        ));
        entries.add(new Entry(
                new ItemStack(Items.BOOK),
                soulSpeedVariants(registries, Items.ENCHANTED_BOOK),
                1,
                1
        ));
        entries.add(new Entry(
                new ItemStack(Items.IRON_BOOTS),
                soulSpeedVariants(registries, Items.IRON_BOOTS),
                1,
                1
        ));
        addExactFilter(entries, Items.IRON_NUGGET, 10, 36);
        addExactFilter(entries, Items.ENDER_PEARL, 2, 4);
        addExactFilter(entries, Items.DRIED_GHAST, 1, 1);
        addExactFilter(entries, Items.STRING, 3, 9);
        addExactFilter(entries, Items.QUARTZ, 5, 12);
        addExactFilter(entries, Items.OBSIDIAN, 1, 1);
        addExactFilter(entries, Items.CRYING_OBSIDIAN, 1, 3);
        addExactFilter(entries, Items.FIRE_CHARGE, 1, 1);
        addExactFilter(entries, Items.LEATHER, 2, 4);
        addExactFilter(entries, Items.SOUL_SAND, 2, 8);
        addExactFilter(entries, Items.NETHER_BRICK, 2, 8);
        addExactFilter(entries, Items.SPECTRAL_ARROW, 6, 12);
        addExactFilter(entries, Items.GRAVEL, 8, 16);
        addExactFilter(entries, Items.BLACKSTONE, 8, 16);
        return List.copyOf(entries);
    }

    private static void addExactFilter(
            List<Entry> entries,
            Item item,
            int minimumAmount,
            int maximumAmount
    ) {
        ItemStack stack = new ItemStack(item);
        entries.add(new Entry(stack.copy(), List.of(stack), minimumAmount, maximumAmount));
    }

    private static List<ItemStack> soulSpeedVariants(@Nullable RegistryAccess registries, Item item) {
        if (registries == null) {
            return List.of(new ItemStack(item));
        }

        Holder<Enchantment> soulSpeed = registries.getOrThrow(Enchantments.SOUL_SPEED);
        int maximumLevel = Math.max(1, soulSpeed.value().getMaxLevel());
        List<ItemStack> variants = new ArrayList<>(maximumLevel);
        for (int level = 1; level <= maximumLevel; level++) {
            ItemStack stack = new ItemStack(item);
            ItemEnchantments.Mutable enchantments = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
            enchantments.set(soulSpeed, level);
            EnchantmentHelper.setEnchantments(stack, enchantments.toImmutable());
            variants.add(stack);
        }
        return List.copyOf(variants);
    }

    public record Entry(
            ItemStack filter,
            List<ItemStack> outputs,
            int minimumAmount,
            int maximumAmount
    ) {
        public Entry {
            filter = filter.copy();
            outputs = outputs.stream().map(ItemStack::copy).toList();
            if (minimumAmount < 1 || maximumAmount < minimumAmount) {
                throw new IllegalArgumentException("Invalid piglin barter amount range");
            }
        }
    }
}
