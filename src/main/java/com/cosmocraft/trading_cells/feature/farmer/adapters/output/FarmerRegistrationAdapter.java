package com.cosmocraft.trading_cells.feature.farmer.adapters.output;

import com.cosmocraft.trading_cells.feature.farmer.adapters.input.FarmerBlock;
import com.cosmocraft.trading_cells.feature.farmer.adapters.input.FarmerBlockEntity;
import com.cosmocraft.trading_cells.feature.farmer.adapters.input.FarmerMenu;
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

public final class FarmerRegistrationAdapter {
    public static final String FARMER_ID = "farmer";

    public static final DeferredBlock<FarmerBlock> FARMER_BLOCK = Registration.BLOCKS.register(FARMER_ID, () ->
            new FarmerBlock(MachineBlockProperties.villager(FARMER_ID))
    );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FarmerBlockEntity>> FARMER_BLOCK_ENTITY =
            Registration.BLOCK_ENTITY_TYPES.register(FARMER_ID, () ->
                    new BlockEntityType<>(FarmerBlockEntity::new, FARMER_BLOCK.get())
            );

    public static final DeferredItem<BlockItem> FARMER_ITEM = Registration.ITEMS.register(FARMER_ID, () ->
            new BlockItem(FARMER_BLOCK.get(), new Item.Properties().setId(
                    ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(TradingCells.MOD_ID, FARMER_ID))
            ))
    );

    public static final DeferredHolder<MenuType<?>, MenuType<FarmerMenu>> FARMER_MENU =
            Registration.MENU_TYPES.register(FARMER_ID, () -> new MenuType<>(FarmerMenu::new, FeatureFlags.VANILLA_SET));

    private FarmerRegistrationAdapter() {
    }

    public static void load() {
        // Forces class loading so all DeferredRegister entries are created.
    }
}
