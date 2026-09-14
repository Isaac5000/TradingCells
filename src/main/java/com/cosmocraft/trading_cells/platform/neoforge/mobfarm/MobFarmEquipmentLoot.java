package com.cosmocraft.trading_cells.platform.neoforge.mobfarm;

import com.cosmocraft.trading_cells.feature.configuredmobfarm.adapters.input.ConfiguredMobFarmTargetCatalog;
import com.cosmocraft.trading_cells.feature.configuredmobfarm.domain.model.ConfiguredMobFarmDropRules;
import com.cosmocraft.trading_cells.feature.raiderfarm.adapters.input.RaiderFarmLootAdapter;
import com.cosmocraft.trading_cells.feature.raiderfarm.adapters.input.RaiderFarmTargetCatalog;
import com.cosmocraft.trading_cells.feature.raiderfarm.domain.model.RaiderFarmDropRules;
import com.cosmocraft.trading_cells.feature.skeletonfarm.adapters.input.SkeletonFarmTargetCatalog;
import com.cosmocraft.trading_cells.feature.skeletonfarm.domain.model.SkeletonFarmDropRules;
import com.cosmocraft.trading_cells.feature.skeletonfarm.domain.model.SkeletonFarmKind;
import com.cosmocraft.trading_cells.feature.skeletonfarm.domain.model.SkeletonFarmLoot;
import com.cosmocraft.trading_cells.feature.zombiefarm.adapters.input.ZombieFarmTargetCatalog;
import com.cosmocraft.trading_cells.feature.zombiefarm.domain.model.ZombieFarmDropRules;
import com.cosmocraft.trading_cells.platform.neoforge.mobfarm.MobFarmSimulationLoot.Style;
import com.cosmocraft.trading_cells.platform.neoforge.mobfarm.MobFarmSimulationLoot.Supplement;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Legacy equipment rules are supplements only; ordinary loot still comes from the actual instance. */
final class MobFarmEquipmentLoot {
    private MobFarmEquipmentLoot() { }

    static List<Supplement> supplements(LivingEntity target, int looting) {
        var id = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType());
        var result = new ArrayList<Supplement>();
        if (SkeletonFarmTargetCatalog.isStaticTarget(id)) {
            Item weapon = SkeletonFarmTargetCatalog.staticKind(id) == SkeletonFarmKind.WITHER_SKELETON
                    ? Items.STONE_SWORD : Items.BOW;
            result.add(weapon(weapon, SkeletonFarmDropRules.chance(SkeletonFarmLoot.WEAPONS, looting)));
        }
        if (ZombieFarmTargetCatalog.isStaticTarget(id)) {
            boolean hard = target.level().getDifficulty() == Difficulty.HARD;
            switch (ZombieFarmTargetCatalog.staticKind(id)) {
                case ZOMBIE, ZOMBIE_VILLAGER, HUSK -> {
                    result.add(weapon(Items.IRON_SWORD, ZombieFarmDropRules.zombieSwordChance(looting, hard)));
                    result.add(weapon(Items.IRON_SPEAR, ZombieFarmDropRules.zombieSpearChance(looting, hard)));
                    result.add(weapon(Items.IRON_SHOVEL, ZombieFarmDropRules.zombieShovelChance(looting, hard)));
                }
                case DROWNED -> {
                    result.add(weapon(Items.TRIDENT, ZombieFarmDropRules.drownedTridentChance(looting)));
                    result.add(weapon(Items.FISHING_ROD, ZombieFarmDropRules.drownedFishingRodChance(looting)));
                    result.add(new Supplement(new ItemStack(Items.NAUTILUS_SHELL), ZombieFarmDropRules.drownedNautilusChance(), 1, 1));
                }
                case ZOMBIFIED_PIGLIN -> {
                    result.add(weapon(Items.GOLDEN_SWORD, ZombieFarmDropRules.zombifiedPiglinSwordChance(looting)));
                    result.add(weapon(Items.GOLDEN_SPEAR, ZombieFarmDropRules.zombifiedPiglinSpearChance(looting)));
                }
                case ZOGLIN -> { }
            }
        }
        for (ItemStack equipment : ConfiguredMobFarmTargetCatalog.equipment(id)) {
            double chance = ConfiguredMobFarmDropRules.spawnedEquipmentChance(
                    ConfiguredMobFarmTargetCatalog.equipmentSpawnChance(id, equipment.getItem()), looting);
            boolean weapon = equipment.is(Items.CROSSBOW) || equipment.is(Items.GOLDEN_SWORD)
                    || equipment.is(Items.GOLDEN_SPEAR) || equipment.is(Items.GOLDEN_AXE);
            result.add(new Supplement(equipment, chance, 1, 1, weapon, Style.WORN));
        }
        ItemStack raiderWeapon = RaiderFarmTargetCatalog.defaultWeapon(id);
        if (!raiderWeapon.isEmpty()) {
            result.add(new Supplement(raiderWeapon, RaiderFarmDropRules.weaponChance(looting), 1, 1, true, Style.PLAIN));
        }
        if (RaiderFarmTargetCatalog.isPillager(id)) {
            result.add(new Supplement(RaiderFarmLootAdapter.ominousBanner(target.registryAccess()), 1, 1, 1));
            result.add(new Supplement(new ItemStack(Items.OMINOUS_BOTTLE), 1, 1, 1, false, Style.OMINOUS_BOTTLE));
        }
        return List.copyOf(result);
    }

    private static Supplement weapon(Item item, double chance) {
        return new Supplement(new ItemStack(item), chance, 1, 1, true, Style.WORN);
    }
}
