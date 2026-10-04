package com.cosmocraft.trading_cells.feature.mobfarm.adapters.mixin;

import com.cosmocraft.trading_cells.feature.mobfarm.adapters.interaction.InteractionIsolation;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
abstract class InteractionLevelMixin {
    @Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z", at = @At("HEAD"))
    private void tradingCells$localBlocks(CallbackInfoReturnable<Boolean> callback) {
        InteractionIsolation.requireLocal((Level) (Object) this);
    }
    @Inject(method = {"setBlockEntity", "removeBlockEntity"}, at = @At("HEAD"))
    private void tradingCells$localBlockEntities(CallbackInfo callback) {
        InteractionIsolation.requireLocal((Level) (Object) this);
    }
}
