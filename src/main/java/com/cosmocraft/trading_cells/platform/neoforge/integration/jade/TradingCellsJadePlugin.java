package com.cosmocraft.trading_cells.platform.neoforge.integration.jade;

import com.cosmocraft.trading_cells.feature.breeders.adapters.input.BreederBlock;
import com.cosmocraft.trading_cells.feature.breeders.adapters.input.BreederBlockEntity;
import com.cosmocraft.trading_cells.feature.incubators.adapters.input.IncubatorBlock;
import com.cosmocraft.trading_cells.feature.incubators.adapters.input.IncubatorBlockEntity;
import com.cosmocraft.trading_cells.feature.trader.adapters.input.VillagerTradingCellBlock;
import com.cosmocraft.trading_cells.feature.trader.adapters.input.VillagerTradingCellBlockEntity;
import com.cosmocraft.trading_cells.platform.neoforge.machine.AbstractPortableMachineBlock;
import com.cosmocraft.trading_cells.platform.neoforge.machine.PortableMachineBlockEntity;
import net.minecraft.world.level.block.Block;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/** Optional Jade entry point. This class is discovered only when Jade is installed. */
@WailaPlugin
public final class TradingCellsJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(
                PortableMachineJadeDataProvider.INSTANCE,
                PortableMachineBlockEntity.class
        );
        registration.registerBlockDataProvider(
                PortableMachineJadeDataProvider.INSTANCE,
                BreederBlockEntity.class
        );
        registration.registerBlockDataProvider(
                PortableMachineJadeDataProvider.INSTANCE,
                IncubatorBlockEntity.class
        );
        registration.registerBlockDataProvider(
                PortableMachineJadeDataProvider.INSTANCE,
                VillagerTradingCellBlockEntity.class
        );
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(
                PortableMachineJadeComponentProvider.INSTANCE,
                AbstractPortableMachineBlock.class
        );
        registration.registerBlockComponent(
                PortableMachineJadeComponentProvider.INSTANCE,
                BreederBlock.class
        );
        registration.registerBlockComponent(
                PortableMachineJadeComponentProvider.INSTANCE,
                IncubatorBlock.class
        );
        registration.registerBlockComponent(
                PortableMachineJadeComponentProvider.INSTANCE,
                VillagerTradingCellBlock.class
        );
        registration.registerBlockComponent(SilkTouchTwoJadeProvider.INSTANCE, Block.class);
    }
}
