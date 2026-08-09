package com.cosmocraft.trading_cells.feature.quarry.adapters.input;

import com.cosmocraft.trading_cells.feature.quarry.domain.model.QuarryKind;
import com.cosmocraft.trading_cells.feature.quarry.domain.model.QuarryUpgradeTier;
import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.TradingCells;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.Tags;

/** Cached vanilla and datapack-aware result tables for both quarry variants. */
public final class QuarryMaterialCatalog {
    public static final int PROBABILITY_SCALE = 1_000_000;
    public static final int DYNAMIC_RARE_PROBABILITY = 500;

    private static final String MINECRAFT = "minecraft";
    private static final Identifier ANCIENT_DEBRIS_ID = Identifier.fromNamespaceAndPath(
            TradingCells.MOD_ID,
            "ancient_debris"
    );
    private static final double WOOD_SPEED = 2.0D;
    private static final double STONE_SPEED = 4.0D;
    private static final double IRON_SPEED = 6.0D;
    private static final double DIAMOND_SPEED = 8.0D;
    private static final int DYNAMIC_ORE_PERCENT = 20;
    private static final Comparator<QuarryMaterialDefinition> DEFINITION_ORDER = Comparator
            .comparing((QuarryMaterialDefinition definition) -> definition.kind().ordinal())
            .thenComparing(definition -> definition.id().toString());

    private static final TagKey<Block> EXCLUDED = ownTag("quarry/excluded");
    private static final Map<QuarryKind, Map<QuarryUpgradeTier, TagKey<Block>>> TIER_TAGS = tierTags();
    private static final Map<QuarryKind, TagKey<Block>> ROCK_TAGS = Map.of(
            QuarryKind.VILLAGER, ownTag("quarry/overworld/rocks"),
            QuarryKind.PIGLIN, ownTag("quarry/nether/rocks")
    );
    private static final List<QuarryMaterialDefinition> VANILLA = vanillaDefinitions();
    private static final Set<Identifier> VANILLA_IDS = vanillaIds();
    private static final QuarryMaterialDefinition DEEP_DEEPSLATE = definition(
            "deep_deepslate",
            QuarryKind.VILLAGER,
            QuarryUpgradeTier.DIAMOND,
            WOOD_SPEED,
            weights(0, 0, 0, 0, 0, 0),
            1,
            4,
            "minecraft:cobbled_deepslate",
            "minecraft:deepslate",
            null,
            false,
            true,
            false,
            false,
            MINECRAFT,
            QuarryMaterialDefinition.Pool.NORMAL
    );
    private static final QuarryMaterialDefinition DEEP_TUFF = definition(
            "deep_tuff",
            QuarryKind.VILLAGER,
            QuarryUpgradeTier.DIAMOND,
            WOOD_SPEED,
            weights(0, 0, 0, 0, 0, 0),
            1,
            4,
            "minecraft:tuff",
            "minecraft:tuff",
            null,
            false,
            true,
            false,
            false,
            MINECRAFT,
            QuarryMaterialDefinition.Pool.NORMAL
    );
    private static final AtomicReference<List<QuarryMaterialDefinition>> DATAPACK =
            new AtomicReference<>(List.of());
    private static final AtomicReference<Set<String>> DATAPACK_EXCLUSIONS =
            new AtomicReference<>(Set.of());
    private static final AtomicReference<List<QuarryMaterialDefinition>> DEFINITIONS =
            new AtomicReference<>(VANILLA);
    private static final AtomicInteger REVISION = new AtomicInteger();

    private QuarryMaterialCatalog() {
    }

    public static int revision() {
        return REVISION.get();
    }

    public static List<QuarryMaterialDefinition> definitions(QuarryKind kind) {
        return DEFINITIONS.get().stream().filter(definition -> definition.kind() == kind).toList();
    }

    public static void replaceDatapackDefinitions(List<QuarryMaterialDefinition> definitions) {
        replaceDatapackDefinitions(definitions, Set.of());
    }

    public static void replaceDatapackDefinitions(
            List<QuarryMaterialDefinition> definitions,
            Set<String> exclusions
    ) {
        DATAPACK.set(List.copyOf(definitions));
        DATAPACK_EXCLUSIONS.set(Set.copyOf(exclusions));
        refreshDynamic();
    }

    public static void refreshDynamic() {
        QuarryPickaxeCatalog.invalidate();
        try {
            List<QuarryMaterialDefinition> automatic = discoverTaggedMaterials();
            LinkedHashMap<String, QuarryMaterialDefinition> merged = new LinkedHashMap<>();
            VANILLA.forEach(definition -> merged.put(key(definition), definition));
            automatic.forEach(definition -> merged.put(key(definition), definition));
            DATAPACK.get().forEach(definition -> merged.put(key(definition), definition));
            DATAPACK_EXCLUSIONS.get().forEach(merged::remove);
            List<QuarryMaterialDefinition> next = new ArrayList<>(merged.values());
            next.sort(DEFINITION_ORDER);
            DEFINITIONS.set(List.copyOf(next));
            REVISION.incrementAndGet();
        } catch (RuntimeException | LinkageError exception) {
            TradingCells.LOGGER.warn(
                    "The dynamic quarry catalog failed to rebuild; the fixed vanilla table remains active.",
                    exception
            );
            DEFINITIONS.set(VANILLA);
            REVISION.incrementAndGet();
        }
    }

    public static CatalogSnapshot snapshot(
            QuarryKind kind,
            QuarryUpgradeTier upgrade,
            ItemStack pickaxe,
            boolean deepMining
    ) {
        return snapshot(kind, upgrade, pickaxe, deepMining, 0, false);
    }

    public static CatalogSnapshot snapshot(
            QuarryKind kind,
            QuarryUpgradeTier upgrade,
            ItemStack pickaxe,
            boolean deepMining,
            int fortuneLevel,
            boolean silkTouch
    ) {
        boolean deepActive = kind == QuarryKind.VILLAGER
                && deepMining
                && upgrade.supportsDeepMining();
        List<WeightedMaterial> weighted = buildWeightedMaterials(kind, upgrade, deepActive);
        applyDynamicOreLimit(weighted);
        if (silkTouch && fortuneLevel > 0) {
            applySilkFortuneWeightBoost(weighted, fortuneLevel);
        }

        List<CatalogEntry> entries = new ArrayList<>();
        long normalWeight = 0L;
        long dynamicRareWeight = 0L;
        int ancientDebrisProbability = 0;
        for (WeightedMaterial material : weighted) {
            QuarryMaterialDefinition definition = material.definition();
            boolean unlocked = upgrade.unlocks(definition.minimumUpgrade()) && material.weight() > 0;
            boolean mineable = unlocked && canMine(pickaxe, definition, deepActive);
            BlockedReason reason = !unlocked
                    ? BlockedReason.UPGRADE
                    : mineable ? BlockedReason.NONE : BlockedReason.PICKAXE;
            if (mineable) {
                if (definition.pool() == QuarryMaterialDefinition.Pool.PROTECTED_RARE) {
                    if (isAncientDebris(definition)) {
                        ancientDebrisProbability = (int) Math.clamp(
                                material.weight() * 100L,
                                0L,
                                PROBABILITY_SCALE
                        );
                    } else {
                        dynamicRareWeight += material.weight();
                    }
                } else {
                    normalWeight += material.weight();
                }
            }
            entries.add(new CatalogEntry(definition, material.weight(), 0, reason));
        }

        int dynamicRareProbability = dynamicRareWeight > 0L
                ? Math.min(DYNAMIC_RARE_PROBABILITY, PROBABILITY_SCALE - ancientDebrisProbability)
                : 0;
        int normalProbabilityScale = PROBABILITY_SCALE
                - ancientDebrisProbability
                - dynamicRareProbability;
        List<CatalogEntry> withProbabilities = new ArrayList<>(entries.size());
        for (CatalogEntry entry : entries) {
            int probability = 0;
            if (entry.blockedReason() == BlockedReason.NONE) {
                if (isAncientDebris(entry.definition())) {
                    probability = ancientDebrisProbability;
                } else if (entry.definition().pool() == QuarryMaterialDefinition.Pool.PROTECTED_RARE) {
                    probability = probability(entry.weight(), dynamicRareWeight, dynamicRareProbability);
                } else {
                    probability = probability(entry.weight(), normalWeight, normalProbabilityScale);
                }
            }
            withProbabilities.add(new CatalogEntry(
                    entry.definition(),
                    entry.weight(),
                    probability,
                    entry.blockedReason()
            ));
        }
        return new CatalogSnapshot(List.copyOf(withProbabilities), deepActive, REVISION.get());
    }

    private static void applySilkFortuneWeightBoost(
            List<WeightedMaterial> entries,
            int fortuneLevel
    ) {
        int level = Math.max(0, fortuneLevel);
        long denominator = level + 2L;
        long numerator = (level + 2L) * (level + 1L) / 2L + 1L;
        for (WeightedMaterial entry : entries) {
            QuarryMaterialDefinition definition = entry.definition();
            boolean supportedPool = definition.pool() == QuarryMaterialDefinition.Pool.NORMAL
                    || isAncientDebris(definition);
            if (!fortuneAffectsSelection(definition)
                    || !supportedPool
                    || entry.weight() <= 0) {
                continue;
            }
            long boosted = Math.round(entry.weight() * (double) numerator / denominator);
            entry.setWeight((int) Math.clamp(boosted, 0L, Integer.MAX_VALUE));
        }
    }

    private static List<WeightedMaterial> buildWeightedMaterials(
            QuarryKind kind,
            QuarryUpgradeTier upgrade,
            boolean deepActive
    ) {
        List<WeightedMaterial> result = new ArrayList<>();
        int rockWeight = 0;
        for (QuarryMaterialDefinition definition : definitions(kind)) {
            int weight = definition.weight(upgrade);
            if (deepActive && definition.rock()) {
                rockWeight += weight;
                continue;
            }
            result.add(new WeightedMaterial(definition, weight));
        }
        if (deepActive && rockWeight > 0) {
            int deepslateWeight = rockWeight * 80 / 100;
            result.add(new WeightedMaterial(DEEP_DEEPSLATE, deepslateWeight));
            result.add(new WeightedMaterial(DEEP_TUFF, rockWeight - deepslateWeight));
        }
        result.sort(Comparator.comparing(material -> material.definition().id().toString()));
        return result;
    }

    private static void applyDynamicOreLimit(List<WeightedMaterial> entries) {
        for (QuarryUpgradeTier tier : QuarryUpgradeTier.values()) {
            applyDynamicOreLimit(entries, tier);
        }
    }

    private static void applyDynamicOreLimit(
            List<WeightedMaterial> entries,
            QuarryUpgradeTier tier
    ) {
        List<WeightedMaterial> vanillaOres = entries.stream()
                .filter(WeightedMaterial::isAdjustableVanillaOre)
                .filter(entry -> entry.definition().minimumUpgrade() == tier)
                .filter(entry -> entry.weight() > 0)
                .toList();
        List<WeightedMaterial> dynamicOres = entries.stream()
                .filter(WeightedMaterial::isDynamicOre)
                .filter(entry -> entry.definition().minimumUpgrade() == tier)
                .filter(entry -> entry.weight() > 0)
                .toList();
        long vanillaOreWeight = vanillaOres.stream().mapToLong(WeightedMaterial::weight).sum();
        long dynamicOreWeight = dynamicOres.stream().mapToLong(WeightedMaterial::weight).sum();
        if (vanillaOreWeight <= 0L || dynamicOreWeight <= 0L) {
            return;
        }
        long allowedDynamic = Math.min(
                dynamicOreWeight,
                vanillaOreWeight * DYNAMIC_ORE_PERCENT / 100L
        );
        long assignedDynamic = 0L;
        for (WeightedMaterial entry : dynamicOres) {
            int scaled = (int) (entry.weight() * allowedDynamic / dynamicOreWeight);
            entry.setWeight(scaled);
            assignedDynamic += scaled;
        }
        long remainingDynamic = allowedDynamic - assignedDynamic;
        for (WeightedMaterial entry : dynamicOres) {
            if (remainingDynamic <= 0L) {
                break;
            }
            entry.setWeight(entry.weight() + 1);
            remainingDynamic--;
        }

        long reduction = allowedDynamic;
        long reduced = 0L;
        for (WeightedMaterial entry : vanillaOres) {
            int decrease = (int) (entry.weight() * reduction / vanillaOreWeight);
            entry.setWeight(Math.max(0, entry.weight() - decrease));
            reduced += decrease;
        }
        long remainingReduction = reduction - reduced;
        for (WeightedMaterial entry : vanillaOres) {
            if (remainingReduction <= 0L) {
                break;
            }
            if (entry.weight() > 0) {
                entry.setWeight(entry.weight() - 1);
                remainingReduction--;
            }
        }
    }

    private static boolean canMine(
            ItemStack pickaxe,
            QuarryMaterialDefinition definition,
            boolean deepMining
    ) {
        Identifier blockId = definition.silkResult(deepMining);
        Optional<Block> block = BuiltInRegistries.BLOCK.getOptional(blockId);
        return block.isPresent()
                && QuarryPickaxeCatalog.canMine(pickaxe, block.get(), definition.minimumPickaxeSpeed());
    }

    private static int probability(int weight, long totalWeight, int scale) {
        if (weight <= 0 || totalWeight <= 0L) {
            return 0;
        }
        return (int) Math.clamp(Math.round(weight * (double) scale / totalWeight), 0L, scale);
    }

    private static boolean isAncientDebris(QuarryMaterialDefinition definition) {
        return definition.kind() == QuarryKind.PIGLIN
                && definition.pool() == QuarryMaterialDefinition.Pool.PROTECTED_RARE
                && definition.id().equals(ANCIENT_DEBRIS_ID);
    }

    public static boolean fortuneAffectsSelection(QuarryMaterialDefinition definition) {
        return definition.fortuneCompatible() || isAncientDebris(definition);
    }

    private static List<QuarryMaterialDefinition> discoverTaggedMaterials() {
        List<QuarryMaterialDefinition> discovered = new ArrayList<>();
        Set<Identifier> deepVariants = new HashSet<>();
        for (Block block : BuiltInRegistries.BLOCK) {
            Identifier id = BuiltInRegistries.BLOCK.getKey(block);
            if (id == null || MINECRAFT.equals(id.getNamespace()) || VANILLA_IDS.contains(id)) {
                continue;
            }
            try {
                if (block.defaultBlockState().is(EXCLUDED)) {
                    continue;
                }
                Optional<QuarryMaterialDefinition> definition = automaticDefinition(block, id);
                if (definition.isEmpty()) {
                    continue;
                }
                QuarryMaterialDefinition value = definition.get();
                if (value.deepSilkResult() != null) {
                    deepVariants.add(value.deepSilkResult());
                }
                discovered.add(value);
            } catch (RuntimeException | LinkageError exception) {
                TradingCells.LOGGER.warn("Discarding invalid dynamic quarry material '{}'.", id, exception);
            }
        }
        discovered.removeIf(definition -> deepVariants.contains(definition.id()));
        return discovered;
    }

    private static Optional<QuarryMaterialDefinition> automaticDefinition(Block block, Identifier id) {
        QuarryKind kind = explicitKind(block).orElseGet(() ->
                block.defaultBlockState().is(Tags.Blocks.ORES_IN_GROUND_NETHERRACK)
                        ? QuarryKind.PIGLIN
                        : QuarryKind.VILLAGER
        );
        boolean rock = block.defaultBlockState().is(ROCK_TAGS.get(kind));
        boolean ore = block.defaultBlockState().is(Tags.Blocks.ORES);
        if (!rock && !ore && explicitTier(block, kind).isEmpty()) {
            return Optional.empty();
        }
        QuarryUpgradeTier tier = explicitTier(block, kind).orElseGet(() -> automaticTier(block));
        double minimumSpeed = minimumPickaxeSpeed(block);
        int maximumAmount = rock ? 4 : block.defaultBlockState().is(Tags.Blocks.ORE_RATES_DENSE) ? 3 : 1;
        Identifier deepVariant = kind == QuarryKind.VILLAGER ? findDeepVariant(id) : null;
        return Optional.of(new QuarryMaterialDefinition(
                id,
                kind,
                tier,
                minimumSpeed,
                weightsFrom(tier, 100),
                1,
                maximumAmount,
                id,
                id,
                deepVariant,
                ore,
                rock,
                true,
                ore,
                id.getNamespace(),
                tier == QuarryUpgradeTier.NETHERITE
                        ? QuarryMaterialDefinition.Pool.PROTECTED_RARE
                        : QuarryMaterialDefinition.Pool.NORMAL
        ));
    }

    private static Optional<QuarryKind> explicitKind(Block block) {
        if (TIER_TAGS.get(QuarryKind.PIGLIN).values().stream().anyMatch(block.defaultBlockState()::is)
                || block.defaultBlockState().is(ROCK_TAGS.get(QuarryKind.PIGLIN))) {
            return Optional.of(QuarryKind.PIGLIN);
        }
        if (TIER_TAGS.get(QuarryKind.VILLAGER).values().stream().anyMatch(block.defaultBlockState()::is)
                || block.defaultBlockState().is(ROCK_TAGS.get(QuarryKind.VILLAGER))) {
            return Optional.of(QuarryKind.VILLAGER);
        }
        return Optional.empty();
    }

    private static Optional<QuarryUpgradeTier> explicitTier(Block block, QuarryKind kind) {
        return TIER_TAGS.get(kind).entrySet().stream()
                .filter(entry -> block.defaultBlockState().is(entry.getValue()))
                .map(Map.Entry::getKey)
                .max(Comparator.comparingInt(Enum::ordinal));
    }

    private static QuarryUpgradeTier automaticTier(Block block) {
        if (block.defaultBlockState().is(Tags.Blocks.ORES_NETHERITE_SCRAP)) {
            return QuarryUpgradeTier.NETHERITE;
        }
        if (block.defaultBlockState().is(Tags.Blocks.ORES_EMERALD)) {
            return QuarryUpgradeTier.NETHERITE;
        }
        if (block.defaultBlockState().is(Tags.Blocks.ORES_DIAMOND)
                || block.defaultBlockState().is(BlockTags.NEEDS_DIAMOND_TOOL)) {
            return QuarryUpgradeTier.DIAMOND;
        }
        if (block.defaultBlockState().is(Tags.Blocks.ORES_GOLD)
                || block.defaultBlockState().is(Tags.Blocks.ORES_LAPIS)
                || block.defaultBlockState().is(Tags.Blocks.ORES_REDSTONE)
                || block.defaultBlockState().is(BlockTags.NEEDS_IRON_TOOL)) {
            return QuarryUpgradeTier.GOLD;
        }
        if (block.defaultBlockState().is(Tags.Blocks.ORES_IRON)) {
            return QuarryUpgradeTier.IRON;
        }
        return QuarryUpgradeTier.COPPER;
    }

    private static double minimumPickaxeSpeed(Block block) {
        if (block.defaultBlockState().is(BlockTags.NEEDS_DIAMOND_TOOL)) {
            return DIAMOND_SPEED;
        }
        if (block.defaultBlockState().is(BlockTags.NEEDS_IRON_TOOL)) {
            return IRON_SPEED;
        }
        if (block.defaultBlockState().is(BlockTags.NEEDS_STONE_TOOL)) {
            return STONE_SPEED;
        }
        return WOOD_SPEED;
    }

    private static Identifier findDeepVariant(Identifier id) {
        Identifier deepId = Identifier.fromNamespaceAndPath(id.getNamespace(), "deepslate_" + id.getPath());
        return BuiltInRegistries.BLOCK.containsKey(deepId) ? deepId : null;
    }

    private static List<QuarryMaterialDefinition> vanillaDefinitions() {
        List<QuarryMaterialDefinition> definitions = new ArrayList<>();
        definitions.add(vanilla("stone", QuarryKind.VILLAGER, QuarryUpgradeTier.NONE, WOOD_SPEED,
                weights(4800, 3800, 3400, 3100, 3100, 2000), 1, 4,
                "minecraft:cobblestone", "minecraft:stone", null, false, true));
        definitions.add(vanilla("granite", QuarryKind.VILLAGER, QuarryUpgradeTier.NONE, WOOD_SPEED,
                weights(1400, 1000, 900, 800, 800, 400), 1, 4,
                "minecraft:granite", "minecraft:granite", null, false, true));
        definitions.add(vanilla("diorite", QuarryKind.VILLAGER, QuarryUpgradeTier.NONE, WOOD_SPEED,
                weights(1400, 1000, 900, 800, 800, 400), 1, 4,
                "minecraft:diorite", "minecraft:diorite", null, false, true));
        definitions.add(vanilla("andesite", QuarryKind.VILLAGER, QuarryUpgradeTier.NONE, WOOD_SPEED,
                weights(1400, 1000, 900, 800, 800, 400), 1, 4,
                "minecraft:andesite", "minecraft:andesite", null, false, true));
        definitions.add(vanilla("tuff", QuarryKind.VILLAGER, QuarryUpgradeTier.NONE, WOOD_SPEED,
                weights(500, 500, 500, 500, 500, 400), 1, 4,
                "minecraft:tuff", "minecraft:tuff", null, false, true));
        definitions.add(vanilla("calcite", QuarryKind.VILLAGER, QuarryUpgradeTier.NONE, WOOD_SPEED,
                weights(300, 200, 200, 200, 200, 200), 1, 3,
                "minecraft:calcite", "minecraft:calcite", null, false, true));
        definitions.add(vanilla("dripstone_block", QuarryKind.VILLAGER, QuarryUpgradeTier.NONE, WOOD_SPEED,
                weights(200, 100, 100, 100, 100, 200), 1, 3,
                "minecraft:dripstone_block", "minecraft:dripstone_block", null, false, true));
        definitions.add(vanillaOre("coal", QuarryUpgradeTier.COPPER, WOOD_SPEED,
                weights(0, 1200, 1000, 900, 800, 1200), 1, 3,
                "minecraft:coal", "minecraft:coal_ore", "minecraft:deepslate_coal_ore"));
        definitions.add(vanillaOre("copper", QuarryUpgradeTier.COPPER, STONE_SPEED,
                weights(0, 1200, 1000, 900, 800, 1200), 2, 5,
                "minecraft:raw_copper", "minecraft:copper_ore", "minecraft:deepslate_copper_ore"));
        definitions.add(vanillaOre("iron", QuarryUpgradeTier.IRON, STONE_SPEED,
                weights(0, 0, 1100, 1000, 900, 1400), 1, 3,
                "minecraft:raw_iron", "minecraft:iron_ore", "minecraft:deepslate_iron_ore"));
        definitions.add(vanillaOre("gold", QuarryUpgradeTier.GOLD, IRON_SPEED,
                weights(0, 0, 0, 300, 300, 600), 1, 2,
                "minecraft:raw_gold", "minecraft:gold_ore", "minecraft:deepslate_gold_ore"));
        definitions.add(vanillaOre("lapis", QuarryUpgradeTier.GOLD, STONE_SPEED,
                weights(0, 0, 0, 300, 300, 500), 4, 9,
                "minecraft:lapis_lazuli", "minecraft:lapis_ore", "minecraft:deepslate_lapis_ore"));
        definitions.add(vanillaOre("redstone", QuarryUpgradeTier.GOLD, IRON_SPEED,
                weights(0, 0, 0, 300, 400, 700), 4, 8,
                "minecraft:redstone", "minecraft:redstone_ore", "minecraft:deepslate_redstone_ore"));
        definitions.add(vanillaOre("diamond", QuarryUpgradeTier.DIAMOND, IRON_SPEED,
                weights(0, 0, 0, 0, 150, 300), 1, 1,
                "minecraft:diamond", "minecraft:diamond_ore", "minecraft:deepslate_diamond_ore"));
        definitions.add(vanillaOre("emerald", QuarryUpgradeTier.NETHERITE, IRON_SPEED,
                weights(0, 0, 0, 0, 0, 100), 1, 1,
                "minecraft:emerald", "minecraft:emerald_ore", "minecraft:deepslate_emerald_ore"));

        definitions.add(vanilla("netherrack", QuarryKind.PIGLIN, QuarryUpgradeTier.NONE, WOOD_SPEED,
                weights(10000, 8500, 6800, 6000, 4800, 4795), 1, 4,
                "minecraft:netherrack", "minecraft:netherrack", null, false, true));
        definitions.add(vanilla("nether_quartz", QuarryKind.PIGLIN, QuarryUpgradeTier.COPPER, WOOD_SPEED,
                weights(0, 1500, 1200, 1200, 2000, 2000), 1, 4,
                "minecraft:quartz", "minecraft:nether_quartz_ore", null, true, false));
        definitions.add(vanilla("basalt", QuarryKind.PIGLIN, QuarryUpgradeTier.IRON, WOOD_SPEED,
                weights(0, 0, 700, 700, 600, 600), 1, 3,
                "minecraft:basalt", "minecraft:basalt", null, false, true));
        definitions.add(vanilla("blackstone", QuarryKind.PIGLIN, QuarryUpgradeTier.IRON, WOOD_SPEED,
                weights(0, 0, 700, 700, 600, 600), 1, 3,
                "minecraft:blackstone", "minecraft:blackstone", null, false, true));
        definitions.add(vanilla("magma_block", QuarryKind.PIGLIN, QuarryUpgradeTier.IRON, WOOD_SPEED,
                weights(0, 0, 600, 600, 700, 700), 1, 1,
                "minecraft:magma_block", "minecraft:magma_block", null, false, true));
        definitions.add(vanilla("nether_gold", QuarryKind.PIGLIN, QuarryUpgradeTier.GOLD, WOOD_SPEED,
                weights(0, 0, 0, 800, 1300, 1300), 2, 6,
                "minecraft:gold_nugget", "minecraft:nether_gold_ore", null, true, false));
        definitions.add(definition(
                "ancient_debris",
                QuarryKind.PIGLIN,
                QuarryUpgradeTier.NETHERITE,
                DIAMOND_SPEED,
                weights(0, 0, 0, 0, 0, 5),
                1,
                1,
                "minecraft:ancient_debris",
                "minecraft:ancient_debris",
                null,
                false,
                false,
                false,
                false,
                MINECRAFT,
                QuarryMaterialDefinition.Pool.PROTECTED_RARE
        ));
        definitions.sort(DEFINITION_ORDER);
        return List.copyOf(definitions);
    }

    private static QuarryMaterialDefinition vanillaOre(
            String id,
            QuarryUpgradeTier tier,
            double speed,
            List<Integer> weights,
            int minimum,
            int maximum,
            String normal,
            String silk,
            String deep
    ) {
        return vanilla(id, QuarryKind.VILLAGER, tier, speed, weights, minimum, maximum,
                normal, silk, deep, true, false);
    }

    private static QuarryMaterialDefinition vanilla(
            String id,
            QuarryKind kind,
            QuarryUpgradeTier tier,
            double speed,
            List<Integer> weights,
            int minimum,
            int maximum,
            String normal,
            String silk,
            String deep,
            boolean fortune,
            boolean rock
    ) {
        return definition(
                id,
                kind,
                tier,
                speed,
                weights,
                minimum,
                maximum,
                normal,
                silk,
                deep,
                fortune,
                rock,
                false,
                false,
                MINECRAFT,
                QuarryMaterialDefinition.Pool.NORMAL
        );
    }

    private static QuarryMaterialDefinition definition(
            String id,
            QuarryKind kind,
            QuarryUpgradeTier tier,
            double speed,
            List<Integer> weights,
            int minimum,
            int maximum,
            String normal,
            String silk,
            String deep,
            boolean fortune,
            boolean rock,
            boolean dynamic,
            boolean blockLoot,
            String sourceMod,
            QuarryMaterialDefinition.Pool pool
    ) {
        return new QuarryMaterialDefinition(
                Identifier.fromNamespaceAndPath(TradingCells.MOD_ID, id),
                kind,
                tier,
                speed,
                weights,
                minimum,
                maximum,
                Identifier.parse(normal),
                Identifier.parse(silk),
                deep == null ? null : Identifier.parse(deep),
                fortune,
                rock,
                dynamic,
                blockLoot,
                sourceMod,
                pool
        );
    }

    private static List<Integer> weights(int none, int copper, int iron, int gold, int diamond, int netherite) {
        return List.of(none, copper, iron, gold, diamond, netherite);
    }

    private static List<Integer> weightsFrom(QuarryUpgradeTier tier, int weight) {
        List<Integer> result = new ArrayList<>(QuarryMaterialDefinition.TIER_COUNT);
        for (QuarryUpgradeTier candidate : QuarryUpgradeTier.values()) {
            result.add(candidate.unlocks(tier) ? weight : 0);
        }
        return List.copyOf(result);
    }

    private static Set<Identifier> vanillaIds() {
        Set<Identifier> ids = new HashSet<>();
        for (QuarryMaterialDefinition definition : VANILLA) {
            ids.add(definition.silkResult());
            if (definition.deepSilkResult() != null) {
                ids.add(definition.deepSilkResult());
            }
        }
        return Set.copyOf(ids);
    }

    private static Map<QuarryKind, Map<QuarryUpgradeTier, TagKey<Block>>> tierTags() {
        Map<QuarryKind, Map<QuarryUpgradeTier, TagKey<Block>>> tags = new EnumMap<>(QuarryKind.class);
        for (QuarryKind kind : QuarryKind.values()) {
            String dimension = kind == QuarryKind.VILLAGER ? "overworld" : "nether";
            Map<QuarryUpgradeTier, TagKey<Block>> tiers = new EnumMap<>(QuarryUpgradeTier.class);
            for (QuarryUpgradeTier tier : QuarryUpgradeTier.values()) {
                if (tier == QuarryUpgradeTier.NONE) {
                    continue;
                }
                tiers.put(tier, ownTag("quarry/" + dimension + "/" + tier.name().toLowerCase() + "_tier"));
            }
            tags.put(kind, Map.copyOf(tiers));
        }
        return Map.copyOf(tags);
    }

    private static TagKey<Block> ownTag(String path) {
        return TagKey.create(
                Registries.BLOCK,
                Identifier.fromNamespaceAndPath(TradingCells.MOD_ID, path)
        );
    }

    private static String key(QuarryMaterialDefinition definition) {
        return definitionKey(definition.kind(), definition.id());
    }

    static String definitionKey(QuarryKind kind, Identifier id) {
        return kind.name() + ':' + id;
    }

    public enum BlockedReason {
        NONE,
        UPGRADE,
        PICKAXE
    }

    public record CatalogEntry(
            QuarryMaterialDefinition definition,
            int weight,
            int probabilityPartsPerMillion,
            BlockedReason blockedReason
    ) {
        public boolean available() {
            return blockedReason == BlockedReason.NONE;
        }
    }

    public record CatalogSnapshot(List<CatalogEntry> entries, boolean deepMining, int revision) {
        public CatalogSnapshot {
            entries = List.copyOf(entries);
        }

        public Optional<CatalogEntry> select(RandomSource random) {
            Optional<CatalogEntry> ancientDebris = entries.stream()
                    .filter(CatalogEntry::available)
                    .filter(entry -> isAncientDebris(entry.definition()))
                    .findFirst();
            List<CatalogEntry> dynamicRare = entries.stream()
                    .filter(CatalogEntry::available)
                    .filter(entry -> entry.definition().pool() == QuarryMaterialDefinition.Pool.PROTECTED_RARE)
                    .filter(entry -> !isAncientDebris(entry.definition()))
                    .filter(entry -> entry.weight() > 0)
                    .toList();
            int protectedRoll = random.nextInt(PROBABILITY_SCALE);
            int dynamicRareStart = 0;
            if (ancientDebris.isPresent()) {
                int ancientProbability = ancientDebris.get().probabilityPartsPerMillion();
                if (protectedRoll < ancientProbability) {
                    return ancientDebris;
                }
                dynamicRareStart = ancientProbability;
            }
            if (!dynamicRare.isEmpty()
                    && protectedRoll >= dynamicRareStart
                    && protectedRoll < dynamicRareStart + dynamicRare.stream()
                            .mapToInt(CatalogEntry::probabilityPartsPerMillion)
                            .sum()) {
                return selectWeighted(dynamicRare, random);
            }

            List<CatalogEntry> normal = entries.stream()
                    .filter(CatalogEntry::available)
                    .filter(entry -> entry.definition().pool() == QuarryMaterialDefinition.Pool.NORMAL)
                    .filter(entry -> entry.weight() > 0)
                    .toList();
            return selectWeighted(normal, random);
        }

        private static Optional<CatalogEntry> selectWeighted(
                List<CatalogEntry> entries,
                RandomSource random
        ) {
            long total = entries.stream().mapToLong(CatalogEntry::weight).sum();
            if (total <= 0L) {
                return Optional.empty();
            }
            long selected = Math.floorMod(random.nextLong(), total);
            for (CatalogEntry entry : entries) {
                selected -= entry.weight();
                if (selected < 0L) {
                    return Optional.of(entry);
                }
            }
            return Optional.of(entries.getLast());
        }
    }

    private static final class WeightedMaterial {
        private final QuarryMaterialDefinition definition;
        private int weight;

        private WeightedMaterial(QuarryMaterialDefinition definition, int weight) {
            this.definition = definition;
            this.weight = Math.max(0, weight);
        }

        private QuarryMaterialDefinition definition() {
            return definition;
        }

        private int weight() {
            return weight;
        }

        private void setWeight(int weight) {
            this.weight = Math.max(0, weight);
        }

        private boolean isDynamicOre() {
            return definition.dynamic()
                    && !definition.rock()
                    && definition.pool() == QuarryMaterialDefinition.Pool.NORMAL;
        }

        private boolean isAdjustableVanillaOre() {
            return !definition.dynamic()
                    && !definition.rock()
                    && definition.pool() == QuarryMaterialDefinition.Pool.NORMAL;
        }
    }
}
