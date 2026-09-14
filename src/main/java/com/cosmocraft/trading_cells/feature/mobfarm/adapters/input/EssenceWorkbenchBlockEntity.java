package com.cosmocraft.trading_cells.feature.mobfarm.adapters.input;

import com.cosmocraft.trading_cells.feature.mobfarm.adapters.output.MobFarmRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.mobfarm.domain.model.MobFarmRules;
import com.cosmocraft.trading_cells.platform.neoforge.experience.PlayerExperienceTransfer;
import com.cosmocraft.trading_cells.shared.machines.domain.model.MinecraftExperience;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;

public final class EssenceWorkbenchBlockEntity extends SimulationInventoryBlockEntity implements MenuProvider {
    public EssenceWorkbenchBlockEntity(BlockPos pos, BlockState state) {
        super(MobFarmRegistrationAdapter.WORKBENCH_BLOCK_ENTITY.get(), pos, state, 4, 3);
    }

    @Override public void processTick() { }
    @Override public boolean supportsRedstoneControl() { return false; }
    @Override public Component getDisplayName() { return Component.translatable("block.trading_cells.essence_workbench"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new EssenceWorkbenchMenu(id, inventory, this);
    }

    @Override protected boolean acceptsInput(int slot, ItemStack stack) {
        return switch (slot) {
            case 0 -> stack.is(MobFarmRegistrationAdapter.ENTITY_ESSENCE.get()) && EntityEssenceData.entityTypeId(stack) != null;
            case 1 -> stack.is(Items.AMETHYST_SHARD);
            case 2 -> stack.is(Items.IRON_INGOT) || stack.is(Items.NETHERITE_INGOT);
            default -> false;
        };
    }

    public boolean synthesize(ServerPlayer player) {
        if (!stillValid(player) || !items.get(3).isEmpty() || !acceptsInput(0, items.get(0))) { return false; }
        boolean high = EntityEssenceData.isHighLevel(items.get(0));
        int shards = MobFarmRules.essenceShardCost(high);
        int material = MobFarmRules.essenceMaterialCost(high);
        int cost = MobFarmRules.essenceExperienceCost(high);
        if (!items.get(1).is(Items.AMETHYST_SHARD) || items.get(1).getCount() < shards
                || !items.get(2).is(high ? Items.NETHERITE_INGOT : Items.IRON_INGOT)
                || items.get(2).getCount() < material
                || MinecraftExperience.totalPoints(player.experienceLevel, player.experienceProgress) < cost
                || EntityEssenceData.createEntity(player.level(), items.get(0)) == null) { return false; }
        ItemStack module = EntityEssenceData.moduleOf(items.get(0));
        if (module.isEmpty()) { return false; }
        int paid = PlayerExperienceTransfer.removePoints(player, cost);
        if (paid != cost) {
            PlayerExperienceTransfer.addPoints(player, paid);
            return false;
        }
        items.get(0).shrink(1);
        items.get(1).shrink(shards);
        items.get(2).shrink(material);
        items.set(3, module);
        markChangedAndSync();
        return true;
    }
}
