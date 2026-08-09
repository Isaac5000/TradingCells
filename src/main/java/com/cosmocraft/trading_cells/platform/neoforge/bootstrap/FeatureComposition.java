package com.cosmocraft.trading_cells.platform.neoforge.bootstrap;

import com.cosmocraft.trading_cells.feature.trader.application.port.input.AutotraderUseCase;
import com.cosmocraft.trading_cells.feature.trader.application.port.input.PiglinBarterUseCase;
import com.cosmocraft.trading_cells.feature.trader.application.port.input.VillagerTraderUseCase;
import com.cosmocraft.trading_cells.feature.trader.application.service.AutotraderService;
import com.cosmocraft.trading_cells.feature.trader.application.service.PiglinBarterService;
import com.cosmocraft.trading_cells.feature.trader.application.service.VillagerTraderService;
import com.cosmocraft.trading_cells.feature.breeders.application.port.input.BreederUseCase;
import com.cosmocraft.trading_cells.feature.breeders.application.service.BreederService;
import com.cosmocraft.trading_cells.feature.captures.application.port.input.CaptureUseCase;
import com.cosmocraft.trading_cells.feature.captures.application.service.CaptureService;
import com.cosmocraft.trading_cells.feature.converter.application.port.input.ConverterUseCase;
import com.cosmocraft.trading_cells.feature.converter.application.service.ConverterService;
import com.cosmocraft.trading_cells.feature.farmer.application.port.input.FarmerUseCase;
import com.cosmocraft.trading_cells.feature.farmer.application.service.FarmerService;
import com.cosmocraft.trading_cells.feature.experience.application.port.input.ExperienceStorageUseCase;
import com.cosmocraft.trading_cells.feature.experience.application.service.ExperienceStorageService;
import com.cosmocraft.trading_cells.feature.incubators.application.port.input.IncubatorUseCase;
import com.cosmocraft.trading_cells.feature.incubators.application.service.IncubatorService;
import com.cosmocraft.trading_cells.feature.infusion.application.port.input.ArcaneInfusionUseCase;
import com.cosmocraft.trading_cells.feature.infusion.application.service.ArcaneInfusionService;
import com.cosmocraft.trading_cells.feature.ironfarm.application.port.input.IronFarmUseCase;
import com.cosmocraft.trading_cells.feature.ironfarm.application.service.IronFarmService;
import com.cosmocraft.trading_cells.feature.quarry.application.port.input.QuarryUseCase;
import com.cosmocraft.trading_cells.feature.quarry.application.service.QuarryService;
import com.cosmocraft.trading_cells.platform.neoforge.config.FeatureSettingsProvider;

/** NeoForge composition root for feature use cases. */
public final class FeatureComposition {
    private static final CaptureUseCase CAPTURES = new CaptureService(
            () -> FeatureSettingsProvider.values().capturerDurability()
    );
    private static final ExperienceStorageUseCase EXPERIENCE_STORAGE = new ExperienceStorageService();
    private static final ArcaneInfusionUseCase ARCANE_INFUSION = new ArcaneInfusionService();

    private FeatureComposition() {
    }

    public static AutotraderUseCase autotrader() {
        return new AutotraderService(FeatureSettingsProvider.values());
    }

    public static BreederUseCase breeder() {
        return new BreederService(FeatureSettingsProvider.values());
    }

    public static CaptureUseCase captures() {
        return CAPTURES;
    }

    public static ConverterUseCase converter() {
        return new ConverterService(FeatureSettingsProvider.values());
    }

    public static FarmerUseCase farmer() {
        return new FarmerService(FeatureSettingsProvider.values());
    }

    public static ExperienceStorageUseCase experienceStorage() {
        return EXPERIENCE_STORAGE;
    }

    public static ArcaneInfusionUseCase arcaneInfusion() {
        return ARCANE_INFUSION;
    }

    public static IncubatorUseCase incubator() {
        return new IncubatorService(FeatureSettingsProvider.values());
    }

    public static IronFarmUseCase ironFarm() {
        return new IronFarmService(FeatureSettingsProvider.values());
    }

    public static QuarryUseCase quarry() {
        return new QuarryService();
    }

    public static VillagerTraderUseCase villagerTrader() {
        return new VillagerTraderService(FeatureSettingsProvider.values());
    }

    public static PiglinBarterUseCase piglinBarter() {
        return new PiglinBarterService(FeatureSettingsProvider.values());
    }
}
