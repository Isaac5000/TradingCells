package com.cosmocraft.trading_cells.feature.configuredmobfarm.adapters.input;

import com.cosmocraft.trading_cells.feature.configuredmobfarm.application.port.input.ConfiguredMobFarmUseCase;
import com.cosmocraft.trading_cells.feature.configuredmobfarm.domain.model.ConfiguredMobFarmDropRules;
import com.cosmocraft.trading_cells.feature.configuredmobfarm.domain.model.ConfiguredMobFarmKind;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

/** Executes the selected creature's real loot table and applies its individual output filters. */
public final class ConfiguredMobFarmLootAdapter {
    private static final Identifier PIGLIN = Identifier.withDefaultNamespace("piglin");
    private static final Identifier PIGLIN_BRUTE = Identifier.withDefaultNamespace("piglin_brute");

    private ConfiguredMobFarmLootAdapter() {
    }

    static List<ItemStack> generate(
            Identifier targetId,
            ConfiguredMobFarmKind ignoredKind,
            int ignoredEnabledMask,
            Set<Identifier> disabledDynamicLoot,
            int kills,
            int lootingLevel,
            int ignoredDecapitationLevel,
            ServerLevel level,
            ItemStack sword,
            RandomSource random,
            ConfiguredMobFarmUseCase ignoredRules
    ) {
        int killCount = Math.max(1, kills);
        List<ItemStack> drops = new ArrayList<>();
        List<List<ItemStack>> tableDrops = generateLootTableDrops(targetId, killCount, level, sword);
        for (int kill = 0; kill < killCount; kill++) {
            if (kill < tableDrops.size()) {
                addFilteredTableDrops(drops, tableDrops.get(kill), disabledDynamicLoot);
            }
            addEquipmentDrops(drops, targetId, disabledDynamicLoot, lootingLevel, random);
        }
        return List.copyOf(drops);
    }

    private static List<List<ItemStack>> generateLootTableDrops(
            Identifier targetId,
            int kills,
            ServerLevel level,
            ItemStack sword
    ) {
        LivingEntity target = createTarget(level, targetId);
        if (target == null || target.getLootTable().isEmpty()) {
            return List.of();
        }
        FakePlayer attacker = FakePlayerFactory.getMinecraft(level);
        ItemStack previousWeapon = attacker.getMainHandItem().copy();
        List<List<ItemStack>> batches = new ArrayList<>(kills);
        try {
            attacker.setItemSlot(EquipmentSlot.MAINHAND, sword.copy());
            target.setLastHurtByPlayer(attacker, 100);
            for (int kill = 0; kill < kills; kill++) {
                List<ItemStack> batch = new ArrayList<>();
                target.dropFromLootTable(
                        level,
                        level.damageSources().playerAttack(attacker),
                        true,
                        target.getLootTable().orElseThrow(),
                        stack -> batch.add(stack.copy())
                );
                batches.add(List.copyOf(batch));
            }
            return List.copyOf(batches);
        } catch (RuntimeException | LinkageError ignored) {
            return List.of();
        } finally {
            attacker.setItemSlot(EquipmentSlot.MAINHAND, previousWeapon);
        }
    }

    private static LivingEntity createTarget(ServerLevel level, Identifier targetId) {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(targetId).orElse(null);
        Entity entity = type == null ? null : type.create(level, EntitySpawnReason.LOAD);
        return entity instanceof LivingEntity living ? living : null;
    }

    private static void addEquipmentDrops(
            List<ItemStack> drops,
            Identifier targetId,
            Set<Identifier> disabledDynamicLoot,
            int lootingLevel,
            RandomSource random
    ) {
        if (PIGLIN_BRUTE.equals(targetId)) {
            addEquippedDrop(drops, Items.GOLDEN_AXE, disabledDynamicLoot, lootingLevel, random);
            return;
        }
        if (!PIGLIN.equals(targetId)) {
            return;
        }

        Item weapon = random.nextFloat() < 0.50F
                ? Items.CROSSBOW
                : random.nextFloat() < 0.10F ? Items.GOLDEN_SPEAR : Items.GOLDEN_SWORD;
        addEquippedDrop(drops, weapon, disabledDynamicLoot, lootingLevel, random);
        addNaturallyEquippedDrop(
                drops, Items.GOLDEN_HELMET, 0.10D, disabledDynamicLoot, lootingLevel, random
        );
        addNaturallyEquippedDrop(
                drops, Items.GOLDEN_CHESTPLATE, 0.10D, disabledDynamicLoot, lootingLevel, random
        );
        addNaturallyEquippedDrop(
                drops, Items.GOLDEN_LEGGINGS, 0.10D, disabledDynamicLoot, lootingLevel, random
        );
        addNaturallyEquippedDrop(
                drops, Items.GOLDEN_BOOTS, 0.10D, disabledDynamicLoot, lootingLevel, random
        );
    }

    private static void addNaturallyEquippedDrop(
            List<ItemStack> drops,
            Item item,
            double spawnChance,
            Set<Identifier> disabledDynamicLoot,
            int lootingLevel,
            RandomSource random
    ) {
        if (random.nextDouble() < spawnChance) {
            addEquippedDrop(drops, item, disabledDynamicLoot, lootingLevel, random);
        }
    }

    private static void addEquippedDrop(
            List<ItemStack> drops,
            Item item,
            Set<Identifier> disabledDynamicLoot,
            int lootingLevel,
            RandomSource random
    ) {
        if (random.nextDouble() >= ConfiguredMobFarmDropRules.equipmentDropChance(lootingLevel)) {
            return;
        }
        Identifier itemId = BuiltInRegistries.ITEM.getKey(item);
        ItemStack drop = worn(item, random);
        if (itemId != null && !disabledDynamicLoot.contains(itemId)) {
            addStack(drops, drop);
        }
    }

    private static void addFilteredTableDrops(
            List<ItemStack> drops,
            List<ItemStack> generated,
            Set<Identifier> disabledDynamicLoot
    ) {
        for (ItemStack stack : generated) {
            Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (itemId != null && !disabledDynamicLoot.contains(itemId)) {
                addStack(drops, stack.copy());
            }
        }
    }

    public static Optional<ConfiguredMobFarmDropRules.BaseDrop> currentDynamicCycleDrop(
            Identifier targetId,
            ItemStack previewStack,
            int lootingLevel,
            int simulatedKills
    ) {
        boolean equipment = ConfiguredMobFarmTargetCatalog.equipment(targetId).stream()
                .anyMatch(stack -> ItemStack.isSameItemSameComponents(stack, previewStack));
        if (!equipment) {
            return Optional.empty();
        }
        double spawnChance = ConfiguredMobFarmTargetCatalog.equipmentSpawnChance(
                targetId,
                previewStack.getItem()
        );
        if (spawnChance <= 0.0D) {
            return Optional.empty();
        }
        return Optional.of(ConfiguredMobFarmDropRules.binaryCycleDrop(
                ConfiguredMobFarmDropRules.spawnedEquipmentChance(spawnChance, lootingLevel),
                simulatedKills
        ));
    }

    private static ItemStack worn(Item item, RandomSource random) {
        ItemStack equipment = new ItemStack(item);
        int maximumDamage = equipment.getMaxDamage();
        if (maximumDamage > 1) {
            int minimumDamage = maximumDamage / 2;
            equipment.setDamageValue(
                    minimumDamage + random.nextInt(Math.max(1, maximumDamage - minimumDamage))
            );
        }
        return equipment;
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
