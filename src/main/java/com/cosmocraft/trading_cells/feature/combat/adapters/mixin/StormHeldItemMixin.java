package com.cosmocraft.trading_cells.feature.combat.adapters.mixin;

import com.cosmocraft.trading_cells.feature.combat.adapters.output.client.StormArmAnimation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandLayer.class)
public abstract class StormHeldItemMixin {
    @Inject(method = "submitArmWithItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V"))
    private void stormReload(ArmedEntityRenderState state, ItemStackRenderState item, ItemStack stack, HumanoidArm arm,
            PoseStack pose, SubmitNodeCollector collector, int light, CallbackInfo ci) {
        StormArmAnimation.thirdPersonItem(pose, state, arm, stack);
    }
}
