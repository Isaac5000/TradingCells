package com.cosmocraft.trading_cells.feature.combat.adapters.output.client;

import com.cosmocraft.trading_cells.feature.combat.adapters.minecraft.ElectricArcWelderItem;
import com.cosmocraft.trading_cells.feature.combat.adapters.minecraft.StormWeaponItem;
import com.cosmocraft.trading_cells.feature.combat.adapters.output.CombatRegistrationAdapter;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = "trading_cells", value = Dist.CLIENT)
public final class StormArcSound extends AbstractTickableSoundInstance {
    private static final Map<Player, StormArcSound> ACTIVE = new HashMap<>();
    private final Player player;
    private StormArcSound(Player player) {
        super(CombatRegistrationAdapter.ARC_LOOP.get(), SoundSource.PLAYERS, RandomSource.create());
        this.player = player;
        looping = true; delay = 0; volume = 0.55F; pitch = 1;
        tick();
    }
    private static boolean firing(Player player) {
        return player.isAlive() && !player.isRemoved() && player.isUsingItem()
                && player.getUseItem().getItem() instanceof ElectricArcWelderItem && !StormWeaponItem.reloading(player.getUseItem());
    }
    @Override public void tick() {
        if (!firing(player) || player.level() != Minecraft.getInstance().level) { stop(); return; }
        x = player.getX(); y = player.getEyeY(); z = player.getZ();
    }
    @SubscribeEvent public static void clientTick(ClientTickEvent.Post event) {
        var client = Minecraft.getInstance();
        ACTIVE.entrySet().removeIf(entry -> {
            if (client.level != entry.getKey().level() || !firing(entry.getKey()) || entry.getValue().isStopped()) {
                entry.getValue().stop(); return true;
            }
            return false;
        });
        if (client.level == null || client.player == null) { return; }
        for (var player : client.level.players()) {
            if (firing(player) && player.distanceToSqr(client.player) < 32*32 && !ACTIVE.containsKey(player)) {
                var sound = new StormArcSound(player);
                ACTIVE.put(player, sound);
                client.getSoundManager().play(sound);
            }
        }
    }
}
