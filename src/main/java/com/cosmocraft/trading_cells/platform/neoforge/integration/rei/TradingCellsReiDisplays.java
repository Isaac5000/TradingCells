package com.cosmocraft.trading_cells.platform.neoforge.integration.rei;

import com.cosmocraft.trading_cells.feature.breeders.adapters.input.MinecraftBreederFood;
import com.cosmocraft.trading_cells.feature.breeders.application.port.input.BreederUseCase;
import com.cosmocraft.trading_cells.feature.breeders.domain.model.BreederKind;
import com.cosmocraft.trading_cells.feature.captures.adapters.api.CapturedMobStackAdapter;
import com.cosmocraft.trading_cells.feature.captures.adapters.output.CaptureRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.captures.domain.model.CapturedMobKind;
import com.cosmocraft.trading_cells.feature.converter.application.port.input.ConverterUseCase;
import com.cosmocraft.trading_cells.feature.converter.domain.model.ConverterStage;
import com.cosmocraft.trading_cells.feature.farmer.adapters.input.FarmerCropStackAdapter;
import com.cosmocraft.trading_cells.feature.farmer.application.port.input.FarmerUseCase;
import com.cosmocraft.trading_cells.feature.farmer.domain.model.FarmerCrop;
import com.cosmocraft.trading_cells.feature.farmer.domain.model.FarmerHarvest;
import com.cosmocraft.trading_cells.feature.ironfarm.application.port.input.IronFarmUseCase;
import com.cosmocraft.trading_cells.feature.ironfarm.domain.model.IronFarmCycle;
import com.cosmocraft.trading_cells.feature.trader.adapters.minecraft.EnhancedPiglinBarterRewards;
import com.cosmocraft.trading_cells.feature.trader.adapters.minecraft.PiglinBarterCatalog;
import com.cosmocraft.trading_cells.feature.trader.adapters.output.TraderRegistrationAdapter;
import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.FeatureComposition;
import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.TradingCells;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.basic.BasicDisplay;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.ItemLike;

/** Builds client-side displays from the same domain rules used by the machines. */
public final class TradingCellsReiDisplays {
    private TradingCellsReiDisplays() {
    }

    public static List<TradingCellsReiDisplay> createAll() {
        List<TradingCellsReiDisplay> displays = new ArrayList<>();
        addBreeding(displays);
        addIncubation(displays);
        addFarming(displays);
        addConversion(displays);
        addIronFarm(displays);
        addPiglinBartering(displays);
        return List.copyOf(displays);
    }

    private static void addBreeding(List<TradingCellsReiDisplay> displays) {
        BreederUseCase breeder = FeatureComposition.breeder();
        for (BreederKind kind : BreederKind.values()) {
            CapturedMobKind capturedKind = kind == BreederKind.VILLAGER
                    ? CapturedMobKind.VILLAGER
                    : CapturedMobKind.PIGLIN;
            Item capturer = capturer(capturedKind);
            EntryIngredient adult = captured(capturedKind, false, true);
            EntryIngredient emptyCapturer = described(
                    capturer,
                    1,
                    tooltip("rei.trading_cells.empty_capturer")
            );
            EntryIngredient baby = captured(capturedKind, true, false);

            for (MinecraftBreederFood.Option option : MinecraftBreederFood.options(kind)) {
                int foodCost = breeder.foodCost(kind, option.food());
                EntryIngredient food = described(option.item(), foodCost);
                List<EntryIngredient> inputs = List.of(adult, adult, food, emptyCapturer);
                displays.add(display(
                        kind == BreederKind.VILLAGER
                                ? TradingCellsReiClientPlugin.VILLAGER_BREEDING
                                : TradingCellsReiClientPlugin.PIGLIN_BREEDING,
                        kind == BreederKind.VILLAGER
                                ? TradingCellsReiLayout.VILLAGER_BREEDING
                                : TradingCellsReiLayout.PIGLIN_BREEDING,
                        "breeding/" + kind.name().toLowerCase() + "/"
                                + BuiltInRegistries.ITEM.getKey(option.item()).getPath(),
                        inputs,
                        inputs,
                        List.of(baby),
                        breeder.durationTicks(kind),
                        List.of(Component.translatable("rei.trading_cells.breeding_note"))
                ));
            }
        }
    }

    private static void addIncubation(List<TradingCellsReiDisplay> displays) {
        var incubator = FeatureComposition.incubator();
        for (CapturedMobKind kind : CapturedMobKind.values()) {
            List<EntryIngredient> inputs = List.of(captured(kind, true, false));
            displays.add(display(
                    kind == CapturedMobKind.VILLAGER
                            ? TradingCellsReiClientPlugin.VILLAGER_INCUBATION
                            : TradingCellsReiClientPlugin.PIGLIN_INCUBATION,
                    kind == CapturedMobKind.VILLAGER
                            ? TradingCellsReiLayout.VILLAGER_INCUBATION
                            : TradingCellsReiLayout.PIGLIN_INCUBATION,
                    "incubation/" + kind.name().toLowerCase(),
                    inputs,
                    inputs,
                    List.of(captured(kind, false, false)),
                    incubator.durationTicks(kind),
                    List.of(Component.translatable("rei.trading_cells.incubation_note"))
            ));
        }
    }

    private static void addFarming(List<TradingCellsReiDisplay> displays) {
        FarmerUseCase farmer = FeatureComposition.farmer();
        EntryIngredient adultVillager = captured(CapturedMobKind.VILLAGER, false, true);
        EntryIngredient optionalHoe = described(
                List.of(
                        new ItemStack(Items.WOODEN_HOE),
                        new ItemStack(Items.STONE_HOE),
                        new ItemStack(Items.IRON_HOE),
                        new ItemStack(Items.GOLDEN_HOE),
                        new ItemStack(Items.DIAMOND_HOE),
                        new ItemStack(Items.NETHERITE_HOE)
                ),
                tooltip("rei.trading_cells.optional_hoe")
        );
        for (FarmerCrop crop : List.of(
                FarmerCrop.WHEAT,
                FarmerCrop.CARROT,
                FarmerCrop.POTATO,
                FarmerCrop.BEETROOT
        )) {
            EntryIngredient cropInput = described(cropInput(crop), 1, tooltip("rei.trading_cells.not_consumed"));
            List<EntryIngredient> inputs = List.of(adultVillager, cropInput, optionalHoe);
            FarmerHarvest harvest = farmer.harvest(crop, 0);
            List<EntryIngredient> outputs = new ArrayList<>();
            ItemStack produce = FarmerCropStackAdapter.produce(crop, harvest.produceCount());
            if (!produce.isEmpty()) {
                outputs.add(described(produce));
            }
            ItemStack seeds = FarmerCropStackAdapter.seeds(crop, harvest.seedCount());
            if (!seeds.isEmpty()) {
                outputs.add(described(seeds));
            }
            displays.add(display(
                    TradingCellsReiClientPlugin.FARMING,
                    TradingCellsReiLayout.FARMING,
                    "farming/" + crop.name().toLowerCase(),
                    inputs,
                    List.of(adultVillager, cropInput),
                    outputs,
                    farmer.effectiveGrowthTicks(0.0D, 0),
                    List.of(Component.translatable("rei.trading_cells.farming_note"))
            ));
        }
    }

    private static void addConversion(List<TradingCellsReiDisplay> displays) {
        ConverterUseCase converter = FeatureComposition.converter();
        EntryIngredient adultVillager = captured(CapturedMobKind.VILLAGER, false, true);
        EntryIngredient weakness = described(
                List.of(
                        PotionContents.createItemStack(Items.POTION, Potions.WEAKNESS),
                        PotionContents.createItemStack(Items.POTION, Potions.LONG_WEAKNESS),
                        PotionContents.createItemStack(Items.SPLASH_POTION, Potions.WEAKNESS),
                        PotionContents.createItemStack(Items.SPLASH_POTION, Potions.LONG_WEAKNESS),
                        PotionContents.createItemStack(Items.LINGERING_POTION, Potions.WEAKNESS),
                        PotionContents.createItemStack(Items.LINGERING_POTION, Potions.LONG_WEAKNESS)
                )
        );
        List<EntryIngredient> inputs = List.of(
                adultVillager,
                weakness,
                described(Items.GOLDEN_APPLE, 1)
        );
        displays.add(display(
                TradingCellsReiClientPlugin.CONVERSION,
                TradingCellsReiLayout.CONVERSION,
                "conversion/villager",
                inputs,
                inputs,
                List.of(captured(CapturedMobKind.VILLAGER, false, false)),
                converter.durationTicks(ConverterStage.INFECTING)
                        + converter.durationTicks(ConverterStage.CURING),
                List.of(Component.translatable("rei.trading_cells.conversion_note"))
        ));
    }

    private static void addIronFarm(List<TradingCellsReiDisplay> displays) {
        IronFarmUseCase ironFarm = FeatureComposition.ironFarm();
        IronFarmCycle cycle = ironFarm.cycle();
        for (int villagers = 1; villagers <= 3; villagers++) {
            List<EntryIngredient> inputs = new ArrayList<>();
            for (int count = 0; count < villagers; count++) {
                inputs.add(captured(CapturedMobKind.VILLAGER, false, true));
            }
            int multiplier = cycle.multiplier(villagers);
            List<EntryIngredient> outputs = new ArrayList<>();
            outputs.add(described(Items.IRON_INGOT, ironFarm.baseIron() * multiplier));
            int maximumPoppies = ironFarm.maximumPoppies() * multiplier;
            if (maximumPoppies > 0) {
                outputs.add(described(
                        Items.POPPY,
                        maximumPoppies,
                        tooltip("rei.trading_cells.amount_range", 0, maximumPoppies)
                ));
            }
            displays.add(display(
                    TradingCellsReiClientPlugin.IRON_FARM,
                    TradingCellsReiLayout.IRON_FARM,
                    "iron_farm/" + villagers,
                    inputs,
                    inputs,
                    outputs,
                    cycle.cycleTicks(),
                    List.of(Component.translatable("rei.trading_cells.iron_farm_note"))
            ));
        }
    }

    private static void addPiglinBartering(List<TradingCellsReiDisplay> displays) {
        int durationTicks = FeatureComposition.piglinBarter().advance(0, true).ticksRemaining();
        EntryIngredient adultPiglin = captured(CapturedMobKind.PIGLIN, false, true);
        EntryIngredient gold = described(Items.GOLD_INGOT, 1);
        EntryIngredient netheriteUpgrade = described(
                TraderRegistrationAdapter.PIGLIN_BARTER_NETHERITE_UPGRADE_ITEM.get(),
                1,
                tooltip("rei.trading_cells.barter_upgrade"),
                tooltip("rei.trading_cells.not_consumed")
        );

        for (PiglinBarterCatalog.Entry outcome : PiglinBarterCatalog.entries(BasicDisplay.registryAccess())) {
            String path = BuiltInRegistries.ITEM.getKey(outcome.filter().getItem()).getPath();
            List<EntryIngredient> normalInputs = List.of(adultPiglin, gold);
            displays.add(barterDisplay(
                    TradingCellsReiClientPlugin.PIGLIN_BARTERING,
                    TradingCellsReiLayout.PIGLIN_BARTERING,
                    "piglin_bartering/" + path,
                    normalInputs,
                    normalInputs,
                    List.of(barterResult(
                            outcome,
                            outcome.minimumAmount(),
                            outcome.maximumAmount()
                    )),
                    durationTicks,
                    List.of(Component.translatable("rei.trading_cells.piglin_bartering_note")),
                    outcome.minimumAmount(),
                    outcome.maximumAmount()
            ));

            EntryIngredient filter = described(
                    outcome.filter(),
                    tooltip("rei.trading_cells.barter_filter"),
                    tooltip("rei.trading_cells.not_consumed")
            );
            List<EntryIngredient> netheriteInputs = List.of(adultPiglin, gold, netheriteUpgrade, filter);
            ItemStack representativeOutput = outcome.outputs().getFirst();
            int upgradedMinimum = EnhancedPiglinBarterRewards.upgradedStackAmount(
                    representativeOutput,
                    outcome.minimumAmount(),
                    EnhancedPiglinBarterRewards.NETHERITE_UPGRADE_LEVEL
            );
            int upgradedMaximum = EnhancedPiglinBarterRewards.upgradedStackAmount(
                    representativeOutput,
                    outcome.maximumAmount(),
                    EnhancedPiglinBarterRewards.NETHERITE_UPGRADE_LEVEL
            );
            displays.add(barterDisplay(
                    TradingCellsReiClientPlugin.NETHERITE_PIGLIN_BARTERING,
                    TradingCellsReiLayout.NETHERITE_PIGLIN_BARTERING,
                    "netherite_piglin_bartering/" + path,
                    netheriteInputs,
                    normalInputs,
                    List.of(barterResult(outcome, upgradedMinimum, upgradedMaximum)),
                    durationTicks,
                    List.of(Component.translatable("rei.trading_cells.netherite_bartering_note")),
                    upgradedMinimum,
                    upgradedMaximum
            ));
        }
    }

    private static TradingCellsReiDisplay display(
            CategoryIdentifier<TradingCellsReiDisplay> category,
            TradingCellsReiLayout layout,
            String path,
            List<EntryIngredient> inputs,
            List<EntryIngredient> requiredInputs,
            List<EntryIngredient> outputs,
            int durationTicks,
            List<Component> notes
    ) {
        return new TradingCellsReiDisplay(
                category,
                layout,
                Identifier.fromNamespaceAndPath(TradingCells.MOD_ID, "rei/" + path),
                inputs,
                requiredInputs,
                outputs,
                durationTicks,
                notes,
                Optional.empty()
        );
    }

    private static TradingCellsReiDisplay barterDisplay(
            CategoryIdentifier<TradingCellsReiDisplay> category,
            TradingCellsReiLayout layout,
            String path,
            List<EntryIngredient> inputs,
            List<EntryIngredient> requiredInputs,
            List<EntryIngredient> outputs,
            int durationTicks,
            List<Component> notes,
            int minimumAmount,
            int maximumAmount
    ) {
        return new TradingCellsReiDisplay(
                category,
                layout,
                Identifier.fromNamespaceAndPath(TradingCells.MOD_ID, "rei/" + path),
                inputs,
                requiredInputs,
                outputs,
                durationTicks,
                notes,
                Optional.of(new TradingCellsReiDisplay.OutputAmount(minimumAmount, maximumAmount))
        );
    }

    private static Item capturer(CapturedMobKind kind) {
        return kind == CapturedMobKind.VILLAGER
                ? CaptureRegistrationAdapter.VILLAGER_CAPTURER_ITEM.get()
                : CaptureRegistrationAdapter.PIGLIN_CAPTURER_ITEM.get();
    }

    private static EntryIngredient captured(CapturedMobKind kind, boolean baby, boolean notConsumed) {
        String entity = kind == CapturedMobKind.VILLAGER ? "villager" : "piglin";
        List<Component> tooltips = new ArrayList<>();
        tooltips.add(tooltip("rei.trading_cells." + (baby ? "baby_" : "adult_") + entity));
        if (notConsumed) {
            tooltips.add(tooltip("rei.trading_cells.not_consumed"));
        }
        return described(capturedStack(kind, baby), tooltips.toArray(Component[]::new));
    }

    private static ItemStack capturedStack(CapturedMobKind kind, boolean baby) {
        ItemStack stack = new ItemStack(capturer(kind));
        CompoundTag entityData = new CompoundTag();
        entityData.putInt("Age", baby ? -24000 : 0);
        if (kind == CapturedMobKind.VILLAGER) {
            CompoundTag villagerData = new CompoundTag();
            villagerData.putString("type", "minecraft:plains");
            villagerData.putString("profession", "minecraft:none");
            villagerData.putInt("level", 1);
            entityData.put("VillagerData", villagerData);
        } else {
            entityData.putBoolean("IsBaby", baby);
        }
        CapturedMobStackAdapter.setData(kind, stack, entityData);
        return stack;
    }

    private static Item cropInput(FarmerCrop crop) {
        return switch (crop) {
            case WHEAT -> Items.WHEAT_SEEDS;
            case CARROT -> Items.CARROT;
            case POTATO -> Items.POTATO;
            case BEETROOT -> Items.BEETROOT_SEEDS;
            case NONE -> Items.AIR;
        };
    }

    private static EntryIngredient barterResult(
            PiglinBarterCatalog.Entry outcome,
            int minimumAmount,
            int maximumAmount
    ) {
        List<Component> tooltips;
        if (minimumAmount != maximumAmount) {
            tooltips = List.of(tooltip(
                    "rei.trading_cells.amount_range",
                    minimumAmount,
                    maximumAmount
            ));
        } else if (maximumAmount > 1) {
            tooltips = List.of(tooltip("rei.trading_cells.amount_exact", maximumAmount));
        } else {
            tooltips = List.of();
        }
        List<ItemStack> outputs = outcome.outputs().stream().map(stack -> {
            ItemStack copy = stack.copy();
            copy.setCount(1);
            return copy;
        }).toList();
        return described(outputs, tooltips.toArray(Component[]::new));
    }

    private static EntryIngredient described(ItemLike item, int amount, Component... tooltips) {
        return described(new ItemStack(item, amount), tooltips);
    }

    private static EntryIngredient described(ItemStack stack, Component... tooltips) {
        EntryStack<ItemStack> entry = EntryStacks.of(stack.copy());
        if (tooltips.length > 0) {
            entry = entry.tooltip(Arrays.asList(tooltips));
        }
        return EntryIngredient.of(entry);
    }

    private static EntryIngredient described(List<ItemStack> stacks, Component... tooltips) {
        return EntryIngredient.of(stacks.stream().map(stack -> {
            EntryStack<ItemStack> entry = EntryStacks.of(stack.copy());
            return tooltips.length == 0 ? entry : entry.tooltip(Arrays.asList(tooltips));
        }).toList());
    }

    private static Component tooltip(String key, Object... arguments) {
        return Component.translatable(key, arguments).withStyle(ChatFormatting.GRAY);
    }
}
