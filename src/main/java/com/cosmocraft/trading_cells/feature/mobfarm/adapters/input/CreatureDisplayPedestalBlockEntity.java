package com.cosmocraft.trading_cells.feature.mobfarm.adapters.input;

import com.cosmocraft.trading_cells.feature.mobfarm.adapters.output.MobFarmRegistrationAdapter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.NonNull;

/** Stores only an item and the visual rotation preference; never ticks or spawns mobs. */
public final class CreatureDisplayPedestalBlockEntity extends BlockEntity {
    private ItemStack model = ItemStack.EMPTY;
    private boolean rotating = false;

    public CreatureDisplayPedestalBlockEntity(BlockPos pos, BlockState state) {
        super(MobFarmRegistrationAdapter.DISPLAY_PEDESTAL_BLOCK_ENTITY.get(), pos, state);
    }

    public static boolean accepts(ItemStack stack) {
        return stack.is(MobFarmRegistrationAdapter.ENTITY_MODULE.get())
                && EntityEssenceData.entityTypeId(stack) != null;
    }

    public ItemStack model() { return model; }
    public boolean hasModel() { return !model.isEmpty(); }
    public boolean rotating() { return rotating; }

    public boolean insert(ItemStack stack) {
        if (hasModel() || !accepts(stack)) { return false; }
        model = stack.copyWithCount(1);
        changed();
        return true;
    }

    public ItemStack removeModel() {
        ItemStack result = model;
        model = ItemStack.EMPTY;
        changed();
        return result;
    }

    public void toggleRotation() {
        rotating = !rotating;
        changed();
    }

    private void changed() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(@NonNull ValueOutput output) {
        super.saveAdditional(output);
        if (!model.isEmpty()) { output.store("DisplayModel", ItemStack.CODEC, model); }
        output.putBoolean("DisplayRotating", rotating);
    }

    @Override
    protected void loadAdditional(@NonNull ValueInput input) {
        super.loadAdditional(input);
        ItemStack saved = input.read("DisplayModel", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        model = accepts(saved) ? saved.copyWithCount(1) : ItemStack.EMPTY;
        rotating = input.getBooleanOr("DisplayRotating", false);
    }

    @Override
    public @NonNull Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public @NonNull CompoundTag getUpdateTag(HolderLookup.@NonNull Provider registries) {
        return saveCustomOnly(registries);
    }
}
