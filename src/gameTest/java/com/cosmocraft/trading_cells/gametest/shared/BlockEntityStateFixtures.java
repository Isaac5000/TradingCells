package com.cosmocraft.trading_cells.gametest.shared;

import com.mojang.serialization.DynamicOps;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.TagValueInput;

/** Builds persisted Block Entity states without depending on implementation fields. */
public final class BlockEntityStateFixtures {
    private BlockEntityStateFixtures() {
    }

    public static void fillIndexedSlots(
            GameTestHelper helper,
            BlockEntity blockEntity,
            String keyPrefix,
            int firstSlot,
            int slotCount,
            ItemStack stack
    ) {
        CompoundTag data = blockEntity.saveWithFullMetadata(helper.getLevel().registryAccess());
        DynamicOps<Tag> ops = helper.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE);
        Tag encoded = ItemStack.CODEC.encodeStart(ops, stack)
                .result()
                .orElseThrow(() -> new IllegalStateException("Could not encode fixture item stack"));
        for (int slot = firstSlot; slot < firstSlot + slotCount; slot++) {
            data.put(keyPrefix + slot, encoded.copy());
        }
        blockEntity.loadWithComponents(TagValueInput.create(
                ProblemReporter.DISCARDING,
                helper.getLevel().registryAccess(),
                data
        ));
    }

    public static void clearIndexedSlots(
            GameTestHelper helper,
            BlockEntity blockEntity,
            String keyPrefix,
            int firstSlot,
            int slotCount
    ) {
        CompoundTag data = blockEntity.saveWithFullMetadata(helper.getLevel().registryAccess());
        for (int slot = firstSlot; slot < firstSlot + slotCount; slot++) {
            data.remove(keyPrefix + slot);
        }
        blockEntity.loadWithComponents(TagValueInput.create(
                ProblemReporter.DISCARDING,
                helper.getLevel().registryAccess(),
                data
        ));
    }

    public static void setInt(
            GameTestHelper helper,
            BlockEntity blockEntity,
            String key,
            int value
    ) {
        CompoundTag data = blockEntity.saveWithFullMetadata(helper.getLevel().registryAccess());
        data.putInt(key, value);
        blockEntity.loadWithComponents(TagValueInput.create(
                ProblemReporter.DISCARDING,
                helper.getLevel().registryAccess(),
                data
        ));
    }
}
