package com.cosmocraft.trading_cells.feature.trader.adapters.minecraft;

import com.cosmocraft.trading_cells.feature.trader.domain.model.PiglinBarterUpgradeYield;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.jspecify.annotations.NonNull;

/** Minecraft adapter for filtered, quality-weighted piglin barter rewards. */
public final class EnhancedPiglinBarterRewards {
    public static final int NETHERITE_UPGRADE_LEVEL = 5;

    private static final int[] QUALITY_ROLLS = new int[]{1, 2, 3, 4, 5, 6};
    private static final int[] ENCHANTMENT_BONUSES = new int[]{0, 0, 0, 0, 1, 1};

    private EnhancedPiglinBarterRewards() {
    }

    public static @NonNull ItemStack roll(
            ServerLevel level,
            Piglin piglin,
            ItemStack filter,
            int upgradeLevel
    ) {
        if (!isSupportedFilter(filter)) {
            return ItemStack.EMPTY;
        }
        int safeLevel = safeUpgradeLevel(upgradeLevel);
        int qualityRolls = QUALITY_ROLLS[safeLevel];
        LootTable lootTable = level.getServer().reloadableRegistries().getLootTable(BuiltInLootTables.PIGLIN_BARTERING);
        LootParams lootParams = new LootParams.Builder(level)
                .withParameter(LootContextParams.THIS_ENTITY, piglin)
                .create(LootContextParamSets.PIGLIN_BARTER);

        ItemStack best = ItemStack.EMPTY;
        int bestScore = Integer.MIN_VALUE;
        for (int attempt = 0; attempt < qualityRolls; attempt++) {
            List<ItemStack> rewards = filter.isEmpty()
                    ? lootTable.getRandomItems(lootParams)
                    : List.of(fallbackFilteredReward(level, filter));
            for (ItemStack reward : rewards) {
                if (reward.isEmpty() || !matchesFilter(reward, filter)) {
                    continue;
                }
                int score = qualityScore(reward);
                if (best.isEmpty() || score > bestScore) {
                    best = reward.copy();
                    bestScore = score;
                }
            }
        }
        if (best.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return applyYieldUpgrade(best, safeLevel);
    }

    public static boolean isSupportedFilter(ItemStack filter) {
        if (filter.isEmpty()) {
            return true;
        }
        return isPotionFilter(filter)
                || isBookFilter(filter)
                || PiglinBarterCatalog.exactFilterItems().contains(filter.getItem());
    }

    public static boolean matchesFilter(ItemStack reward, ItemStack filter) {
        if (filter.isEmpty()) {
            return true;
        }
        if (isPotionFilter(filter)) {
            return isPotionReward(reward);
        }
        if (isBookFilter(filter)) {
            return isBookReward(reward);
        }
        return reward.is(filter.getItem());
    }

    /**
     * Calculates the stack amount displayed by recipe viewers using the same
     * cap and multiplier as the runtime reward.
     */
    public static int upgradedStackAmount(ItemStack reward, int baseAmount, int upgradeLevel) {
        if (!usesStackYield(reward)) {
            return baseAmount;
        }
        return PiglinBarterUpgradeYield.upgradedAmount(
                baseAmount,
                reward.getMaxStackSize(),
                upgradeLevel
        );
    }

    private static ItemStack applyYieldUpgrade(ItemStack reward, int upgradeLevel) {
        float yieldMultiplier = PiglinBarterUpgradeYield.multiplier(upgradeLevel);
        int enchantmentBonus = ENCHANTMENT_BONUSES[upgradeLevel];
        if (yieldMultiplier == 1.0F && enchantmentBonus == 0) {
            return reward;
        }

        if (isPotionReward(reward)) {
            float currentScale = reward.getOrDefault(DataComponents.POTION_DURATION_SCALE, 1.0F);
            reward.set(DataComponents.POTION_DURATION_SCALE, currentScale * yieldMultiplier);
            return reward;
        }

        if (isBookReward(reward) || reward.is(Items.IRON_BOOTS)) {
            upgradeEnchantments(reward, enchantmentBonus);
            return reward;
        }

        if (usesStackYield(reward)) {
            reward.setCount(upgradedStackAmount(reward, reward.getCount(), upgradeLevel));
        }
        return reward;
    }

    private static void upgradeEnchantments(ItemStack reward, int bonus) {
        if (bonus == 0) {
            return;
        }
        ItemEnchantments current = EnchantmentHelper.getEnchantmentsForCrafting(reward);
        if (current.isEmpty()) {
            return;
        }
        ItemEnchantments.Mutable upgraded = new ItemEnchantments.Mutable(current);
        for (Holder<Enchantment> enchantment : new ArrayList<>(upgraded.keySet())) {
            int currentLevel = upgraded.getLevel(enchantment);
            upgraded.set(enchantment, Math.min(enchantment.value().getMaxLevel(), currentLevel + bonus));
        }
        EnchantmentHelper.setEnchantments(reward, upgraded.toImmutable());
    }

    private static int safeUpgradeLevel(int upgradeLevel) {
        return Math.clamp(upgradeLevel, 0, NETHERITE_UPGRADE_LEVEL);
    }

    private static boolean usesStackYield(ItemStack reward) {
        return reward.getMaxStackSize() > 1
                && !isPotionReward(reward)
                && !isBookReward(reward)
                && !reward.is(Items.IRON_BOOTS);
    }

    private static int qualityScore(ItemStack reward) {
        Item item = reward.getItem();
        int base;
        if (item == Items.ENCHANTED_BOOK || item == Items.BOOK) {
            base = 100_000;
        } else if (item == Items.IRON_BOOTS) {
            base = 90_000;
        } else if (item == Items.POTION || item == Items.SPLASH_POTION) {
            base = 80_000;
        } else if (item == Items.DRIED_GHAST) {
            base = 75_000;
        } else if (item == Items.ENDER_PEARL) {
            base = 70_000;
        } else if (item == Items.OBSIDIAN) {
            base = 60_000;
        } else if (item == Items.CRYING_OBSIDIAN) {
            base = 58_000;
        } else if (item == Items.FIRE_CHARGE) {
            base = 55_000;
        } else if (item == Items.QUARTZ) {
            base = 50_000;
        } else if (item == Items.IRON_NUGGET) {
            base = 45_000;
        } else {
            base = 10_000;
        }
        return base + enchantmentScore(reward) * 100 + potionScore(reward) + reward.getCount();
    }

    private static int enchantmentScore(ItemStack reward) {
        ItemEnchantments enchantments = EnchantmentHelper.getEnchantmentsForCrafting(reward);
        int score = 0;
        for (var entry : enchantments.entrySet()) {
            score += entry.getIntValue();
        }
        return score;
    }

    private static int potionScore(ItemStack reward) {
        PotionContents contents = reward.get(DataComponents.POTION_CONTENTS);
        if (contents == null) {
            return 0;
        }
        int score = 0;
        for (var effect : contents.getAllEffects()) {
            score += effect.getDuration() / 20 + effect.getAmplifier() * 1_000;
        }
        return score;
    }

    private static ItemStack fallbackFilteredReward(ServerLevel level, ItemStack filter) {
        if (isPotionFilter(filter)) {
            return fallbackPotion(level.getRandom());
        }
        if (isBookFilter(filter)) {
            return fallbackSoulSpeedBook(level);
        }
        if (!PiglinBarterCatalog.exactFilterItems().contains(filter.getItem())) {
            return ItemStack.EMPTY;
        }
        return fallbackExactItem(level, filter.getItem());
    }

    private static ItemStack fallbackPotion(RandomSource random) {
        int roll = random.nextInt(26);
        if (roll < 8) {
            return PotionContents.createItemStack(Items.POTION, Potions.FIRE_RESISTANCE);
        }
        if (roll < 16) {
            return PotionContents.createItemStack(Items.SPLASH_POTION, Potions.FIRE_RESISTANCE);
        }
        return PotionContents.createItemStack(Items.POTION, Potions.WATER);
    }

    private static ItemStack fallbackSoulSpeedBook(ServerLevel level) {
        Holder<Enchantment> soulSpeed = level.registryAccess().getOrThrow(Enchantments.SOUL_SPEED);
        int maxLevel = soulSpeed.value().getMaxLevel();
        int levelValue = 1 + level.getRandom().nextInt(maxLevel);
        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
        ItemEnchantments.Mutable enchantments = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        enchantments.set(soulSpeed, levelValue);
        EnchantmentHelper.setEnchantments(book, enchantments.toImmutable());
        return book;
    }

    private static ItemStack fallbackExactItem(ServerLevel level, Item item) {
        RandomSource random = level.getRandom();
        if (item == Items.IRON_BOOTS) {
            ItemStack boots = new ItemStack(Items.IRON_BOOTS);
            Holder<Enchantment> soulSpeed = level.registryAccess().getOrThrow(Enchantments.SOUL_SPEED);
            int maxLevel = soulSpeed.value().getMaxLevel();
            int levelValue = 1 + random.nextInt(maxLevel);
            ItemEnchantments.Mutable enchantments = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
            enchantments.set(soulSpeed, levelValue);
            EnchantmentHelper.setEnchantments(boots, enchantments.toImmutable());
            return boots;
        }
        if (item == Items.IRON_NUGGET) {
            return stack(item, between(random, 10, 36));
        }
        if (item == Items.ENDER_PEARL) {
            return stack(item, between(random, 2, 4));
        }
        if (item == Items.DRIED_GHAST || item == Items.OBSIDIAN || item == Items.FIRE_CHARGE) {
            return stack(item, 1);
        }
        if (item == Items.STRING) {
            return stack(item, between(random, 3, 9));
        }
        if (item == Items.QUARTZ) {
            return stack(item, between(random, 5, 12));
        }
        if (item == Items.CRYING_OBSIDIAN) {
            return stack(item, between(random, 1, 3));
        }
        if (item == Items.LEATHER) {
            return stack(item, between(random, 2, 4));
        }
        if (item == Items.SOUL_SAND || item == Items.NETHER_BRICK) {
            return stack(item, between(random, 2, 8));
        }
        if (item == Items.SPECTRAL_ARROW) {
            return stack(item, between(random, 6, 12));
        }
        if (item == Items.GRAVEL || item == Items.BLACKSTONE) {
            return stack(item, between(random, 8, 16));
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack stack(Item item, int count) {
        return new ItemStack(item, count);
    }

    private static int between(RandomSource random, int min, int max) {
        return min + random.nextInt(max - min + 1);
    }

    private static boolean isPotionFilter(ItemStack stack) {
        return stack.is(Items.POTION) || stack.is(Items.SPLASH_POTION);
    }

    private static boolean isPotionReward(ItemStack stack) {
        return stack.is(Items.POTION) || stack.is(Items.SPLASH_POTION);
    }

    private static boolean isBookFilter(ItemStack stack) {
        return stack.is(Items.BOOK) || stack.is(Items.ENCHANTED_BOOK);
    }

    private static boolean isBookReward(ItemStack stack) {
        return stack.is(Items.BOOK) || stack.is(Items.ENCHANTED_BOOK);
    }
}
