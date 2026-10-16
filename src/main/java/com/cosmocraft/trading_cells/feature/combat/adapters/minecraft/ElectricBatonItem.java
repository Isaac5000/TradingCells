package com.cosmocraft.trading_cells.feature.combat.adapters.minecraft;

import com.cosmocraft.trading_cells.feature.combat.adapters.input.StormLightningEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;

public final class ElectricBatonItem extends StormWeaponItem {
    // Keep the charged total at 16; the unpowered weapon now deals one base damage.
    public static final float CHARGED_DAMAGE_BONUS = 15;
    public ElectricBatonItem(Properties properties) { super(properties); }
    @Override public float getAttackDamageBonus(net.minecraft.world.entity.Entity target, float damage,
            net.minecraft.world.damagesource.DamageSource source) {
        return source.getEntity() instanceof Player player && StormChargeData.charge(battery(player.getMainHandItem())) > 0
                ? CHARGED_DAMAGE_BONUS : 0;
    }
    @Override public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker.level() instanceof ServerLevel level && attacker instanceof Player player
                && consumeCharge(stack, level)) {
            StormLightningEvents.strike(level, target, attacker);
            StormLightningEvents.arc(level, attacker.getEyePosition(), target.getBoundingBox().getCenter());
            level.playSound(null, target.blockPosition(), com.cosmocraft.trading_cells.feature.combat.adapters.output.CombatRegistrationAdapter.BATON_HIT.get(),
                    net.minecraft.sounds.SoundSource.PLAYERS, 0.8F, 1);
        }
    }
}
