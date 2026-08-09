package com.cosmocraft.trading_cells.platform.neoforge.registration;

import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class ExperienceFluidRegistration {
    public static final String FLUID_ID = "liquid_experience";
    private static final String FLOWING_FLUID_ID = "flowing_liquid_experience";

    public static final DeferredHolder<FluidType, FluidType> FLUID_TYPE = Registration.FLUID_TYPES.register(
            FLUID_ID,
            () -> new FluidType(FluidType.Properties.create()
                    .density(800)
                    .viscosity(1_200)
                    .lightLevel(5))
    );
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> SOURCE = Registration.FLUIDS.register(
            FLUID_ID,
            () -> new BaseFlowingFluid.Source(properties())
    );
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FLOWING = Registration.FLUIDS.register(
            FLOWING_FLUID_ID,
            () -> new BaseFlowingFluid.Flowing(properties())
    );

    private ExperienceFluidRegistration() {
    }

    public static void load() {
        // Forces class loading before the central deferred registers are attached.
    }

    private static BaseFlowingFluid.Properties properties() {
        return new BaseFlowingFluid.Properties(FLUID_TYPE, SOURCE, FLOWING)
                .slopeFindDistance(2)
                .levelDecreasePerBlock(2)
                .tickRate(10);
    }
}
