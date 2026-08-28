package com.cosmocraft.trading_cells.feature.skeletonfarm.adapters.input;

import com.cosmocraft.trading_cells.feature.skeletonfarm.application.port.input.SkeletonFarmUseCase;
import com.cosmocraft.trading_cells.feature.combat.domain.model.DecapitationRules;
import com.cosmocraft.trading_cells.feature.skeletonfarm.domain.model.SkeletonFarmDropRules;
import com.cosmocraft.trading_cells.feature.skeletonfarm.domain.model.SkeletonFarmKind;
import com.cosmocraft.trading_cells.feature.skeletonfarm.domain.model.SkeletonFarmLoot;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

public final class SkeletonFarmLootAdapter {
    private SkeletonFarmLootAdapter() {
    }

    static List<ItemStack> generate(
            Identifier targetId,
            SkeletonFarmKind kind,
            int enabledMask,
            Set<Identifier> disabledDynamicLoot,
            int kills,
            int lootingLevel,
            int decapitationLevel,
            ServerLevel level,
            ItemStack sword,
            RandomSource random,
            SkeletonFarmUseCase rules
    ) {
        if (!SkeletonFarmTargetCatalog.isStaticTarget(targetId)) {
            return generateDynamic(
                    targetId,
                    kind,
                    enabledMask,
                    disabledDynamicLoot,
                    kills,
                    level,
                    sword,
                    rules
            );
        }
        int looting = Math.max(0, lootingLevel);
        List<ItemStack> drops = new ArrayList<>();
        for (int kill = 0; kill < Math.max(1, kills); kill++) {
            int bones = random.nextInt(SkeletonFarmDropRules.amountRollBound(SkeletonFarmLoot.BONES, looting));
            int arrows = random.nextInt(SkeletonFarmDropRules.amountRollBound(SkeletonFarmLoot.ARROWS, looting));
            int coal = random.nextInt(SkeletonFarmDropRules.amountRollBound(SkeletonFarmLoot.COAL, looting));
            boolean skull = random.nextFloat() < DecapitationRules.farmHeadChance(
                    looting,
                    kind == SkeletonFarmKind.WITHER_SKELETON,
                    decapitationLevel
            );
            boolean weapon = random.nextFloat() < SkeletonFarmDropRules.chance(SkeletonFarmLoot.WEAPONS, looting);

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
                addStack(drops, head(kind));
            }
            if (weapon && rules.isEnabled(enabledMask, kind, SkeletonFarmLoot.WEAPONS)) {
                drops.add(wornWeapon(kind, random));
            }
        }
        appendTableExtensions(
                drops,
                targetId,
                kind,
                enabledMask,
                disabledDynamicLoot,
                kills,
                level,
                sword,
                rules
        );
        return List.copyOf(drops);
    }

    private static void appendTableExtensions(
            List<ItemStack> drops,
            Identifier targetId,
            SkeletonFarmKind fallbackKind,
            int enabledMask,
            Set<Identifier> disabledDynamicLoot,
            int kills,
            ServerLevel level,
            ItemStack sword,
            SkeletonFarmUseCase rules
    ) {
        Set<Identifier> extensions = SkeletonFarmTargetCatalog.tableExtensions(targetId);
        if (extensions.isEmpty()) {
            return;
        }
        LivingEntity target = createTarget(level, targetId);
        if (target == null || target.getLootTable().isEmpty()) {
            return;
        }
        FakePlayer attacker = FakePlayerFactory.getMinecraft(level);
        ItemStack previousWeapon = attacker.getMainHandItem().copy();
        try {
            attacker.setItemSlot(EquipmentSlot.MAINHAND, sword.copy());
            target.setLastHurtByPlayer(attacker, 100);
            for (int kill = 0; kill < Math.max(1, kills); kill++) {
                target.dropFromLootTable(
                        level,
                        level.damageSources().playerAttack(attacker),
                        true,
                        target.getLootTable().orElseThrow(),
                        stack -> {
                            Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
                            if (itemId != null && extensions.contains(itemId)) {
                                addDynamicDrop(
                                        drops,
                                        stack,
                                        fallbackKind,
                                        enabledMask,
                                        disabledDynamicLoot,
                                        rules
                                );
                            }
                        }
                );
            }
        } catch (RuntimeException | LinkageError ignored) {
            // A broken external table must not disable the fixed Skeleton Farm drops.
        } finally {
            attacker.setItemSlot(EquipmentSlot.MAINHAND, previousWeapon);
        }
    }

    private static List<ItemStack> generateDynamic(
            Identifier targetId,
            SkeletonFarmKind fallbackKind,
            int enabledMask,
            Set<Identifier> disabledDynamicLoot,
            int kills,
            ServerLevel level,
            ItemStack sword,
            SkeletonFarmUseCase rules
    ) {
        LivingEntity target = createTarget(level, targetId);
        if (target == null || target.getLootTable().isEmpty()) {
            return List.of();
        }
        FakePlayer attacker = FakePlayerFactory.getMinecraft(level);
        ItemStack previousWeapon = attacker.getMainHandItem().copy();
        List<ItemStack> drops = new ArrayList<>();
        try {
            attacker.setItemSlot(EquipmentSlot.MAINHAND, sword.copy());
            target.setLastHurtByPlayer(attacker, 100);
            for (int kill = 0; kill < Math.max(1, kills); kill++) {
                target.dropFromLootTable(
                        level,
                        level.damageSources().playerAttack(attacker),
                        true,
                        target.getLootTable().orElseThrow(),
                        stack -> addDynamicDrop(
                                drops,
                                stack,
                                fallbackKind,
                                enabledMask,
                                disabledDynamicLoot,
                                rules
                        )
                );
            }
        } catch (RuntimeException | LinkageError ignored) {
            return List.of();
        } finally {
            attacker.setItemSlot(EquipmentSlot.MAINHAND, previousWeapon);
        }
        return List.copyOf(drops);
    }

    private static LivingEntity createTarget(ServerLevel level, Identifier targetId) {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(targetId).orElse(null);
        Entity entity = type == null ? null : type.create(level, EntitySpawnReason.LOAD);
        return entity instanceof LivingEntity living ? living : null;
    }

    private static void addDynamicDrop(
            List<ItemStack> drops,
            ItemStack stack,
            SkeletonFarmKind fallbackKind,
            int enabledMask,
            Set<Identifier> disabledDynamicLoot,
            SkeletonFarmUseCase rules
    ) {
        SkeletonFarmLoot category = SkeletonFarmTargetCatalog.category(stack.getItem());
        Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        boolean enabled = category == null
                ? itemId != null && !disabledDynamicLoot.contains(itemId)
                : rules.isEnabled(enabledMask, fallbackKind, category);
        if (enabled) {
            addStack(drops, stack.copy());
        }
    }

    public static List<ItemStack> previewOutputs(SkeletonFarmKind kind) {
        return previewOutputChances(kind).stream()
                .map(PreviewOutput::stack)
                .toList();
    }

    public static List<PreviewOutput> previewOutputChances(SkeletonFarmKind kind) {
        List<PreviewOutput> outputs = new ArrayList<>();
        for (SkeletonFarmLoot loot : kind.availableLoot()) {
            SkeletonFarmDropRules.BaseDrop baseDrop = loot == SkeletonFarmLoot.SKULLS
                    ? SkeletonFarmDropRules.headCycleDrop(
                            kind,
                            0,
                            1,
                            kind == SkeletonFarmKind.WITHER_SKELETON ? 0 : 1
                    )
                    : SkeletonFarmDropRules.baseDrop(loot);
            ItemStack stack = switch (loot) {
                case WEAPONS -> kind == SkeletonFarmKind.WITHER_SKELETON
                        ? new ItemStack(Items.STONE_SWORD)
                        : new ItemStack(Items.BOW);
                case BONES -> new ItemStack(Items.BONE);
                case ARROWS -> arrow(kind, 1);
                case SKULLS -> head(kind);
                case COAL -> new ItemStack(Items.COAL);
            };
            outputs.add(new PreviewOutput(
                    loot,
                    stack,
                    baseDrop.probabilityPartsPerMillion(),
                    baseDrop.minimumAmount(),
                    baseDrop.maximumAmount()
            ));
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

    private static ItemStack head(SkeletonFarmKind kind) {
        return new ItemStack(kind == SkeletonFarmKind.WITHER_SKELETON
                ? Items.WITHER_SKELETON_SKULL
                : Items.SKELETON_SKULL);
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

    public record PreviewOutput(
            SkeletonFarmLoot loot,
            ItemStack stack,
            int probabilityPartsPerMillion,
            int minimumAmount,
            int maximumAmount
    ) {
        public PreviewOutput {
            stack = stack.copy();
            if (stack.isEmpty()
                    || probabilityPartsPerMillion < 0
                    || probabilityPartsPerMillion > SkeletonFarmDropRules.PROBABILITY_PARTS_PER_MILLION
                    || minimumAmount < 1
                    || maximumAmount < minimumAmount) {
                throw new IllegalArgumentException("Invalid Skeleton Farm preview output");
            }
        }

        @Override
        public ItemStack stack() {
            return stack.copy();
        }
    }
}
