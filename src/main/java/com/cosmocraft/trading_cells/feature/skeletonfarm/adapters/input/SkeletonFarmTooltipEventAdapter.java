package com.cosmocraft.trading_cells.feature.skeletonfarm.adapters.input;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.tooltip.TooltipLocation;
import net.neoforged.neoforge.event.RegisterTooltipAppendersEvent;

public final class SkeletonFarmTooltipEventAdapter {
    private SkeletonFarmTooltipEventAdapter() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(SkeletonFarmTooltipEventAdapter::onRegisterTooltipAppenders);
    }

    private static void onRegisterTooltipAppenders(RegisterTooltipAppendersEvent event) {
        event.registerAppender(
                TooltipLocation.POST_CUSTOM,
                (stack, context, display, player, flag, builder) -> appendDescription(stack, context, builder)
        );
    }

    private static void appendDescription(
            ItemStack stack,
            Item.TooltipContext context,
            Consumer<Component> builder
    ) {
        if (SkeletonFarmEnchantments.isStoredOnBook(stack, context.registries())) {
            builder.accept(Component.translatable(
                    "enchantment.trading_cells.warriors_touch.description"
            ).withStyle(ChatFormatting.DARK_GREEN));
        }
    }
}
