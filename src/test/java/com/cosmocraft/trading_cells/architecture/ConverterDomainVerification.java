package com.cosmocraft.trading_cells.architecture;

import com.cosmocraft.trading_cells.feature.converter.domain.model.ConverterCycle;
import com.cosmocraft.trading_cells.feature.converter.domain.model.ConverterStage;

final class ConverterDomainVerification {
    private ConverterDomainVerification() {
    }

    static void verify() {
        verifyConverterRules();
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

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
