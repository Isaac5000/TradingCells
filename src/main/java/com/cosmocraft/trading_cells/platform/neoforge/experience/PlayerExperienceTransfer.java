package com.cosmocraft.trading_cells.platform.neoforge.experience;

import com.cosmocraft.trading_cells.shared.machines.domain.model.MinecraftExperience;
import net.minecraft.server.level.ServerPlayer;

/** Keeps Minecraft's float XP bar aligned with the exact integer XP total after machine transfers. */
public final class PlayerExperienceTransfer {
    private PlayerExperienceTransfer() {
    }

    public static int removePoints(ServerPlayer player, int requestedPoints) {
        int before = synchronizeTotalFromVisibleState(player);
        int requested = Math.min(Math.max(0, requestedPoints), before);
        if (requested == 0) {
            return 0;
        }
        player.giveExperiencePoints(-requested);
        int after = normalizeVisibleState(player);
        return Math.clamp(before - after, 0, requested);
    }

    public static int addPoints(ServerPlayer player, int requestedPoints) {
        int before = synchronizeTotalFromVisibleState(player);
        int freeCapacity = Integer.MAX_VALUE - before;
        int requested = Math.min(Math.max(0, requestedPoints), freeCapacity);
        if (requested == 0) {
            return 0;
        }
        player.giveExperiencePoints(requested);
        int after = normalizeVisibleState(player);
        return Math.clamp(after - before, 0, requested);
    }

    private static int synchronizeTotalFromVisibleState(ServerPlayer player) {
        int exactPoints = MinecraftExperience.totalPoints(
                player.experienceLevel,
                player.experienceProgress
        );
        player.totalExperience = exactPoints;
        return exactPoints;
    }

    private static int normalizeVisibleState(ServerPlayer player) {
        int exactPoints = Math.max(0, player.totalExperience);
        MinecraftExperience.ExperienceState state = MinecraftExperience.stateForTotalPoints(exactPoints);
        player.setExperienceLevels(state.level());
        player.setExperiencePoints(state.pointsIntoLevel());
        player.totalExperience = state.totalPoints();
        return state.totalPoints();
    }
}
