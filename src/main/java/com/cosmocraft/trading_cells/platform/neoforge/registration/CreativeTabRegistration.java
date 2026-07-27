package com.cosmocraft.trading_cells.platform.neoforge.registration;

import com.cosmocraft.trading_cells.feature.trader.adapters.output.AutotraderRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.breeders.adapters.output.BreederRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.captures.adapters.output.CaptureRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.converter.adapters.output.ConverterRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.farmer.adapters.output.FarmerRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.incubators.adapters.output.IncubatorRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.ironfarm.adapters.output.IronFarmRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.trader.adapters.output.TraderRegistrationAdapter;
import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.TradingCells;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class CreativeTabRegistration {
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN_TAB =
            Registration.CREATIVE_MODE_TABS.register("villager_trader_cage_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + TradingCells.MOD_ID))
                    .icon(() -> CaptureRegistrationAdapter.VILLAGER_CAPTURER_ITEM.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(TraderRegistrationAdapter.VILLAGER_TRADER_ITEM.get());
                        output.accept(TraderRegistrationAdapter.PIGLIN_BARTERING_CELL_ITEM.get());
                        output.accept(BreederRegistrationAdapter.VILLAGER_BREEDER_ITEM.get());
                        output.accept(BreederRegistrationAdapter.PIGLIN_BREEDER_ITEM.get());
                        output.accept(IncubatorRegistrationAdapter.VILLAGER_INCUBATOR_ITEM.get());
                        output.accept(IncubatorRegistrationAdapter.PIGLIN_INCUBATOR_ITEM.get());
                        output.accept(FarmerRegistrationAdapter.FARMER_ITEM.get());
                        output.accept(AutotraderRegistrationAdapter.AUTOTRADER_ITEM.get());
                        output.accept(IronFarmRegistrationAdapter.IRON_FARM_ITEM.get());
                        output.accept(ConverterRegistrationAdapter.CONVERTER_ITEM.get());
                        output.accept(CaptureRegistrationAdapter.VILLAGER_CAPTURER_ITEM.get());
                        output.accept(CaptureRegistrationAdapter.PIGLIN_CAPTURER_ITEM.get());
                    })
                    .build());

    private CreativeTabRegistration() {
    }

    public static void load() {
        // Forces class loading so the DeferredRegister entry is created.
    }
}
