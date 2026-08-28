package com.cosmocraft.trading_cells.feature.creeperfarm.adapters.input;

import com.cosmocraft.trading_cells.feature.combat.adapters.api.CombatItems;
import com.cosmocraft.trading_cells.feature.combat.domain.model.DecapitationRules;
import com.cosmocraft.trading_cells.feature.combat.domain.model.StormShardDropRules;
import com.cosmocraft.trading_cells.feature.creeperfarm.application.port.input.CreeperFarmUseCase;
import com.cosmocraft.trading_cells.feature.creeperfarm.domain.model.CreeperFarmDropRules;
import com.cosmocraft.trading_cells.feature.creeperfarm.domain.model.CreeperFarmKind;
import com.cosmocraft.trading_cells.feature.creeperfarm.domain.model.CreeperFarmLoot;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

/** Executes real creeper loot and adds the mod's charged-creeper and Decapitation drops. */
public final class CreeperFarmLootAdapter {
    private CreeperFarmLootAdapter() {
    }

    static List<ItemStack> generate(
            Identifier targetId,
            CreeperFarmKind kind,
            int enabledMask,
            Set<Identifier> disabledDynamicLoot,
            int kills,
            int lootingLevel,
            int decapitationLevel,
            ServerLevel level,
            ItemStack sword,
            RandomSource random,
            CreeperFarmUseCase rules
    ) {
        int killCount = Math.max(1, kills);
        int looting = Math.max(0, lootingLevel);
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
            if (kind == CreeperFarmKind.CHARGED_CREEPER
                    && rules.isEnabled(enabledMask, kind, CreeperFarmLoot.STORM_SHARDS)) {
                int amount = 1 + random.nextInt(StormShardDropRules.maximumAmount(looting));
                addStack(drops, new ItemStack(CombatItems.stormShard(), amount));
            }
            if (random.nextDouble() < DecapitationRules.decapitationHeadChance(decapitationLevel)
                    && rules.isEnabled(enabledMask, kind, CreeperFarmLoot.HEADS)) {
                addStack(drops, new ItemStack(Items.CREEPER_HEAD));
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
        Identifier entityTypeId = CreeperFarmTargetCatalog.entityTypeId(targetId);
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(entityTypeId).orElse(null);
        Entity entity = type == null ? null : type.create(level, EntitySpawnReason.LOAD);
        return entity instanceof LivingEntity living ? living : null;
    }

    private static void addFilteredTableDrops(
            List<ItemStack> drops,
            List<ItemStack> generated,
            CreeperFarmKind kind,
            int enabledMask,
            Set<Identifier> disabledDynamicLoot,
            CreeperFarmUseCase rules
    ) {
        for (ItemStack stack : generated) {
            if (CreeperFarmTargetCatalog.isMusicDisc(stack.getItem())) {
                continue;
            }
            CreeperFarmLoot category = CreeperFarmTargetCatalog.category(stack.getItem());
            Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
            boolean enabled = category == null
                    ? itemId != null && !disabledDynamicLoot.contains(itemId)
                    : rules.isEnabled(enabledMask, kind, category);
            if (enabled) {
                addStack(drops, stack.copy());
            }
        }
    }

    public static List<ItemStack> previewOutputs(CreeperFarmKind kind) {
        return previewOutputChances(kind).stream().map(PreviewOutput::stack).toList();
    }

    public static List<PreviewOutput> previewOutputChances(CreeperFarmKind kind) {
        List<PreviewOutput> outputs = new ArrayList<>();
        addPreview(outputs, CreeperFarmLoot.GUNPOWDER, Items.GUNPOWDER,
                CreeperFarmDropRules.gunpowderCycleDrop(0, 1));
        if (kind == CreeperFarmKind.CHARGED_CREEPER) {
            addPreview(outputs, CreeperFarmLoot.STORM_SHARDS, CombatItems.stormShard(),
                    CreeperFarmDropRules.stormShardCycleDrop(0, 1));
        }
        addPreview(outputs, CreeperFarmLoot.HEADS, Items.CREEPER_HEAD,
                CreeperFarmDropRules.headCycleDrop(1, 1));
        return List.copyOf(outputs);
    }

    public static CreeperFarmDropRules.BaseDrop currentCycleDrop(
            CreeperFarmKind ignoredKind,
            CreeperFarmLoot loot,
            ItemStack ignoredPreviewStack,
            int lootingLevel,
            int simulatedKills,
            int decapitationLevel
    ) {
        return switch (loot) {
            case GUNPOWDER -> CreeperFarmDropRules.gunpowderCycleDrop(lootingLevel, simulatedKills);
            case STORM_SHARDS -> CreeperFarmDropRules.stormShardCycleDrop(lootingLevel, simulatedKills);
            case HEADS -> CreeperFarmDropRules.headCycleDrop(simulatedKills, decapitationLevel);
        };
    }

    private static void addPreview(
            List<PreviewOutput> outputs,
            CreeperFarmLoot loot,
            Item item,
            CreeperFarmDropRules.BaseDrop drop
    ) {
        outputs.add(new PreviewOutput(
                loot,
                new ItemStack(item),
                drop.probabilityPartsPerMillion(),
                drop.minimumAmount(),
                drop.maximumAmount()
        ));
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
            CreeperFarmLoot loot,
            ItemStack stack,
            int probabilityPartsPerMillion,
            int minimumAmount,
            int maximumAmount
    ) {
        public PreviewOutput {
            stack = stack.copy();
            if (stack.isEmpty()
                    || probabilityPartsPerMillion < 0
                    || probabilityPartsPerMillion > CreeperFarmDropRules.PROBABILITY_PARTS_PER_MILLION
                    || minimumAmount < 1
                    || maximumAmount < minimumAmount) {
                throw new IllegalArgumentException("Invalid Creeper Farm preview output");
            }
        }

        @Override
        public ItemStack stack() {
            return stack.copy();
        }
    }
}
