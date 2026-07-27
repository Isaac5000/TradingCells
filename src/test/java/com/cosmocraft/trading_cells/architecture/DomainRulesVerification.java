package com.cosmocraft.trading_cells.architecture;

import com.cosmocraft.trading_cells.feature.trader.domain.model.AutotraderOfferLifecycle;
import com.cosmocraft.trading_cells.feature.trader.domain.model.AutotraderOfferSelection;
import com.cosmocraft.trading_cells.feature.breeders.domain.model.BreederFood;
import com.cosmocraft.trading_cells.feature.breeders.domain.model.BreederKind;
import com.cosmocraft.trading_cells.feature.breeders.domain.model.BreederRecipe;
import com.cosmocraft.trading_cells.feature.breeders.domain.model.BreederRules;
import com.cosmocraft.trading_cells.feature.converter.domain.model.ConverterCycle;
import com.cosmocraft.trading_cells.feature.converter.domain.model.ConverterStage;
import com.cosmocraft.trading_cells.feature.captures.adapters.output.client.CapturedEntityGuiTransform;
import com.cosmocraft.trading_cells.feature.farmer.domain.model.FarmerCycle;
import com.cosmocraft.trading_cells.feature.incubators.domain.model.IncubationCycle;
import com.cosmocraft.trading_cells.feature.ironfarm.domain.model.IronFarmCycle;
import com.cosmocraft.trading_cells.shared.machines.domain.model.TimedProcess;
import com.cosmocraft.trading_cells.feature.milkcookie.application.usecase.CreateMilkCookieUseCaseImp;
import com.cosmocraft.trading_cells.feature.milkcookie.domain.model.MilkCookieCreationRequest;
import com.cosmocraft.trading_cells.feature.trader.domain.model.PiglinBarterCycle;
import com.cosmocraft.trading_cells.feature.trader.domain.model.VillagerOfferPersistence;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.SlotRenderer;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.trader.VillagerTradeScreenLayout;
import com.cosmocraft.trading_cells.platform.neoforge.menu.VillagerTradeMenuLayout;

public final class DomainRulesVerification {
    private DomainRulesVerification() {
    }

    public static void main(String[] args) { // NOSONAR - The JVM entry-point signature requires this parameter.
        verifyOfferLifecycle();
        verifyOfferSelection();
        verifyTimedProcesses();
        verifyBreederRules();
        verifyFarmerRules();
        verifyConverterRules();
        verifyIronFarmRules();
        verifyPiglinBarterRules();
        verifyMilkCookieRules();
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
        BreederRules rules = new BreederRules(100, 200, 3, 12, 2, 4, 64);
        require(BreederRecipe.breedTicks(BreederKind.PIGLIN, rules) == 200,
                "Piglin duration must come from configuration");
        require(BreederRecipe.cost(BreederKind.PIGLIN, BreederFood.PORK, rules) == 2,
                "Piglin pork cost must come from configuration");
        require(BreederRecipe.cost(BreederKind.PIGLIN, BreederFood.CRIMSON_FUNGUS, rules) == 4,
                "Crimson fungus must remain a valid configured food");
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
        IronFarmCycle cycle = new IronFarmCycle(1_200, 2, 4, 8, 80, 16, 5);
        require(cycle.multiplier(1) == 2, "One villager multiplier must be configurable");
        require(cycle.multiplier(3) == 8, "Three villager multiplier must be configurable");
        require(cycle.isGolemVisible(1_120), "The golem must appear during its attack window");
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

    private static void verifyMilkCookieRules() {
        CreateMilkCookieUseCaseImp useCase = new CreateMilkCookieUseCaseImp();
        require(useCase.canCreate(new MilkCookieCreationRequest(true, true)),
                "An adult horse and a cookie must create the item");
        require(!useCase.canCreate(new MilkCookieCreationRequest(true, false)),
                "A non-adult horse must not create the item");
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
