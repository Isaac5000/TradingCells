package com.cosmocraft.trading_cells.platform.neoforge.client;

import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.TradingCells;
import com.mojang.datafixers.util.Either;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@EventBusSubscriber(modid = TradingCells.MOD_ID, value = Dist.CLIENT)
public final class TradingCellsItemTooltips {
    private static final Component MOD_NAME = Component.literal("Trading Cells")
            .withStyle(ChatFormatting.BLUE, ChatFormatting.ITALIC);

    private TradingCellsItemTooltips() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onItemTooltip(ItemTooltipEvent event) {
        List<Component> lines = event.getToolTip();
        if (!isOwnItem(event.getItemStack()) || lines.isEmpty()) {
            return;
        }
        // Preserve titles and lore with the same text; only deduplicate the styled footer.
        lines.subList(1, lines.size()).removeIf(MOD_NAME::equals);
        lines.add(MOD_NAME.copy());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onGatherTooltip(RenderTooltipEvent.GatherComponents event) {
        if (!isOwnItem(event.getItemStack()) || event.getTooltipElements().isEmpty()) {
            return;
        }
        var elements = event.getTooltipElements();
        // Creative search/inventory inserts these copies AFTER ItemTooltipEvent.
        // Match the components, not translated strings, and leave the tab titles untouched.
        var titles = BuiltInRegistries.CREATIVE_MODE_TAB.entrySet().stream()
                .filter(entry -> entry.getKey().identifier().getNamespace().equals(TradingCells.MOD_ID))
                .map(entry -> entry.getValue().getDisplayName().copy().withStyle(ChatFormatting.BLUE))
                .toList();
        for (int index = elements.size() - 1; index > 0; index--) {
            if (elements.get(index).left().filter(line -> titles.contains(line)
                    || MOD_NAME.equals(line)).isPresent()) {
                elements.remove(index);
            }
        }
        elements.add(Either.left(MOD_NAME.copy()));
    }

    private static boolean isOwnItem(ItemStack stack) {
        return !stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem())
                .getNamespace().equals(TradingCells.MOD_ID);
    }
}
