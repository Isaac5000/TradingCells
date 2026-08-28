package com.cosmocraft.trading_cells.feature.raiderfarm.adapters.input;

import com.cosmocraft.trading_cells.feature.raiderfarm.application.port.input.RaiderFarmUseCase;
import com.cosmocraft.trading_cells.feature.raiderfarm.domain.model.RaiderFarmDropRules;
import com.cosmocraft.trading_cells.feature.raiderfarm.domain.model.RaiderFarmKind;
import com.cosmocraft.trading_cells.feature.raiderfarm.domain.model.RaiderFarmLoot;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.OminousBottleAmplifier;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

/** Executes the selected raider's real loot table and filters its concrete results. */
public final class RaiderFarmLootAdapter {
    private static final Identifier WHITE_BANNER_ID = Identifier.withDefaultNamespace("white_banner");

    private RaiderFarmLootAdapter() {
    }

    static List<ItemStack> generate(
            Identifier targetId,
            RaiderFarmKind kind,
            int enabledMask,
            Set<Identifier> disabledDynamicLoot,
            int kills,
            int lootingLevel,
            int ignoredDecapitationLevel,
            ServerLevel level,
            ItemStack sword,
            RandomSource random,
            RaiderFarmUseCase rules
    ) {
        int killCount = Math.max(1, kills);
        List<ItemStack> drops = new ArrayList<>();
        List<List<ItemStack>> tableDrops = generateLootTableDrops(targetId, killCount, level, sword);
        for (int kill = 0; kill < killCount; kill++) {
            if (kill < tableDrops.size()) {
                addFilteredTableDrops(
                        drops,
                        tableDrops.get(kill),
                        kind,
                        enabledMask,
                        disabledDynamicLoot,
                        rules
                );
            }
            addEquipmentDrop(
                    drops,
                    targetId,
                    kind,
                    enabledMask,
                    lootingLevel,
                    random,
                    rules
            );
            addOminousBottle(drops, targetId, disabledDynamicLoot, random);
        }
        if (RaiderFarmTargetCatalog.isPillager(targetId)
                && rules.isEnabled(enabledMask, kind, RaiderFarmLoot.OMINOUS_BANNER)) {
            for (int kill = 0; kill < killCount; kill++) {
                addStack(drops, ominousBanner(level.registryAccess()));
            }
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
        Identifier entityTypeId = RaiderFarmTargetCatalog.entityTypeId(targetId);
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(entityTypeId).orElse(null);
        Entity entity = type == null ? null : type.create(level, EntitySpawnReason.LOAD);
        return entity instanceof LivingEntity living ? living : null;
    }

    private static void addEquipmentDrop(
            List<ItemStack> drops,
            Identifier targetId,
            RaiderFarmKind kind,
            int enabledMask,
            int lootingLevel,
            RandomSource random,
            RaiderFarmUseCase rules
    ) {
        ItemStack weapon = RaiderFarmTargetCatalog.defaultWeapon(targetId);
        if (weapon.isEmpty()
                || !rules.isEnabled(enabledMask, kind, RaiderFarmLoot.WEAPONS)
                || random.nextDouble() >= RaiderFarmDropRules.weaponChance(lootingLevel)) {
            return;
        }
        addStack(drops, weapon);
    }

    private static void addOminousBottle(
            List<ItemStack> drops,
            Identifier targetId,
            Set<Identifier> disabledDynamicLoot,
            RandomSource random
    ) {
        Identifier bottleId = BuiltInRegistries.ITEM.getKey(Items.OMINOUS_BOTTLE);
        if (!RaiderFarmTargetCatalog.isPillager(targetId)
                || bottleId == null
                || disabledDynamicLoot.contains(bottleId)) {
            return;
        }
        ItemStack bottle = new ItemStack(Items.OMINOUS_BOTTLE);
        bottle.set(
                DataComponents.OMINOUS_BOTTLE_AMPLIFIER,
                new OminousBottleAmplifier(random.nextInt(5))
        );
        addStack(drops, bottle);
    }

    private static void addFilteredTableDrops(
            List<ItemStack> drops,
            List<ItemStack> generated,
            RaiderFarmKind kind,
            int enabledMask,
            Set<Identifier> disabledDynamicLoot,
            RaiderFarmUseCase rules
    ) {
        for (ItemStack stack : generated) {
            RaiderFarmLoot category = RaiderFarmTargetCatalog.category(stack.getItem());
            Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
            boolean enabled = category == null
                    ? itemId != null && !disabledDynamicLoot.contains(itemId)
                    : rules.isEnabled(enabledMask, kind, category);
            if (enabled) {
                addStack(drops, stack.copy());
            }
        }
    }

    public static List<ItemStack> previewOutputs(Identifier targetId, RaiderFarmKind kind) {
        return previewOutputChances(targetId, kind).stream().map(PreviewOutput::stack).toList();
    }

    public static List<PreviewOutput> previewOutputChances(Identifier targetId, RaiderFarmKind kind) {
        List<PreviewOutput> outputs = new ArrayList<>();
        ItemStack weapon = RaiderFarmTargetCatalog.defaultWeapon(targetId);
        if (!weapon.isEmpty()) {
            outputs.add(new PreviewOutput(
                    RaiderFarmLoot.WEAPONS,
                    weapon,
                    probabilityParts(RaiderFarmDropRules.weaponChance(0)),
                    1,
                    1
            ));
        }
        if (RaiderFarmTargetCatalog.isPillager(targetId)
                && kind.supports(RaiderFarmLoot.OMINOUS_BANNER)) {
            outputs.add(new PreviewOutput(
                    RaiderFarmLoot.OMINOUS_BANNER,
                    new ItemStack(BuiltInRegistries.ITEM.getOptional(WHITE_BANNER_ID).orElseThrow()),
                    RaiderFarmDropRules.PROBABILITY_PARTS_PER_MILLION,
                    1,
                    1
            ));
        }
        return List.copyOf(outputs);
    }

    public static java.util.Optional<RaiderFarmDropRules.BaseDrop> currentDynamicCycleDrop(
            Identifier targetId,
            ItemStack previewStack,
            int lootingLevel,
            int simulatedKills
    ) {
        if (RaiderFarmTargetCatalog.isPillager(targetId) && previewStack.is(Items.OMINOUS_BOTTLE)) {
            return java.util.Optional.of(RaiderFarmDropRules.guaranteedCycleDrop(1, 1, simulatedKills));
        }
        if (!RaiderFarmTargetCatalog.isWitch(targetId)) {
            return java.util.Optional.empty();
        }
        if (previewStack.is(Items.REDSTONE)) {
            return java.util.Optional.of(RaiderFarmDropRules.guaranteedCycleDrop(
                    4,
                    8 + Math.max(0, lootingLevel),
                    simulatedKills
            ));
        }
        int weight = previewStack.is(Items.STICK) ? 2 : isWitchCommonDrop(previewStack) ? 1 : 0;
        return weight == 0
                ? java.util.Optional.empty()
                : java.util.Optional.of(RaiderFarmDropRules.witchCommonCycleDrop(
                        weight,
                        lootingLevel,
                        simulatedKills
                ));
    }

    private static boolean isWitchCommonDrop(ItemStack stack) {
        return stack.is(Items.GLOWSTONE_DUST)
                || stack.is(Items.SUGAR)
                || stack.is(Items.SPIDER_EYE)
                || stack.is(Items.GLASS_BOTTLE)
                || stack.is(Items.GUNPOWDER);
    }

    public static RaiderFarmDropRules.BaseDrop currentCycleDrop(
            RaiderFarmKind ignoredKind,
            RaiderFarmLoot ignoredLoot,
            ItemStack ignoredPreviewStack,
            int lootingLevel,
            int simulatedKills,
            int ignoredDecapitationLevel
    ) {
        return ignoredLoot == RaiderFarmLoot.OMINOUS_BANNER
                ? RaiderFarmDropRules.binaryCycleDrop(1.0D, simulatedKills)
                : RaiderFarmDropRules.binaryCycleDrop(
                        RaiderFarmDropRules.weaponChance(lootingLevel),
                        simulatedKills
                );
    }

    public static ItemStack ominousBanner(HolderLookup.Provider registries) {
        return Raid.getOminousBannerInstance(registries.lookupOrThrow(Registries.BANNER_PATTERN));
    }

    private static int probabilityParts(double chance) {
        return (int) Math.round(Math.clamp(chance, 0.0D, 1.0D)
                * RaiderFarmDropRules.PROBABILITY_PARTS_PER_MILLION);
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
            RaiderFarmLoot loot,
            ItemStack stack,
            int probabilityPartsPerMillion,
            int minimumAmount,
            int maximumAmount
    ) {
        public PreviewOutput {
            stack = stack.copy();
            if (stack.isEmpty()
                    || probabilityPartsPerMillion < 0
                    || probabilityPartsPerMillion > RaiderFarmDropRules.PROBABILITY_PARTS_PER_MILLION
                    || minimumAmount < 1
                    || maximumAmount < minimumAmount) {
                throw new IllegalArgumentException("Invalid Raider Farm preview output");
            }
        }

        @Override
        public ItemStack stack() {
            return stack.copy();
        }
    }
}
