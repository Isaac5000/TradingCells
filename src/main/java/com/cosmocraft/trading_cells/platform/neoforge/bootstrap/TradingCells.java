package com.cosmocraft.trading_cells.platform.neoforge.bootstrap;

import com.cosmocraft.trading_cells.platform.neoforge.config.FeatureSettingsProvider;
import com.cosmocraft.trading_cells.platform.neoforge.config.NeoForgeFeatureSettingsAdapter;
import com.cosmocraft.trading_cells.platform.neoforge.network.TradingCellPayloadRegistration;
import com.cosmocraft.trading_cells.platform.neoforge.registration.Registration;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@SuppressWarnings("java:S1118") // Fake
@Mod(TradingCells.MOD_ID)
public class TradingCells {
    public static final String MOD_ID = "trading_cells";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TradingCells(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(TradingCellPayloadRegistration::onRegisterPayloads);

        // 1. Initialize Registries (Output Adapters to Minecraft)
        Registration.init(modEventBus);

        // 2. Register the configuration
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        FeatureSettingsProvider.configure(new NeoForgeFeatureSettingsAdapter());
    }
}
