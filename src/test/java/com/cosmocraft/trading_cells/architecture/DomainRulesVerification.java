package com.cosmocraft.trading_cells.architecture;

import com.cosmocraft.trading_cells.feature.trader.domain.model.AutotraderOfferLifecycle;
import com.cosmocraft.trading_cells.feature.trader.domain.model.AutotraderOfferSelection;
import com.cosmocraft.trading_cells.feature.breeders.domain.model.BreederFood;
import com.cosmocraft.trading_cells.feature.breeders.domain.model.BreederKind;
import com.cosmocraft.trading_cells.feature.breeders.domain.model.BreederRecipe;
import com.cosmocraft.trading_cells.feature.breeders.domain.model.BreederRules;
import com.cosmocraft.trading_cells.feature.converter.domain.model.ConverterCycle;
import com.cosmocraft.trading_cells.feature.converter.domain.model.ConverterStage;
import com.cosmocraft.trading_cells.feature.captures.domain.model.CapturerDurability;
import com.cosmocraft.trading_cells.feature.captures.adapters.output.client.CapturedEntityGuiTransform;
import com.cosmocraft.trading_cells.feature.farmer.domain.model.FarmerCycle;
import com.cosmocraft.trading_cells.feature.incubators.domain.model.IncubationCycle;
import com.cosmocraft.trading_cells.feature.ironfarm.domain.model.IronFarmCycle;
import com.cosmocraft.trading_cells.shared.machines.domain.model.TimedProcess;
import com.cosmocraft.trading_cells.feature.trader.domain.model.PiglinBarterCycle;
import com.cosmocraft.trading_cells.feature.trader.domain.model.VillagerOfferPersistence;
import com.cosmocraft.trading_cells.feature.trader.domain.service.TradeDiscountPolicy;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.SlotRenderer;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.trader.VillagerTradeScreenLayout;
import com.cosmocraft.trading_cells.platform.neoforge.menu.VillagerTradeMenuLayout;
import java.util.List;

public final class DomainRulesVerification {
    private DomainRulesVerification() {
    }

    public static void main(String[] args) { // NOSONAR - The JVM entry-point signature requires this parameter.
        verifyOfferLifecycle();
        verifyOfferSelection();
        verifyTemporaryTradeDiscounts();
        verifyTimedProcesses();
        verifyBreederRules();
        verifyFarmerRules();
        verifyConverterRules();
        verifyIronFarmRules();
        verifyPiglinBarterRules();
        verifyCapturerRules();
        verifyCapturedEntityGuiCenter();
        verifyVillagerTradeGeometry();
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

    private static void verifyTimedProcesses() {
        TimedProcess.Step paused = TimedProcess.advance(
                7,
                20,
                TimedProcess.Availability.BLOCKED
        );
        require(paused.ticks() == 7 && paused.transition() == TimedProcess.Transition.PAUSED,
                "A blocked output must pause progress");

        TimedProcess.Step completed = IncubationCycle.advance(9, 10, true, true);
        require(completed.transition() == TimedProcess.Transition.COMPLETED,
                "Incubation must complete on the configured server tick");

        TimedProcess.Step reset = IncubationCycle.advance(5, 10, false, true);
        require(reset.ticks() == 0 && reset.transition() == TimedProcess.Transition.RESET,
                "Invalid incubation input must reset progress");
    }

    private static void verifyBreederRules() {
        BreederRules rules = new BreederRules(100, 200, 3, 12, 64);
        require(BreederRecipe.breedTicks(BreederKind.PIGLIN, rules) == 200,
                "Piglin duration must come from configuration");
        require(BreederRecipe.cost(BreederKind.PIGLIN, BreederFood.COOKED_PORKCHOP, rules) == 2,
                "Cooked porkchops must cost two");
        require(BreederRecipe.cost(BreederKind.PIGLIN, BreederFood.NETHER_WART_BLOCK, rules) == 2,
                "Nether wart blocks must cost two");
        require(BreederRecipe.cost(BreederKind.PIGLIN, BreederFood.RAW_PORKCHOP, rules) == 4,
                "Raw porkchops must cost four");
        require(BreederRecipe.cost(BreederKind.PIGLIN, BreederFood.CRIMSON_FUNGUS, rules) == 6,
                "Crimson fungi must cost six");
        require(BreederRecipe.cost(BreederKind.PIGLIN, BreederFood.NETHER_WART, rules) == 12,
                "Nether wart must cost twelve");
    }

    private static void verifyFarmerRules() {
        require(FarmerCycle.rescaleProgress(50, 100, 200) == 100,
                "Changing a stack count or tool must preserve proportional progress");
        require(FarmerCycle.effectiveGrowthTicks(3_000, 9.0D, 5) < 3_000,
                "Efficiency must reduce the real growth duration");
    }

    private static void verifyConverterRules() {
        ConverterCycle.Step started = ConverterCycle.advance(
                ConverterStage.IDLE,
                0,
                true,
                true,
                false,
                1,
                1
        );
        require(started.transition() == ConverterCycle.Transition.STARTED,
                "A valid converter must start");

        ConverterCycle.Step infected = ConverterCycle.advance(
                started.stage(),
                started.ticks(),
                true,
                false,
                false,
                1,
                1
        );
        require(infected.transition() == ConverterCycle.Transition.INFECTED,
                "Infection must transition into curing");

        ConverterCycle.Step cured = ConverterCycle.advance(
                infected.stage(),
                infected.ticks(),
                true,
                false,
                false,
                1,
                1
        );
        require(cured.transition() == ConverterCycle.Transition.CURED,
                "Curing must return the converter to idle");
    }

    private static void verifyIronFarmRules() {
        IronFarmCycle cycle = new IronFarmCycle(
                1_200,
                IronFarmCycle.BASE_ONE_VILLAGER_MULTIPLIER,
                IronFarmCycle.BASE_TWO_VILLAGER_MULTIPLIER,
                IronFarmCycle.BASE_THREE_VILLAGER_MULTIPLIER,
                80,
                16,
                5
        );
        require(cycle.multiplier(1) == 1, "One villager must use the x1 base multiplier");
        require(cycle.multiplier(3) == 3, "Three villagers must use the x3 base multiplier");
        require(cycle.isGolemVisible(1_120), "The golem must appear during its attack window");

        int bonus = 15;
        IronFarmCycle configuredCycle = new IronFarmCycle(
                1_200,
                IronFarmCycle.BASE_ONE_VILLAGER_MULTIPLIER + bonus,
                IronFarmCycle.BASE_TWO_VILLAGER_MULTIPLIER + bonus,
                IronFarmCycle.BASE_THREE_VILLAGER_MULTIPLIER + bonus,
                80,
                16,
                5
        );
        require(configuredCycle.multiplier(1) == 16 && configuredCycle.multiplier(3) == 18,
                "The configured iron-farm value must be added to every base multiplier");
    }

    private static void verifyCapturerRules() {
        require(CapturerDurability.maximum(10) == 10,
                "The default capturer durability must allow ten releases");
        require(CapturerDurability.maximum(0) == 1,
                "Capturer durability must never become non-positive");
    }

    private static void verifyPiglinBarterRules() {
        PiglinBarterCycle.Step started = PiglinBarterCycle.advance(0, 20, true);
        require(started.ticksRemaining() == 20
                        && started.transition() == PiglinBarterCycle.Transition.STARTED,
                "A valid barter must use the configured duration");
        require(PiglinBarterCycle.advance(1, 20, false).transition()
                        == PiglinBarterCycle.Transition.COMPLETED,
                "A barter must complete when its countdown reaches zero");
    }

    private static void verifyCapturedEntityGuiCenter() {
        double adultCenter = CapturedEntityGuiTransform.effectiveCenterX(0.38F);
        require(close(adultCenter, CapturedEntityGuiTransform.effectiveCenterX(0.48F)),
                "Villager adults and babies must share the GUI X center");
        require(close(adultCenter, CapturedEntityGuiTransform.effectiveCenterX(0.55F)),
                "Piglin adults and babies must share the GUI X center");
    }

    private static void verifyVillagerTradeGeometry() {
        require(VillagerTradeMenuLayout.WIDTH == 348 && VillagerTradeMenuLayout.HEIGHT == 210,
                "Villager trade menus must keep their 348x210 visible bounds");
        require(SlotRenderer.FRAME_SIZE == 18 && SlotRenderer.ITEM_SIZE == 16,
                "Trade slots must use an 18x18 frame around a 16x16 item area");
        require(
                VillagerTradeMenuLayout.itemX(VillagerTradeMenuLayout.MANUAL_PAYMENT_A_X)
                        == VillagerTradeMenuLayout.MANUAL_PAYMENT_A_X + 1,
                "Menu item coordinates must match the visual slot inset"
        );
        require(
                VillagerTradeMenuLayout.EQUIPMENT_OFFHAND_Y + SlotRenderer.FRAME_SIZE
                        <= VillagerTradeMenuLayout.INVENTORY_PANEL_Y
                        + VillagerTradeMenuLayout.INVENTORY_PANEL_HEIGHT,
                "Five equipment slots must remain inside the inventory panel"
        );
        require(
                VillagerTradeScreenLayout.AUTOTRADER_DROPDOWN_Y
                        + VillagerTradeScreenLayout.AUTOTRADER_DROPDOWN_HEIGHT + 4
                        <= VillagerTradeMenuLayout.TRADES_CONTENT_Y
                        + VillagerTradeMenuLayout.TRADES_CONTENT_HEIGHT,
                "Autotrader dropdown must remain inside the menu"
        );
        require(
                VillagerTradeScreenLayout.MANUAL_SCROLL_X
                        >= VillagerTradeScreenLayout.MANUAL_ROW_X
                        + VillagerTradeScreenLayout.MANUAL_ROW_WIDTH + 2,
                "Trader scrollbar must keep two pixels of separation from offer rows"
        );
        require(
                VillagerTradeMenuLayout.AUTOTRADER_OUTPUT_Y + SlotRenderer.FRAME_SIZE
                        <= VillagerTradeMenuLayout.AUTOTRADER_OUTPUT_PANEL_Y
                        + VillagerTradeMenuLayout.AUTOTRADER_PANEL_HEIGHT,
                "Autotrader output slots must remain inside their visual panel"
        );
        require(
                VillagerTradeScreenLayout.DROPDOWN_VISIBLE_ROWS == 8
                        && VillagerTradeScreenLayout.dropdownContentHeight(1)
                        == VillagerTradeScreenLayout.DROPDOWN_ROW_HEIGHT
                        && VillagerTradeScreenLayout.dropdownContentHeight(2)
                        == VillagerTradeScreenLayout.DROPDOWN_ROW_HEIGHT * 2
                        && VillagerTradeScreenLayout.dropdownContentHeight(8)
                        == VillagerTradeScreenLayout.AUTOTRADER_DROPDOWN_HEIGHT
                        && VillagerTradeScreenLayout.dropdownContentHeight(9)
                        == VillagerTradeScreenLayout.AUTOTRADER_DROPDOWN_HEIGHT,
                "Autotrader dropdown height must follow its rows up to the eight-row maximum"
        );
    }

    private static boolean close(double left, double right) {
        return Math.abs(left - right) < 1.0E-12D;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
