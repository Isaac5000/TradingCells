package com.cosmocraft.trading_cells.feature.logistics.domain.model;

public final class NetworkAmountFormat {
    private NetworkAmountFormat() { }

    public static String compact(long amount, String language) {
        boolean spanish = language.startsWith("es");
        if (amount >= 1_000_000_000_000_000_000L) { return amount / 1_000_000_000_000_000_000L + (spanish ? "T" : "Qi"); }
        if (!spanish && amount >= 1_000_000_000_000_000L) { return amount / 1_000_000_000_000_000L + "Q"; }
        if (amount >= 1_000_000_000_000L) { return amount / 1_000_000_000_000L + (spanish ? "B" : "T"); }
        if (!spanish && amount >= 1_000_000_000L) { return amount / 1_000_000_000L + "B"; }
        if (amount >= 1_000_000L) { return amount / 1_000_000L + "M"; }
        if (amount >= 1_000L) { return amount / 1_000L + "k"; }
        return Long.toString(amount);
    }

    public static float scaleToFit(int textWidth, int availableWidth) {
        return Math.min(1.0F, Math.max(1, availableWidth) / (float) Math.max(1, textWidth));
    }
}
