package com.cosmocraft.trading_cells.feature.trader.adapters.output;

import com.cosmocraft.trading_cells.feature.trader.adapters.input.PiglinBarteringCellBlock;
import com.cosmocraft.trading_cells.feature.trader.adapters.input.PiglinBarteringCellBlockEntity;
import com.cosmocraft.trading_cells.feature.trader.adapters.input.PiglinBarteringCellBlockItem;
import com.cosmocraft.trading_cells.feature.trader.adapters.input.VillagerTradingCellBlock;
import com.cosmocraft.trading_cells.feature.trader.adapters.input.VillagerTradingCellBlockEntity;
import com.cosmocraft.trading_cells.feature.trader.adapters.input.VillagerTradingCellBlockItem;
import com.cosmocraft.trading_cells.feature.trader.adapters.input.VillagerTradingCellMenu;
import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.TradingCells;
import com.cosmocraft.trading_cells.platform.neoforge.machine.MachineBlockProperties;
import com.cosmocraft.trading_cells.platform.neoforge.registration.Registration;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

@SuppressWarnings("java:S1118")
public class TraderRegistrationAdapter {
    private static final String VILLAGER_TRADER_ID = "villager_trading_cell";
    private static final String PIGLIN_BARTERING_CELL_ID = "piglin_bartering_cell";

    public static final DeferredBlock<VillagerTradingCellBlock> VILLAGER_TRADER_BLOCK =
            Registration.BLOCKS.register(VILLAGER_TRADER_ID, () ->
                    new VillagerTradingCellBlock(MachineBlockProperties.villager(VILLAGER_TRADER_ID))
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<VillagerTradingCellBlockEntity>> VILLAGER_TRADING_CELL_BLOCK_ENTITY =
            Registration.BLOCK_ENTITY_TYPES.register(VILLAGER_TRADER_ID, () ->
                    new BlockEntityType<>(VillagerTradingCellBlockEntity::new, VILLAGER_TRADER_BLOCK.get())
            );

    public static final DeferredHolder<MenuType<?>, MenuType<VillagerTradingCellMenu>> VILLAGER_TRADING_CELL_MENU =
            Registration.MENU_TYPES.register(VILLAGER_TRADER_ID, () ->
                    new MenuType<>(VillagerTradingCellMenu::new, FeatureFlags.VANILLA_SET)
            );

    public static final DeferredBlock<PiglinBarteringCellBlock> PIGLIN_BARTERING_CELL_BLOCK =
            Registration.BLOCKS.register(PIGLIN_BARTERING_CELL_ID, () ->
                    new PiglinBarteringCellBlock(MachineBlockProperties.piglin(PIGLIN_BARTERING_CELL_ID))
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PiglinBarteringCellBlockEntity>> PIGLIN_BARTERING_CELL_BLOCK_ENTITY =
            Registration.BLOCK_ENTITY_TYPES.register(PIGLIN_BARTERING_CELL_ID, () ->
                    new BlockEntityType<>(PiglinBarteringCellBlockEntity::new, PIGLIN_BARTERING_CELL_BLOCK.get())
            );

    public static final DeferredItem<VillagerTradingCellBlockItem> VILLAGER_TRADER_ITEM =
            Registration.ITEMS.register(VILLAGER_TRADER_ID, () -> new VillagerTradingCellBlockItem(
                    VILLAGER_TRADER_BLOCK.get(),
                    new Item.Properties()
                            .setId(ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(TradingCells.MOD_ID, VILLAGER_TRADER_ID)))
            ));

    public static final DeferredItem<PiglinBarteringCellBlockItem> PIGLIN_BARTERING_CELL_ITEM =
            Registration.ITEMS.register(PIGLIN_BARTERING_CELL_ID, () -> new PiglinBarteringCellBlockItem(
                    PIGLIN_BARTERING_CELL_BLOCK.get(),
                    new Item.Properties()
                            .setId(ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(TradingCells.MOD_ID, PIGLIN_BARTERING_CELL_ID)))
            ));

    public static void load() {
        // Forces class loading so all DeferredRegister entries are created.
    }
}
