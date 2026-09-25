package com.cosmocraft.trading_cells.platform.neoforge.mobfarm;

import com.cosmocraft.trading_cells.feature.combat.adapters.api.CombatEnchantments;
import com.cosmocraft.trading_cells.feature.combat.adapters.api.CombatItems;
import com.cosmocraft.trading_cells.feature.combat.adapters.input.DecapitationLootAdapter;
import com.cosmocraft.trading_cells.feature.combat.domain.model.DecapitationRules;
import com.cosmocraft.trading_cells.feature.combat.domain.model.StormShardDropRules;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.OminousBottleAmplifier;

/** Composes native instance loot with simulation rewards without changing legacy farm rolls. */
public final class MobFarmSimulationLoot {
    private MobFarmSimulationLoot() { }

    enum Style { PLAIN, WORN, OMINOUS_BOTTLE }

    record Supplement(ItemStack stack, double chance, int minimum, int maximum,
                      boolean exclusiveWeapon, Style style) {
        Supplement(ItemStack stack, double chance, int minimum, int maximum) {
            this(stack, chance, minimum, maximum, false, Style.PLAIN);
        }
        Identifier id() { return BuiltInRegistries.ITEM.getKey(stack.getItem()); }
    }

    private static List<Supplement> supplements(LivingEntity target, ItemStack sword) {
        var result = new ArrayList<Supplement>();
        int looting = CombatEnchantments.lootingLevel(sword, target.registryAccess());
        int decapitation = CombatEnchantments.decapitationLevel(sword, target.registryAccess());
        ItemStack head = DecapitationLootAdapter.headFor(target);
        if (!head.isEmpty()) {
            double chance = target.getType() == EntityTypes.WITHER_SKELETON
                    ? DecapitationRules.supplementalNativeHeadChance(looting, decapitation)
                    : DecapitationRules.decapitationHeadChance(decapitation);
            result.add(new Supplement(head, chance, 1, 1));
        }
        if (target instanceof Creeper creeper && creeper.isPowered()) {
            result.add(new Supplement(new ItemStack(CombatItems.stormShard()), 1, 1,
                    StormShardDropRules.maximumAmount(looting)));
        }
        if (target.getType() == EntityTypes.WITHER) {
            // Vanilla emits this from WitherBoss.dropCustomDeathLoot, not its loot table.
            result.add(new Supplement(new ItemStack(Items.NETHER_STAR), 1, 1, 1));
        }
        result.addAll(MobFarmEquipmentLoot.supplements(target, looting));
        return result;
    }

    public static List<Identifier> filterItems(LivingEntity target, ItemStack sword) {
        var result = new LinkedHashSet<>(MobFarmLootTables.filterItems(target));
        supplements(target, sword).forEach(extra -> result.add(extra.id()));
        return List.copyOf(result);
    }

    public static void roll(ServerLevel level, LivingEntity target, ItemStack sword, int kills,
                            BiConsumer<Integer, ItemStack> output) {
        if (target == null || kills <= 0) { return; }
        if (kills > 256) { throw new IllegalArgumentException("Simulation kill limit exceeded"); }
        var extras = supplements(target, sword);
        boolean[][] nativeDrops = new boolean[kills][extras.size()];
        MobFarmLootTables.roll(level, target, sword, kills, (kill, stack) -> {
            if (!stack.isEmpty()) {
                for (int index = 0; index < extras.size(); index++) {
                    nativeDrops[kill][index] |= stack.is(extras.get(index).stack().getItem());
                }
            }
            output.accept(kill, stack);
        });
        for (int kill = 0; kill < kills; kill++) {
            double weaponRoll = -1;
            double weaponThreshold = 0;
            for (int index = 0; index < extras.size(); index++) {
                Supplement extra = extras.get(index);
                boolean selected;
                if (extra.exclusiveWeapon()) {
                    if (weaponRoll < 0) { weaponRoll = target.getRandom().nextDouble(); }
                    selected = weaponRoll >= weaponThreshold && weaponRoll < weaponThreshold + extra.chance();
                    weaponThreshold += extra.chance();
                } else {
                    selected = extra.chance() > 0 && (extra.chance() >= 1 || target.getRandom().nextDouble() < extra.chance());
                }
                if (!selected || nativeDrops[kill][index]) { continue; }
                int amount = extra.minimum() + target.getRandom().nextInt(extra.maximum() - extra.minimum() + 1);
                ItemStack stack = extra.stack().copyWithCount(amount);
                if (extra.style() == Style.WORN && stack.getMaxDamage() > 1) {
                    int minimumDamage = stack.getMaxDamage() / 2;
                    stack.setDamageValue(minimumDamage + target.getRandom().nextInt(stack.getMaxDamage() - minimumDamage));
                } else if (extra.style() == Style.OMINOUS_BOTTLE) {
                    stack.set(DataComponents.OMINOUS_BOTTLE_AMPLIFIER, new OminousBottleAmplifier(target.getRandom().nextInt(5)));
                }
                output.accept(kill, stack);
            }
        }
    }

    public static Map<Identifier, MobFarmLootTables.DropSummary> preview(
            ServerLevel level, LivingEntity target, ItemStack sword, int kills) {
        if (kills <= 0) { return Map.of(); }
        int count = Math.clamp(kills, 1, 256);
        var result = new LinkedHashMap<>(MobFarmLootTables.preview(level, target, sword, count));
        var nativeSingle = MobFarmLootTables.preview(level, target, sword, 1);
        for (Supplement extra : supplements(target, sword)) {
            Identifier id = extra.id();
            var nativeDrop = nativeSingle.get(id);
            if (extra.chance() <= 0) {
                result.putIfAbsent(id, new MobFarmLootTables.DropSummary(0, 0, 0));
                continue;
            }
            // An unsupported native table must not be presented as a known probability.
            if (nativeDrop != null && nativeDrop.probability() < 0) {
                result.put(id, new MobFarmLootTables.DropSummary(-1, 0, 0));
                continue;
            }
            double nativeChance = nativeDrop == null ? 0 : nativeDrop.probability() / 1_000_000.0;
            double chance = nativeChance + (1 - nativeChance) * extra.chance();
            int minimum = nativeChance == 0 ? extra.minimum() : nativeDrop.minimum();
            int maximum = nativeChance == 0 ? extra.maximum() : nativeDrop.maximum();
            if (nativeChance < 1) {
                minimum = Math.min(minimum, extra.minimum());
                maximum = Math.max(maximum, extra.maximum());
            }
            result.put(id, new MobFarmLootTables.DropSummary(
                    (int) Math.round((1 - Math.pow(1 - chance, count)) * 1_000_000),
                    chance >= 1 ? (int) Math.min(Integer.MAX_VALUE, (long) minimum * count) : minimum,
                    (int) Math.min(Integer.MAX_VALUE, (long) maximum * count)));
        }
        return Map.copyOf(result);
    }
}
