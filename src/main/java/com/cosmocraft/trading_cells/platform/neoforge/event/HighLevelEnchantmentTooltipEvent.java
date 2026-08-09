package com.cosmocraft.trading_cells.platform.neoforge.event;

import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.TradingCells;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
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
        Map<String, Component> replacements = new HashMap<>();
        collectReplacements(stack.get(DataComponents.ENCHANTMENTS), replacements);
        collectReplacements(stack.get(DataComponents.STORED_ENCHANTMENTS), replacements);
        if (replacements.isEmpty()) {
            return;
        }

        List<Component> tooltip = event.getToolTip();
        for (int index = 0; index < tooltip.size(); index++) {
            Component replacement = replacements.get(tooltip.get(index).getString());
            if (replacement != null) {
                tooltip.set(index, replacement);
            }
        }
    }

    private static void collectReplacements(
            ItemEnchantments enchantments,
            Map<String, Component> replacements
    ) {
        if (enchantments == null || enchantments.isEmpty()) {
            return;
        }
        for (var entry : enchantments.entrySet()) {
            Holder<Enchantment> enchantment = entry.getKey();
            int level = entry.getIntValue();
            if (level <= enchantment.value().getMaxLevel()) {
                continue;
            }
            Component original = Enchantment.getFullname(enchantment, level);
            Component colored = enchantment.value().description().copy()
                    .append(CommonComponents.SPACE)
                    .append(Component.translatable("enchantment.level." + level))
                    .withStyle(Style.EMPTY.withColor(colorFor(level)));
            replacements.put(original.getString(), colored);
        }
    }

    public static int colorFor(int level) {
        return HighLevelEnchantmentPalette.colorFor(level, Enchantment.MAX_LEVEL);
    }
}
