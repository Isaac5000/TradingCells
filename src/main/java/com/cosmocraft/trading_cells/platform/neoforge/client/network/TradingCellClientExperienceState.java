package com.cosmocraft.trading_cells.platform.neoforge.client.network;

/** Client-only snapshot associated with one vanilla merchant-menu instance. */
public final class TradingCellClientExperienceState {
    private static int containerId = -1;
    private static int experience;

    private TradingCellClientExperienceState() {
    }

    public static void update(int newContainerId, int newExperience) {
        containerId = newContainerId;
        experience = Math.max(0, newExperience);
    }

    public static boolean belongsTo(int currentContainerId) {
        return containerId == currentContainerId;
    }

    public static int experienceFor(int currentContainerId) {
        return belongsTo(currentContainerId) ? experience : 0;
    }

    public static void clear(int closingContainerId) {
        if (containerId == closingContainerId) {
            containerId = -1;
            experience = 0;
        }
    }
}
