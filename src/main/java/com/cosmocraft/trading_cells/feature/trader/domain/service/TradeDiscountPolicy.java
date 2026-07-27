package com.cosmocraft.trading_cells.feature.trader.domain.service;

/**
 * Shared rules for temporary per-offer purchase discounts.
 * One offer can contribute at most one temporary price reduction during a window.
 */
public final class TradeDiscountPolicy {
    public static final int TEMPORARY_DISCOUNT_TICKS = 12_000;
    public static final int TEMPORARY_DISCOUNT_PER_OFFER = 1;
    public static final int MAX_TRACKED_OFFERS = Long.SIZE;

    private TradeDiscountPolicy() {
    }

    public static long markOffer(long mask, int offerIndex) {
        if (offerIndex < 0 || offerIndex >= MAX_TRACKED_OFFERS) {
            return mask;
        }
        return mask | (1L << offerIndex);
    }

    public static boolean appliesTo(long mask, int offerIndex) {
        return offerIndex >= 0
                && offerIndex < MAX_TRACKED_OFFERS
                && (mask & (1L << offerIndex)) != 0L;
    }

    public static boolean expired(long expiresAt, long gameTime) {
        return expiresAt <= 0L || gameTime >= expiresAt;
    }
}
