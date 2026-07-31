package com.cosmocraft.trading_cells.feature.trader.adapters.output;

import com.cosmocraft.trading_cells.feature.trader.adapters.input.NetheritePiglinBarteringCellBlock;
import com.cosmocraft.trading_cells.feature.trader.adapters.input.NetheritePiglinBarteringCellBlockEntity;
import com.cosmocraft.trading_cells.feature.trader.adapters.input.NetheritePiglinBarteringCellMenu;
import com.cosmocraft.trading_cells.feature.trader.adapters.input.PiglinBarterUpgradeItem;
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
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

@SuppressWarnings("java:S1118")
public class TraderRegistrationAdapter {
    private static final String VILLAGER_TRADER_ID = "villager_trading_cell";
    private static final String PIGLIN_BARTERING_CELL_ID = "piglin_bartering_cell";
    public static final String NETHERITE_PIGLIN_BARTERING_CELL_ID = "netherite_piglin_bartering_cell";
    public static final String PIGLIN_BARTER_COPPER_UPGRADE_ID = "piglin_barter_copper_upgrade";
    public static final String PIGLIN_BARTER_IRON_UPGRADE_ID = "piglin_barter_iron_upgrade";
    public static final String PIGLIN_BARTER_GOLD_UPGRADE_ID = "piglin_barter_gold_upgrade";
    public static final String PIGLIN_BARTER_DIAMOND_UPGRADE_ID = "piglin_barter_diamond_upgrade";
    public static final String PIGLIN_BARTER_NETHERITE_UPGRADE_ID = "piglin_barter_netherite_upgrade";

    // Legacy IDs retained only so existing worlds can migrate old stored upgrades safely.
    public static final String PIGLIN_BARTER_QUALITY_UPGRADE_ID = "piglin_barter_quality_upgrade";
    public static final String PIGLIN_BARTER_YIELD_UPGRADE_ID = "piglin_barter_yield_upgrade";
    public static final String PIGLIN_BARTER_HYBRID_UPGRADE_ID = "piglin_barter_hybrid_upgrade";

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

    public static final DeferredBlock<NetheritePiglinBarteringCellBlock> NETHERITE_PIGLIN_BARTERING_CELL_BLOCK =
            Registration.BLOCKS.register(NETHERITE_PIGLIN_BARTERING_CELL_ID, () ->
                    new NetheritePiglinBarteringCellBlock(
                            MachineBlockProperties.netherite(NETHERITE_PIGLIN_BARTERING_CELL_ID)
                    )
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<NetheritePiglinBarteringCellBlockEntity>>
            NETHERITE_PIGLIN_BARTERING_CELL_BLOCK_ENTITY = Registration.BLOCK_ENTITY_TYPES.register(
                    NETHERITE_PIGLIN_BARTERING_CELL_ID,
                    () -> new BlockEntityType<>(
                            NetheritePiglinBarteringCellBlockEntity::new,
                            NETHERITE_PIGLIN_BARTERING_CELL_BLOCK.get()
                    )
            );

    public static final DeferredHolder<MenuType<?>, MenuType<NetheritePiglinBarteringCellMenu>>
            NETHERITE_PIGLIN_BARTERING_CELL_MENU = Registration.MENU_TYPES.register(
                    NETHERITE_PIGLIN_BARTERING_CELL_ID,
                    () -> new MenuType<>(NetheritePiglinBarteringCellMenu::new, FeatureFlags.VANILLA_SET)
            );

    public static final DeferredItem<VillagerTradingCellBlockItem> VILLAGER_TRADER_ITEM =
            Registration.ITEMS.register(VILLAGER_TRADER_ID, () -> new VillagerTradingCellBlockItem(
                    VILLAGER_TRADER_BLOCK.get(),
                    itemProperties(VILLAGER_TRADER_ID)
            ));

    public static final DeferredItem<PiglinBarteringCellBlockItem> PIGLIN_BARTERING_CELL_ITEM =
            Registration.ITEMS.register(PIGLIN_BARTERING_CELL_ID, () -> new PiglinBarteringCellBlockItem(
                    PIGLIN_BARTERING_CELL_BLOCK.get(),
                    itemProperties(PIGLIN_BARTERING_CELL_ID)
            ));

    public static final DeferredItem<PiglinBarteringCellBlockItem> NETHERITE_PIGLIN_BARTERING_CELL_ITEM =
            Registration.ITEMS.register(
                    NETHERITE_PIGLIN_BARTERING_CELL_ID,
                    () -> new PiglinBarteringCellBlockItem(
                            NETHERITE_PIGLIN_BARTERING_CELL_BLOCK.get(),
                            itemProperties(NETHERITE_PIGLIN_BARTERING_CELL_ID)
                    )
            );

    public static final DeferredItem<PiglinBarterUpgradeItem> PIGLIN_BARTER_COPPER_UPGRADE_ITEM =
            registerUpgrade(PIGLIN_BARTER_COPPER_UPGRADE_ID, PiglinBarterUpgradeItem.Tier.COPPER_BASE);
    public static final DeferredItem<PiglinBarterUpgradeItem> PIGLIN_BARTER_IRON_UPGRADE_ITEM =
            registerUpgrade(PIGLIN_BARTER_IRON_UPGRADE_ID, PiglinBarterUpgradeItem.Tier.IRON);
    public static final DeferredItem<PiglinBarterUpgradeItem> PIGLIN_BARTER_GOLD_UPGRADE_ITEM =
            registerUpgrade(PIGLIN_BARTER_GOLD_UPGRADE_ID, PiglinBarterUpgradeItem.Tier.GOLD);
    public static final DeferredItem<PiglinBarterUpgradeItem> PIGLIN_BARTER_DIAMOND_UPGRADE_ITEM =
            registerUpgrade(PIGLIN_BARTER_DIAMOND_UPGRADE_ID, PiglinBarterUpgradeItem.Tier.DIAMOND);
    public static final DeferredItem<PiglinBarterUpgradeItem> PIGLIN_BARTER_NETHERITE_UPGRADE_ITEM =
            registerUpgrade(PIGLIN_BARTER_NETHERITE_UPGRADE_ID, PiglinBarterUpgradeItem.Tier.NETHERITE);

    public static final DeferredItem<PiglinBarterUpgradeItem> PIGLIN_BARTER_QUALITY_UPGRADE_ITEM =
            registerUpgrade(PIGLIN_BARTER_QUALITY_UPGRADE_ID, PiglinBarterUpgradeItem.Tier.LEGACY_QUALITY);
    public static final DeferredItem<PiglinBarterUpgradeItem> PIGLIN_BARTER_YIELD_UPGRADE_ITEM =
            registerUpgrade(PIGLIN_BARTER_YIELD_UPGRADE_ID, PiglinBarterUpgradeItem.Tier.LEGACY_YIELD);
    public static final DeferredItem<PiglinBarterUpgradeItem> PIGLIN_BARTER_HYBRID_UPGRADE_ITEM =
            registerUpgrade(PIGLIN_BARTER_HYBRID_UPGRADE_ID, PiglinBarterUpgradeItem.Tier.LEGACY_HYBRID);

    private static DeferredItem<PiglinBarterUpgradeItem> registerUpgrade(
            String id,
            PiglinBarterUpgradeItem.Tier tier
    ) {
        return Registration.ITEMS.register(
                id,
                () -> new PiglinBarterUpgradeItem(tier, itemProperties(id).stacksTo(64))
        );
    }

    private static Item.Properties itemProperties(String id) {
        return new Item.Properties().setId(ResourceKey.create(
                Registries.ITEM,
                Identifier.fromNamespaceAndPath(TradingCells.MOD_ID, id)
        ));
    }

    public static void load() {
        // Forces class loading so all DeferredRegister entries are created.
    }
}
