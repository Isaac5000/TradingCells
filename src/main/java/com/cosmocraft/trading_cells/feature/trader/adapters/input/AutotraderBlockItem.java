package com.cosmocraft.trading_cells.feature.trader.adapters.input;

import com.cosmocraft.trading_cells.feature.captures.adapters.api.CapturedMobStackAdapter;
import com.cosmocraft.trading_cells.feature.captures.domain.model.CapturedMobKind;
import com.cosmocraft.trading_cells.feature.trader.adapters.minecraft.VillagerEmploymentTooltip;
import com.mojang.serialization.DynamicOps;
import java.util.function.Consumer;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jspecify.annotations.Nullable;

public final class AutotraderBlockItem extends BlockItem {
    private static final String VILLAGER_STACK_TAG = "Slot0";
    private static final String POI_STACK_TAG = "StoredPoi";

    public AutotraderBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    void appendTooltip(
            ItemStack stack,
            Item.TooltipContext context,
            Consumer<Component> tooltip
    ) {
        CompoundTag blockEntityTag = blockEntityTag(stack);
        ItemStack villagerStack = decodeStack(blockEntityTag, VILLAGER_STACK_TAG, context.registries());
        ItemStack poiStack = decodeStack(blockEntityTag, POI_STACK_TAG, context.registries());
        VillagerEmploymentTooltip.append(
                CapturedMobStackAdapter.copyData(CapturedMobKind.VILLAGER, villagerStack),
                poiStack,
                context.registries(),
                tooltip
        );
    }

    private static @Nullable CompoundTag blockEntityTag(ItemStack stack) {
        TypedEntityData<BlockEntityType<?>> data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        return data == null ? null : data.copyTagWithoutId();
    }

    private static ItemStack decodeStack(
            @Nullable CompoundTag blockEntityTag,
            String key,
            HolderLookup.@Nullable Provider registries
    ) {
        if (blockEntityTag == null || registries == null) {
            return ItemStack.EMPTY;
        }
        CompoundTag stackTag = blockEntityTag.getCompound(key).orElse(null);
        if (stackTag == null || stackTag.isEmpty()) {
            return ItemStack.EMPTY;
        }
        DynamicOps<net.minecraft.nbt.Tag> ops = registries.createSerializationContext(NbtOps.INSTANCE);
        return ItemStack.OPTIONAL_CODEC.parse(ops, stackTag).result().orElse(ItemStack.EMPTY);
    }
}
