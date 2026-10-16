package com.cosmocraft.trading_cells.feature.combat.adapters.minecraft;

import com.cosmocraft.trading_cells.feature.combat.adapters.input.StormLightningEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class ElectricArcWelderItem extends StormWeaponItem {
    public static final int CHARGE_TICKS = 20;
    public static final int DAMAGE_TICKS = 5;
    public static final float PULSE_DAMAGE = 2;
    private static final double REACH = 8;
    public ElectricArcWelderItem(Properties properties) { super(properties); }

    @Override public boolean canDestroyBlock(ItemStack stack, net.minecraft.world.level.block.state.BlockState state,
            Level level, net.minecraft.core.BlockPos pos, LivingEntity user) { return false; }
    @Override public boolean onLeftClickEntity(ItemStack stack, Player player, net.minecraft.world.entity.Entity entity) {
        return true;
    }

    @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack weapon = player.getItemInHand(hand);
        if (reloading(weapon) || StormChargeData.charge(battery(weapon)) == 0) { return InteractionResult.FAIL; }
        if (level instanceof ServerLevel server && !consumeCharge(weapon, server)) { return InteractionResult.FAIL; }
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }
    @Override public int getUseDuration(ItemStack stack, LivingEntity user) { return 72_000; }
    @Override public ItemUseAnimation getUseAnimation(ItemStack stack) { return ItemUseAnimation.NONE; }
    @Override public void onUseTick(Level world, LivingEntity user, ItemStack stack, int remaining) {
        if (reloading(stack) || !(world instanceof ServerLevel level) || !(user instanceof Player player)) { return; }
        int elapsed = getUseDuration(stack, user) - remaining;
        // Each charge prepays exactly one second, including the first partial second.
        if (elapsed > 0 && elapsed % CHARGE_TICKS == 0 && !consumeCharge(stack, level)) { user.stopUsingItem(); return; }
        if (elapsed % DAMAGE_TICKS != 0) { return; }
        var hit = trace(level, user, 1);
        if (hit.target() != null) {
            var target = hit.target();
            var type = level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE).getOrThrow(
                    net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE,
                            net.minecraft.resources.Identifier.fromNamespaceAndPath("trading_cells", "storm_arc")));
            if (target.hurtServer(level, new net.minecraft.world.damagesource.DamageSource(type, user), PULSE_DAMAGE)
                    && elapsed % CHARGE_TICKS == 0) {
                StormLightningEvents.strike(level, target, user);
            }
        }
    }

    public record ArcHit(Vec3 end, @org.jspecify.annotations.Nullable LivingEntity target) { }

    public static ArcHit trace(Level level, LivingEntity user, float partialTick) {
        Vec3 start = user.getEyePosition(partialTick);
        Vec3 end = level.clip(new ClipContext(start, start.add(user.getViewVector(partialTick).scale(REACH)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, user)).getLocation();
        LivingEntity target = null;
        double nearest = start.distanceToSqr(end);
        for (var entity : level.getEntitiesOfClass(LivingEntity.class, new AABB(start, end).inflate(0.3),
                entity -> entity != user && entity.isAlive() && !entity.isSpectator())) {
            var hit = entity.getBoundingBox().inflate(0.15).clip(start, end);
            if (hit.isPresent() && start.distanceToSqr(hit.get()) < nearest) {
                target = entity; end = hit.get(); nearest = start.distanceToSqr(end);
            }
        }
        return new ArcHit(end, target);
    }
}
