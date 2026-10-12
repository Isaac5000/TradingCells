package com.cosmocraft.trading_cells.feature.combat.adapters.minecraft;

import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/** A real, rechargeable battery moves into the weapon only when loading completes. */
public class StormWeaponItem extends ProjectileWeaponItem {
    public static final int RELOAD_TICKS = 30;
    private static final String RELOADING = "TradingCellsStormReloading";
    private static final String UNLOADING = "TradingCellsStormUnloading";
    private static final String START = "TradingCellsStormReloadStart";
    private static final String OWNER = "TradingCellsStormReloadOwner";
    public StormWeaponItem(Properties properties) {
        super(properties.component(DataComponents.TOOLTIP_DISPLAY,
                TooltipDisplay.DEFAULT.withHidden(DataComponents.CHARGED_PROJECTILES, true)));
    }
    @Override public Predicate<ItemStack> getAllSupportedProjectiles() { return stack -> StormChargeData.charge(stack) > 0; }
    @Override public int getDefaultProjectileRange() { return 8; }
    @Override public ItemStack getDefaultCreativeAmmo(@Nullable Player player, ItemStack weapon) { return ItemStack.EMPTY; }
    @Override protected void shootProjectile(LivingEntity shooter, Projectile projectile, int index, float power,
            float uncertainty, float angle, @Nullable LivingEntity target) { throw new UnsupportedOperationException("Batteries are not projectiles"); }

    public static ItemStack battery(ItemStack weapon) {
        var loaded = weapon.getOrDefault(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.EMPTY).itemCopies();
        return loaded.size() == 1 && StormChargeData.capacity(loaded.getFirst()) > 0 ? loaded.getFirst() : ItemStack.EMPTY;
    }
    public static void setBattery(ItemStack weapon, ItemStack battery) {
        weapon.set(DataComponents.CHARGED_PROJECTILES, battery.isEmpty() ? ChargedProjectiles.EMPTY
                : ChargedProjectiles.ofNonEmpty(List.of(battery.copyWithCount(1))));
    }
    public static boolean consumeCharge(ItemStack weapon) {
        ItemStack battery = battery(weapon);
        int charge = StormChargeData.charge(battery);
        if (charge == 0) { return false; }
        StormChargeData.setCharge(battery, charge - 1);
        setBattery(weapon, battery);
        return true;
    }
    public static boolean reloading(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getBooleanOr(RELOADING, false);
    }
    public static boolean unloading(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getBooleanOr(UNLOADING, false);
    }
    public static boolean animating(ItemStack stack) { return reloading(stack) || unloading(stack); }
    public static float animationProgress(ItemStack stack, Level level, float partial) {
        float progress = reloadProgress(stack, level, partial);
        return unloading(stack) ? 1 - progress : progress;
    }
    public static void cancelReload(ItemStack stack) {
        var data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        data.remove(RELOADING);
        data.remove(UNLOADING);
        data.remove(START);
        data.remove(OWNER);
        if (data.isEmpty()) { stack.remove(DataComponents.CUSTOM_DATA); }
        else { stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data)); }
    }
    public static float reloadProgress(ItemStack stack, Level level, float partial) {
        var data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return Math.clamp((level.getGameTime() - data.getLongOr(START, level.getGameTime()) + partial) / RELOAD_TICKS, 0, 1);
    }
    public static void requestReload(Player player, InteractionHand hand, boolean eject) {
        if (player.level().isClientSide() || player.isSpectator() || !player.isAlive()) { return; }
        ItemStack weapon = player.getItemInHand(hand);
        if (!(weapon.getItem() instanceof StormWeaponItem)) { return; }
        if (eject) {
            cancelReload(weapon);
            player.stopUsingItem();
            ejectBattery(player, weapon);
            return;
        }
        if (reloading(weapon)) { return; }
        player.stopUsingItem();
        ItemStack loaded = battery(weapon);
        if (!loaded.isEmpty() && StormChargeData.charge(loaded) == 0) {
            ejectBattery(player, weapon);
            return;
        }
        if (StormChargeData.charge(player.getProjectile(weapon)) <= 0) { return; }
        cancelReload(weapon);
        var data = weapon.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        data.putBoolean(RELOADING, true);
        data.putLong(START, player.level().getGameTime());
        data.putString(OWNER, player.getStringUUID());
        weapon.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        player.getInventory().setChanged();
    }
    public static void ejectBattery(Player player, ItemStack weapon) {
        ItemStack previous = battery(weapon);
        setBattery(weapon, ItemStack.EMPTY);
        if (!previous.isEmpty()) {
            // Return the real battery once; the reverse animation is visual only and can be interrupted by a new press.
            cancelReload(weapon);
            var data = weapon.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            data.putBoolean(UNLOADING, true);
            data.putLong(START, player.level().getGameTime());
            data.putString(OWNER, player.getStringUUID());
            weapon.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        }
        if (!previous.isEmpty() && !player.getInventory().add(previous)) { player.drop(previous, false); }
        player.getInventory().setChanged();
    }
    @Override public InteractionResult use(Level level, Player player, InteractionHand hand) { return InteractionResult.PASS; }
    @Override public int getUseDuration(ItemStack stack, LivingEntity user) { return RELOAD_TICKS; }
    @Override public ItemUseAnimation getUseAnimation(ItemStack stack) { return ItemUseAnimation.NONE; }
    @Override public void inventoryTick(ItemStack weapon, net.minecraft.server.level.ServerLevel level,
            net.minecraft.world.entity.Entity owner, net.minecraft.world.entity.@Nullable EquipmentSlot slot) {
        if (!animating(weapon)) { return; }
        var data = weapon.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!(owner instanceof Player player) || !player.isAlive()
                || (player.getMainHandItem() != weapon && player.getOffhandItem() != weapon)
                || !data.getStringOr(OWNER, "").equals(player.getStringUUID())
                || level.getGameTime() < data.getLongOr(START, Long.MAX_VALUE)) {
            cancelReload(weapon);
            return;
        }
        if (reloadProgress(weapon, level, 0) < 1) { return; }
        if (unloading(weapon)) { cancelReload(weapon); return; }
        cancelReload(weapon);
        // Revalidate at commit. Key release does not cancel this timed server operation.
        ItemStack source = player.getProjectile(weapon);
        if (StormChargeData.charge(source) <= 0) { return; }
        ItemStack previous = battery(weapon);
        setBattery(weapon, source.copyWithCount(1));
        source.shrink(1);
        if (!previous.isEmpty() && !player.getInventory().add(previous)) { player.drop(previous, false); }
        player.getInventory().setChanged();
        level.playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.IRON_DOOR_CLOSE,
                net.minecraft.sounds.SoundSource.PLAYERS, 0.5F, 1.5F);
    }
    @Override public boolean shouldCauseReequipAnimation(ItemStack before, ItemStack after, boolean changedSlot) {
        return changedSlot || !ItemStack.isSameItem(before, after);
    }
}
