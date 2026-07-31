package com.cosmocraft.trading_cells.feature.trader.adapters.input;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;

/** One non-stackable upgrade item for each progression level of the netherite piglin bartering cell. */
public final class PiglinBarterUpgradeItem extends Item {
    public enum Tier {
        COPPER_BASE("tooltip.trading_cells.piglin_barter_copper_upgrade"),
        IRON("tooltip.trading_cells.piglin_barter_iron_upgrade"),
        GOLD("tooltip.trading_cells.piglin_barter_gold_upgrade"),
        DIAMOND("tooltip.trading_cells.piglin_barter_diamond_upgrade"),
        NETHERITE("tooltip.trading_cells.piglin_barter_netherite_upgrade"),

        // Kept registered only so old worlds can load and migrate their previous upgrade items.
        LEGACY_QUALITY("tooltip.trading_cells.piglin_barter_legacy_upgrade"),
        LEGACY_YIELD("tooltip.trading_cells.piglin_barter_legacy_upgrade"),
        LEGACY_HYBRID("tooltip.trading_cells.piglin_barter_legacy_upgrade");

        private final String tooltipKey;

        Tier(String tooltipKey) {
            this.tooltipKey = tooltipKey;
        }
    }

    private final Tier tier;

    public PiglinBarterUpgradeItem(Tier tier, Properties properties) {
        super(properties);
        this.tier = tier;
    }

    public Tier tier() {
        return tier;
    }

    void appendTooltip(Consumer<Component> builder) {
        builder.accept(Component.translatable(tier.tooltipKey).withStyle(ChatFormatting.GRAY));
        builder.accept(Component.translatable(
                "tooltip.trading_cells.piglin_barter_upgrade_machine",
                Component.translatable("block.trading_cells.netherite_piglin_bartering_cell")
        ).withStyle(ChatFormatting.DARK_GRAY));
    }
}
