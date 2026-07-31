package com.cosmocraft.trading_cells.platform.neoforge.registration;

import com.cosmocraft.trading_cells.feature.trader.adapters.output.AutotraderRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.breeders.adapters.output.BreederRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.captures.adapters.output.CaptureRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.converter.adapters.output.ConverterRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.farmer.adapters.output.FarmerRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.farmer.adapters.input.FarmerTooltipEventAdapter;
import com.cosmocraft.trading_cells.feature.incubators.adapters.output.IncubatorRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.ironfarm.adapters.output.IronFarmRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.trader.adapters.input.TraderTooltipEventAdapter;
import com.cosmocraft.trading_cells.feature.trader.adapters.output.TraderRegistrationAdapter;
import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.TradingCells;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public class Registration {

    private Registration() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(TradingCells.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TradingCells.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, TradingCells.MOD_ID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, TradingCells.MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU, TradingCells.MOD_ID);

    public static void init(IEventBus modEventBus) {
        loadFeatures(modEventBus);

        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITY_TYPES.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        MENU_TYPES.register(modEventBus);
    }

    private static void loadFeatures(IEventBus modEventBus) {
        CaptureRegistrationAdapter.load();
        TraderRegistrationAdapter.load();
        AutotraderRegistrationAdapter.load();
        ConverterRegistrationAdapter.load();
        BreederRegistrationAdapter.load();
        IncubatorRegistrationAdapter.load();
        FarmerRegistrationAdapter.load();
        IronFarmRegistrationAdapter.load();
        CreativeTabRegistration.load();
        FarmerTooltipEventAdapter.register(modEventBus);
        TraderTooltipEventAdapter.register(modEventBus);
    }
}
