package com.cosmocraft.trading_cells.feature.mobfarm.adapters.input;

import com.cosmocraft.trading_cells.feature.mobfarm.adapters.output.MobFarmRegistrationAdapter;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public final class CreatureGeneratorBlockEntity extends EssenceWorkbenchBlockEntity {
    public CreatureGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(MobFarmRegistrationAdapter.CREATURE_GENERATOR_BLOCK_ENTITY.get(), pos, state);
    }
    @Override public Component getDisplayName() { return Component.translatable("block.trading_cells.creature_generator"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new EssenceWorkbenchMenu(id, inventory, this, data, true);
    }
    @Override protected boolean allowsCore(ItemStack core) {
        return level instanceof ServerLevel && CreatureGeneratorEggs.allowed(EntityEssenceData.entityTypeId(core));
    }
    @Override protected boolean acceptsInput(int slot, ItemStack stack) {
        return slot == 1 ? CreatureGeneratorEggs.isIngredient(stack) : super.acceptsInput(slot, stack);
    }
    @Override protected boolean matchesMaterial(ItemStack core, ItemStack material) {
        return CreatureGeneratorEggs.isIngredient(material);
    }
    @Override protected ItemStack createResult(ItemStack core) {
        return level instanceof ServerLevel ? CreatureGeneratorEggs.create(core) : ItemStack.EMPTY;
    }
}
