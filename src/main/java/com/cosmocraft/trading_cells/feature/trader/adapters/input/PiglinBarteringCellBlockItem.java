package com.cosmocraft.trading_cells.feature.trader.adapters.input;

import com.cosmocraft.trading_cells.feature.captures.adapters.api.CapturedMobStackAdapter;
import com.cosmocraft.trading_cells.feature.captures.domain.model.CapturedMobKind;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class PiglinBarteringCellBlockItem extends BlockItem {
    private static final String PIGLIN_DATA_TAG = "StoredPiglin";

    public PiglinBarteringCellBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    void appendTooltip(ItemStack itemStack, Consumer<Component> builder) {
        TypedEntityData<BlockEntityType<?>> data = itemStack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (data == null) {
            return;
        }

        CompoundTag blockEntityTag = data.copyTagWithoutId();
        CompoundTag piglinData = blockEntityTag.getCompound(PIGLIN_DATA_TAG).orElse(null);
        if (piglinData == null || piglinData.isEmpty()) {
            return;
        }

        Component age = CapturedMobStackAdapter.isBaby(CapturedMobKind.PIGLIN, piglinData)
                ? Component.translatable("tooltip.trading_cells.baby")
                : Component.translatable("tooltip.trading_cells.adult");
        builder.accept(Component.translatable("tooltip.trading_cells.piglin", age).withStyle(ChatFormatting.GRAY));
    }
}
