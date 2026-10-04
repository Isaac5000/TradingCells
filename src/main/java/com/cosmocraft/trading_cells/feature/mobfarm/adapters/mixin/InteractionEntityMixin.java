package com.cosmocraft.trading_cells.feature.mobfarm.adapters.mixin;

import com.cosmocraft.trading_cells.feature.mobfarm.adapters.interaction.InteractionIsolation;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
abstract class InteractionEntityMixin {
    @Inject(method = {"teleport", "startRiding(Lnet/minecraft/world/entity/Entity;ZZ)Z"}, at = @At("HEAD"))
    private void tradingCells$denyMovement(CallbackInfoReturnable<?> callback) {
        if (InteractionIsolation.active()) { InteractionIsolation.deny(); }
    }
}
