package com.cosmocraft.trading_cells.feature.skeletonfarm.adapters.input;

import com.cosmocraft.trading_cells.feature.skeletonfarm.domain.model.SkeletonFarmKind;
import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.TradingCells;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;

public final class SkeletonFarmEnchantments {
    private static final double SMITE_DAMAGE_PER_LEVEL = 2.5D;
    public static final ResourceKey<Enchantment> WARRIORS_TOUCH = ResourceKey.create(
            Registries.ENCHANTMENT,
            Identifier.fromNamespaceAndPath(TradingCells.MOD_ID, "warriors_touch")
    );
    public static final ResourceKey<Enchantment> DECAPITATION = ResourceKey.create(
            Registries.ENCHANTMENT,
            Identifier.fromNamespaceAndPath(TradingCells.MOD_ID, "decapitation")
    );

    private SkeletonFarmEnchantments() {
    }

    public static boolean protectsSword(ItemStack stack, HolderLookup.Provider registries) {
        return level(stack, registries, WARRIORS_TOUCH) > 0;
    }

    public static boolean hasDecapitation(ItemStack stack, HolderLookup.Provider registries) {
        return decapitationLevel(stack, registries) > 0;
    }

    public static int decapitationLevel(ItemStack stack, HolderLookup.Provider registries) {
        return Math.max(0, level(stack, registries, DECAPITATION));
    }

    public static int smiteLevel(ItemStack stack, HolderLookup.Provider registries) {
        return level(stack, registries, Enchantments.SMITE);
    }

    public static double effectiveDamageLevel(
            ItemStack stack,
            ServerLevel level,
            SkeletonFarmKind kind
    ) {
        int smite = smiteLevel(stack, level.registryAccess());
        int sharpness = level(stack, level.registryAccess(), Enchantments.SHARPNESS);
        double fallback = Math.max(smite, sharpnessEquivalentLevel(sharpness));
        if (stack.isEmpty()) {
            return fallback;
        }

        Entity target = createTarget(level, kind);
        Villager attacker = EntityTypes.VILLAGER.create(level, EntitySpawnReason.LOAD);
        if (target == null || attacker == null) {
            return fallback;
        }
        attacker.setItemSlot(EquipmentSlot.MAINHAND, stack.copy());
        try {
            float modifiedDamage = EnchantmentHelper.modifyDamage(
                    level,
                    stack,
                    target,
                    level.damageSources().mobAttack(attacker),
                    1.0F
            );
            double standardEffect = Math.max(0.0D, modifiedDamage - 1.0D) / SMITE_DAMAGE_PER_LEVEL;
            return Double.isFinite(standardEffect) ? Math.max(fallback, standardEffect) : fallback;
        } catch (RuntimeException | LinkageError ignored) {
            return fallback;
        }
    }

    public static int lootingLevel(ItemStack stack, HolderLookup.Provider registries) {
        return level(stack, registries, Enchantments.LOOTING);
    }

    public static int sweepingEdgeLevel(ItemStack stack, HolderLookup.Provider registries) {
        return level(stack, registries, Enchantments.SWEEPING_EDGE);
    }

    public static boolean isStoredOnBook(ItemStack stack, HolderLookup.Provider registries) {
        return isStoredOnBook(stack, registries, WARRIORS_TOUCH);
    }

    public static boolean isDecapitationStoredOnBook(ItemStack stack, HolderLookup.Provider registries) {
        return isStoredOnBook(stack, registries, DECAPITATION);
    }

    private static boolean isStoredOnBook(
            ItemStack stack,
            HolderLookup.Provider registries,
            ResourceKey<Enchantment> key
    ) {
        if (!stack.is(Items.ENCHANTED_BOOK)) {
            return false;
        }
        ItemEnchantments stored = stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
        return registries.lookup(Registries.ENCHANTMENT)
                .flatMap(enchantments -> enchantments.get(key))
                .map(enchantment -> stored.getLevel(enchantment) > 0)
                .orElse(false);
    }

    private static double sharpnessEquivalentLevel(int sharpnessLevel) {
        if (sharpnessLevel <= 0) {
            return 0.0D;
        }
        return (1.0D + 0.5D * (sharpnessLevel - 1)) / SMITE_DAMAGE_PER_LEVEL;
    }

    private static Entity createTarget(
            ServerLevel level,
            SkeletonFarmKind kind
    ) {
        return switch (kind) {
            case SKELETON -> EntityTypes.SKELETON.create(level, EntitySpawnReason.LOAD);
            case WITHER_SKELETON -> EntityTypes.WITHER_SKELETON.create(level, EntitySpawnReason.LOAD);
            case STRAY -> EntityTypes.STRAY.create(level, EntitySpawnReason.LOAD);
            case BOGGED -> EntityTypes.BOGGED.create(level, EntitySpawnReason.LOAD);
            case PARCHED -> EntityTypes.PARCHED.create(level, EntitySpawnReason.LOAD);
            case SKELETON_HORSE -> EntityTypes.SKELETON_HORSE.create(level, EntitySpawnReason.LOAD);
        };
    }

    private static int level(
            ItemStack stack,
            HolderLookup.Provider registries,
            ResourceKey<Enchantment> key
    ) {
        if (stack.isEmpty()) {
            return 0;
        }
        return registries.lookup(Registries.ENCHANTMENT)
                .flatMap(enchantments -> enchantments.get(key))
                .map(stack::getEnchantmentLevel)
                .orElse(0);
    }
}
