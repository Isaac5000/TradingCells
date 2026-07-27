package com.cosmocraft.trading_cells.feature.incubators.adapters.output;

import com.cosmocraft.trading_cells.feature.incubators.adapters.input.IncubatorMenu;
import com.cosmocraft.trading_cells.feature.incubators.adapters.input.PiglinIncubatorBlock;
import com.cosmocraft.trading_cells.feature.incubators.adapters.input.PiglinIncubatorBlockEntity;
import com.cosmocraft.trading_cells.feature.incubators.adapters.input.VillagerIncubatorBlock;
import com.cosmocraft.trading_cells.feature.incubators.adapters.input.VillagerIncubatorBlockEntity;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

public final class IncubatorRegistrationAdapter {
    public static final String VILLAGER_INCUBATOR_ID = "villager_incubator";
    public static final String PIGLIN_INCUBATOR_ID = "piglin_incubator";

    public static final DeferredBlock<VillagerIncubatorBlock> VILLAGER_INCUBATOR_BLOCK =
            Registration.BLOCKS.register(VILLAGER_INCUBATOR_ID, () -> new VillagerIncubatorBlock(
                    MachineBlockProperties.villagerCopyOf(VILLAGER_INCUBATOR_ID, Blocks.WOOL.yellow())
            ));

    public static final DeferredBlock<PiglinIncubatorBlock> PIGLIN_INCUBATOR_BLOCK =
            Registration.BLOCKS.register(PIGLIN_INCUBATOR_ID, () -> new PiglinIncubatorBlock(
                    MachineBlockProperties.piglinCopyOf(PIGLIN_INCUBATOR_ID, Blocks.WOOL.red())
            ));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<VillagerIncubatorBlockEntity>> VILLAGER_INCUBATOR_BLOCK_ENTITY =
            Registration.BLOCK_ENTITY_TYPES.register(VILLAGER_INCUBATOR_ID, () ->
                    new BlockEntityType<>(VillagerIncubatorBlockEntity::new, VILLAGER_INCUBATOR_BLOCK.get())
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PiglinIncubatorBlockEntity>> PIGLIN_INCUBATOR_BLOCK_ENTITY =
            Registration.BLOCK_ENTITY_TYPES.register(PIGLIN_INCUBATOR_ID, () ->
                    new BlockEntityType<>(PiglinIncubatorBlockEntity::new, PIGLIN_INCUBATOR_BLOCK.get())
            );

    public static final DeferredItem<BlockItem> VILLAGER_INCUBATOR_ITEM =
            Registration.ITEMS.register(VILLAGER_INCUBATOR_ID, () -> new BlockItem(
                    VILLAGER_INCUBATOR_BLOCK.get(),
                    itemProperties(VILLAGER_INCUBATOR_ID)
            ));

    public static final DeferredItem<BlockItem> PIGLIN_INCUBATOR_ITEM =
            Registration.ITEMS.register(PIGLIN_INCUBATOR_ID, () -> new BlockItem(
                    PIGLIN_INCUBATOR_BLOCK.get(),
                    itemProperties(PIGLIN_INCUBATOR_ID)
            ));

    public static final DeferredHolder<MenuType<?>, MenuType<IncubatorMenu>> VILLAGER_INCUBATOR_MENU =
            Registration.MENU_TYPES.register(VILLAGER_INCUBATOR_ID, () ->
                    new MenuType<>(IncubatorMenu::villager, FeatureFlags.VANILLA_SET)
            );

    public static final DeferredHolder<MenuType<?>, MenuType<IncubatorMenu>> PIGLIN_INCUBATOR_MENU =
            Registration.MENU_TYPES.register(PIGLIN_INCUBATOR_ID, () ->
                    new MenuType<>(IncubatorMenu::piglin, FeatureFlags.VANILLA_SET)
            );

    private IncubatorRegistrationAdapter() {
    }

    public static void load() {
        // Forces class loading so all DeferredRegister entries are created.
    }

    private static Item.Properties itemProperties(String id) {
        return new Item.Properties().setId(ResourceKey.create(
                Registries.ITEM,
                Identifier.fromNamespaceAndPath(TradingCells.MOD_ID, id)
        ));
    }
}
