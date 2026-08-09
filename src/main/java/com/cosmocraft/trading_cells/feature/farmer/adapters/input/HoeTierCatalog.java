package com.cosmocraft.trading_cells.feature.farmer.adapters.input;

import com.cosmocraft.trading_cells.feature.farmer.domain.model.VanillaHoeTier;
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
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Tool;

/** Stable vanilla hoe tiers extended at runtime with valid modded hoes. */
public final class HoeTierCatalog {
    private static final Comparator<Tier> TIER_ORDER = Comparator
            .comparingDouble(Tier::timingPosition)
            .thenComparingDouble(Tier::miningSpeed)
            .thenComparing(tier -> tier.id().toString());
    private static final List<Tier> VANILLA_TIERS = Arrays.stream(VanillaHoeTier.values())
            .map(HoeTierCatalog::vanilla)
            .sorted(TIER_ORDER)
            .toList();
    private static final Catalog VANILLA_CATALOG = Catalog.create(VANILLA_TIERS);
    private static final AtomicReference<Catalog> CATALOG = new AtomicReference<>();

    private HoeTierCatalog() {
    }

    public static boolean isSupported(ItemStack stack) {
        return !stack.isEmpty() && catalog().byItem().containsKey(stack.getItem());
    }

    public static double miningSpeed(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0.0D;
        }
        Tier tier = catalog().byItem().get(stack.getItem());
        return tier == null ? 0.0D : tier.miningSpeed();
    }

    public static double timingPosition(ItemStack stack) {
        if (stack.isEmpty()) {
            return VanillaHoeTier.WOODEN.timingPosition();
        }
        Tier tier = catalog().byItem().get(stack.getItem());
        return tier == null ? VanillaHoeTier.WOODEN.timingPosition() : tier.timingPosition();
    }

    public static List<ItemStack> itemStacks() {
        return catalog().tiers().stream()
                .map(tier -> new ItemStack(tier.item()))
                .toList();
    }

    static List<Tier> tiers() {
        return catalog().tiers();
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
            List<Tier> dynamicTiers = SafeDynamicCatalog.discover(
                    "hoe tiers",
                    () -> BuiltInRegistries.ITEM,
                    item -> dynamicTier(item, vanillaItems),
                    TIER_ORDER,
                    item -> BuiltInRegistries.ITEM.getKey(item).toString()
            );
            if (dynamicTiers.isEmpty()) {
                return VANILLA_CATALOG;
            }
            List<Tier> combined = new ArrayList<>(VANILLA_TIERS);
            combined.addAll(dynamicTiers);
            combined.sort(TIER_ORDER);
            return Catalog.create(combined);
        } catch (RuntimeException | LinkageError exception) {
            TradingCells.LOGGER.warn(
                    "The dynamic hoe tier catalog could not be assembled; only vanilla tiers will be used.",
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
        Tool tool = stack.get(DataComponents.TOOL);
        if (tool == null || (!stack.is(ItemTags.HOES) && !hasHoeMiningRule(tool))) {
            return Optional.empty();
        }
        double speed = miningSpeed(tool);
        if (!Double.isFinite(speed) || speed <= 0.0D) {
            throw new IllegalArgumentException("Hoe mining speed must be finite and positive");
        }
        Identifier id = BuiltInRegistries.ITEM.getKey(item);
        if (id == null) {
            throw new IllegalArgumentException("Hoe has no registered item identifier");
        }
        return Optional.of(new Tier(item, id, speed, dynamicTimingPosition(speed)));
    }

    private static boolean hasHoeMiningRule(Tool tool) {
        return tool.rules().stream().anyMatch(rule -> rule.blocks()
                .unwrapKey()
                .filter(BlockTags.MINEABLE_WITH_HOE::equals)
                .isPresent());
    }

    private static double miningSpeed(Tool tool) {
        double fastestRuleSpeed = Math.max(0.0D, tool.defaultMiningSpeed());
        for (Tool.Rule rule : tool.rules()) {
            if (rule.speed().isEmpty()) {
                continue;
            }
            double ruleSpeed = Math.max(0.0D, rule.speed().orElse(0.0F));
            if (rule.blocks().unwrapKey().filter(BlockTags.MINEABLE_WITH_HOE::equals).isPresent()) {
                return ruleSpeed;
            }
            fastestRuleSpeed = Math.max(fastestRuleSpeed, ruleSpeed);
        }
        return fastestRuleSpeed;
    }

    private static Tier vanilla(VanillaHoeTier tier) {
        Item item = switch (tier) {
            case WOODEN -> Items.WOODEN_HOE;
            case STONE -> Items.STONE_HOE;
            case COPPER -> Items.COPPER_HOE;
            case IRON -> Items.IRON_HOE;
            case DIAMOND -> Items.DIAMOND_HOE;
            case NETHERITE -> Items.NETHERITE_HOE;
            case GOLDEN -> Items.GOLDEN_HOE;
        };
        return new Tier(
                item,
                BuiltInRegistries.ITEM.getKey(item),
                tier.miningSpeed(),
                tier.timingPosition()
        );
    }

    private static double dynamicTimingPosition(double miningSpeed) {
        VanillaHoeTier[] anchors = {
                VanillaHoeTier.WOODEN,
                VanillaHoeTier.STONE,
                VanillaHoeTier.COPPER,
                VanillaHoeTier.IRON,
                VanillaHoeTier.DIAMOND,
                VanillaHoeTier.NETHERITE
        };
        if (miningSpeed <= anchors[0].miningSpeed()) {
            return anchors[0].timingPosition();
        }
        for (int index = 1; index < anchors.length; index++) {
            VanillaHoeTier upper = anchors[index];
            if (miningSpeed > upper.miningSpeed()) {
                continue;
            }
            VanillaHoeTier lower = anchors[index - 1];
            double ratio = (miningSpeed - lower.miningSpeed())
                    / (upper.miningSpeed() - lower.miningSpeed());
            return lower.timingPosition()
                    + ratio * (upper.timingPosition() - lower.timingPosition());
        }
        VanillaHoeTier netherite = VanillaHoeTier.NETHERITE;
        return netherite.timingPosition() + miningSpeed - netherite.miningSpeed();
    }

    record Tier(Item item, Identifier id, double miningSpeed, double timingPosition) {
    }

    private record Catalog(List<Tier> tiers, Map<Item, Tier> byItem) {
        private static Catalog create(List<Tier> tiers) {
            List<Tier> immutableTiers = List.copyOf(tiers);
            Map<Item, Tier> byItem = new HashMap<>();
            for (Tier tier : immutableTiers) {
                if (byItem.put(tier.item(), tier) != null) {
                    throw new IllegalArgumentException("Duplicate hoe tier " + tier.id());
                }
            }
            return new Catalog(immutableTiers, Map.copyOf(byItem));
        }
    }
}
