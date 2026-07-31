package com.cosmocraft.trading_cells.platform.neoforge.integration.rei.mixin;

import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.TradingCells;
import com.cosmocraft.trading_cells.platform.neoforge.integration.rei.TradingCellsReiCategory;
import java.util.Map;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps REI's outer frame wide enough when Trading Cells shares a view with narrower categories. */
@Pseudo
@Mixin(targets = "me.shedaniel.rei.impl.client.gui.screen.DefaultDisplayViewingScreen", remap = false)
abstract class ReiDisplayWidthMixin {
    @Shadow
    @Final
    @Mutable
    private int bestWidthDisplay;

    @SuppressWarnings("unused")
    @Inject(method = "<init>", at = @At("RETURN"))
    private void trading_cells$fitTradingCellsDisplay(
            Map<?, ?> categories,
            @Nullable CategoryIdentifier<?> selectedCategory,
            CallbackInfo callbackInfo
    ) {
        if (containsTradingCellsCategory(categories, selectedCategory)) {
            bestWidthDisplay = Math.max(bestWidthDisplay, TradingCellsReiCategory.DISPLAY_WIDTH);
        }
    }

    private static boolean containsTradingCellsCategory(
            Map<?, ?> categories,
            @Nullable CategoryIdentifier<?> selectedCategory
    ) {
        if (isTradingCellsCategory(selectedCategory)) {
            return true;
        }
        for (Object category : categories.keySet()) {
            if (category instanceof DisplayCategory<?> displayCategory
                    && isTradingCellsCategory(displayCategory.getCategoryIdentifier())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isTradingCellsCategory(@Nullable CategoryIdentifier<?> category) {
        return category != null
                && TradingCells.MOD_ID.equals(category.getIdentifier().getNamespace());
    }
}
