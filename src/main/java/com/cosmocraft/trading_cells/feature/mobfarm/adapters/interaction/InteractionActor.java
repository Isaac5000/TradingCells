package com.cosmocraft.trading_cells.feature.mobfarm.adapters.interaction;

import com.mojang.authlib.GameProfile;
import java.util.OptionalInt;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.common.util.FakePlayer;
import org.jspecify.annotations.Nullable;

final class InteractionActor extends FakePlayer {
    InteractionActor(ServerLevel level) {
        super(level, new GameProfile(UUID.randomUUID(), "[TC Interaction]"));
        getAbilities().instabuild = false;
        getAbilities().invulnerable = false;
    }
    @Override public PermissionSet permissions() { return PermissionSet.NO_PERMISSIONS; }
    @Override public GameType gameMode() { return GameType.SURVIVAL; }
    @Override public @Nullable ServerPlayer teleport(TeleportTransition transition) {
        InteractionIsolation.deny(); return null;
    }
    @Override public void teleportTo(double x, double y, double z) { InteractionIsolation.deny(); }
    @Override public OptionalInt openMenu(@Nullable MenuProvider provider) {
        InteractionIsolation.deny(); return OptionalInt.empty();
    }
    @Override public boolean startRiding(Entity entity, boolean force, boolean broadcast) {
        InteractionIsolation.deny(); return false;
    }
}
