package com.cosmocraft.trading_cells.feature.trader.adapters.input;

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
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class VillagerTradingCellBlockItem extends BlockItem {
    private static final String LEGACY_VILLAGER_DATA_TAG = "StoredVillager";
    private static final String ENTITY_KIND_TAG = "StoredEntityKind";
    private static final String ENTITY_DATA_TAG = "StoredEntity";
    private static final String POI_STACK_TAG = "StoredPoi";
    private static final String VILLAGER_KIND = "villager";

    public VillagerTradingCellBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(
            @NonNull ItemStack itemStack,
            Item.@NonNull TooltipContext context,
            @NonNull TooltipDisplay display,
            @NonNull Consumer<Component> builder,
            @NonNull TooltipFlag tooltipFlag
    ) {
        super.appendHoverText(itemStack, context, display, builder, tooltipFlag);

        CompoundTag blockEntityTag = getBlockEntityTag(itemStack);
        CompoundTag villagerData = blockEntityTag == null ? null : getVillagerData(blockEntityTag);
        ItemStack poiStack = blockEntityTag == null
                ? ItemStack.EMPTY
                : getPoiStack(blockEntityTag, context.registries());
        VillagerEmploymentTooltip.append(villagerData, poiStack, context.registries(), builder);
    }

    private static @Nullable CompoundTag getBlockEntityTag(ItemStack stack) {
        TypedEntityData<BlockEntityType<?>> data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        return data == null ? null : data.copyTagWithoutId();
    }

    private static @Nullable CompoundTag getVillagerData(CompoundTag blockEntityTag) {
        String kind = blockEntityTag.getString(ENTITY_KIND_TAG).orElse("");
        if (VILLAGER_KIND.equals(kind)) {
            return blockEntityTag.getCompound(ENTITY_DATA_TAG).map(CompoundTag::copy).orElse(null);
        }
        return blockEntityTag.getCompound(LEGACY_VILLAGER_DATA_TAG).map(CompoundTag::copy).orElse(null);
    }

    private static ItemStack getPoiStack(CompoundTag blockEntityTag, HolderLookup.@Nullable Provider registries) {
        CompoundTag poiTag = blockEntityTag.getCompound(POI_STACK_TAG).orElse(null);
        if (poiTag == null || poiTag.isEmpty() || registries == null) {
            return ItemStack.EMPTY;
        }

        DynamicOps<net.minecraft.nbt.Tag> ops = registries.createSerializationContext(NbtOps.INSTANCE);
        return ItemStack.OPTIONAL_CODEC.parse(ops, poiTag).result().orElse(ItemStack.EMPTY);
    }

}
