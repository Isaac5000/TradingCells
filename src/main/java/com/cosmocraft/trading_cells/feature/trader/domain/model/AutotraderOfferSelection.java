package com.cosmocraft.trading_cells.feature.trader.domain.model;

/** Selection rules independent of menu size and Minecraft classes. */
public final class AutotraderOfferSelection {
    private AutotraderOfferSelection() {
    }

    public static int normalize(int selectedIndex, int offerCount) {
        return offerCount <= 0 ? 0 : Math.floorMod(selectedIndex, offerCount);
    }
}
