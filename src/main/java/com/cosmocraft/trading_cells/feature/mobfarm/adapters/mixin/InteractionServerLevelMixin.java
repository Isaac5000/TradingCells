package com.cosmocraft.trading_cells.feature.mobfarm.adapters.mixin;

import com.cosmocraft.trading_cells.feature.mobfarm.adapters.interaction.InteractionIsolation;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerLevel.class)
abstract class InteractionServerLevelMixin {
    @Inject(method = "addEntity", at = @At("HEAD"))
    private void tradingCells$denyPersistentEntities(CallbackInfoReturnable<Boolean> callback) {
        if (InteractionIsolation.active()) { InteractionIsolation.deny(); }
    }
}
