package com.cosmocraft.trading_cells.architecture;

import com.cosmocraft.trading_cells.feature.combat.domain.model.DecapitationRules;
import com.cosmocraft.trading_cells.feature.combat.domain.model.StormShardDropRules;
import com.cosmocraft.trading_cells.feature.combat.domain.model.StormReloadGesture;

/** Combat contracts survive removal of the old farm-specific test fixtures. */
final class CombatDomainVerification {
    private CombatDomainVerification() { }

    static void verify() {
        verifyReloadGesture();
        require(DecapitationRules.vanillaHeadChance(0) == 0.025D
                        && DecapitationRules.vanillaHeadChance(3) == 0.055D,
                "Vanilla skull chance retains Looting");
        require(Math.abs(DecapitationRules.decapitationHeadChance(1) - 0.035D) < 1.0E-12D
                        && Math.abs(DecapitationRules.decapitationHeadChance(6) - 0.085D) < 1.0E-12D
                        && Math.abs(DecapitationRules.decapitationHeadChance(30) - 0.325D) < 1.0E-12D
                        && DecapitationRules.decapitationHeadChance(255) == 1.0D,
                "Decapitation scales and caps at certainty");
        require(DecapitationRules.farmHeadChance(30, false, 0) == 0.0D
                        && Math.abs(DecapitationRules.farmHeadChance(0, false, 3) - 0.055D) < 1.0E-12D
                        && Math.abs(DecapitationRules.farmHeadChance(30, false, 3) - 0.055D) < 1.0E-12D
                        && DecapitationRules.farmHeadChance(0, false, 255) == 1.0D,
                "Non-native heads require Decapitation");
        require(Math.abs(DecapitationRules.farmHeadChance(3, true, 6) - 0.115D) < 1.0E-12D,
                "Wither skull chance adds Looting III and Decapitation VI once");
        double combined = 1.0D - (1.0D - DecapitationRules.vanillaHeadChance(3))
                * (1.0D - DecapitationRules.supplementalNativeHeadChance(3, 6));
        require(Math.abs(combined - 0.115D) < 1.0E-12D, "Supplemental roll avoids duplicate heads");
        require(StormShardDropRules.maximumAmount(0) == 1 && StormShardDropRules.maximumAmount(3) == 4,
                "Charged Creeper shard quantity retains Looting");
    }

    private static void verifyReloadGesture() {
        var gesture = new StormReloadGesture();
        for (int ticks : new int[] {0, 1, 11}) {
            require(gesture.setDown(true) == StormReloadGesture.Action.NONE, "Press waits for tap or hold");
            for (int i = 0; i < ticks; i++) {
                require(gesture.tick() == StormReloadGesture.Action.NONE, "Short hold never ejects");
                require(gesture.setDown(true) == StormReloadGesture.Action.NONE, "Autorepeat is ignored");
            }
            require(gesture.setDown(false) == StormReloadGesture.Action.RELOAD, "Short release reloads once, including between ticks");
            require(gesture.setDown(false) == StormReloadGesture.Action.NONE, "Repeated release does nothing");
        }
        gesture.setDown(true);
        for (int i = 0; i < 11; i++) { gesture.tick(); }
        require(gesture.tick() == StormReloadGesture.Action.EJECT, "Hold ejects at twelve ticks");
        for (int i = 0; i < 40; i++) { require(gesture.tick() == StormReloadGesture.Action.NONE, "Hold ejects only once"); }
        require(gesture.setDown(false) == StormReloadGesture.Action.NONE, "Held release never reloads");
        gesture.setDown(true);
        require(gesture.setDown(false) == StormReloadGesture.Action.RELOAD, "Fresh tap after hold reloads immediately");
        gesture.setDown(true);
        gesture.reset();
        require(gesture.setDown(false) == StormReloadGesture.Action.NONE, "Cancelled gesture never reloads on release");
    }

    private static void require(boolean condition, String message) {
        if (!condition) { throw new AssertionError(message); }
    }
}
