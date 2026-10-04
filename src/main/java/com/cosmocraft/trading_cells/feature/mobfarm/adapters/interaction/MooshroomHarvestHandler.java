package com.cosmocraft.trading_cells.feature.mobfarm.adapters.interaction;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.cow.MushroomCow;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.neoforged.neoforge.common.ItemAbilities;

/** Explicit non-converting harvest; preserves vanilla shearing loot and tool wear. */
public final class MooshroomHarvestHandler implements InteractionHarvestHandler {
    @Override public boolean supports(LivingEntity entity, ItemStack input) {
        return entity instanceof MushroomCow && input.canPerformAction(ItemAbilities.SHEARS_HARVEST);
    }

    @Override public void interact(DisposableInteractionLevel level, LivingEntity entity, Player actor) {
        if (!((MushroomCow) entity).readyForShearing()) { return; }
        ItemStack tool = actor.getMainHandItem();
        var params = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, entity.position())
                .withParameter(LootContextParams.THIS_ENTITY, entity)
                .withParameter(LootContextParams.TOOL, tool)
                .create(LootContextParamSets.SHEARING);
        var products = level.getServer().reloadableRegistries().getLootTable(BuiltInLootTables.SHEAR_MOOSHROOM).getRandomItems(params);
        if (products.isEmpty()) { return; }
        for (var product : products) { entity.spawnAtLocation(level, product); }
        tool.hurtAndBreak(1, actor, EquipmentSlot.MAINHAND);
    }
}
