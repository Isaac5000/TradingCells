package com.cosmocraft.trading_cells.feature.silktouch.adapters.mixin;

import com.cosmocraft.trading_cells.feature.silktouch.adapters.input.PreservedSpawnerItemAdapter;
import com.cosmocraft.trading_cells.feature.silktouch.adapters.input.SilkTouchTwoDropAdapter;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
abstract class BlockItemWarningMixin {
    @Inject(method = "shouldPrintOpWarning", at = @At("HEAD"), cancellable = true)
    private void tradingCells$hideWarningForTrustedSpawner(
            ItemStack stack,
            @Nullable Player player,
            CallbackInfoReturnable<Boolean> callback
    ) {
        if (PreservedSpawnerItemAdapter.isTrustedSpawner(stack)
                || (SilkTouchTwoDropAdapter.hasPreservedDataMarker(stack)
                        && (stack.is(Items.SPAWNER)
                                || stack.is(Items.TRIAL_SPAWNER)
                                || stack.is(Items.VAULT)))) {
            callback.setReturnValue(false);
        }
    }
}
