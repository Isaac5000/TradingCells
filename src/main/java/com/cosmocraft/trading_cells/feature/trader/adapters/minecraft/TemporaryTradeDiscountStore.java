package com.cosmocraft.trading_cells.feature.trader.adapters.minecraft;

import com.cosmocraft.trading_cells.feature.trader.domain.service.TradeDiscountPolicy;
import com.mojang.serialization.DynamicOps;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

/**
 * Minecraft persistence adapter for stable, per-offer temporary discounts.
 * Mutable offer state such as uses, demand and special price is deliberately
 * excluded from the identity.
 */
public final class TemporaryTradeDiscountStore {
    private static final String DISCOUNTS_TAG = "TradingCellsTemporaryDiscounts";
    private static final String OFFER_TAG = "Offer";
    private static final String EXPIRES_AT_TAG = "ExpiresAt";
    private static final String BUY_A_TAG = "BuyA";
    private static final String BUY_B_TAG = "BuyB";
    private static final String SELL_TAG = "Sell";
    private static final String MAX_USES_TAG = "MaxUses";
    private static final String REWARD_EXPERIENCE_TAG = "RewardExperience";
    private static final String PRICE_MULTIPLIER_TAG = "PriceMultiplier";
    private static final String EXPERIENCE_TAG = "Experience";

    private static final String LEGACY_MASK_TAG = "TradingCellsTemporaryDiscountMask";
    private static final String LEGACY_EXPIRES_TAG = "TradingCellsTemporaryDiscountExpires";

    private TemporaryTradeDiscountStore() {
    }

    public static ActiveDiscounts activeDiscounts(
            CompoundTag persistentData,
            MerchantOffers offers,
            long gameTime,
            HolderLookup.Provider registries
    ) {
        ReadResult stored =
                readAndMigrate(persistentData, offers, gameTime, registries);
        List<TradeDiscountPolicy.ActiveDiscount<CompoundTag>> active =
                TradeDiscountPolicy.active(stored.discounts(), gameTime);
        if (stored.migrated() || !active.equals(stored.discounts())) {
            write(persistentData, active);
        }
        return new ActiveDiscounts(active, registries);
    }

    public static void markOffer(
            CompoundTag persistentData,
            MerchantOffers offers,
            MerchantOffer offer,
            long gameTime,
            HolderLookup.Provider registries
    ) {
        ReadResult stored =
                readAndMigrate(persistentData, offers, gameTime, registries);
        CompoundTag identity = identityOf(offer, registries);
        List<TradeDiscountPolicy.ActiveDiscount<CompoundTag>> active =
                TradeDiscountPolicy.active(stored.discounts(), gameTime);
        long renewedExpiry = TradeDiscountPolicy.renewedExpiry(gameTime);
        for (TradeDiscountPolicy.ActiveDiscount<CompoundTag> discount : active) {
            if (discount.offerIdentity().equals(identity) && discount.expiresAt() == renewedExpiry) {
                if (stored.migrated() || !active.equals(stored.discounts())) {
                    write(persistentData, active);
                }
                return;
            }
        }
        List<TradeDiscountPolicy.ActiveDiscount<CompoundTag>> renewed =
                TradeDiscountPolicy.renew(active, identity, gameTime);
        write(persistentData, renewed);
    }

    private static ReadResult readAndMigrate(
            CompoundTag persistentData,
            MerchantOffers offers,
            long gameTime,
            HolderLookup.Provider registries
    ) {
        List<TradeDiscountPolicy.ActiveDiscount<CompoundTag>> discounts = read(persistentData);
        if (!persistentData.contains(LEGACY_MASK_TAG) && !persistentData.contains(LEGACY_EXPIRES_TAG)) {
            return new ReadResult(discounts, false);
        }

        long mask = persistentData.getLongOr(LEGACY_MASK_TAG, 0L);
        long expiresAt = persistentData.getLongOr(LEGACY_EXPIRES_TAG, 0L);
        if (expiresAt > gameTime) {
            int migratedOffers = Math.min(Long.SIZE, offers.size());
            for (int index = 0; index < migratedOffers; index++) {
                if ((mask & (1L << index)) != 0L) {
                    discounts.add(new TradeDiscountPolicy.ActiveDiscount<>(
                            identityOf(offers.get(index), registries),
                            expiresAt
                    ));
                }
            }
        }
        persistentData.remove(LEGACY_MASK_TAG);
        persistentData.remove(LEGACY_EXPIRES_TAG);
        return new ReadResult(discounts, true);
    }

    private static List<TradeDiscountPolicy.ActiveDiscount<CompoundTag>> read(CompoundTag persistentData) {
        List<TradeDiscountPolicy.ActiveDiscount<CompoundTag>> discounts = new ArrayList<>();
        for (Tag rawEntry : persistentData.getListOrEmpty(DISCOUNTS_TAG)) {
            if (rawEntry instanceof CompoundTag entry) {
                CompoundTag identity = entry.getCompoundOrEmpty(OFFER_TAG);
                long expiresAt = entry.getLongOr(EXPIRES_AT_TAG, 0L);
                if (!identity.isEmpty() && expiresAt > 0L) {
                    discounts.add(new TradeDiscountPolicy.ActiveDiscount<>(identity.copy(), expiresAt));
                }
            }
        }
        return discounts;
    }

    private static void write(
            CompoundTag persistentData,
            List<TradeDiscountPolicy.ActiveDiscount<CompoundTag>> discounts
    ) {
        if (discounts.isEmpty()) {
            persistentData.remove(DISCOUNTS_TAG);
            return;
        }
        ListTag serialized = new ListTag();
        for (TradeDiscountPolicy.ActiveDiscount<CompoundTag> discount : discounts) {
            CompoundTag entry = new CompoundTag();
            entry.put(OFFER_TAG, discount.offerIdentity().copy());
            entry.putLong(EXPIRES_AT_TAG, discount.expiresAt());
            serialized.add(entry);
        }
        persistentData.put(DISCOUNTS_TAG, serialized);
    }

    private static CompoundTag identityOf(
            MerchantOffer offer,
            HolderLookup.Provider registries
    ) {
        DynamicOps<Tag> ops = registries.createSerializationContext(NbtOps.INSTANCE);
        CompoundTag identity = new CompoundTag();
        identity.store(BUY_A_TAG, ItemCost.CODEC, ops, offer.getItemCostA());
        offer.getItemCostB().ifPresent(cost -> identity.store(BUY_B_TAG, ItemCost.CODEC, ops, cost));
        identity.store(SELL_TAG, ItemStack.CODEC, ops, offer.getResult().copy());
        identity.putInt(MAX_USES_TAG, offer.getMaxUses());
        identity.putBoolean(REWARD_EXPERIENCE_TAG, offer.shouldRewardExp());
        identity.putFloat(PRICE_MULTIPLIER_TAG, offer.getPriceMultiplier());
        identity.putInt(EXPERIENCE_TAG, offer.getXp());
        return identity;
    }

    public record ActiveDiscounts(
            List<TradeDiscountPolicy.ActiveDiscount<CompoundTag>> entries,
            HolderLookup.Provider registries
    ) {
        public boolean appliesTo(MerchantOffer offer) {
            return TradeDiscountPolicy.appliesTo(entries, identityOf(offer, registries));
        }

        public long nextExpiry() {
            return TradeDiscountPolicy.nextExpiry(entries);
        }
    }

    private record ReadResult(
            List<TradeDiscountPolicy.ActiveDiscount<CompoundTag>> discounts,
            boolean migrated
    ) {
    }
}
