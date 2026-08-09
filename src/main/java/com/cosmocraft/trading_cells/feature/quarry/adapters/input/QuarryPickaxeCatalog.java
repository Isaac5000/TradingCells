package com.cosmocraft.trading_cells.feature.quarry.adapters.input;

import com.cosmocraft.trading_cells.feature.quarry.domain.model.VanillaPickaxeTier;
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
import net.minecraft.world.level.block.Block;

/** Stable vanilla pickaxe catalog extended with registry-backed modded tools. */
public final class QuarryPickaxeCatalog {
    private static final Comparator<Tier> TIER_ORDER = Comparator
            .comparingDouble(Tier::timingPosition)
            .thenComparingDouble(Tier::miningSpeed)
            .thenComparing(tier -> tier.id().toString());
    private static final List<Tier> VANILLA_TIERS = Arrays.stream(VanillaPickaxeTier.values())
            .map(QuarryPickaxeCatalog::vanilla)
            .sorted(TIER_ORDER)
            .toList();
    private static final Catalog VANILLA_CATALOG = Catalog.create(VANILLA_TIERS);
    private static final AtomicReference<Catalog> CATALOG = new AtomicReference<>();

    private QuarryPickaxeCatalog() {
    }

    public static boolean isSupported(ItemStack stack) {
        return !stack.isEmpty() && catalog().byItem().containsKey(stack.getItem());
    }

    public static double miningSpeed(ItemStack stack) {
        Tier tier = stack.isEmpty() ? null : catalog().byItem().get(stack.getItem());
        return tier == null ? 0.0D : tier.miningSpeed();
    }

    public static double timingPosition(ItemStack stack) {
        Tier tier = stack.isEmpty() ? null : catalog().byItem().get(stack.getItem());
        return tier == null ? VanillaPickaxeTier.WOODEN.timingPosition() : tier.timingPosition();
    }

    public static boolean canMine(ItemStack stack, Block block, double minimumSpeed) {
        return isSupported(stack)
                && miningSpeed(stack) >= minimumSpeed
                && stack.isCorrectToolForDrops(block.defaultBlockState());
    }

    public static List<ItemStack> itemStacks() {
        return catalog().tiers().stream().map(tier -> new ItemStack(tier.item())).toList();
    }

    public static void invalidate() {
        CATALOG.set(null);
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
                    "pickaxe tiers",
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
                    "The dynamic pickaxe catalog could not be assembled; only vanilla tiers will be used.",
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
        if (tool == null || (!stack.is(ItemTags.PICKAXES) && !hasPickaxeMiningRule(tool))) {
            return Optional.empty();
        }
        double speed = miningSpeed(tool);
        if (!Double.isFinite(speed) || speed <= 0.0D) {
            throw new IllegalArgumentException("Pickaxe mining speed must be finite and positive");
        }
        Identifier id = BuiltInRegistries.ITEM.getKey(item);
        if (id == null) {
            throw new IllegalArgumentException("Pickaxe has no registered identifier");
        }
        return Optional.of(new Tier(item, id, speed, dynamicTimingPosition(speed)));
    }

    private static boolean hasPickaxeMiningRule(Tool tool) {
        return tool.rules().stream().anyMatch(rule -> rule.blocks()
                .unwrapKey()
                .filter(BlockTags.MINEABLE_WITH_PICKAXE::equals)
                .isPresent());
    }

    private static double miningSpeed(Tool tool) {
        double fastestRuleSpeed = Math.max(0.0D, tool.defaultMiningSpeed());
        for (Tool.Rule rule : tool.rules()) {
            if (rule.speed().isEmpty()) {
                continue;
            }
            double speed = Math.max(0.0D, rule.speed().orElse(0.0F));
            if (rule.blocks().unwrapKey().filter(BlockTags.MINEABLE_WITH_PICKAXE::equals).isPresent()) {
                return speed;
            }
            fastestRuleSpeed = Math.max(fastestRuleSpeed, speed);
        }
        return fastestRuleSpeed;
    }

    private static Tier vanilla(VanillaPickaxeTier tier) {
        Item item = switch (tier) {
            case WOODEN -> Items.WOODEN_PICKAXE;
            case STONE -> Items.STONE_PICKAXE;
            case COPPER -> Items.COPPER_PICKAXE;
            case IRON -> Items.IRON_PICKAXE;
            case DIAMOND -> Items.DIAMOND_PICKAXE;
            case NETHERITE -> Items.NETHERITE_PICKAXE;
            case GOLDEN -> Items.GOLDEN_PICKAXE;
        };
        return new Tier(
                item,
                BuiltInRegistries.ITEM.getKey(item),
                tier.miningSpeed(),
                tier.timingPosition()
        );
    }

    private static double dynamicTimingPosition(double miningSpeed) {
        VanillaPickaxeTier[] anchors = {
                VanillaPickaxeTier.WOODEN,
                VanillaPickaxeTier.STONE,
                VanillaPickaxeTier.COPPER,
                VanillaPickaxeTier.IRON,
                VanillaPickaxeTier.DIAMOND,
                VanillaPickaxeTier.NETHERITE
        };
        if (miningSpeed <= anchors[0].miningSpeed()) {
            return anchors[0].timingPosition();
        }
        for (int index = 1; index < anchors.length; index++) {
            VanillaPickaxeTier upper = anchors[index];
            if (miningSpeed > upper.miningSpeed()) {
                continue;
            }
            VanillaPickaxeTier lower = anchors[index - 1];
            double ratio = (miningSpeed - lower.miningSpeed())
                    / (upper.miningSpeed() - lower.miningSpeed());
            return lower.timingPosition()
                    + ratio * (upper.timingPosition() - lower.timingPosition());
        }
        VanillaPickaxeTier netherite = VanillaPickaxeTier.NETHERITE;
        return netherite.timingPosition() + miningSpeed - netherite.miningSpeed();
    }

    private record Tier(Item item, Identifier id, double miningSpeed, double timingPosition) {
    }

    private record Catalog(List<Tier> tiers, Map<Item, Tier> byItem) {
        private static Catalog create(List<Tier> tiers) {
            List<Tier> immutableTiers = List.copyOf(tiers);
            Map<Item, Tier> byItem = new HashMap<>();
            for (Tier tier : immutableTiers) {
                if (byItem.put(tier.item(), tier) != null) {
                    throw new IllegalArgumentException("Duplicate pickaxe tier " + tier.id());
                }
            }
            return new Catalog(immutableTiers, Map.copyOf(byItem));
        }
    }
}
