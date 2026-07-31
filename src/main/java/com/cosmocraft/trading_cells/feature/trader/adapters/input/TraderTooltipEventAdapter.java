package com.cosmocraft.trading_cells.feature.trader.adapters.input;

import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.tooltip.TooltipLocation;
import net.neoforged.neoforge.event.RegisterTooltipAppendersEvent;

public final class TraderTooltipEventAdapter {
    private TraderTooltipEventAdapter() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(TraderTooltipEventAdapter::onRegisterTooltipAppenders);
    }

    private static void onRegisterTooltipAppenders(RegisterTooltipAppendersEvent event) {
        event.registerAppender(
                TooltipLocation.POST_CUSTOM,
                (stack, context, display, player, flag, builder) ->
                        appendTooltip(stack, context, builder)
        );
    }

    private static void appendTooltip(
            ItemStack stack,
            Item.TooltipContext context,
            Consumer<Component> builder
    ) {
        if (stack.getItem() instanceof AutotraderBlockItem autotrader) {
            autotrader.appendTooltip(stack, context, builder);
        } else if (stack.getItem() instanceof VillagerTradingCellBlockItem trader) {
            trader.appendTooltip(stack, context, builder);
        } else if (stack.getItem() instanceof PiglinBarteringCellBlockItem piglinTrader) {
            piglinTrader.appendTooltip(stack, builder);
        } else if (stack.getItem() instanceof PiglinBarterUpgradeItem upgrade) {
            upgrade.appendTooltip(builder);
        }
    }
}
