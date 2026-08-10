package com.cosmocraft.trading_cells.feature.skeletonfarm.adapters.input;

import com.cosmocraft.trading_cells.feature.skeletonfarm.domain.model.VanillaSwordTier;
import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.TradingCells;
import com.cosmocraft.trading_cells.platform.neoforge.catalog.SafeDynamicCatalog;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Stable vanilla sword tiers extended at runtime with tagged modded swords. */
public final class SwordTierCatalog {
    private static final Comparator<Tier> TIER_ORDER = Comparator
            .comparingDouble(Tier::timingPosition)
            .thenComparingInt(Tier::durability)
            .thenComparing(tier -> tier.id().toString());
    private static final List<Tier> VANILLA_TIERS = Arrays.stream(VanillaSwordTier.values())
            .map(SwordTierCatalog::vanilla)
            .sorted(TIER_ORDER)
            .toList();
    private static final Catalog VANILLA_CATALOG = Catalog.create(VANILLA_TIERS);
    private static final AtomicReference<Catalog> CATALOG = new AtomicReference<>();

    private SwordTierCatalog() {
    }

    public static boolean isSupported(ItemStack stack) {
        return !stack.isEmpty() && catalog().byItem().containsKey(stack.getItem());
    }

    public static double timingPosition(ItemStack stack) {
        Tier tier = stack.isEmpty() ? null : catalog().byItem().get(stack.getItem());
        return tier == null ? VanillaSwordTier.WOODEN.timingPosition() : tier.timingPosition();
    }

    public static List<ItemStack> itemStacks() {
        return catalog().tiers().stream().map(tier -> new ItemStack(tier.item())).toList();
    }

    private static Catalog catalog() {
        Catalog cached = CATALOG.get();
        if (cached != null) {
            return cached;
        }
        Catalog discovered = discoverCatalog();
        CATALOG.compareAndSet(null, discovered);
        return CATALOG.get();
    }

    private static Catalog discoverCatalog() {
        try {
            Set<Item> vanillaItems = new HashSet<>();
            VANILLA_TIERS.forEach(tier -> vanillaItems.add(tier.item()));
            List<Tier> dynamic = SafeDynamicCatalog.discover(
                    "sword tiers",
                    () -> BuiltInRegistries.ITEM,
                    item -> dynamicTier(item, vanillaItems),
                    TIER_ORDER,
                    item -> BuiltInRegistries.ITEM.getKey(item).toString()
            );
            if (dynamic.isEmpty()) {
                return VANILLA_CATALOG;
            }
            List<Tier> combined = new ArrayList<>(VANILLA_TIERS);
            combined.addAll(dynamic);
            combined.sort(TIER_ORDER);
            return Catalog.create(combined);
        } catch (RuntimeException | LinkageError exception) {
            TradingCells.LOGGER.warn(
                    "The dynamic sword tier catalog could not be assembled; only vanilla tiers will be used.",
                    exception
            );
            return VANILLA_CATALOG;
        }
    }

    private static Optional<Tier> dynamicTier(Item item, Set<Item> vanillaItems) {
        if (vanillaItems.contains(item)) {
            return Optional.empty();
        }
        ItemStack stack = new ItemStack(item);
        if (!stack.is(ItemTags.SWORDS)) {
            return Optional.empty();
        }
        Identifier id = BuiltInRegistries.ITEM.getKey(item);
        if (id == null) {
            throw new IllegalArgumentException("Sword has no registered item identifier");
        }
        int durability = Math.max(1, stack.getMaxDamage());
        return Optional.of(new Tier(item, id, durability, dynamicTimingPosition(durability)));
    }

    private static Tier vanilla(VanillaSwordTier tier) {
        Item item = switch (tier) {
            case WOODEN -> Items.WOODEN_SWORD;
            case GOLDEN -> Items.GOLDEN_SWORD;
            case STONE -> Items.STONE_SWORD;
            case COPPER -> Items.COPPER_SWORD;
            case IRON -> Items.IRON_SWORD;
            case DIAMOND -> Items.DIAMOND_SWORD;
            case NETHERITE -> Items.NETHERITE_SWORD;
        };
        return new Tier(
                item,
                BuiltInRegistries.ITEM.getKey(item),
                item.getDefaultInstance().getMaxDamage(),
                tier.timingPosition()
        );
    }

    private static double dynamicTimingPosition(int durability) {
        VanillaSwordTier[] anchors = {
                VanillaSwordTier.WOODEN,
                VanillaSwordTier.STONE,
                VanillaSwordTier.COPPER,
                VanillaSwordTier.IRON,
                VanillaSwordTier.DIAMOND,
                VanillaSwordTier.NETHERITE
        };
        int firstDurability = vanillaItem(anchors[0]).getDefaultInstance().getMaxDamage();
        if (durability <= firstDurability) {
            return anchors[0].timingPosition();
        }
        for (int index = 1; index < anchors.length; index++) {
            VanillaSwordTier upper = anchors[index];
            int upperDurability = vanillaItem(upper).getDefaultInstance().getMaxDamage();
            if (durability > upperDurability) {
                continue;
            }
            VanillaSwordTier lower = anchors[index - 1];
            int lowerDurability = vanillaItem(lower).getDefaultInstance().getMaxDamage();
            double ratio = (durability - lowerDurability)
                    / (double) Math.max(1, upperDurability - lowerDurability);
            return lower.timingPosition()
                    + ratio * (upper.timingPosition() - lower.timingPosition());
        }
        int netheriteDurability = Items.NETHERITE_SWORD.getDefaultInstance().getMaxDamage();
        int diamondDurability = Items.DIAMOND_SWORD.getDefaultInstance().getMaxDamage();
        return VanillaSwordTier.NETHERITE.timingPosition()
                + (durability - netheriteDurability)
                / (double) Math.max(1, netheriteDurability - diamondDurability);
    }

    private static Item vanillaItem(VanillaSwordTier tier) {
        return switch (tier) {
            case WOODEN -> Items.WOODEN_SWORD;
            case GOLDEN -> Items.GOLDEN_SWORD;
            case STONE -> Items.STONE_SWORD;
            case COPPER -> Items.COPPER_SWORD;
            case IRON -> Items.IRON_SWORD;
            case DIAMOND -> Items.DIAMOND_SWORD;
            case NETHERITE -> Items.NETHERITE_SWORD;
        };
    }

    private record Tier(Item item, Identifier id, int durability, double timingPosition) {
    }

    private record Catalog(List<Tier> tiers, Map<Item, Tier> byItem) {
        private static Catalog create(List<Tier> tiers) {
            List<Tier> immutable = List.copyOf(tiers);
            Map<Item, Tier> byItem = new HashMap<>();
            for (Tier tier : immutable) {
                if (byItem.put(tier.item(), tier) != null) {
                    throw new IllegalArgumentException("Duplicate sword tier " + tier.id());
                }
            }
            return new Catalog(immutable, Map.copyOf(byItem));
        }
    }
}
