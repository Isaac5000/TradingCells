package com.cosmocraft.trading_cells.platform.neoforge.registration;

import com.cosmocraft.trading_cells.feature.experience.adapters.output.ExperienceStorageRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.trader.adapters.output.AutotraderRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.trader.adapters.output.TraderRegistrationAdapter;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

public final class ExperienceFluidCapabilityRegistration {
    private ExperienceFluidCapabilityRegistration() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(ExperienceFluidCapabilityRegistration::onRegisterCapabilities);
    }

    private static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.Fluid.BLOCK,
                ExperienceStorageRegistrationAdapter.BLOCK_ENTITY.get(),
                (storage, side) -> storage.fluidHandler()
        );
        event.registerBlockEntity(
                Capabilities.Fluid.BLOCK,
                TraderRegistrationAdapter.VILLAGER_TRADING_CELL_BLOCK_ENTITY.get(),
                (trader, side) -> trader.experienceFluidHandler()
        );
        event.registerBlockEntity(
                Capabilities.Fluid.BLOCK,
                AutotraderRegistrationAdapter.AUTOTRADER_BLOCK_ENTITY.get(),
                (autotrader, side) -> autotrader.experienceFluidHandler()
        );
    }
}
