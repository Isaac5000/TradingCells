package com.cosmocraft.trading_cells.platform.neoforge.registration;

import com.cosmocraft.trading_cells.feature.trader.adapters.output.AutotraderRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.breeders.adapters.output.BreederRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.captures.adapters.output.CaptureRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.converter.adapters.output.ConverterRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.farmer.adapters.input.FarmerEnchantments;
import com.cosmocraft.trading_cells.feature.farmer.adapters.output.FarmerRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.incubators.adapters.output.IncubatorRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.infusion.adapters.output.ArcaneInfuserRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.experience.adapters.output.ExperienceStorageRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.ironfarm.adapters.output.IronFarmRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.quarry.adapters.input.QuarryEnchantments;
import com.cosmocraft.trading_cells.feature.quarry.adapters.output.QuarryRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.trader.adapters.output.TraderRegistrationAdapter;
import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.TradingCells;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class CreativeTabRegistration {
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN_TAB =
            Registration.CREATIVE_MODE_TABS.register("villager_trader_cage_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + TradingCells.MOD_ID))
                    .icon(() -> TraderRegistrationAdapter.VILLAGER_TRADER_ITEM.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(TraderRegistrationAdapter.VILLAGER_TRADER_ITEM.get());
                        output.accept(BreederRegistrationAdapter.VILLAGER_BREEDER_ITEM.get());
                        output.accept(IncubatorRegistrationAdapter.VILLAGER_INCUBATOR_ITEM.get());
                        output.accept(FarmerRegistrationAdapter.FARMER_ITEM.get());
                        output.accept(QuarryRegistrationAdapter.QUARRY_ITEM.get());
                        output.accept(AutotraderRegistrationAdapter.AUTOTRADER_ITEM.get());
                        output.accept(IronFarmRegistrationAdapter.IRON_FARM_ITEM.get());
                        output.accept(ConverterRegistrationAdapter.CONVERTER_ITEM.get());
                    })
                    .build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> PIGLIN_MACHINES_TAB =
            Registration.CREATIVE_MODE_TABS.register("piglin_machines_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + TradingCells.MOD_ID + ".piglin_machines"))
                    .icon(() -> TraderRegistrationAdapter.NETHERITE_PIGLIN_BARTERING_CELL_ITEM.get()
                            .getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(TraderRegistrationAdapter.PIGLIN_BARTERING_CELL_ITEM.get());
                        output.accept(TraderRegistrationAdapter.NETHERITE_PIGLIN_BARTERING_CELL_ITEM.get());
                        output.accept(BreederRegistrationAdapter.PIGLIN_BREEDER_ITEM.get());
                        output.accept(IncubatorRegistrationAdapter.PIGLIN_INCUBATOR_ITEM.get());
                        output.accept(FarmerRegistrationAdapter.PIGLIN_FARMER_ITEM.get());
                        output.accept(QuarryRegistrationAdapter.PIGLIN_QUARRY_ITEM.get());
                    })
                    .build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ITEMS_TAB =
            Registration.CREATIVE_MODE_TABS.register("trading_cells_items", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + TradingCells.MOD_ID + ".items"))
                    .icon(() -> CaptureRegistrationAdapter.VILLAGER_CAPTURER_ITEM.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(ExperienceStorageRegistrationAdapter.ITEM.get());
                        output.accept(ArcaneInfuserRegistrationAdapter.ITEM.get());
                        output.accept(TraderRegistrationAdapter.PIGLIN_BARTER_COPPER_UPGRADE_ITEM.get());
                        output.accept(TraderRegistrationAdapter.PIGLIN_BARTER_IRON_UPGRADE_ITEM.get());
                        output.accept(TraderRegistrationAdapter.PIGLIN_BARTER_GOLD_UPGRADE_ITEM.get());
                        output.accept(TraderRegistrationAdapter.PIGLIN_BARTER_DIAMOND_UPGRADE_ITEM.get());
                        output.accept(TraderRegistrationAdapter.PIGLIN_BARTER_NETHERITE_UPGRADE_ITEM.get());
                        output.accept(QuarryRegistrationAdapter.QUARRY_COPPER_UPGRADE_ITEM.get());
                        output.accept(QuarryRegistrationAdapter.QUARRY_IRON_UPGRADE_ITEM.get());
                        output.accept(QuarryRegistrationAdapter.QUARRY_GOLD_UPGRADE_ITEM.get());
                        output.accept(QuarryRegistrationAdapter.QUARRY_DIAMOND_UPGRADE_ITEM.get());
                        output.accept(QuarryRegistrationAdapter.QUARRY_NETHERITE_UPGRADE_ITEM.get());
                        parameters.holders().lookup(Registries.ENCHANTMENT)
                                .flatMap(enchantments -> enchantments.get(FarmerEnchantments.FARMERS_TOUCH))
                                .ifPresent(enchantment -> output.accept(EnchantmentHelper.createBook(
                                        new EnchantmentInstance(enchantment, 1)
                                )));
                        parameters.holders().lookup(Registries.ENCHANTMENT)
                                .flatMap(enchantments -> enchantments.get(QuarryEnchantments.MINERS_TOUCH))
                                .ifPresent(enchantment -> output.accept(EnchantmentHelper.createBook(
                                        new EnchantmentInstance(enchantment, 1)
                                )));
                        output.accept(CaptureRegistrationAdapter.VILLAGER_CAPTURER_ITEM.get());
                        output.accept(CaptureRegistrationAdapter.PIGLIN_CAPTURER_ITEM.get());
                        output.accept(CaptureRegistrationAdapter.UNBREAKABLE_VILLAGER_CAPTURER_ITEM.get());
                        output.accept(CaptureRegistrationAdapter.UNBREAKABLE_PIGLIN_CAPTURER_ITEM.get());
                    })
                    .build());

    private CreativeTabRegistration() {
    }

    public static void load() {
        // Forces class loading so the DeferredRegister entries are created.
    }
}
