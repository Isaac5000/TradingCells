package com.cosmocraft.trading_cells.feature.mobfarm.adapters.mixin;

import com.cosmocraft.trading_cells.feature.mobfarm.adapters.output.client.SyringeArmAnimation;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidModel.class)
abstract class SyringeHumanoidModelMixin {
    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V", at = @At("TAIL"))
    private void tradingCells$syringeArms(HumanoidRenderState state, CallbackInfo callback) {
        SyringeArmAnimation.apply((HumanoidModel<?>) (Object) this, state);
    }
}
