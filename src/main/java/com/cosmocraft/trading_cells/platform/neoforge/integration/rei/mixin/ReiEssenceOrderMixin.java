package com.cosmocraft.trading_cells.platform.neoforge.integration.rei.mixin;

import com.cosmocraft.trading_cells.platform.neoforge.integration.rei.EssenceReiDisplay;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.impl.display.DisplaySpec;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** REI sync/search order is unspecified; sort a view copy without changing its registry. */
@Pseudo
@Mixin(targets = "me.shedaniel.rei.impl.client.gui.screen.AbstractDisplayViewingScreen", remap = false)
abstract class ReiEssenceOrderMixin {
    @Shadow @Final @Mutable
    protected Map<DisplayCategory<?>, List<DisplaySpec>> categoryMap;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void trading_cells$orderEssenceTiers(Map<DisplayCategory<?>, List<DisplaySpec>> original,
                                                CategoryIdentifier<?> selected, CallbackInfo callbackInfo) {
        var ordered = new LinkedHashMap<>(original);
        boolean changed = false;
        for (var entry : original.entrySet()) {
            var id = entry.getKey().getCategoryIdentifier();
            if (!EssenceReiDisplay.STABILIZATION.equals(id) && !EssenceReiDisplay.SYNTHESIS.equals(id)) { continue; }
            ordered.put(entry.getKey(), entry.getValue().stream().sorted(Comparator
                    .comparingInt((DisplaySpec spec) -> spec.provideInternalDisplay() instanceof EssenceReiDisplay display
                            ? display.tier() : Integer.MAX_VALUE)
                    .thenComparing(spec -> spec.provideInternalDisplay().getDisplayLocation().map(Object::toString).orElse("")))
                    .toList());
            changed = true;
        }
        if (changed) { categoryMap = ordered; }
    }
}
