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
import com.cosmocraft.trading_cells.feature.farmer.adapters.input.HoeTierCatalog;
import com.cosmocraft.trading_cells.feature.farmer.application.port.input.FarmerUseCase;
import com.cosmocraft.trading_cells.feature.farmer.domain.model.FarmerCrop;
import com.cosmocraft.trading_cells.feature.farmer.domain.model.FarmerHarvest;
import com.cosmocraft.trading_cells.feature.farmer.domain.model.FarmerKind;
import com.cosmocraft.trading_cells.feature.farmer.domain.model.FarmerYield;
import com.cosmocraft.trading_cells.feature.ironfarm.application.port.input.IronFarmUseCase;
import com.cosmocraft.trading_cells.feature.ironfarm.domain.model.IronFarmCycle;
import com.cosmocraft.trading_cells.feature.quarry.adapters.input.QuarryMaterialCatalog;
import com.cosmocraft.trading_cells.feature.quarry.adapters.input.QuarryMaterialDefinition;
import com.cosmocraft.trading_cells.feature.quarry.adapters.input.QuarryPickaxeCatalog;
import com.cosmocraft.trading_cells.feature.quarry.adapters.output.QuarryRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.quarry.domain.model.QuarryCycle;
import com.cosmocraft.trading_cells.feature.quarry.domain.model.QuarryKind;
import com.cosmocraft.trading_cells.feature.quarry.domain.model.QuarryUpgradeTier;
import com.cosmocraft.trading_cells.feature.skeletonfarm.adapters.input.SkeletonFarmLootAdapter;
import com.cosmocraft.trading_cells.feature.combat.adapters.api.CombatEnchantments;
import com.cosmocraft.trading_cells.feature.combat.adapters.output.CombatRegistrationAdapter;
import com.cosmocraft.trading_cells.platform.neoforge.mobfarm.MobFarmSwordTierCatalog;
import com.cosmocraft.trading_cells.feature.skeletonfarm.adapters.output.SkeletonFarmRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.combat.domain.model.DecapitationRules;
import com.cosmocraft.trading_cells.feature.skeletonfarm.domain.model.SkeletonFarmCycle;
import com.cosmocraft.trading_cells.feature.skeletonfarm.domain.model.SkeletonFarmKind;
import com.cosmocraft.trading_cells.feature.zombiefarm.adapters.input.ZombieFarmLootAdapter;
import com.cosmocraft.trading_cells.feature.zombiefarm.domain.model.ZombieFarmCycle;
import com.cosmocraft.trading_cells.feature.zombiefarm.domain.model.ZombieFarmKind;
import com.cosmocraft.trading_cells.feature.raiderfarm.domain.model.RaiderFarmCycle;
import com.cosmocraft.trading_cells.feature.raiderfarm.domain.model.RaiderFarmKind;
import com.cosmocraft.trading_cells.feature.creeperfarm.adapters.input.CreeperFarmLootAdapter;
import com.cosmocraft.trading_cells.feature.creeperfarm.domain.model.CreeperFarmCycle;
import com.cosmocraft.trading_cells.feature.creeperfarm.domain.model.CreeperFarmKind;
import com.cosmocraft.trading_cells.feature.configuredmobfarm.adapters.input.ConfiguredMobFarmTargetCatalog;
import com.cosmocraft.trading_cells.feature.configuredmobfarm.domain.model.ConfiguredMobFarmCycle;
import com.cosmocraft.trading_cells.feature.configuredmobfarm.domain.model.ConfiguredMobFarmKind;
import com.cosmocraft.trading_cells.feature.trader.adapters.minecraft.EnhancedPiglinBarterRewards;
import com.cosmocraft.trading_cells.feature.trader.adapters.minecraft.PiglinBarterCatalog;
import com.cosmocraft.trading_cells.feature.trader.adapters.output.TraderRegistrationAdapter;
import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.FeatureComposition;
import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.TradingCells;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.basic.BasicDisplay;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.ItemLike;

/** Builds client-side displays from the same domain rules used by the machines. */
public final class TradingCellsReiDisplays {
    private static final TagKey<Item> DECAPITATION_SMITHING_BASES = TagKey.create(
            Registries.ITEM,
            Identifier.fromNamespaceAndPath(TradingCells.MOD_ID, "decapitation_smithing_bases")
    );

    private TradingCellsReiDisplays() {
    }

    public static List<TradingCellsReiDisplay> createAll() {
        List<TradingCellsReiDisplay> displays = new ArrayList<>();
        addBreeding(displays);
        addIncubation(displays);
        addFarming(displays);
        addConversion(displays);
        addIronFarm(displays);
        addSkeletonFarm(displays);
        addZombieFarm(displays);
        addRaiderFarm(displays);
        addCreeperFarm(displays);
        addConfiguredMobFarms(displays);
        addDecapitationSmithing(displays);
        addPiglinBartering(displays);
        addQuarries(displays);
        addSpawnerRedstoneControl(displays);
        return List.copyOf(displays);
    }

    private static void addSpawnerRedstoneControl(List<TradingCellsReiDisplay> displays) {
        addSpawnerRedstoneControl(displays, Items.SPAWNER, "spawner", false);
        addSpawnerRedstoneControl(displays, Items.TRIAL_SPAWNER, "trial_spawner", true);
    }

    private static void addSpawnerRedstoneControl(
            List<TradingCellsReiDisplay> displays,
            ItemLike spawner,
            String path,
            boolean trialSpawner
    ) {
        EntryIngredient spawnerEntry = described(spawner, 1);
        EntryIngredient comparator = described(Items.COMPARATOR, 1);
        List<Component> notes = new ArrayList<>();
        notes.add(Component.translatable("rei.trading_cells.spawner_redstone_control.install"));
        notes.add(Component.translatable("rei.trading_cells.spawner_redstone_control.consumed"));
        notes.add(Component.translatable(trialSpawner
                ? "rei.trading_cells.spawner_redstone_control.trial"
                : "rei.trading_cells.spawner_redstone_control.standard"));
        if (trialSpawner) {
            notes.add(Component.translatable("rei.trading_cells.spawner_redstone_control.active_trial"));
        }
        displays.add(display(
                TradingCellsReiClientPlugin.SPAWNER_REDSTONE_CONTROL,
                TradingCellsReiLayout.SPAWNER_REDSTONE_CONTROL,
                "spawner_redstone_control/" + path,
                List.of(spawnerEntry, comparator),
                List.of(spawnerEntry, comparator),
                List.of(spawnerEntry),
                0,
                notes
        ));
    }

    private static void addDecapitationSmithing(List<TradingCellsReiDisplay> displays) {
        Holder<Enchantment> decapitation = BasicDisplay.registryAccess()
                .lookup(Registries.ENCHANTMENT)
                .flatMap(enchantments -> enchantments.get(CombatEnchantments.DECAPITATION))
                .orElse(null);
        if (decapitation == null) {
            return;
        }

        EntryIngredient shard = described(CombatRegistrationAdapter.STORM_SHARD_ITEM.get(), 1);
        for (int level = 1; level < DecapitationRules.MAX_DECAPITATION_LEVEL; level++) {
            EntryIngredient bases = described(decapitationStacks(decapitation, level));
            EntryIngredient results = described(decapitationStacks(decapitation, level + 1));
            if (bases.isEmpty() || results.isEmpty()) {
                continue;
            }
            displays.add(display(
                    TradingCellsReiClientPlugin.DECAPITATION_SMITHING,
                    TradingCellsReiLayout.DECAPITATION_SMITHING,
                    "decapitation_smithing/" + level,
                    List.of(bases, shard),
                    List.of(bases, shard),
                    List.of(results),
                    0,
                    List.of()
            ));
        }
    }

    private static List<ItemStack> decapitationStacks(Holder<Enchantment> enchantment, int level) {
        var items = BasicDisplay.registryAccess().lookup(Registries.ITEM).orElse(null);
        if (items == null) {
            return List.of();
        }

        List<ItemStack> stacks = new ArrayList<>();
        for (Holder<Item> holder : items.getTagOrEmpty(DECAPITATION_SMITHING_BASES)) {
            if (holder.value() == Items.ENCHANTED_BOOK) {
                stacks.add(EnchantmentHelper.createBook(new EnchantmentInstance(enchantment, level)));
                continue;
            }
            ItemStack stack = new ItemStack(holder.value());
            ItemEnchantments.Mutable enchantments = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
            enchantments.set(enchantment, level);
            EnchantmentHelper.setEnchantments(stack, enchantments.toImmutable());
            stacks.add(stack);
        }
        return List.copyOf(stacks);
    }

    private static void addQuarries(List<TradingCellsReiDisplay> displays) {
        EntryIngredient pickaxes = described(
                QuarryPickaxeCatalog.itemStacks(),
                tooltip("rei.trading_cells.quarry_pickaxe")
        );
        for (QuarryKind kind : QuarryKind.values()) {
            CapturedMobKind workerKind = kind == QuarryKind.VILLAGER
                    ? CapturedMobKind.VILLAGER
                    : CapturedMobKind.PIGLIN;
            EntryIngredient worker = captured(workerKind, false, true);
            for (QuarryMaterialDefinition definition : QuarryMaterialCatalog.definitions(kind)) {
                List<ItemStack> outputStacks = quarryOutputStacks(definition);
                if (outputStacks.isEmpty() || definition.weight(QuarryUpgradeTier.NETHERITE) <= 0) {
                    continue;
                }
                QuarryUpgradeTier tier = definition.minimumUpgrade();
                EntryIngredient upgrades = quarryUpgrades(tier);
                List<EntryIngredient> inputs = List.of(worker, pickaxes, upgrades);
                List<EntryIngredient> required = tier == QuarryUpgradeTier.NONE
                        ? List.of(worker, pickaxes)
                        : inputs;
                int probability = QuarryMaterialCatalog.snapshot(
                                kind,
                                tier,
                                new ItemStack(Items.NETHERITE_PICKAXE),
                                false
                        ).entries().stream()
                        .filter(entry -> entry.definition().id().equals(definition.id()))
                        .mapToInt(QuarryMaterialCatalog.CatalogEntry::probabilityPartsPerMillion)
                        .findFirst()
                        .orElse(0);
                EntryIngredient output = described(
                        outputStacks,
                        definition.minimumAmount() == definition.maximumAmount()
                                ? tooltip("rei.trading_cells.amount_exact", definition.maximumAmount())
                                : tooltip(
                                        "rei.trading_cells.amount_range",
                                        definition.minimumAmount(),
                                        definition.maximumAmount()
                                ),
                        tooltip("rei.trading_cells.quarry_probability", quarryProbability(probability)),
                        quarryFortuneTooltip(definition)
                );
                String path = definition.id().getNamespace() + "/" + definition.id().getPath();
                displays.add(barterDisplay(
                        kind == QuarryKind.VILLAGER
                                ? TradingCellsReiClientPlugin.QUARRY
                                : TradingCellsReiClientPlugin.PIGLIN_QUARRY,
                        kind == QuarryKind.VILLAGER
                                ? TradingCellsReiLayout.QUARRY
                                : TradingCellsReiLayout.PIGLIN_QUARRY,
                        (kind == QuarryKind.VILLAGER ? "quarry/" : "piglin_quarry/") + path,
                        inputs,
                        required,
                        List.of(output),
                        QuarryCycle.BASE_DURATION_TICKS,
                        List.of(Component.translatable(kind == QuarryKind.VILLAGER
                                ? "rei.trading_cells.quarry_note"
                                : "rei.trading_cells.piglin_quarry_note")),
                        definition.minimumAmount(),
                        definition.maximumAmount()
                ));
            }
        }
    }

    private static List<ItemStack> quarryOutputStacks(QuarryMaterialDefinition definition) {
        Set<Identifier> resultIds = new LinkedHashSet<>();
        resultIds.add(definition.normalResult());
        resultIds.add(definition.silkResult());
        if (definition.deepSilkResult() != null) {
            resultIds.add(definition.deepSilkResult());
        }
        return resultIds.stream()
                .map(BuiltInRegistries.ITEM::getOptional)
                .flatMap(Optional::stream)
                .map(ItemStack::new)
                .toList();
    }

    private static Component quarryFortuneTooltip(QuarryMaterialDefinition definition) {
        if (definition.fortuneCompatible()) {
            return tooltip("rei.trading_cells.quarry_fortune");
        }
        if (QuarryMaterialCatalog.fortuneAffectsSelection(definition)) {
            return tooltip("rei.trading_cells.quarry_fortune_chance");
        }
        return tooltip("rei.trading_cells.quarry_no_fortune");
    }

    private static EntryIngredient quarryUpgrades(QuarryUpgradeTier minimum) {
        List<ItemStack> stacks = new ArrayList<>();
        if (minimum.ordinal() <= QuarryUpgradeTier.COPPER.ordinal()) {
            stacks.add(QuarryRegistrationAdapter.QUARRY_COPPER_UPGRADE_ITEM.get().getDefaultInstance());
        }
        if (minimum.ordinal() <= QuarryUpgradeTier.IRON.ordinal()) {
            stacks.add(QuarryRegistrationAdapter.QUARRY_IRON_UPGRADE_ITEM.get().getDefaultInstance());
        }
        if (minimum.ordinal() <= QuarryUpgradeTier.GOLD.ordinal()) {
            stacks.add(QuarryRegistrationAdapter.QUARRY_GOLD_UPGRADE_ITEM.get().getDefaultInstance());
        }
        if (minimum.ordinal() <= QuarryUpgradeTier.DIAMOND.ordinal()) {
            stacks.add(QuarryRegistrationAdapter.QUARRY_DIAMOND_UPGRADE_ITEM.get().getDefaultInstance());
        }
        stacks.add(QuarryRegistrationAdapter.QUARRY_NETHERITE_UPGRADE_ITEM.get().getDefaultInstance());
        return described(
                stacks,
                tooltip(minimum == QuarryUpgradeTier.NONE
                        ? "rei.trading_cells.quarry_optional_upgrade"
                        : "rei.trading_cells.quarry_minimum_upgrade",
                        Component.translatable("upgrade.trading_cells.quarry."
                                + minimum.name().toLowerCase(Locale.ROOT))),
                tooltip("rei.trading_cells.not_consumed")
        );
    }

    private static String quarryProbability(int partsPerMillion) {
        int value = Math.max(0, partsPerMillion);
        double percent = value / 10_000.0D;
        if (value < 1_000) {
            return String.format(Locale.ROOT, "%.4f", percent);
        }
        if (value < 10_000) {
            return String.format(Locale.ROOT, "%.3f", percent);
        }
        return String.format(Locale.ROOT, "%.2f", percent);
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
        EntryIngredient optionalHoe = described(
                HoeTierCatalog.itemStacks(),
                tooltip("rei.trading_cells.optional_hoe")
        );
        for (FarmerKind kind : FarmerKind.values()) {
            boolean villager = kind == FarmerKind.VILLAGER;
            EntryIngredient adultWorker = captured(
                    villager ? CapturedMobKind.VILLAGER : CapturedMobKind.PIGLIN,
                    false,
                    true
            );
            if (villager) {
                addVillagerFarming(displays, farmer, adultWorker, optionalHoe);
                continue;
            }
            for (FarmerCrop crop : FarmerCrop.supportedBy(kind)) {
                FarmerHarvest harvest = farmer.harvest(crop, 0);
                List<EntryIngredient> outputs = harvest.yields().stream()
                        .map(TradingCellsReiDisplays::farmerOutput)
                        .toList();
                addFarmingDisplay(
                        displays,
                        farmer,
                        adultWorker,
                        optionalHoe,
                        FarmerCropStackAdapter.input(crop),
                        outputs,
                        "piglin_farming/" + crop.name().toLowerCase(Locale.ROOT),
                        false
                );
            }
        }
    }

    private static void addVillagerFarming(
            List<TradingCellsReiDisplay> displays,
            FarmerUseCase farmer,
            EntryIngredient adultWorker,
            EntryIngredient optionalHoe
    ) {
        for (FarmerCropStackAdapter.Option option : FarmerCropStackAdapter.villagerOptions()) {
            List<EntryIngredient> outputs;
            if (option.crop() == FarmerCrop.NONE) {
                List<FarmerCropStackAdapter.PreviewYield> previewYields =
                        FarmerCropStackAdapter.previewYields(option);
                outputs = previewYields.isEmpty()
                        ? List.of(described(
                                FarmerCropStackAdapter.previewOutput(option),
                                tooltip("rei.trading_cells.dynamic_crop_output")
                        ))
                        : previewYields.stream()
                                .map(TradingCellsReiDisplays::farmerOutput)
                                .toList();
            } else {
                outputs = farmer.harvest(option.crop(), 0).yields().stream()
                        .map(TradingCellsReiDisplays::farmerOutput)
                        .toList();
            }
            Identifier itemId = BuiltInRegistries.ITEM.getKey(option.item());
            addFarmingDisplay(
                    displays,
                    farmer,
                    adultWorker,
                    optionalHoe,
                    new ItemStack(option.item()),
                    outputs,
                    "farming/" + itemId.getNamespace() + "/" + itemId.getPath(),
                    true
            );
        }
    }

    private static void addFarmingDisplay(
            List<TradingCellsReiDisplay> displays,
            FarmerUseCase farmer,
            EntryIngredient adultWorker,
            EntryIngredient optionalHoe,
            ItemStack crop,
            List<EntryIngredient> outputs,
            String path,
            boolean villager
    ) {
        EntryIngredient cropInput = described(crop, tooltip("rei.trading_cells.not_consumed"));
        displays.add(display(
                villager ? TradingCellsReiClientPlugin.FARMING : TradingCellsReiClientPlugin.PIGLIN_FARMING,
                villager ? TradingCellsReiLayout.FARMING : TradingCellsReiLayout.PIGLIN_FARMING,
                path,
                List.of(adultWorker, cropInput, optionalHoe),
                List.of(adultWorker, cropInput),
                outputs,
                farmer.baseGrowthTicks(),
                List.of(Component.translatable(villager
                        ? "rei.trading_cells.farming_note"
                        : "rei.trading_cells.piglin_farming_note"))
        ));
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

    private static void addSkeletonFarm(List<TradingCellsReiDisplay> displays) {
        EntryIngredient worker = captured(CapturedMobKind.VILLAGER, false, true);
        EntryIngredient swords = described(
                MobFarmSwordTierCatalog.itemStacks(),
                tooltip("rei.trading_cells.skeleton_sword")
        );
        for (SkeletonFarmKind kind : SkeletonFarmKind.values()) {
            List<SkeletonFarmLootAdapter.PreviewOutput> previews =
                    SkeletonFarmLootAdapter.previewOutputChances(kind);
            EntryIngredient target = described(
                    skeletonSpawnEgg(kind),
                    tooltip("rei.trading_cells.skeleton_target"),
                    tooltip("rei.trading_cells.not_consumed")
            );
            EntryIngredient outputs = skeletonOutputs(kind, previews);
            displays.add(display(
                    TradingCellsReiClientPlugin.SKELETON_FARM,
                    TradingCellsReiLayout.SKELETON_FARM,
                    "skeleton_farm/" + kind.name().toLowerCase(Locale.ROOT),
                    List.of(worker, swords, target),
                    List.of(worker, swords),
                    List.of(outputs),
                    SkeletonFarmCycle.effectiveCycleTicks(0.0D, 0),
                    List.of(
                            Component.translatable("rei.trading_cells.skeleton_farm_note"),
                            Component.translatable("rei.trading_cells.skeleton_filters_note"),
                            Component.translatable("rei.trading_cells.skeleton_decapitation_note")
                    )
            ));
        }
    }

    private static EntryIngredient skeletonOutputs(
            SkeletonFarmKind kind,
            List<SkeletonFarmLootAdapter.PreviewOutput> outputs
    ) {
        return EntryIngredient.of(outputs.stream().map(output -> {
            EntryStack<ItemStack> entry = EntryStacks.of(output.stack());
            Component amount = output.minimumAmount() == output.maximumAmount()
                    ? tooltip("rei.trading_cells.skeleton_base_amount_exact", output.maximumAmount())
                    : tooltip(
                            "rei.trading_cells.skeleton_base_amount_range",
                            output.minimumAmount(),
                            output.maximumAmount()
                    );
            List<Component> tooltip = new ArrayList<>(List.of(
                    tooltip("rei.trading_cells.skeleton_drop"),
                    tooltip(
                            "rei.trading_cells.skeleton_base_probability",
                            percentage(output.probabilityPartsPerMillion())
                    ),
                    amount
            ));
            if (output.loot() == com.cosmocraft.trading_cells.feature.skeletonfarm.domain.model
                    .SkeletonFarmLoot.SKULLS && kind != SkeletonFarmKind.WITHER_SKELETON) {
                tooltip.add(Component.translatable("rei.trading_cells.requires_decapitation")
                        .withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            return entry.tooltip(List.copyOf(tooltip));
        }).toList());
    }

    private static Component percentage(int partsPerMillion) {
        int hundredths = (int) Math.round(partsPerMillion / 100.0D);
        return Component.translatable(
                "rei.trading_cells.percentage",
                hundredths / 100,
                String.format(Locale.ROOT, "%02d", hundredths % 100)
        );
    }

    private static ItemStack skeletonSpawnEgg(SkeletonFarmKind kind) {
        return new ItemStack(switch (kind) {
            case SKELETON -> Items.SKELETON_SPAWN_EGG;
            case WITHER_SKELETON -> Items.WITHER_SKELETON_SPAWN_EGG;
            case STRAY -> Items.STRAY_SPAWN_EGG;
            case BOGGED -> Items.BOGGED_SPAWN_EGG;
            case PARCHED -> Items.PARCHED_SPAWN_EGG;
            case SKELETON_HORSE -> Items.SKELETON_HORSE_SPAWN_EGG;
        });
    }

    private static void addZombieFarm(List<TradingCellsReiDisplay> displays) {
        EntryIngredient worker = captured(CapturedMobKind.VILLAGER, false, true);
        EntryIngredient swords = described(
                MobFarmSwordTierCatalog.itemStacks(),
                tooltip("rei.trading_cells.zombie_sword")
        );
        for (ZombieFarmKind kind : ZombieFarmKind.values()) {
            List<ZombieFarmLootAdapter.PreviewOutput> previews =
                    ZombieFarmLootAdapter.previewOutputChances(kind);
            EntryIngredient target = described(
                    zombieSpawnEgg(kind),
                    tooltip("rei.trading_cells.zombie_target"),
                    tooltip("rei.trading_cells.not_consumed")
            );
            EntryIngredient outputs = zombieOutputs(previews);
            displays.add(display(
                    TradingCellsReiClientPlugin.ZOMBIE_FARM,
                    TradingCellsReiLayout.ZOMBIE_FARM,
                    "zombie_farm/" + kind.name().toLowerCase(Locale.ROOT),
                    List.of(worker, swords, target),
                    List.of(worker, swords),
                    List.of(outputs),
                    ZombieFarmCycle.effectiveCycleTicks(0.0D, 0),
                    List.of(
                            Component.translatable("rei.trading_cells.zombie_farm_note"),
                            Component.translatable("rei.trading_cells.zombie_filters_note"),
                            Component.translatable("rei.trading_cells.zombie_decapitation_note")
                    )
            ));
        }
    }

    private static EntryIngredient zombieOutputs(
            List<ZombieFarmLootAdapter.PreviewOutput> outputs
    ) {
        return EntryIngredient.of(outputs.stream().map(output -> {
            EntryStack<ItemStack> entry = EntryStacks.of(output.stack());
            Component amount = output.minimumAmount() == output.maximumAmount()
                    ? tooltip("rei.trading_cells.zombie_base_amount_exact", output.maximumAmount())
                    : tooltip(
                            "rei.trading_cells.zombie_base_amount_range",
                            output.minimumAmount(),
                            output.maximumAmount()
                    );
            List<Component> tooltip = new ArrayList<>(List.of(
                    tooltip("rei.trading_cells.zombie_drop"),
                    tooltip(
                            "rei.trading_cells.zombie_base_probability",
                            percentage(output.probabilityPartsPerMillion())
                    ),
                    amount
            ));
            if (output.loot() == com.cosmocraft.trading_cells.feature.zombiefarm.domain.model
                    .ZombieFarmLoot.HEADS) {
                tooltip.add(Component.translatable("rei.trading_cells.requires_decapitation")
                        .withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            return entry.tooltip(List.copyOf(tooltip));
        }).toList());
    }

    private static ItemStack zombieSpawnEgg(ZombieFarmKind kind) {
        return new ItemStack(switch (kind) {
            case ZOMBIE -> Items.ZOMBIE_SPAWN_EGG;
            case ZOMBIE_VILLAGER -> Items.ZOMBIE_VILLAGER_SPAWN_EGG;
            case HUSK -> Items.HUSK_SPAWN_EGG;
            case DROWNED -> Items.DROWNED_SPAWN_EGG;
            case ZOMBIFIED_PIGLIN -> Items.ZOMBIFIED_PIGLIN_SPAWN_EGG;
            case ZOGLIN -> Items.ZOGLIN_SPAWN_EGG;
        });
    }

    private static void addRaiderFarm(List<TradingCellsReiDisplay> displays) {
        EntryIngredient worker = captured(CapturedMobKind.VILLAGER, false, true);
        EntryIngredient swords = described(
                MobFarmSwordTierCatalog.itemStacks(),
                tooltip("rei.trading_cells.raider_sword")
        );
        for (RaiderFarmKind kind : RaiderFarmKind.selectableValues()) {
            ItemStack targetStack = raiderTarget(kind);
            EntryIngredient target = described(
                    targetStack,
                    tooltip("rei.trading_cells.raider_target"),
                    tooltip("rei.trading_cells.not_consumed")
            );
            displays.add(display(
                    TradingCellsReiClientPlugin.RAIDER_FARM,
                    TradingCellsReiLayout.RAIDER_FARM,
                    "raider_farm/" + kind.name().toLowerCase(Locale.ROOT),
                    List.of(worker, swords, target),
                    List.of(worker, swords),
                    List.of(raiderOutputs(kind)),
                    RaiderFarmCycle.effectiveCycleTicks(0.0D, 0),
                    List.of(
                            Component.translatable("rei.trading_cells.raider_farm_note"),
                            Component.translatable("rei.trading_cells.raider_filters_note")
                    )
            ));
        }
    }

    private static EntryIngredient raiderOutputs(RaiderFarmKind kind) {
        List<ItemStack> outputs = switch (kind) {
            case PILLAGER -> List.of(
                    new ItemStack(Items.CROSSBOW),
                    new ItemStack(BuiltInRegistries.ITEM.getOptional(
                            Identifier.withDefaultNamespace("white_banner")
                    ).orElseThrow())
            );
            case EVOKER -> List.of(new ItemStack(Items.TOTEM_OF_UNDYING), new ItemStack(Items.EMERALD));
            case RAVAGER -> List.of(new ItemStack(Items.SADDLE));
            case WITCH -> List.of(
                    new ItemStack(Items.REDSTONE),
                    new ItemStack(Items.GLOWSTONE_DUST),
                    new ItemStack(Items.SUGAR),
                    new ItemStack(Items.SPIDER_EYE),
                    new ItemStack(Items.GLASS_BOTTLE),
                    new ItemStack(Items.GUNPOWDER),
                    new ItemStack(Items.STICK)
            );
        };
        return EntryIngredient.of(outputs.stream()
                .map(stack -> EntryStacks.of(stack).tooltip(
                        tooltip("rei.trading_cells.dynamic_drop")
                ))
                .toList());
    }

    private static ItemStack raiderTarget(RaiderFarmKind kind) {
        return new ItemStack(switch (kind) {
            case PILLAGER -> Items.PILLAGER_SPAWN_EGG;
            case EVOKER -> Items.EVOKER_SPAWN_EGG;
            case RAVAGER -> Items.RAVAGER_SPAWN_EGG;
            case WITCH -> Items.WITCH_SPAWN_EGG;
        });
    }

    private static void addCreeperFarm(List<TradingCellsReiDisplay> displays) {
        EntryIngredient worker = captured(CapturedMobKind.VILLAGER, false, true);
        EntryIngredient swords = described(
                MobFarmSwordTierCatalog.itemStacks(),
                tooltip("rei.trading_cells.creeper_sword")
        );
        for (CreeperFarmKind kind : CreeperFarmKind.values()) {
            EntryIngredient target = described(
                    creeperTarget(kind),
                    tooltip("rei.trading_cells.creeper_target"),
                    tooltip("rei.trading_cells.not_consumed")
            );
            EntryIngredient outputs = creeperOutputs(CreeperFarmLootAdapter.previewOutputChances(kind));
            displays.add(display(
                    TradingCellsReiClientPlugin.CREEPER_FARM,
                    TradingCellsReiLayout.CREEPER_FARM,
                    "creeper_farm/" + kind.name().toLowerCase(Locale.ROOT),
                    List.of(worker, swords, target),
                    List.of(worker, swords),
                    List.of(outputs),
                    CreeperFarmCycle.effectiveCycleTicks(0.0D, 0),
                    List.of(
                            Component.translatable("rei.trading_cells.creeper_farm_note"),
                            Component.translatable("rei.trading_cells.creeper_filters_note"),
                            Component.translatable("rei.trading_cells.creeper_decapitation_note")
                    )
            ));
        }
    }

    private static EntryIngredient creeperOutputs(List<CreeperFarmLootAdapter.PreviewOutput> outputs) {
        return EntryIngredient.of(outputs.stream().map(output -> {
            EntryStack<ItemStack> entry = EntryStacks.of(output.stack());
            Component amount = output.minimumAmount() == output.maximumAmount()
                    ? tooltip("rei.trading_cells.creeper_base_amount_exact", output.maximumAmount())
                    : tooltip(
                            "rei.trading_cells.creeper_base_amount_range",
                            output.minimumAmount(),
                            output.maximumAmount()
                    );
            List<Component> lines = new ArrayList<>(List.of(
                    tooltip("rei.trading_cells.creeper_drop"),
                    tooltip(
                            "rei.trading_cells.creeper_base_probability",
                            percentage(output.probabilityPartsPerMillion())
                    ),
                    amount
            ));
            if (output.loot() == com.cosmocraft.trading_cells.feature.creeperfarm.domain.model
                    .CreeperFarmLoot.HEADS) {
                lines.add(Component.translatable("rei.trading_cells.requires_decapitation")
                        .withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            return entry.tooltip(List.copyOf(lines));
        }).toList());
    }

    private static ItemStack creeperTarget(CreeperFarmKind kind) {
        return kind == CreeperFarmKind.CHARGED_CREEPER
                ? new ItemStack(CombatRegistrationAdapter.STORM_SHARD_ITEM.get())
                : new ItemStack(Items.CREEPER_SPAWN_EGG);
    }

    private static void addConfiguredMobFarms(List<TradingCellsReiDisplay> displays) {
        EntryIngredient worker = captured(CapturedMobKind.VILLAGER, false, true);
        EntryIngredient swords = described(
                MobFarmSwordTierCatalog.itemStacks(),
                tooltip("rei.trading_cells.raider_sword")
        );
        for (ConfiguredMobFarmKind kind : ConfiguredMobFarmKind.values()) {
            for (var targetDefinition : ConfiguredMobFarmTargetCatalog.targets(kind)) {
                ItemStack targetStack = BuiltInRegistries.ITEM.getOptional(targetDefinition.generatorItemId())
                        .map(ItemStack::new)
                        .orElseGet(() -> new ItemStack(Items.SPAWNER));
                EntryIngredient target = described(
                        targetStack,
                        tooltip("rei.trading_cells.raider_target"),
                        tooltip("rei.trading_cells.not_consumed")
                );
                List<ItemStack> outputStacks = configuredMobFarmOutputs(kind, targetDefinition);
                List<EntryIngredient> outputs = outputStacks.isEmpty()
                        ? List.of()
                        : List.of(EntryIngredient.of(outputStacks.stream()
                                .map(stack -> EntryStacks.of(stack).tooltip(
                                        tooltip("rei.trading_cells.dynamic_drop")
                                ))
                                .toList()));
                Identifier targetId = targetDefinition.entityTypeId();
                displays.add(display(
                        TradingCellsReiClientPlugin.CONFIGURED_MOB_FARM,
                        TradingCellsReiLayout.RAIDER_FARM,
                        "configured_mob_farm/" + kind.path() + "/"
                                + targetId.getNamespace() + "_" + targetId.getPath(),
                        List.of(worker, swords, target),
                        List.of(worker, swords),
                        outputs,
                        ConfiguredMobFarmCycle.effectiveCycleTicks(0.0D, 0),
                        List.of(
                                Component.translatable("rei.trading_cells.raider_farm_note"),
                                Component.translatable("rei.trading_cells.raider_filters_note")
                        )
                ));
            }
        }
    }

    private static List<ItemStack> configuredMobFarmOutputs(
            ConfiguredMobFarmKind kind,
            com.cosmocraft.trading_cells.platform.neoforge.mobfarm.MobFarmCatalog.Target target
    ) {
        LinkedHashSet<Item> items = new LinkedHashSet<>();
        target.lootItemIds().stream()
                .map(id -> BuiltInRegistries.ITEM.getOptional(id).orElse(null))
                .filter(java.util.Objects::nonNull)
                .forEach(items::add);
        ConfiguredMobFarmTargetCatalog.equipment(target.entityTypeId()).stream()
                .map(ItemStack::getItem)
                .forEach(items::add);
        if (items.isEmpty()) {
            addKnownConfiguredOutputs(items, kind, target.entityTypeId().getPath());
        }
        return items.stream().map(ItemStack::new).toList();
    }

    private static void addKnownConfiguredOutputs(Set<Item> items, ConfiguredMobFarmKind kind, String targetPath) {
        switch (kind) {
            case ARTHROPOD -> {
                if (targetPath.contains("spider")) {
                    items.add(Items.STRING);
                    items.add(Items.SPIDER_EYE);
                }
            }
            case SLIME -> {
                if ("slime".equals(targetPath)) {
                    items.add(Items.SLIME_BALL);
                } else if ("magma_cube".equals(targetPath)) {
                    items.add(Items.MAGMA_CREAM);
                }
            }
            case GUARDIAN -> {
                items.add(Items.PRISMARINE_SHARD);
                items.add(Items.PRISMARINE_CRYSTALS);
                items.add(Items.COD);
                if ("elder_guardian".equals(targetPath)) {
                    items.add(Items.WET_SPONGE);
                }
            }
            case BLAZE -> items.add(Items.BLAZE_ROD);
            case GHAST -> {
                if ("ghast".equals(targetPath)) {
                    items.add(Items.GHAST_TEAR);
                    items.add(Items.GUNPOWDER);
                }
            }
            case ENDERMAN -> items.add(Items.ENDER_PEARL);
            case SHULKER -> items.add(Items.SHULKER_SHELL);
            case BREEZE -> items.add(Items.BREEZE_ROD);
            case PHANTOM -> items.add(Items.PHANTOM_MEMBRANE);
            case PIGLIN -> {
                // Equipment is inserted above; no guaranteed table drop exists.
            }
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

    private static EntryIngredient farmerOutput(FarmerYield yield) {
        ItemStack stack = FarmerCropStackAdapter.output(yield);
        if (yield.isGuaranteed()) {
            return described(stack);
        }
        return described(
                stack,
                tooltip(
                        "rei.trading_cells.base_chance",
                        chancePercentage(yield.chanceBasisPoints())
                )
        );
    }

    private static EntryIngredient farmerOutput(FarmerCropStackAdapter.PreviewYield yield) {
        List<Component> tooltips = new ArrayList<>(2);
        if (!yield.isGuaranteed()) {
            tooltips.add(tooltip(
                    "rei.trading_cells.base_chance",
                    chancePercentage(yield.chanceBasisPoints())
            ));
        }
        if (yield.requiresSilkTouch()) {
            tooltips.add(tooltip("rei.trading_cells.requires_silk_touch"));
        }
        return described(yield.stack(), tooltips.toArray(Component[]::new));
    }

    private static String chancePercentage(int chanceBasisPoints) {
        if (chanceBasisPoints % 100 == 0) {
            return Integer.toString(chanceBasisPoints / 100);
        }
        return String.format(Locale.ROOT, "%.1f", chanceBasisPoints / 100.0D);
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
