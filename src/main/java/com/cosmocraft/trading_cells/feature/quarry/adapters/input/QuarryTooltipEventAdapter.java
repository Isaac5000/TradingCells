package com.cosmocraft.trading_cells.feature.quarry.adapters.input;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.tooltip.TooltipLocation;
import net.neoforged.neoforge.event.RegisterTooltipAppendersEvent;

public final class QuarryTooltipEventAdapter {
    private QuarryTooltipEventAdapter() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(QuarryTooltipEventAdapter::onRegisterTooltipAppenders);
    }

    private static void onRegisterTooltipAppenders(RegisterTooltipAppendersEvent event) {
        event.registerAppender(
                TooltipLocation.PRE_ITEM_INFO,
                (stack, context, display, player, flag, builder) -> appendDescription(stack, context, builder)
        );
    }

    private static void appendDescription(
            ItemStack stack,
            Item.TooltipContext context,
            Consumer<Component> builder
    ) {
        if (QuarryEnchantments.isStoredOnBook(stack, context.registries())) {
            builder.accept(Component.translatable(
                    "enchantment.trading_cells.miners_touch.description.1"
            ).withStyle(ChatFormatting.DARK_GREEN));
            builder.accept(Component.translatable(
                    "enchantment.trading_cells.miners_touch.description.2"
            ).withStyle(ChatFormatting.DARK_GREEN));
        }
        if (QuarryEnchantments.isFortuneStoredOnBook(stack, context.registries())) {
            builder.accept(Component.translatable(
                    "tooltip.trading_cells.quarry_fortune_silk_description.1"
            ).withStyle(ChatFormatting.DARK_GREEN));
            builder.accept(Component.translatable(
                    "tooltip.trading_cells.quarry_fortune_silk_description.2"
            ).withStyle(ChatFormatting.DARK_GREEN));
        }
    }
}
