package com.cosmocraft.trading_cells.platform.neoforge.mobfarm;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntitySpawnRequest;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

/** Shared native loot execution, independent of farm families and output filters. */
public final class MobFarmLootTables {
    private MobFarmLootTables() { }

    public record DropSummary(int probability, int minimum, int maximum) { }

    public static ItemStack displayStack(Identifier id, LivingEntity target) {
        if (id.equals(Identifier.withDefaultNamespace("tipped_arrow"))) {
            Identifier targetId = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType());
            return switch (targetId.toString()) {
                case "minecraft:stray" -> PotionContents.createItemStack(Items.TIPPED_ARROW, Potions.SLOWNESS);
                case "minecraft:bogged" -> PotionContents.createItemStack(Items.TIPPED_ARROW, Potions.POISON);
                case "minecraft:parched" -> PotionContents.createItemStack(Items.TIPPED_ARROW, Potions.WEAKNESS);
                default -> new ItemStack(Items.TIPPED_ARROW);
            };
        }
        return BuiltInRegistries.ITEM.getOptional(id).map(ItemStack::new).orElse(ItemStack.EMPTY);
    }

    public static java.util.Map<Identifier, DropSummary> preview(ServerLevel level, LivingEntity target,
                                                                ItemStack sword, int kills) {
        if (target == null) { return java.util.Map.of(); }
        int looting = com.cosmocraft.trading_cells.feature.combat.adapters.api.CombatEnchantments
                .lootingLevel(sword, level.registryAccess());
        var analysis = MobFarmLootPreview.analyse(level, target, sword, looting, kills);
        var result = new java.util.LinkedHashMap<>(analysis.drops());
        for (Identifier id : filterItems(target)) {
            result.putIfAbsent(id, new DropSummary(analysis.complete() ? 0 : -1, 0, 0));
        }
        return java.util.Map.copyOf(result);
    }

    /** Uses the actual instance's table, so module variants are not limited to family catalogs. */
    public static List<Identifier> filterItems(LivingEntity target) {
        if (target.getLootTable().isEmpty()) { return List.of(); }
        var result = new java.util.LinkedHashSet<Identifier>();
        Identifier tableId = target.getLootTable().orElseThrow().identifier();
        var references = new LinkedHashSet<>(MobFarmLootTableReloadListener.references(tableId));
        for (Identifier injected : MobFarmLootTableReloadListener.modifierTables(tableId)) {
            references.addAll(MobFarmLootTableReloadListener.references(injected));
        }
        for (var reference : references) {
            if (reference.tag()) {
                var tag = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, reference.id());
                BuiltInRegistries.ITEM.getTagOrEmpty(tag).forEach(holder -> result.add(BuiltInRegistries.ITEM.getKey(holder.value())));
            } else if (BuiltInRegistries.ITEM.containsKey(reference.id())) { result.add(reference.id()); }
        }
        return List.copyOf(result);
    }

    /** Creates a detached target. Future capture adapters can restore its state before rolling. */
    public static LivingEntity createTarget(ServerLevel level, Identifier entityTypeId) {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(entityTypeId).orElse(null);
        Entity entity = type == null || !type.isEnabled(level.enabledFeatures()) ? null
                : type.create(level, new EntitySpawnRequest(EntitySpawnReason.LOAD, true));
        return entity instanceof LivingEntity living ? living : null;
    }

    /** Collects complete per-kill batches; broken external tables discard the whole result. */
    public static List<List<ItemStack>> batches(ServerLevel level, LivingEntity target, ItemStack sword, int kills) {
        if (target == null || target.getLootTable().isEmpty() || kills <= 0) {
            return List.of();
        }
        List<List<ItemStack>> batches = new ArrayList<>(kills);
        for (int kill = 0; kill < kills; kill++) {
            batches.add(new ArrayList<>());
        }
        try {
            roll(level, target, sword, kills, (kill, stack) -> batches.get(kill).add(stack.copy()));
            return batches.stream().map(List::copyOf).toList();
        } catch (RuntimeException | LinkageError ignored) {
            return List.of();
        }
    }

    /**
     * Uses the actual detached entity's table, including state-dependent overrides, through
     * Minecraft's loot pipeline. Call only on the server thread, never with a live world entity.
     * Failures propagate so each farm can retain its existing fallback/partial-drop policy.
     */
    public static void roll(ServerLevel level, LivingEntity target, ItemStack sword, int kills,
                            BiConsumer<Integer, ItemStack> output) {
        if (target == null || target.getLootTable().isEmpty() || kills <= 0) {
            return;
        }
        var attacker = FakePlayerFactory.getMinecraft(level);
        ItemStack previousWeapon = attacker.getMainHandItem().copy();
        try {
            attacker.setItemSlot(EquipmentSlot.MAINHAND, sword.copy());
            target.setLastHurtByPlayer(attacker, 100);
            for (int kill = 0; kill < kills; kill++) {
                int batch = kill;
                target.dropFromLootTable(level, level.damageSources().playerAttack(attacker), true,
                        target.getLootTable().orElseThrow(), stack -> output.accept(batch, stack));
            }
        } finally {
            attacker.setItemSlot(EquipmentSlot.MAINHAND, previousWeapon);
        }
    }
}
