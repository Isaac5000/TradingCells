package com.cosmocraft.trading_cells.feature.mobfarm.adapters.mixin;

import com.cosmocraft.trading_cells.feature.mobfarm.adapters.interaction.DisposableInteractionLevel;
import com.cosmocraft.trading_cells.feature.mobfarm.adapters.interaction.InteractionIsolation;
import com.cosmocraft.trading_cells.platform.neoforge.machine.MachineToolEnchantments;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
abstract class InteractionToolWearMixin {
    @Inject(method = "processDurabilityChange(ILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;)I",
            at = @At("HEAD"), cancellable = true)
    private void tradingCells$protectedInteraction(int amount, ServerLevel level, @Nullable LivingEntity owner,
            CallbackInfoReturnable<Integer> callback) {
        if (amount > 0 && level instanceof DisposableInteractionLevel && InteractionIsolation.active()
                && MachineToolEnchantments.farmersTouch((ItemStack) (Object) this, level.registryAccess())) {
            callback.setReturnValue(0);
        }
    }
}
