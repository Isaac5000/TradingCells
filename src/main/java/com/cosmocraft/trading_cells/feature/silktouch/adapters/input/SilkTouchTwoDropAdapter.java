package com.cosmocraft.trading_cells.feature.silktouch.adapters.input;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.VaultBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import org.jspecify.annotations.Nullable;

/** Creates the extra block drop granted by vanilla Silk Touch at level two or above. */
public final class SilkTouchTwoDropAdapter {
    public static final int REQUIRED_LEVEL = 2;
    private static final String PRESERVED_DATA_MARKER = "trading_cells:silk_touch_two_preserved";
    private static final TagKey<Block> PICKAXE_BLOCKS = blockTag("silk_touch_two/pickaxe");
    private static final TagKey<Block> SHOVEL_BLOCKS = blockTag("silk_touch_two/shovel");
    private static final TagKey<Block> GENERAL_BLOCKS = blockTag("silk_touch_two/general");

    private SilkTouchTwoDropAdapter() {
    }

    public static ItemStack createDrop(
            BlockState state,
            ItemInstance tool,
            HolderLookup.Provider registries,
            @Nullable BlockEntity blockEntity
    ) {
        if (!hasSilkTouchTwo(tool, registries) || !isCorrectTool(state, tool)) {
            return ItemStack.EMPTY;
        }

        var item = state.getBlock().asItem();
        if (item == Items.AIR) {
            return ItemStack.EMPTY;
        }

        ItemStack drop = new ItemStack(item);
        BlockItemStateProperties stateProperties = copyState(stateForDrop(state));
        if (!stateProperties.isEmpty()) {
            drop.set(DataComponents.BLOCK_STATE, stateProperties);
        }

        if (blockEntity != null && blockEntity.getBlockState().is(state.getBlock())) {
            CompoundTag data = sanitizeBlockEntityData(state, blockEntity.saveCustomOnly(registries));
            drop.set(DataComponents.BLOCK_ENTITY_DATA, TypedEntityData.of(blockEntity.getType(), data));
            CompoundTag marker = new CompoundTag();
            marker.putBoolean(PRESERVED_DATA_MARKER, true);
            drop.set(DataComponents.CUSTOM_DATA, CustomData.of(marker));
        }

        if (state.is(Blocks.VAULT) && state.getValue(VaultBlock.OMINOUS)) {
            drop.set(
                    DataComponents.CUSTOM_NAME,
                    Component.translatable("item.trading_cells.ominous_vault", state.getBlock().getName())
            );
        }
        return drop;
    }

    public static boolean hasPreservedDataMarker(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        return customData != null
                && customData.copyTag().getBoolean(PRESERVED_DATA_MARKER).orElse(false);
    }

    public static boolean hasSilkTouchTwo(ItemInstance tool, HolderLookup.Provider registries) {
        return registries.lookup(Registries.ENCHANTMENT)
                .flatMap(enchantments -> enchantments.get(Enchantments.SILK_TOUCH))
                .map(tool::getEnchantmentLevel)
                .orElse(0) >= REQUIRED_LEVEL;
    }

    public static boolean isCorrectTool(BlockState state, ItemInstance tool) {
        if (state.is(PICKAXE_BLOCKS)) {
            return tool.is(ItemTags.PICKAXES);
        }
        if (state.is(SHOVEL_BLOCKS)) {
            return tool.is(ItemTags.SHOVELS);
        }
        return state.is(GENERAL_BLOCKS) && tool.is(ItemTags.MINING_LOOT_ENCHANTABLE);
    }

    public static boolean isSpecialBlock(BlockState state) {
        return state.is(PICKAXE_BLOCKS) || state.is(SHOVEL_BLOCKS) || state.is(GENERAL_BLOCKS);
    }

    private static BlockState stateForDrop(BlockState state) {
        if ((state.is(Blocks.SUSPICIOUS_SAND) || state.is(Blocks.SUSPICIOUS_GRAVEL))
                && state.hasProperty(BlockStateProperties.DUSTED)) {
            return state.setValue(BlockStateProperties.DUSTED, 0);
        }
        return state;
    }

    private static CompoundTag sanitizeBlockEntityData(BlockState state, CompoundTag data) {
        if (state.is(Blocks.SUSPICIOUS_SAND) || state.is(Blocks.SUSPICIOUS_GRAVEL)) {
            data.remove("hit_direction");
            data.remove("brush_count");
            data.remove("brush_count_resets_at_tick");
            data.remove("cool_down_ends_at_tick");
        }
        return data;
    }

    private static BlockItemStateProperties copyState(BlockState state) {
        Map<String, String> properties = new LinkedHashMap<>();
        for (Property<?> property : state.getProperties()) {
            copyProperty(state, property, properties);
        }
        return properties.isEmpty()
                ? BlockItemStateProperties.EMPTY
                : new BlockItemStateProperties(Map.copyOf(properties));
    }

    private static <T extends Comparable<T>> void copyProperty(
            BlockState state,
            Property<T> property,
            Map<String, String> properties
    ) {
        properties.put(property.getName(), property.getName(state.getValue(property)));
    }

    private static TagKey<Block> blockTag(String path) {
        return TagKey.create(
                Registries.BLOCK,
                Identifier.fromNamespaceAndPath("trading_cells", path)
        );
    }
}
