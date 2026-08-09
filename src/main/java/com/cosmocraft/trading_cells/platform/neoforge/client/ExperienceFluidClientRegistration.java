package com.cosmocraft.trading_cells.platform.neoforge.client;

import com.cosmocraft.trading_cells.platform.neoforge.registration.ExperienceFluidRegistration;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.RegisterFluidModelsEvent;
import net.neoforged.neoforge.client.fluid.FluidTintSource;

public final class ExperienceFluidClientRegistration {
    private static final Material STILL = new Material(Identifier.withDefaultNamespace("block/water_still"));
    private static final Material FLOWING = new Material(Identifier.withDefaultNamespace("block/water_flow"));
    private static final int EXPERIENCE_GREEN = 0xFF5DDF73;

    private ExperienceFluidClientRegistration() {
    }

    public static void onRegisterFluidModels(RegisterFluidModelsEvent event) {
        FluidTintSource tint = state -> EXPERIENCE_GREEN;
        event.register(
                new FluidModel.Unbaked(STILL, FLOWING, null, tint),
                ExperienceFluidRegistration.SOURCE,
                ExperienceFluidRegistration.FLOWING
        );
    }
}
