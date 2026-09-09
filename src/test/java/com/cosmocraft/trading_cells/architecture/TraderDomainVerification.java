package com.cosmocraft.trading_cells.architecture;

import com.cosmocraft.trading_cells.feature.trader.domain.model.AutotraderOfferLifecycle;
import com.cosmocraft.trading_cells.feature.trader.domain.model.AutotraderOfferSelection;
import com.cosmocraft.trading_cells.feature.trader.domain.model.PiglinBarterCycle;
import com.cosmocraft.trading_cells.feature.trader.domain.model.VillagerOfferPersistence;
import com.cosmocraft.trading_cells.feature.trader.domain.service.TradeDiscountPolicy;
import com.cosmocraft.trading_cells.feature.trader.domain.model.PiglinBarterUpgradeYield;
import java.util.List;

final class TraderDomainVerification {
    private TraderDomainVerification() {
    }

    static void verify() {
        verifyOfferLifecycle();
        verifyOfferSelection();
        verifyTemporaryTradeDiscounts();
        verifyPiglinBarterRules();
    }

    private static void verifyOfferLifecycle() {
        require(
                AutotraderOfferLifecycle.decide(true, true, false)
                        == AutotraderOfferLifecycle.Decision.INITIALIZE,
                "An employed adult without offers must initialize them"
        );
        require(
                AutotraderOfferLifecycle.decide(true, true, true)
                        == AutotraderOfferLifecycle.Decision.KEEP,
                "Existing offers must be preserved"
        );
        require(
                AutotraderOfferLifecycle.decide(true, false, false)
                        == AutotraderOfferLifecycle.Decision.UNAVAILABLE,
                "An unemployed villager must remain without offers"
        );
        require(
                AutotraderOfferLifecycle.decide(false, true, false)
                        == AutotraderOfferLifecycle.Decision.UNAVAILABLE,
                "A baby villager must remain without offers"
        );
        require(
                VillagerOfferPersistence.refreshIntervalTicks(20) == 12_000,
                "Offer persistence must enforce the ten minute minimum"
        );
    }

    private static void verifyOfferSelection() {
        require(AutotraderOfferSelection.normalize(3, 1) == 0,
                "One offer must always select index zero");
        require(AutotraderOfferSelection.normalize(3, 4) == 3,
                "Four offers must preserve their last valid index");
        require(AutotraderOfferSelection.normalize(5, 6) == 5,
                "Six offers must remain selectable");
        require(AutotraderOfferSelection.normalize(10, 6) == 4,
                "Selection must wrap lists larger than four");
    }

    private static void verifyTemporaryTradeDiscounts() {
        List<TradeDiscountPolicy.ActiveDiscount<String>> initial = List.of(
                new TradeDiscountPolicy.ActiveDiscount<>("emerald-book", 110L),
                new TradeDiscountPolicy.ActiveDiscount<>("emerald-bread", 220L)
        );
        List<TradeDiscountPolicy.ActiveDiscount<String>> active =
                TradeDiscountPolicy.active(initial, 120L);
        require(!TradeDiscountPolicy.appliesTo(active, "emerald-book")
                        && TradeDiscountPolicy.appliesTo(active, "emerald-bread"),
                "Each offer discount must expire independently");

        List<TradeDiscountPolicy.ActiveDiscount<String>> renewed =
                TradeDiscountPolicy.renew(active, "emerald-bread", 130L);
        renewed = TradeDiscountPolicy.renew(renewed, "emerald-bread", 140L);
        require(renewed.size() == 1
                        && renewed.getFirst().expiresAt() == TradeDiscountPolicy.renewedExpiry(140L),
                "Repeated purchases must renew one layer instead of stacking magnitude");

        List<TradeDiscountPolicy.ActiveDiscount<String>> reordered = List.of(
                new TradeDiscountPolicy.ActiveDiscount<>("second-offer", 300L),
                renewed.getFirst()
        );
        require(TradeDiscountPolicy.appliesTo(reordered, "emerald-bread"),
                "Discount lookup must follow stable offer identity rather than list index");
        require(TradeDiscountPolicy.nextExpiry(reordered) == 300L,
                "The earliest independent offer expiry must schedule the next price refresh");
    }

    private static void verifyPiglinBarterRules() {
        PiglinBarterCycle.Step started = PiglinBarterCycle.advance(0, 20, true);
        require(started.ticksRemaining() == 20
                        && started.transition() == PiglinBarterCycle.Transition.STARTED,
                "A valid barter must use the configured duration");
        require(PiglinBarterCycle.advance(1, 20, false).transition()
                        == PiglinBarterCycle.Transition.COMPLETED,
                "A barter must complete when its countdown reaches zero");
        require(PiglinBarterUpgradeYield.upgradedAmount(10, 64, 4) == 15,
                "The diamond piglin-barter upgrade must apply a x1.5 yield multiplier");
        require(PiglinBarterUpgradeYield.upgradedAmount(10, 64, 5) == 20,
                "The netherite piglin-barter upgrade must apply a x2 yield multiplier");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
