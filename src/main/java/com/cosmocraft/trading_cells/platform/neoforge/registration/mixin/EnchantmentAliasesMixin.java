package com.cosmocraft.trading_cells.platform.neoforge.registration.mixin;

import com.mojang.serialization.Lifecycle;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MappedRegistry.class)
abstract class EnchantmentAliasesMixin {
    // Dynamic registries have no alias registration event. Install before datapack decoding,
    // including reloads and client-side registry copies, so old saved holders resolve normally.
    @Inject(method = "<init>(Lnet/minecraft/resources/ResourceKey;Lcom/mojang/serialization/Lifecycle;Z)V", at = @At("RETURN"))
    private void tradingCells$legacyTouchIds(ResourceKey<?> key, Lifecycle lifecycle, boolean intrusive, CallbackInfo ci) {
        if (!Registries.ENCHANTMENT.equals(key)) { return; }
        Registry<?> registry = (Registry<?>) (Object) this;
        for (String role : new String[]{"farmer", "miner", "warrior"}) {
            registry.addAlias(Identifier.fromNamespaceAndPath("trading_cells", role + "s_touch"),
                    Identifier.fromNamespaceAndPath("trading_cells", role + "_touch"));
        }
    }
}
