package com.cosmocraft.trading_cells.feature.trader.domain.model;

/**
 * Decides how the Autotrader should treat the captured villager's offers.
 *
 * <p>This policy intentionally has no Minecraft dependencies so the lifecycle
 * rules can be tested without constructing a world or an entity.</p>
 */
public final class AutotraderOfferLifecycle {
    private AutotraderOfferLifecycle() {
    }

    public static Decision decide(boolean adult, boolean employed, boolean hasOffers) {
        if (!adult || !employed) {
            return Decision.UNAVAILABLE;
        }
        return hasOffers ? Decision.KEEP : Decision.INITIALIZE;
    }

    public enum Decision {
        UNAVAILABLE,
        KEEP,
        INITIALIZE
    }
}
