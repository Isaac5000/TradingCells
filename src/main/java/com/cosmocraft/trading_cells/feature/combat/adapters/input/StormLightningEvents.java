package com.cosmocraft.trading_cells.feature.combat.adapters.input;

import com.cosmocraft.trading_cells.feature.combat.adapters.output.CombatRegistrationAdapter;
import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.TradingCells;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ExplosionEvent;

@EventBusSubscriber(modid = TradingCells.MOD_ID)
public final class StormLightningEvents {
    private static final ThreadLocal<Conversion> CONVERSION = new ThreadLocal<>();
    private static final class Conversion {
        private Entity target;
        private Conversion(Entity target) { this.target = target; }
    }
    public static final String ARTIFICIAL = "TradingCellsArtificialLightning";
    private static final String PROCESSED = "TradingCellsStormProcessed";
    private static final net.minecraft.core.particles.DustParticleOptions ARC =
            new net.minecraft.core.particles.DustParticleOptions(0x53DBFF, 0.35F);

    private StormLightningEvents() { }

    public static Entity strike(ServerLevel level, Entity target, Entity owner) {
        if (!target.isAlive() || target.isRemoved()) { return target; }
        float health = target instanceof LivingEntity living ? living.getHealth() : Float.NaN;
        // Context only: never add or tick this bolt, so no sky beam, weather flash or world fire.
        LightningBolt bolt = new LightningBolt(EntityTypes.LIGHTNING_BOLT, level);
        bolt.setPos(target.position());
        bolt.getPersistentData().putBoolean(ARTIFICIAL, true);
        if (owner instanceof ServerPlayer player) { bolt.setCause(player); }
        Conversion previous = CONVERSION.get();
        Conversion current = new Conversion(target);
        CONVERSION.set(current);
        try {
            if (!net.neoforged.neoforge.event.EventHooks.onEntityStruckByLightning(target, bolt)
                    && target.isAlive() && !target.isRemoved()) { target.thunderHit(level, bolt); }
        } finally {
            if (previous == null) { CONVERSION.remove(); } else { CONVERSION.set(previous); }
        }
        // Conversion must not restore health already removed by the weapon hit.
        if (current.target != target && current.target instanceof LivingEntity living && living.isAlive()
                && Float.isFinite(health)) {
            living.setHealth(Math.min(living.getHealth(), health));
        }
        return current.target;
    }

    @SubscribeEvent
    public static void converted(net.neoforged.neoforge.event.entity.living.LivingConversionEvent.Post event) {
        Conversion current = CONVERSION.get();
        if (current != null && current.target == event.getEntity()) { current.target = event.getOutcome(); }
    }

    public static void arc(ServerLevel level, Vec3 start, Vec3 end) {
        Vec3 delta = end.subtract(start);
        int steps = Math.clamp((int) (delta.length() * 3), 3, 24);
        for (int step = 0; step <= steps; step++) {
            Vec3 point = start.add(delta.scale((double) step / steps));
            double jitter = step == 0 || step == steps ? 0 : 0.07;
            level.sendParticles(ARC,
                    point.x, point.y, point.z, 1, jitter, jitter, jitter, 0);
        }
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK,
                end.x, end.y, end.z, 1, 0.02, 0.02, 0.02, 0);
    }

    @SubscribeEvent
    public static void explosion(ExplosionEvent.Detonate event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !(event.getExplosion().getDirectSourceEntity() instanceof Creeper creeper)
                || !creeper.isPowered() || creeper.getPersistentData().getBooleanOr(PROCESSED, false)) { return; }
        creeper.getPersistentData().putBoolean(PROCESSED, true);
        // The explosion's affected-entity list is already frozen; the new drop is not in it.
        Block.popResource(level, creeper.blockPosition(), new ItemStack(CombatRegistrationAdapter.STORM_SHARD_ITEM.get()));
    }
}
