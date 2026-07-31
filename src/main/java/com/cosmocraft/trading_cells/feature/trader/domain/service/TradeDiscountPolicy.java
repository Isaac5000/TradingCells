package com.cosmocraft.trading_cells.feature.trader.domain.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Shared rules for temporary per-offer purchase discounts.
 * One offer can contribute at most one temporary price reduction during a window.
 */
public final class TradeDiscountPolicy {
    public static final int TEMPORARY_DISCOUNT_TICKS = 12_000;
    public static final int TEMPORARY_DISCOUNT_PER_OFFER = 1;

    private TradeDiscountPolicy() {
    }

    public static long renewedExpiry(long gameTime) {
        if (gameTime >= Long.MAX_VALUE - TEMPORARY_DISCOUNT_TICKS) {
            return Long.MAX_VALUE;
        }
        return gameTime + TEMPORARY_DISCOUNT_TICKS;
    }

    public static <T> List<ActiveDiscount<T>> renew(
            List<ActiveDiscount<T>> discounts,
            T offerIdentity,
            long gameTime
    ) {
        List<ActiveDiscount<T>> active = active(discounts, gameTime);
        List<ActiveDiscount<T>> renewed = new ArrayList<>(active.size() + 1);
        for (ActiveDiscount<T> discount : active) {
            if (!discount.offerIdentity().equals(offerIdentity)) {
                renewed.add(discount);
            }
        }
        renewed.add(new ActiveDiscount<>(offerIdentity, renewedExpiry(gameTime)));
        return List.copyOf(renewed);
    }

    public static <T> List<ActiveDiscount<T>> active(
            List<ActiveDiscount<T>> discounts,
            long gameTime
    ) {
        Map<T, Long> latestExpiryByOffer = new LinkedHashMap<>();
        for (ActiveDiscount<T> discount : discounts) {
            if (discount.expiresAt() > gameTime) {
                latestExpiryByOffer.merge(discount.offerIdentity(), discount.expiresAt(), Math::max);
            }
        }
        List<ActiveDiscount<T>> active = new ArrayList<>(latestExpiryByOffer.size());
        latestExpiryByOffer.forEach((identity, expiry) -> active.add(new ActiveDiscount<>(identity, expiry)));
        return List.copyOf(active);
    }

    public static <T> boolean appliesTo(List<ActiveDiscount<T>> discounts, T offerIdentity) {
        for (ActiveDiscount<T> discount : discounts) {
            if (discount.offerIdentity().equals(offerIdentity)) {
                return true;
            }
        }
        return false;
    }

    public static <T> long nextExpiry(List<ActiveDiscount<T>> discounts) {
        long nextExpiry = Long.MAX_VALUE;
        for (ActiveDiscount<T> discount : discounts) {
            nextExpiry = Math.min(nextExpiry, discount.expiresAt());
        }
        return nextExpiry;
    }

    public record ActiveDiscount<T>(T offerIdentity, long expiresAt) {
    }
}
