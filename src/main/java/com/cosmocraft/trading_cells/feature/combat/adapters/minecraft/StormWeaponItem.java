package com.cosmocraft.trading_cells.feature.combat.adapters.minecraft;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.Consumer;
import com.cosmocraft.trading_cells.feature.combat.domain.model.StormChargeRules;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
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
    private static final String RELOAD_AFTER_UNLOAD = "TradingCellsStormReloadAfterUnload";
    private static final String SOURCE_SLOT = "TradingCellsStormReloadSourceSlot";
    private static final String START = "TradingCellsStormReloadStart";
    private static final String OWNER = "TradingCellsStormReloadOwner";
    public StormWeaponItem(Properties properties) {
        super(properties.durability(StormChargeRules.CAPACITY).enchantable(14).component(DataComponents.TOOLTIP_DISPLAY,
                TooltipDisplay.DEFAULT.withHidden(DataComponents.CHARGED_PROJECTILES, true)));
    }
    /** Required abstract bridge in NeoForge 26.2; gameplay uses the stack-sensitive overload. */
    @Deprecated
    @Override public Predicate<ItemStack> getAllSupportedProjectiles() { return getAllSupportedProjectiles(ItemStack.EMPTY); }
    @Override public Predicate<ItemStack> getAllSupportedProjectiles(ItemStack weapon) {
        return stack -> StormChargeData.charge(stack) > 0;
    }
    @Override public Predicate<ItemStack> getSupportedHeldProjectiles(ItemStack weapon) {
        return getAllSupportedProjectiles(weapon);
    }
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
        weapon.set(DataComponents.DAMAGE, 0);
    }
    @Override public boolean isBarVisible(ItemStack stack) { return !battery(stack).isEmpty(); }
    @Override public int getBarWidth(ItemStack stack) {
        var cell = battery(stack);
        int capacity = StormChargeData.capacity(cell);
        return capacity == 0 ? 0 : Math.round(13F * StormChargeData.charge(cell) / capacity);
    }
    @Override public int getBarColor(ItemStack stack) {
        var cell = battery(stack);
        int capacity = StormChargeData.capacity(cell);
        float remaining = capacity == 0 ? 0 : (float) StormChargeData.charge(cell) / capacity;
        return net.minecraft.util.Mth.hsvToRgb(remaining / 3F, 1, 1);
    }
    @Override public int getMaxDamage(ItemStack stack) {
        int capacity = StormChargeData.capacity(battery(stack));
        return capacity == 0 ? StormChargeRules.CAPACITY : capacity;
    }
    @Override public int getDamage(ItemStack stack) {
        var cell = battery(stack);
        return StormChargeData.capacity(cell) - StormChargeData.charge(cell);
    }
    @Override public boolean isDamaged(ItemStack stack) { return getDamage(stack) > 0; }
    // Vanilla Mending repairs this view; the installed capacitor remains the only source of charge.
    @Override public void setDamage(ItemStack stack, int damage) {
        var cell = battery(stack);
        if (cell.isEmpty()) { return; }
        int capacity = StormChargeData.capacity(cell);
        StormChargeData.setCharge(cell, capacity - Math.clamp(damage, 0, capacity));
        setBattery(stack, cell);
    }
    @Override public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, @Nullable T entity,
            Consumer<Item> onBroken) {
        // Electrical operations pay once below. Vanilla sword wear must not charge twice or destroy the weapon.
        return 0;
    }
    public static boolean consumeCharge(ItemStack weapon, ServerLevel level) {
        ItemStack battery = battery(weapon);
        int charge = StormChargeData.charge(battery);
        if (charge == 0) { return false; }
        int spent = weapon.has(DataComponents.UNBREAKABLE) ? 0 : Math.max(0,
                EnchantmentHelper.processDurabilityChange(level, weapon, 1));
        StormChargeData.setCharge(battery, charge - spent);
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
        data.remove(RELOAD_AFTER_UNLOAD);
        data.remove(SOURCE_SLOT);
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
        if (reloading(weapon) || weapon.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getBooleanOr(RELOAD_AFTER_UNLOAD, false)) { return; }
        player.stopUsingItem();
        // Select the replacement before returning the old battery to inventory.
        ItemStack source = player.getProjectile(weapon);
        int sourceSlot = source == player.getMainHandItem() ? -1 : source == player.getOffhandItem() ? -2
                : player.getInventory().getNonEquipmentItems().indexOf(source);
        boolean hasSource = StormChargeData.charge(source) > 0;
        if (!battery(weapon).isEmpty()) {
            ejectBattery(player, weapon);
            if (hasSource) {
                var data = weapon.get(DataComponents.CUSTOM_DATA).copyTag();
                data.putBoolean(RELOAD_AFTER_UNLOAD, true);
                data.putInt(SOURCE_SLOT, sourceSlot);
                weapon.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
            }
            return;
        }
        if (hasSource) { beginReload(player, weapon, sourceSlot); }
    }
    private static ItemStack reloadSource(Player player, int slot) {
        if (slot == -1) { return player.getMainHandItem(); }
        if (slot == -2) { return player.getOffhandItem(); }
        var inventory = player.getInventory().getNonEquipmentItems();
        return slot >= 0 && slot < inventory.size() ? inventory.get(slot) : ItemStack.EMPTY;
    }
    private static void beginReload(Player player, ItemStack weapon, int sourceSlot) {
        cancelReload(weapon);
        if (!battery(weapon).isEmpty() || StormChargeData.charge(reloadSource(player, sourceSlot)) <= 0) { return; }
        var data = weapon.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        data.putBoolean(RELOADING, true);
        data.putInt(SOURCE_SLOT, sourceSlot);
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
        if (unloading(weapon)) {
            boolean reload = data.getBooleanOr(RELOAD_AFTER_UNLOAD, false);
            int sourceSlot = data.getIntOr(SOURCE_SLOT, -3);
            cancelReload(weapon);
            if (reload) { beginReload(player, weapon, sourceSlot); }
            return;
        }
        cancelReload(weapon);
        // Revalidate at commit. Key release does not cancel this timed server operation.
        if (!battery(weapon).isEmpty()) { return; }
        ItemStack source = data.contains(SOURCE_SLOT) ? reloadSource(player, data.getIntOr(SOURCE_SLOT, -3))
                : player.getProjectile(weapon);
        if (StormChargeData.charge(source) <= 0) { return; }
        setBattery(weapon, source.copyWithCount(1));
        source.shrink(1);
        player.getInventory().setChanged();
        level.playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.IRON_DOOR_CLOSE,
                net.minecraft.sounds.SoundSource.PLAYERS, 0.5F, 1.5F);
    }
    @Override public boolean shouldCauseReequipAnimation(ItemStack before, ItemStack after, boolean changedSlot) {
        return changedSlot || !ItemStack.isSameItem(before, after);
    }
}
