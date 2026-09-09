package com.cosmocraft.trading_cells.architecture;

import com.cosmocraft.trading_cells.feature.captures.domain.model.CapturerDurability;

final class CapturerDomainVerification {
    private CapturerDomainVerification() {
    }

    static void verify() {
        verifyCapturerRules();
    }

    private static void verifyCapturerRules() {
        require(CapturerDurability.maximum(10) == 10,
                "The default capturer durability must allow ten releases");
        require(CapturerDurability.maximum(0) == 1,
                "Capturer durability must never become non-positive");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
