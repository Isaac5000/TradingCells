package com.cosmocraft.trading_cells.feature.trader.adapters.output.client;

import com.cosmocraft.trading_cells.feature.trader.adapters.output.TraderRegistrationAdapter;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

public final class VillagerTradingCellClientRegistrationAdapter {
    private VillagerTradingCellClientRegistrationAdapter() {
    }

    public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(
                TraderRegistrationAdapter.VILLAGER_TRADING_CELL_MENU.get(),
                VillagerTradingCellScreen::new
        );
    }
}
