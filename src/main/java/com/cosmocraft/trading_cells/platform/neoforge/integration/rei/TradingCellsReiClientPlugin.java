package com.cosmocraft.trading_cells.platform.neoforge.integration.rei;

import com.cosmocraft.trading_cells.feature.breeders.adapters.output.BreederRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.breeders.adapters.output.client.BreederScreen;
import com.cosmocraft.trading_cells.feature.breeders.domain.model.BreederKind;
import com.cosmocraft.trading_cells.feature.captures.domain.model.CapturedMobKind;
import com.cosmocraft.trading_cells.feature.converter.adapters.output.ConverterRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.converter.adapters.output.client.ConverterScreen;
import com.cosmocraft.trading_cells.feature.farmer.adapters.output.FarmerRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.farmer.adapters.output.client.FarmerScreen;
import com.cosmocraft.trading_cells.feature.farmer.domain.model.FarmerKind;
import com.cosmocraft.trading_cells.feature.incubators.adapters.output.IncubatorRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.incubators.adapters.output.client.IncubatorScreen;
import com.cosmocraft.trading_cells.feature.infusion.adapters.output.ArcaneInfuserRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.infusion.adapters.output.client.ArcaneInfuserScreen;
import com.cosmocraft.trading_cells.feature.ironfarm.adapters.output.IronFarmRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.ironfarm.adapters.output.client.IronFarmScreen;
import com.cosmocraft.trading_cells.feature.quarry.adapters.output.QuarryRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.quarry.adapters.output.client.QuarryScreen;
import com.cosmocraft.trading_cells.feature.quarry.domain.model.QuarryKind;
import com.cosmocraft.trading_cells.feature.trader.adapters.output.TraderRegistrationAdapter;
import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.TradingCells;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.MachineScreenLayout;
import java.util.List;
import java.util.function.Function;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.client.registry.entry.EntryRegistry;
import me.shedaniel.rei.api.client.registry.screen.ClickArea;
import me.shedaniel.rei.api.client.registry.screen.ScreenRegistry;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.util.EntryStacks;
import me.shedaniel.rei.forge.REIPluginClient;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;

@REIPluginClient
public final class TradingCellsReiClientPlugin implements REIClientPlugin {
    public static final CategoryIdentifier<TradingCellsReiDisplay> VILLAGER_BREEDING =
            category("villager_breeding");
    public static final CategoryIdentifier<TradingCellsReiDisplay> PIGLIN_BREEDING =
            category("piglin_breeding");
    public static final CategoryIdentifier<TradingCellsReiDisplay> VILLAGER_INCUBATION =
            category("villager_incubation");
    public static final CategoryIdentifier<TradingCellsReiDisplay> PIGLIN_INCUBATION =
            category("piglin_incubation");
    public static final CategoryIdentifier<TradingCellsReiDisplay> FARMING =
            category("farming");
    public static final CategoryIdentifier<TradingCellsReiDisplay> PIGLIN_FARMING =
            category("piglin_farming");
    public static final CategoryIdentifier<TradingCellsReiDisplay> CONVERSION =
            category("conversion");
    public static final CategoryIdentifier<TradingCellsReiDisplay> IRON_FARM =
            category("iron_farm");
    public static final CategoryIdentifier<TradingCellsReiDisplay> PIGLIN_BARTERING =
            category("piglin_bartering");
    public static final CategoryIdentifier<TradingCellsReiDisplay> NETHERITE_PIGLIN_BARTERING =
            category("netherite_piglin_bartering");
    public static final CategoryIdentifier<TradingCellsReiDisplay> QUARRY =
            category("quarry");
    public static final CategoryIdentifier<TradingCellsReiDisplay> PIGLIN_QUARRY =
            category("piglin_quarry");
    public static final CategoryIdentifier<ArcaneInfusionReiDisplay> ARCANE_INFUSION =
            ArcaneInfusionReiDisplay.CATEGORY;

    @Override
    public void registerCategories(CategoryRegistry registry) {
        registry.add(List.of(
                new TradingCellsReiCategory(
                        VILLAGER_BREEDING,
                        "category.trading_cells.villager_breeding",
                        BreederRegistrationAdapter.VILLAGER_BREEDER_ITEM.get()
                ),
                new TradingCellsReiCategory(
                        PIGLIN_BREEDING,
                        "category.trading_cells.piglin_breeding",
                        BreederRegistrationAdapter.PIGLIN_BREEDER_ITEM.get()
                ),
                new TradingCellsReiCategory(
                        VILLAGER_INCUBATION,
                        "category.trading_cells.villager_incubation",
                        IncubatorRegistrationAdapter.VILLAGER_INCUBATOR_ITEM.get()
                ),
                new TradingCellsReiCategory(
                        PIGLIN_INCUBATION,
                        "category.trading_cells.piglin_incubation",
                        IncubatorRegistrationAdapter.PIGLIN_INCUBATOR_ITEM.get()
                ),
                new TradingCellsReiCategory(
                        FARMING,
                        "category.trading_cells.farming",
                        FarmerRegistrationAdapter.FARMER_ITEM.get()
                ),
                new TradingCellsReiCategory(
                        PIGLIN_FARMING,
                        "category.trading_cells.piglin_farming",
                        FarmerRegistrationAdapter.PIGLIN_FARMER_ITEM.get()
                ),
                new TradingCellsReiCategory(
                        CONVERSION,
                        "category.trading_cells.conversion",
                        ConverterRegistrationAdapter.CONVERTER_ITEM.get()
                ),
                new TradingCellsReiCategory(
                        IRON_FARM,
                        "category.trading_cells.iron_farm",
                        IronFarmRegistrationAdapter.IRON_FARM_ITEM.get()
                ),
                new TradingCellsReiCategory(
                        PIGLIN_BARTERING,
                        "category.trading_cells.piglin_bartering",
                        TraderRegistrationAdapter.PIGLIN_BARTERING_CELL_ITEM.get()
                ),
                new TradingCellsReiCategory(
                        NETHERITE_PIGLIN_BARTERING,
                        "category.trading_cells.netherite_piglin_bartering",
                        TraderRegistrationAdapter.NETHERITE_PIGLIN_BARTERING_CELL_ITEM.get()
                ),
                new TradingCellsReiCategory(
                        QUARRY,
                        "category.trading_cells.quarry",
                        QuarryRegistrationAdapter.QUARRY_ITEM.get()
                ),
                new TradingCellsReiCategory(
                        PIGLIN_QUARRY,
                        "category.trading_cells.piglin_quarry",
                        QuarryRegistrationAdapter.PIGLIN_QUARRY_ITEM.get()
                ),
                new ArcaneInfusionReiCategory()
        ));

        registry.addWorkstations(
                VILLAGER_BREEDING,
                EntryStacks.of(BreederRegistrationAdapter.VILLAGER_BREEDER_ITEM.get())
        );
        registry.addWorkstations(
                PIGLIN_BREEDING,
                EntryStacks.of(BreederRegistrationAdapter.PIGLIN_BREEDER_ITEM.get())
        );
        registry.addWorkstations(
                VILLAGER_INCUBATION,
                EntryStacks.of(IncubatorRegistrationAdapter.VILLAGER_INCUBATOR_ITEM.get())
        );
        registry.addWorkstations(
                PIGLIN_INCUBATION,
                EntryStacks.of(IncubatorRegistrationAdapter.PIGLIN_INCUBATOR_ITEM.get())
        );
        registry.addWorkstations(FARMING, EntryStacks.of(FarmerRegistrationAdapter.FARMER_ITEM.get()));
        registry.addWorkstations(
                PIGLIN_FARMING,
                EntryStacks.of(FarmerRegistrationAdapter.PIGLIN_FARMER_ITEM.get())
        );
        registry.addWorkstations(CONVERSION, EntryStacks.of(ConverterRegistrationAdapter.CONVERTER_ITEM.get()));
        registry.addWorkstations(IRON_FARM, EntryStacks.of(IronFarmRegistrationAdapter.IRON_FARM_ITEM.get()));
        registry.addWorkstations(
                PIGLIN_BARTERING,
                EntryStacks.of(TraderRegistrationAdapter.PIGLIN_BARTERING_CELL_ITEM.get())
        );
        registry.addWorkstations(
                NETHERITE_PIGLIN_BARTERING,
                EntryStacks.of(TraderRegistrationAdapter.NETHERITE_PIGLIN_BARTERING_CELL_ITEM.get())
        );
        registry.addWorkstations(QUARRY, EntryStacks.of(QuarryRegistrationAdapter.QUARRY_ITEM.get()));
        registry.addWorkstations(
                PIGLIN_QUARRY,
                EntryStacks.of(QuarryRegistrationAdapter.PIGLIN_QUARRY_ITEM.get())
        );
        registry.addWorkstations(
                ARCANE_INFUSION,
                EntryStacks.of(ArcaneInfuserRegistrationAdapter.ITEM.get())
        );

        registry.configure(
                VILLAGER_BREEDING,
                configuration -> configuration.setQuickCraftingEnabledByDefault(false)
        );
        registry.configure(
                PIGLIN_BREEDING,
                configuration -> configuration.setQuickCraftingEnabledByDefault(false)
        );
        registry.configure(
                VILLAGER_INCUBATION,
                configuration -> configuration.setQuickCraftingEnabledByDefault(false)
        );
        registry.configure(
                PIGLIN_INCUBATION,
                configuration -> configuration.setQuickCraftingEnabledByDefault(false)
        );
        registry.configure(FARMING, configuration -> configuration.setQuickCraftingEnabledByDefault(false));
        registry.configure(PIGLIN_FARMING, configuration -> configuration.setQuickCraftingEnabledByDefault(false));
        registry.configure(CONVERSION, configuration -> configuration.setQuickCraftingEnabledByDefault(false));
        registry.configure(IRON_FARM, configuration -> configuration.setQuickCraftingEnabledByDefault(false));
        registry.configure(PIGLIN_BARTERING, configuration -> configuration.setQuickCraftingEnabledByDefault(false));
        registry.configure(
                NETHERITE_PIGLIN_BARTERING,
                configuration -> configuration.setQuickCraftingEnabledByDefault(false)
        );
        registry.configure(QUARRY, configuration -> configuration.setQuickCraftingEnabledByDefault(false));
        registry.configure(PIGLIN_QUARRY, configuration -> configuration.setQuickCraftingEnabledByDefault(false));
        registry.configure(ARCANE_INFUSION, configuration -> configuration.setQuickCraftingEnabledByDefault(false));
    }

    @Override
    public void registerDisplays(DisplayRegistry registry) {
        for (TradingCellsReiDisplay display : TradingCellsReiDisplays.createAll()) {
            registry.add(display);
        }
    }

    @Override
    public void registerScreens(ScreenRegistry registry) {
        Rectangle progressArea = new Rectangle(
                MachineScreenLayout.machineX(54),
                0,
                MachineScreenLayout.PROGRESS_FRAME_WIDTH,
                MachineScreenLayout.PROGRESS_FRAME_HEIGHT
        );
        registerMachineClickArea(
                registry,
                new Rectangle(progressArea.x, 65, progressArea.width, progressArea.height),
                BreederScreen.class,
                screen -> screen.getMenu().kind() == BreederKind.VILLAGER
                        ? VILLAGER_BREEDING
                        : PIGLIN_BREEDING
        );
        registerMachineClickArea(
                registry,
                new Rectangle(progressArea.x, 77, progressArea.width, progressArea.height),
                IncubatorScreen.class,
                screen -> screen.getMenu().kind() == CapturedMobKind.VILLAGER
                        ? VILLAGER_INCUBATION
                        : PIGLIN_INCUBATION
        );
        registerMachineClickArea(
                registry,
                new Rectangle(progressArea.x, 66, progressArea.width, progressArea.height),
                FarmerScreen.class,
                screen -> screen.getMenu().kind() == FarmerKind.VILLAGER
                        ? FARMING
                        : PIGLIN_FARMING
        );
        registry.registerContainerClickArea(
                new Rectangle(progressArea.x, 95, progressArea.width, progressArea.height),
                ConverterScreen.class,
                CONVERSION
        );
        registry.registerContainerClickArea(
                new Rectangle(progressArea.x, 47, progressArea.width, progressArea.height),
                IronFarmScreen.class,
                IRON_FARM
        );
        registerMachineClickArea(
                registry,
                new Rectangle(
                        (MachineScreenLayout.WIDTH - MachineScreenLayout.PROGRESS_FRAME_WIDTH) / 2,
                        64,
                        progressArea.width,
                        progressArea.height
                ),
                QuarryScreen.class,
                screen -> screen.getMenu().kind() == QuarryKind.VILLAGER
                        ? QUARRY
                        : PIGLIN_QUARRY
        );
        registry.registerContainerClickArea(
                new Rectangle(
                        ArcaneInfuserScreen.RECIPE_VIEWER_X,
                        ArcaneInfuserScreen.RECIPE_VIEWER_Y,
                        ArcaneInfuserScreen.RECIPE_VIEWER_WIDTH,
                        ArcaneInfuserScreen.RECIPE_VIEWER_HEIGHT
                ),
                ArcaneInfuserScreen.class,
                ARCANE_INFUSION
        );
    }

    @Override
    public void registerEntries(EntryRegistry registry) {
        registry.removeEntry(EntryStacks.of(TraderRegistrationAdapter.PIGLIN_BARTER_QUALITY_UPGRADE_ITEM.get()));
        registry.removeEntry(EntryStacks.of(TraderRegistrationAdapter.PIGLIN_BARTER_YIELD_UPGRADE_ITEM.get()));
        registry.removeEntry(EntryStacks.of(TraderRegistrationAdapter.PIGLIN_BARTER_HYBRID_UPGRADE_ITEM.get()));
    }

    private static CategoryIdentifier<TradingCellsReiDisplay> category(String path) {
        return CategoryIdentifier.of(TradingCells.MOD_ID, path);
    }

    private static <M extends AbstractContainerMenu, S extends AbstractContainerScreen<M>>
    void registerMachineClickArea(
            ScreenRegistry registry,
            Rectangle area,
            Class<S> screenClass,
            Function<S, CategoryIdentifier<?>> categoryProvider
    ) {
        registry.registerClickArea(screenClass, context -> {
            S screen = context.getScreen();
            Rectangle absoluteArea = area.clone();
            absoluteArea.translate(screen.getLeftPos(), screen.getTopPos());
            if (!absoluteArea.contains(context.getMousePosition())) {
                return ClickArea.Result.fail();
            }
            return ClickArea.Result.success().category(categoryProvider.apply(screen));
        });
    }
}
