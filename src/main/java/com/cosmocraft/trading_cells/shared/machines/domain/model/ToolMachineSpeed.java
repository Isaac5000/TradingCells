package com.cosmocraft.trading_cells.shared.machines.domain.model;

/** Tool material determines the base cycle; a separate upgrade accelerates it. */
public final class ToolMachineSpeed {
    private ToolMachineSpeed() { }

    public static int apply(int baseTicks, int upgradeTier) {
        int divisor = 1 << Math.clamp(upgradeTier, 0, 5);
        return (int) Math.max(20L, ((long) Math.max(20, baseTicks) + divisor - 1) / divisor);
    }

    public static int baseTicks(double toolPosition, double netheritePosition) {
        double position = Double.isFinite(toolPosition) ? Math.max(0, toolPosition) : 0;
        double ratio = Math.pow(1.0 / 6, 1.0 / Math.max(1, netheritePosition));
        double seconds = position <= netheritePosition ? 120 * Math.pow(ratio, position)
                : 1 + 19 * Math.pow(ratio, position - netheritePosition);
        return Math.max(1, (int) Math.floor(seconds + 1.0E-9)) * 20;
    }
}
