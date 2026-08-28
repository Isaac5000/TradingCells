package com.cosmocraft.trading_cells.feature.combat.adapters.input;

import com.cosmocraft.trading_cells.feature.combat.adapters.api.CombatEnchantments;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.tooltip.TooltipLocation;
import net.neoforged.neoforge.event.RegisterTooltipAppendersEvent;

public final class CombatTooltipEventAdapter {
    private CombatTooltipEventAdapter() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(CombatTooltipEventAdapter::onRegisterTooltipAppenders);
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
        if (CombatEnchantments.isWarriorsTouchStoredOnBook(stack, context.registries())) {
            builder.accept(Component.translatable(
                    "enchantment.trading_cells.warriors_touch.description.1"
            ).withStyle(ChatFormatting.DARK_GREEN));
            builder.accept(Component.translatable(
                    "enchantment.trading_cells.warriors_touch.description.2"
            ).withStyle(ChatFormatting.DARK_GREEN));
        }
        if (CombatEnchantments.isDecapitationStoredOnBook(stack, context.registries())) {
            builder.accept(Component.translatable(
                    "enchantment.trading_cells.decapitation.description.1"
            ).withStyle(ChatFormatting.DARK_GREEN));
            builder.accept(Component.translatable(
                    "enchantment.trading_cells.decapitation.description.2"
            ).withStyle(ChatFormatting.DARK_GREEN));
            builder.accept(Component.translatable(
                    "enchantment.trading_cells.decapitation.description.3"
            ).withStyle(ChatFormatting.DARK_GREEN));
        }
    }
}
