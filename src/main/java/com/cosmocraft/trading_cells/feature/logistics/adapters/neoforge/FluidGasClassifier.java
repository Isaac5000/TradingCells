package com.cosmocraft.trading_cells.feature.logistics.adapters.neoforge;

import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.TradingCells;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

public final class FluidGasClassifier {
    public static final TagKey<net.minecraft.world.level.material.Fluid> GASES = TagKey.create(
            Registries.FLUID,
            Identifier.fromNamespaceAndPath(TradingCells.MOD_ID, "gases")
    );
    public static final TagKey<net.minecraft.world.level.material.Fluid> NOT_GASES = TagKey.create(
            Registries.FLUID,
            Identifier.fromNamespaceAndPath(TradingCells.MOD_ID, "not_gases")
    );

    private FluidGasClassifier() {
    }

    public static boolean isGas(FluidResource resource) {
        if (resource == null || resource.isEmpty() || resource.typeHolder().is(NOT_GASES)) {
            return false;
        }
        return resource.typeHolder().is(GASES) || resource.getFluidType().isLighterThanAir();
    }
}
