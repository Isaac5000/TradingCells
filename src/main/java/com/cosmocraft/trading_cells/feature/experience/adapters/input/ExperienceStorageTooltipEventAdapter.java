package com.cosmocraft.trading_cells.feature.experience.adapters.input;

import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.tooltip.TooltipLocation;
import net.neoforged.neoforge.event.RegisterTooltipAppendersEvent;

public final class ExperienceStorageTooltipEventAdapter {
    private ExperienceStorageTooltipEventAdapter() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(ExperienceStorageTooltipEventAdapter::onRegisterTooltipAppenders);
    }

    private static void onRegisterTooltipAppenders(RegisterTooltipAppendersEvent event) {
        event.registerAppender(
                TooltipLocation.POST_CUSTOM,
                (stack, context, display, player, flag, builder) -> appendTooltip(stack, builder)
        );
    }

    private static void appendTooltip(ItemStack stack, Consumer<Component> builder) {
        if (stack.getItem() instanceof ExperienceStorageBlockItem storage) {
            storage.appendTooltip(stack, builder);
        }
    }
}
