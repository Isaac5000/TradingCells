package com.cosmocraft.trading_cells.platform.neoforge.mobfarm;

import com.cosmocraft.trading_cells.feature.combat.adapters.api.CombatEnchantments;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;

/** Immutable sword-derived state shared by every registered mob-farm family. */
public record MobFarmWeaponSnapshot(
        boolean supported,
        double tierPosition,
        double effectiveDamageLevel,
        int lootingLevel,
        int sweepingEdgeLevel,
        boolean warriorsTouch,
        int decapitationLevel
) {
    public static MobFarmWeaponSnapshot inspect(
            ItemStack weapon,
            ServerLevel level,
            EntityType<?> targetType
    ) {
        return new MobFarmWeaponSnapshot(
                MobFarmSwordTierCatalog.isSupported(weapon),
                MobFarmSwordTierCatalog.timingPosition(weapon),
                CombatEnchantments.effectiveDamageLevel(weapon, level, targetType),
                CombatEnchantments.lootingLevel(weapon, level.registryAccess()),
                CombatEnchantments.sweepingEdgeLevel(weapon, level.registryAccess()),
                CombatEnchantments.protectsWeapon(weapon, level.registryAccess()),
                CombatEnchantments.decapitationLevel(weapon, level.registryAccess())
        );
    }
}
