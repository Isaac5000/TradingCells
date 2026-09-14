package com.cosmocraft.trading_cells.platform.neoforge.event;

import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.TradingCells;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@EventBusSubscriber(modid = TradingCells.MOD_ID, value = Dist.CLIENT)
public final class HighLevelEnchantmentTooltipEvent {
    private HighLevelEnchantmentTooltipEvent() {
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        List<Component> tooltip = event.getToolTip();
        restoreInfusionBookTitle(stack, tooltip);
        Map<Component, Component> replacements = Map.of();
        replacements = collectReplacements(stack.get(DataComponents.ENCHANTMENTS), replacements);
        replacements = collectReplacements(stack.get(DataComponents.STORED_ENCHANTMENTS), replacements);
        if (replacements.isEmpty()) {
            return;
        }

        // The first line is the item name, even when it matches an enchantment.
        for (int index = 1; index < tooltip.size(); index++) {
            Component replacement = replacements.get(tooltip.get(index));
            if (replacement != null) {
                tooltip.set(index, replacement);
            }
        }
    }

    private static void restoreInfusionBookTitle(ItemStack stack, List<Component> tooltip) {
        if (tooltip.isEmpty() || !stack.is(Items.ENCHANTED_BOOK)
                || !Component.translatable("item.trading_cells.silk_touch_two_book")
                        .equals(stack.get(DataComponents.CUSTOM_NAME))) {
            return;
        }
        ItemEnchantments stored = stack.get(DataComponents.STORED_ENCHANTMENTS);
        if (stored == null || stored.entrySet().stream()
                .noneMatch(entry -> entry.getKey().is(Enchantments.SILK_TOUCH) && entry.getIntValue() >= 2)) {
            return;
        }
        // Older infusion results carry this generated name. Change presentation, not saved data.
        ItemStack display = stack.copy();
        display.remove(DataComponents.CUSTOM_NAME);
        tooltip.set(0, display.getStyledHoverName());
    }

    private static Map<Component, Component> collectReplacements(
            ItemEnchantments enchantments,
            Map<Component, Component> replacements
    ) {
        if (enchantments == null || enchantments.isEmpty()) {
            return replacements;
        }
        Map<Component, Component> result = replacements;
        for (var entry : enchantments.entrySet()) {
            Holder<Enchantment> enchantment = entry.getKey();
            int level = entry.getIntValue();
            if (level <= enchantment.value().getMaxLevel()) {
                continue;
            }
            if (result.isEmpty()) {
                result = new HashMap<>();
            }
            Component original = Enchantment.getFullname(enchantment, level);
            Component colored = original.copy().withStyle(style -> style.withColor(colorFor(level)));
            result.put(original, colored);
        }
        return result;
    }

    public static int colorFor(int level) {
        return HighLevelEnchantmentPalette.colorFor(level, Enchantment.MAX_LEVEL);
    }
}
