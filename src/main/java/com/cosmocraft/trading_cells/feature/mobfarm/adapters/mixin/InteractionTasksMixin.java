package com.cosmocraft.trading_cells.feature.mobfarm.adapters.mixin;

import com.cosmocraft.trading_cells.feature.mobfarm.adapters.interaction.InteractionIsolation;
import net.minecraft.util.thread.BlockableEventLoop;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockableEventLoop.class)
abstract class InteractionTasksMixin {
    @Inject(method = {"execute", "schedule"}, at = @At("HEAD"))
    private void tradingCells$denyDeferredWork(CallbackInfo callback) {
        if (InteractionIsolation.active()) { InteractionIsolation.deny(); }
    }
}
