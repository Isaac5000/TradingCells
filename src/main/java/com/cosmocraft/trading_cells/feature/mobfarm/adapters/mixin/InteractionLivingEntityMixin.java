package com.cosmocraft.trading_cells.feature.mobfarm.adapters.mixin;

import com.cosmocraft.trading_cells.feature.mobfarm.adapters.interaction.InteractionIsolation;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
abstract class InteractionLivingEntityMixin {
    @Inject(method = {"addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z", "hurtServer"}, at = @At("HEAD"))
    private void tradingCells$localEffects(CallbackInfoReturnable<Boolean> callback) {
        InteractionIsolation.requireLocal(((LivingEntity) (Object) this).level());
    }
}
