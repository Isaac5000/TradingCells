package com.cosmocraft.trading_cells.platform.neoforge.client.screen;

import net.minecraft.network.chat.Component;

/** Shared formatting for the accumulated trade-experience buttons. */
public final class TradeExperienceDisplay {
    private TradeExperienceDisplay() {
    }

    public static Component label(int experience) {
        int safeExperience = Math.max(0, experience);
        return Component.translatable(
                "button.trading_cells.extract_xp",
                safeExperience,
                experienceLevels(safeExperience)
        );
    }

    public static Component storedExperience(int experience) {
        return Component.translatable("gui.trading_cells.stored_xp", Math.max(0, experience));
    }

    public static Component equivalentLevel(int experience) {
        return Component.translatable(
                "gui.trading_cells.level_equivalent",
                experienceLevels(Math.max(0, experience))
        );
    }

    private static int experienceLevels(int experience) {
        int low = 0;
        int high = 1;
        while (totalExperienceAtLevel(high) <= experience && high < 65_536) {
            high *= 2;
        }
        while (low + 1 < high) {
            int middle = low + (high - low) / 2;
            if (totalExperienceAtLevel(middle) <= experience) {
                low = middle;
            } else {
                high = middle;
            }
        }
        return low;
    }

    private static long totalExperienceAtLevel(int level) {
        long value = level;
        if (level <= 16) {
            return value * value + 6L * value;
        }
        if (level <= 31) {
            return (5L * value * value - 81L * value + 720L) / 2L;
        }
        return (9L * value * value - 325L * value + 4_440L) / 2L;
    }
}
