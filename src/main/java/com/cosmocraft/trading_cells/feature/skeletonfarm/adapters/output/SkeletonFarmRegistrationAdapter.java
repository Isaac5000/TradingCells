package com.cosmocraft.trading_cells.feature.skeletonfarm.adapters.output;

import com.cosmocraft.trading_cells.feature.skeletonfarm.adapters.input.SkeletonFarmBlock;
import com.cosmocraft.trading_cells.feature.skeletonfarm.adapters.input.SkeletonFarmBlockEntity;
import com.cosmocraft.trading_cells.feature.skeletonfarm.adapters.input.SkeletonFarmMenu;
import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.TradingCells;
import com.cosmocraft.trading_cells.platform.neoforge.machine.MachineBlockProperties;
import com.cosmocraft.trading_cells.platform.neoforge.registration.Registration;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

public final class SkeletonFarmRegistrationAdapter {
    public static final String ID = "skeleton_farm";

    public static final DeferredBlock<SkeletonFarmBlock> BLOCK = Registration.BLOCKS.register(ID, () ->
            new SkeletonFarmBlock(MachineBlockProperties.villager(ID))
    );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SkeletonFarmBlockEntity>> BLOCK_ENTITY =
            Registration.BLOCK_ENTITY_TYPES.register(ID, () ->
                    new BlockEntityType<>(SkeletonFarmBlockEntity::new, BLOCK.get())
            );

    public static final DeferredItem<BlockItem> ITEM = Registration.ITEMS.register(ID, () ->
            new BlockItem(BLOCK.get(), new Item.Properties().setId(ResourceKey.create(
                    Registries.ITEM,
                    Identifier.fromNamespaceAndPath(TradingCells.MOD_ID, ID)
            )))
    );

    public static final DeferredHolder<MenuType<?>, MenuType<SkeletonFarmMenu>> MENU =
            Registration.MENU_TYPES.register(ID, () ->
                    new MenuType<>(SkeletonFarmMenu::new, FeatureFlags.VANILLA_SET)
            );

    private SkeletonFarmRegistrationAdapter() {
    }

    public static void load() {
        // Forces class loading so all DeferredRegister entries are created.
    }
}
