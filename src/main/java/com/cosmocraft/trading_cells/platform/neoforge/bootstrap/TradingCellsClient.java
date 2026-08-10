package com.cosmocraft.trading_cells.platform.neoforge.bootstrap;

import com.cosmocraft.trading_cells.feature.trader.adapters.output.client.AutotraderClientRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.trader.adapters.output.client.NetheritePiglinBarteringCellClientRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.breeders.adapters.output.client.BreederClientRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.converter.adapters.output.client.ConverterClientRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.farmer.adapters.output.client.FarmerClientRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.experience.adapters.output.client.ExperienceStorageClientRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.incubators.adapters.output.client.IncubatorClientRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.infusion.adapters.output.client.ArcaneInfuserClientRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.ironfarm.adapters.output.client.IronFarmClientRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.quarry.adapters.output.client.QuarryClientRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.skeletonfarm.adapters.output.client.SkeletonFarmClientRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.trader.adapters.output.client.VillagerTradingCellClientRegistrationAdapter;
import com.cosmocraft.trading_cells.platform.neoforge.client.network.TradingCellClientPayloadRegistration;
import com.cosmocraft.trading_cells.platform.neoforge.client.ExperienceFluidClientRegistration;
import com.cosmocraft.trading_cells.platform.neoforge.event.CapturerClientEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
// NeoForge event bus listener for RenderHand removed: we rely on model-driven special renderers now.

// Only client side
@Mod(value = TradingCells.MOD_ID, dist = Dist.CLIENT)
public class TradingCellsClient {

    public TradingCellsClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        var modBus = container.getEventBus();
        if (modBus != null) {
            modBus.addListener(TradingCellClientPayloadRegistration::onRegisterClientPayloads);
            modBus.addListener(ExperienceFluidClientRegistration::onRegisterFluidModels);
            modBus.addListener(CapturerClientEvent::onRegisterSpecialModelRenderer);
            modBus.addListener(CapturerClientEvent::onRegisterBlockEntityRenderers);
            modBus.addListener(BreederClientRegistrationAdapter::onRegisterMenuScreens);
            modBus.addListener(IncubatorClientRegistrationAdapter::onRegisterMenuScreens);
            modBus.addListener(IncubatorClientRegistrationAdapter::onRegisterRenderers);
            modBus.addListener(FarmerClientRegistrationAdapter::onRegisterMenuScreens);
            modBus.addListener(FarmerClientRegistrationAdapter::onRegisterRenderers);
            modBus.addListener(ExperienceStorageClientRegistrationAdapter::onRegisterMenuScreens);
            modBus.addListener(ExperienceStorageClientRegistrationAdapter::onRegisterRenderers);
            modBus.addListener(ArcaneInfuserClientRegistrationAdapter::onRegisterMenuScreens);
            modBus.addListener(ArcaneInfuserClientRegistrationAdapter::onRegisterRenderers);
            modBus.addListener(AutotraderClientRegistrationAdapter::onRegisterMenuScreens);
            modBus.addListener(VillagerTradingCellClientRegistrationAdapter::onRegisterMenuScreens);
            modBus.addListener(NetheritePiglinBarteringCellClientRegistrationAdapter::onRegisterMenuScreens);
            modBus.addListener(NetheritePiglinBarteringCellClientRegistrationAdapter::onRegisterRenderers);
            modBus.addListener(AutotraderClientRegistrationAdapter::onRegisterRenderers);
            modBus.addListener(IronFarmClientRegistrationAdapter::onRegisterMenuScreens);
            modBus.addListener(IronFarmClientRegistrationAdapter::onRegisterRenderers);
            modBus.addListener(ConverterClientRegistrationAdapter::onRegisterMenuScreens);
            modBus.addListener(ConverterClientRegistrationAdapter::onRegisterRenderers);
            modBus.addListener(QuarryClientRegistrationAdapter::onRegisterMenuScreens);
            modBus.addListener(QuarryClientRegistrationAdapter::onRegisterRenderers);
            modBus.addListener(SkeletonFarmClientRegistrationAdapter::onRegisterMenuScreens);
            modBus.addListener(SkeletonFarmClientRegistrationAdapter::onRegisterRenderers);
        }
        // Rendering is handled by SpecialModelRenderers selected by JSON.
    }
}
