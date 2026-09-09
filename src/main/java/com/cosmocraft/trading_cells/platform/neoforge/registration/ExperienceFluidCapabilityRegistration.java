package com.cosmocraft.trading_cells.platform.neoforge.registration;

import com.cosmocraft.trading_cells.feature.experience.adapters.output.ExperienceStorageRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.configuredmobfarm.adapters.output.ConfiguredMobFarmRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.creeperfarm.adapters.output.CreeperFarmRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.raiderfarm.adapters.output.RaiderFarmRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.skeletonfarm.adapters.output.SkeletonFarmRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.trader.adapters.output.AutotraderRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.trader.adapters.output.TraderRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.zombiefarm.adapters.output.ZombieFarmRegistrationAdapter;
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
        event.registerBlockEntity(
                Capabilities.Fluid.BLOCK,
                SkeletonFarmRegistrationAdapter.BLOCK_ENTITY.get(),
                (farm, side) -> farm.experienceFluidHandler()
        );
        event.registerBlockEntity(
                Capabilities.Fluid.BLOCK,
                ZombieFarmRegistrationAdapter.BLOCK_ENTITY.get(),
                (farm, side) -> farm.experienceFluidHandler()
        );
        event.registerBlockEntity(
                Capabilities.Fluid.BLOCK,
                RaiderFarmRegistrationAdapter.BLOCK_ENTITY.get(),
                (farm, side) -> farm.experienceFluidHandler()
        );
        event.registerBlockEntity(
                Capabilities.Fluid.BLOCK,
                CreeperFarmRegistrationAdapter.BLOCK_ENTITY.get(),
                (farm, side) -> farm.experienceFluidHandler()
        );
        event.registerBlockEntity(
                Capabilities.Fluid.BLOCK,
                ConfiguredMobFarmRegistrationAdapter.BLOCK_ENTITY.get(),
                (farm, side) -> farm.experienceFluidHandler()
        );
    }
}
