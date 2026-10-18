package com.cosmocraft.trading_cells.platform.neoforge.client;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

/** Shared presentation for optional descriptions, never item or enchantment names. */
public final class ItemDetailsTooltip {
    private ItemDetailsTooltip() { }

    public static boolean keyDown() {
        return InputConstants.isKeyDown(InputConstants.KEY_LSHIFT)
                || InputConstants.isKeyDown(InputConstants.KEY_RSHIFT);
    }

    public static Component hint() {
        return Component.translatable("tooltip.trading_cells.spawner.shift_details",
                Component.literal("Shift").withStyle(ChatFormatting.AQUA)).withStyle(ChatFormatting.GRAY);
    }

    public static void apply(List<Component> lines, boolean expanded) {
        if (expanded || lines.size() < 2) { return; }
        int first = -1;
        for (int index = lines.size() - 1; index > 0; index--) {
            if (isDescription(lines.get(index))) { lines.remove(index); first = index; }
        }
        if (first >= 0 && lines.stream().noneMatch(hint()::equals)) { lines.add(first, hint()); }
    }

    private static boolean isDescription(Component line) {
        if (!(line.getContents() instanceof TranslatableContents translated)) { return false; }
        String key = translated.getKey();
        return key.startsWith("enchantment.trading_cells.") && key.contains(".description.")
                || key.startsWith("tooltip.trading_cells.quarry_fortune_silk_description.");
    }
}
