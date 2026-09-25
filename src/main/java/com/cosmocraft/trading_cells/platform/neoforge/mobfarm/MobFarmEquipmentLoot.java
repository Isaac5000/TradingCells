package com.cosmocraft.trading_cells.platform.neoforge.mobfarm;

import com.cosmocraft.trading_cells.platform.neoforge.mobfarm.MobFarmSimulationLoot.Style;
import com.cosmocraft.trading_cells.platform.neoforge.mobfarm.MobFarmSimulationLoot.Supplement;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Legacy equipment rules are supplements only; ordinary loot still comes from the actual instance. */
final class MobFarmEquipmentLoot {
    private MobFarmEquipmentLoot() { }

    static List<Supplement> supplements(LivingEntity target, int looting) {
        var id = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType());
        var result = new ArrayList<Supplement>();
        if (!"minecraft".equals(id.getNamespace())) { return List.of(); }
        double equipmentChance = Math.min(1.0D, 0.085D + Math.max(0, looting) * 0.01D);
        switch (id.getPath()) {
            case "skeleton", "stray", "bogged", "parched", "skeleton_horse", "wither_skeleton" -> {
                // Preserve the historical float calculation used by skeleton equipment.
                double chance = Math.min(1.0F, 0.085F + Math.max(0, looting) * 0.01F);
                result.add(weapon(id.getPath().equals("wither_skeleton") ? Items.STONE_SWORD : Items.BOW, chance));
            }
            case "zombie", "zombie_villager", "husk" -> {
                double chance = (target.level().getDifficulty() == Difficulty.HARD ? 0.05D : 0.01D) * equipmentChance;
                result.add(weapon(Items.IRON_SWORD, chance / 6.0D));
                result.add(weapon(Items.IRON_SPEAR, chance / 6.0D));
                result.add(weapon(Items.IRON_SHOVEL, chance * 4.0D / 6.0D));
            }
            case "drowned" -> {
                result.add(weapon(Items.TRIDENT, 0.10D * (10.0D / 16.0D) * equipmentChance));
                result.add(weapon(Items.FISHING_ROD, 0.10D * (6.0D / 16.0D) * equipmentChance));
                result.add(new Supplement(new ItemStack(Items.NAUTILUS_SHELL), 0.03D, 1, 1));
            }
            case "zombified_piglin" -> {
                result.add(weapon(Items.GOLDEN_SWORD, equipmentChance * 0.95D));
                result.add(weapon(Items.GOLDEN_SPEAR, equipmentChance * 0.05D));
            }
            case "piglin_brute" -> result.add(weapon(Items.GOLDEN_AXE, equipmentChance));
            case "piglin" -> {
                result.add(weapon(Items.CROSSBOW, 0.50D * equipmentChance));
                result.add(weapon(Items.GOLDEN_SPEAR, 0.05D * equipmentChance));
                result.add(weapon(Items.GOLDEN_SWORD, 0.45D * equipmentChance));
                for (Item armor : List.of(Items.GOLDEN_HELMET, Items.GOLDEN_CHESTPLATE, Items.GOLDEN_LEGGINGS, Items.GOLDEN_BOOTS)) {
                    result.add(new Supplement(new ItemStack(armor), 0.10D * equipmentChance, 1, 1, false, Style.WORN));
                }
            }
            case "pillager" -> {
                result.add(new Supplement(new ItemStack(Items.CROSSBOW), equipmentChance, 1, 1, true, Style.PLAIN));
                result.add(new Supplement(Raid.getOminousBannerInstance(target.registryAccess()
                        .lookupOrThrow(Registries.BANNER_PATTERN)), 1, 1, 1));
                result.add(new Supplement(new ItemStack(Items.OMINOUS_BOTTLE), 1, 1, 1, false, Style.OMINOUS_BOTTLE));
            }
            case "vindicator" -> result.add(new Supplement(new ItemStack(Items.IRON_AXE), equipmentChance, 1, 1, true, Style.PLAIN));
            default -> { }
        }
        return List.copyOf(result);
    }

    private static Supplement weapon(Item item, double chance) {
        return new Supplement(new ItemStack(item), chance, 1, 1, true, Style.WORN);
    }
}
