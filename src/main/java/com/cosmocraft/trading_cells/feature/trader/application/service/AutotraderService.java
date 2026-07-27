package com.cosmocraft.trading_cells.feature.trader.application.service;

import com.cosmocraft.trading_cells.feature.trader.application.port.input.AutotraderUseCase;
import com.cosmocraft.trading_cells.feature.trader.application.port.output.TraderSettingsPort;
import com.cosmocraft.trading_cells.feature.trader.domain.model.AutotraderOfferLifecycle;
import com.cosmocraft.trading_cells.feature.trader.domain.model.AutotraderOfferSelection;
import com.cosmocraft.trading_cells.feature.trader.domain.model.VillagerOfferPersistence;
import java.util.Objects;
import java.util.function.IntUnaryOperator;

/** Application boundary for Autotrader decisions independent of Minecraft types. */
public final class AutotraderService implements AutotraderUseCase {
    private final TraderSettingsPort settings;

    public AutotraderService(TraderSettingsPort settings) {
        this.settings = Objects.requireNonNull(settings);
    }

    public AutotraderOfferLifecycle.Decision offers(
            boolean adult,
            boolean employed,
            boolean hasOffers
    ) {
        return AutotraderOfferLifecycle.decide(adult, employed, hasOffers);
    }

    public int normalizeSelection(int selectedIndex, int offerCount) {
        return AutotraderOfferSelection.normalize(selectedIndex, offerCount);
    }

    public int offerRefreshTicks() {
        return VillagerOfferPersistence.refreshIntervalTicks(
                settings.villagerTradeRefreshTicks()
        );
    }

    public boolean infiniteTrades() {
        return settings.villagerInfiniteTrades();
    }

    public int automaticExperience(
            IntUnaryOperator randomBelow,
            boolean rewardExperience,
            boolean leveledUp
    ) {
        if (!rewardExperience) {
            return 0;
        }
        int minimum = Math.min(
                settings.autotraderMinimumExperience(),
                settings.autotraderMaximumExperience()
        );
        int maximum = Math.max(
                settings.autotraderMinimumExperience(),
                settings.autotraderMaximumExperience()
        );
        int reward = minimum + randomBelow.applyAsInt(maximum - minimum + 1);
        return leveledUp
                ? reward + Math.max(0, settings.autotraderLevelUpExperienceBonus())
                : reward;
    }
}
