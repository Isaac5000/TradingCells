package com.cosmocraft.trading_cells.feature.skeletonfarm.adapters.input;

import com.cosmocraft.trading_cells.feature.skeletonfarm.application.port.input.SkeletonFarmUseCase;
import com.cosmocraft.trading_cells.feature.skeletonfarm.domain.model.SkeletonFarmKind;
import com.cosmocraft.trading_cells.feature.skeletonfarm.domain.model.SkeletonFarmLoot;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;

public final class SkeletonFarmLootAdapter {
    private static final float BASE_WEAPON_CHANCE = 0.085F;
    private static final float LOOTING_WEAPON_CHANCE = 0.01F;
    private static final float BASE_SKULL_CHANCE = 0.025F;
    private static final float LOOTING_SKULL_CHANCE = 0.01F;

    private SkeletonFarmLootAdapter() {
    }

    static List<ItemStack> generate(
            SkeletonFarmKind kind,
            int enabledMask,
            int kills,
            int lootingLevel,
            RandomSource random,
            SkeletonFarmUseCase rules
    ) {
        int looting = Math.max(0, lootingLevel);
        List<ItemStack> drops = new ArrayList<>();
        for (int kill = 0; kill < Math.max(1, kills); kill++) {
            int bones = random.nextInt(3 + looting);
            int arrows = random.nextInt(3 + looting);
            int coal = random.nextInt(2 + looting);
            boolean skull = random.nextFloat() < Math.min(1.0F, BASE_SKULL_CHANCE + looting * LOOTING_SKULL_CHANCE);
            boolean weapon = random.nextFloat() < Math.min(
                    1.0F,
                    BASE_WEAPON_CHANCE + looting * LOOTING_WEAPON_CHANCE
            );

            if (rules.isEnabled(enabledMask, kind, SkeletonFarmLoot.BONES)) {
                addStack(drops, new ItemStack(Items.BONE, bones));
            }
            if (rules.isEnabled(enabledMask, kind, SkeletonFarmLoot.ARROWS)) {
                addStack(drops, arrow(kind, arrows));
            }
            if (rules.isEnabled(enabledMask, kind, SkeletonFarmLoot.COAL)) {
                addStack(drops, new ItemStack(Items.COAL, coal));
            }
            if (skull && rules.isEnabled(enabledMask, kind, SkeletonFarmLoot.SKULLS)) {
                addStack(drops, new ItemStack(Items.WITHER_SKELETON_SKULL));
            }
            if (weapon && rules.isEnabled(enabledMask, kind, SkeletonFarmLoot.WEAPONS)) {
                drops.add(wornWeapon(kind, random));
            }
        }
        return List.copyOf(drops);
    }

    public static List<ItemStack> previewOutputs(SkeletonFarmKind kind) {
        List<ItemStack> outputs = new ArrayList<>();
        for (SkeletonFarmLoot loot : kind.availableLoot()) {
            outputs.add(switch (loot) {
                case WEAPONS -> kind == SkeletonFarmKind.WITHER_SKELETON
                        ? new ItemStack(Items.STONE_SWORD)
                        : new ItemStack(Items.BOW);
                case BONES -> new ItemStack(Items.BONE);
                case ARROWS -> arrow(kind, 1);
                case SKULLS -> new ItemStack(Items.WITHER_SKELETON_SKULL);
                case COAL -> new ItemStack(Items.COAL);
            });
        }
        return List.copyOf(outputs);
    }

    private static ItemStack arrow(SkeletonFarmKind kind, int count) {
        if (count <= 0) {
            return ItemStack.EMPTY;
        }
        return switch (kind) {
            case STRAY -> tippedArrow(Potions.SLOWNESS, count);
            case BOGGED -> tippedArrow(Potions.POISON, count);
            case PARCHED -> tippedArrow(Potions.WEAKNESS, count);
            default -> new ItemStack(Items.ARROW, count);
        };
    }

    private static ItemStack tippedArrow(net.minecraft.core.Holder<net.minecraft.world.item.alchemy.Potion> potion, int count) {
        ItemStack arrow = PotionContents.createItemStack(Items.TIPPED_ARROW, potion);
        arrow.setCount(count);
        return arrow;
    }

    private static ItemStack wornWeapon(SkeletonFarmKind kind, RandomSource random) {
        ItemStack weapon = new ItemStack(kind == SkeletonFarmKind.WITHER_SKELETON
                ? Items.STONE_SWORD
                : Items.BOW);
        int maximumDamage = weapon.getMaxDamage();
        if (maximumDamage > 1) {
            int minimumDamage = maximumDamage / 2;
            weapon.setDamageValue(minimumDamage + random.nextInt(Math.max(1, maximumDamage - minimumDamage)));
        }
        return weapon;
    }

    private static void addStack(List<ItemStack> drops, ItemStack incoming) {
        if (incoming.isEmpty()) {
            return;
        }
        for (ItemStack existing : drops) {
            if (ItemStack.isSameItemSameComponents(existing, incoming)) {
                existing.grow(incoming.getCount());
                return;
            }
        }
        drops.add(incoming);
    }
}
