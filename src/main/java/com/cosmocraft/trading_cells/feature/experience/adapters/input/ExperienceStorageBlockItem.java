package com.cosmocraft.trading_cells.feature.experience.adapters.input;

import com.cosmocraft.trading_cells.feature.experience.domain.model.ExperienceMath;
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

public final class ExperienceStorageBlockItem extends BlockItem {
    public ExperienceStorageBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    void appendTooltip(ItemStack stack, Consumer<Component> builder) {
        TypedEntityData<BlockEntityType<?>> data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (data == null) {
            return;
        }

        CompoundTag blockEntityTag = data.copyTagWithoutId();
        int storedExperience = blockEntityTag
                .getInt(ExperienceStorageBlockEntity.STORED_EXPERIENCE_TAG)
                .orElse(0);
        if (storedExperience <= 0) {
            return;
        }

        builder.accept(Component.translatable(
                "tooltip.trading_cells.experience_storage_xp",
                storedExperience
        ).withStyle(ChatFormatting.GRAY));
        builder.accept(Component.translatable(
                "tooltip.trading_cells.experience_storage_levels",
                ExperienceMath.levelForTotalPoints(storedExperience)
        ).withStyle(ChatFormatting.GRAY));
    }
}
