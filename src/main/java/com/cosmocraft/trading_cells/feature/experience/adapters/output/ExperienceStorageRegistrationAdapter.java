package com.cosmocraft.trading_cells.feature.experience.adapters.output;

import com.cosmocraft.trading_cells.feature.experience.adapters.input.ExperienceStorageBlock;
import com.cosmocraft.trading_cells.feature.experience.adapters.input.ExperienceStorageBlockEntity;
import com.cosmocraft.trading_cells.feature.experience.adapters.input.ExperienceStorageBlockItem;
import com.cosmocraft.trading_cells.feature.experience.adapters.input.ExperienceStorageMenu;
import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.TradingCells;
import com.cosmocraft.trading_cells.platform.neoforge.machine.MachineBlockProperties;
import com.cosmocraft.trading_cells.platform.neoforge.registration.Registration;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

public final class ExperienceStorageRegistrationAdapter {
    public static final String ID = "experience_storage";

    public static final DeferredBlock<ExperienceStorageBlock> BLOCK = Registration.BLOCKS.register(
            ID,
            () -> new ExperienceStorageBlock(MachineBlockProperties.villager(ID))
    );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ExperienceStorageBlockEntity>> BLOCK_ENTITY =
            Registration.BLOCK_ENTITY_TYPES.register(
                    ID,
                    () -> new BlockEntityType<>(ExperienceStorageBlockEntity::new, BLOCK.get())
            );
    public static final DeferredItem<ExperienceStorageBlockItem> ITEM = Registration.ITEMS.register(
            ID,
            () -> new ExperienceStorageBlockItem(BLOCK.get(), new Item.Properties().setId(ResourceKey.create(
                    Registries.ITEM,
                    Identifier.fromNamespaceAndPath(TradingCells.MOD_ID, ID)
            )))
    );
    public static final DeferredHolder<MenuType<?>, MenuType<ExperienceStorageMenu>> MENU =
            Registration.MENU_TYPES.register(
                    ID,
                    () -> new MenuType<>(ExperienceStorageMenu::new, FeatureFlags.VANILLA_SET)
            );

    private ExperienceStorageRegistrationAdapter() {
    }

    public static void load() {
        // Forces class loading so all deferred entries are created.
    }
}
