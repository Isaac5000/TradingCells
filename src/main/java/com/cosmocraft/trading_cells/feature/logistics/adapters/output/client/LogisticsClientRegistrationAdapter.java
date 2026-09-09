package com.cosmocraft.trading_cells.feature.logistics.adapters.output.client;

import com.cosmocraft.trading_cells.feature.logistics.adapters.output.LogisticsRegistrationAdapter;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

public final class LogisticsClientRegistrationAdapter {
    private LogisticsClientRegistrationAdapter() {
    }

    public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(LogisticsRegistrationAdapter.TERMINAL_MENU.get(), NetworkTerminalScreen::new);
        event.register(LogisticsRegistrationAdapter.CRAFTING_TERMINAL_MENU.get(), NetworkTerminalScreen::new);
        event.register(LogisticsRegistrationAdapter.PIPE_MENU.get(), PipeConfigurationScreen::new);
    }
}
