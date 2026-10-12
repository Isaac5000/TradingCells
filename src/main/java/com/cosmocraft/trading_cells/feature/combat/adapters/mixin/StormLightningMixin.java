package com.cosmocraft.trading_cells.feature.combat.adapters.mixin;

import com.cosmocraft.trading_cells.feature.combat.adapters.input.StormLightningEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LightningBolt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
abstract class StormLightningMixin {
    @Inject(method = "thunderHit", at = @At("HEAD"), cancellable = true)
    private void tradingCells$skipWeaponFireAndExtraDamage(ServerLevel level, LightningBolt bolt, CallbackInfo ci) {
        // Subclass conversion/charging hooks still run. Only vanilla's base fire/damage is skipped.
        if (bolt.getPersistentData().getBooleanOr(StormLightningEvents.ARTIFICIAL, false)) { ci.cancel(); }
    }
}
