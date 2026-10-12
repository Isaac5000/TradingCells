package com.cosmocraft.trading_cells.feature.combat.adapters.mixin;

import com.cosmocraft.trading_cells.feature.combat.adapters.output.client.StormArmAnimation;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidModel.class)
public abstract class StormHumanoidModelMixin {
    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V", at = @At("TAIL"))
    private void stormArms(HumanoidRenderState state, CallbackInfo ci) {
        StormArmAnimation.apply((HumanoidModel<?>) (Object) this, state);
    }
}
