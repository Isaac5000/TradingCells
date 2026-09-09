package com.cosmocraft.trading_cells.platform.neoforge.registration;

import com.cosmocraft.trading_cells.feature.breeders.adapters.output.BreederRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.converter.adapters.output.ConverterRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.farmer.adapters.output.FarmerRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.incubators.adapters.output.IncubatorRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.ironfarm.adapters.output.IronFarmRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.trader.adapters.output.AutotraderRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.trader.adapters.output.TraderRegistrationAdapter;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import com.cosmocraft.trading_cells.platform.neoforge.machine.PortableMachineItemHandler;

/** External transports retain sided inputs and can reach production outputs from every face. */
public final class MachineItemCapabilityRegistration {
    private MachineItemCapabilityRegistration() {
    }

    public static void register(IEventBus bus) {
        bus.addListener(MachineItemCapabilityRegistration::registerCapabilities);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Item.BLOCK,
                AutotraderRegistrationAdapter.AUTOTRADER_BLOCK_ENTITY.get(), PortableMachineItemHandler::new);
        event.registerBlockEntity(Capabilities.Item.BLOCK,
                TraderRegistrationAdapter.PIGLIN_BARTERING_CELL_BLOCK_ENTITY.get(), PortableMachineItemHandler::new);
        event.registerBlockEntity(Capabilities.Item.BLOCK,
                TraderRegistrationAdapter.NETHERITE_PIGLIN_BARTERING_CELL_BLOCK_ENTITY.get(), PortableMachineItemHandler::new);
        event.registerBlockEntity(Capabilities.Item.BLOCK,
                BreederRegistrationAdapter.VILLAGER_BREEDER_BLOCK_ENTITY.get(), PortableMachineItemHandler::new);
        event.registerBlockEntity(Capabilities.Item.BLOCK,
                BreederRegistrationAdapter.PIGLIN_BREEDER_BLOCK_ENTITY.get(), PortableMachineItemHandler::new);
        event.registerBlockEntity(Capabilities.Item.BLOCK,
                IncubatorRegistrationAdapter.VILLAGER_INCUBATOR_BLOCK_ENTITY.get(), PortableMachineItemHandler::new);
        event.registerBlockEntity(Capabilities.Item.BLOCK,
                IncubatorRegistrationAdapter.PIGLIN_INCUBATOR_BLOCK_ENTITY.get(), PortableMachineItemHandler::new);
        event.registerBlockEntity(Capabilities.Item.BLOCK,
                FarmerRegistrationAdapter.FARMER_BLOCK_ENTITY.get(), PortableMachineItemHandler::new);
        event.registerBlockEntity(Capabilities.Item.BLOCK,
                FarmerRegistrationAdapter.PIGLIN_FARMER_BLOCK_ENTITY.get(), PortableMachineItemHandler::new);
        event.registerBlockEntity(Capabilities.Item.BLOCK,
                ConverterRegistrationAdapter.CONVERTER_BLOCK_ENTITY.get(), PortableMachineItemHandler::new);
        event.registerBlockEntity(Capabilities.Item.BLOCK,
                IronFarmRegistrationAdapter.IRON_FARM_BLOCK_ENTITY.get(), PortableMachineItemHandler::new);
    }
}
