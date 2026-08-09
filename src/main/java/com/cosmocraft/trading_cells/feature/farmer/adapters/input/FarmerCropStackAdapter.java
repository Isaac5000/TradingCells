package com.cosmocraft.trading_cells.feature.farmer.adapters.input;

import com.cosmocraft.trading_cells.feature.farmer.domain.model.FarmerCrop;
import com.cosmocraft.trading_cells.feature.farmer.domain.model.FarmerKind;
import com.cosmocraft.trading_cells.feature.farmer.domain.model.FarmerProduct;
import com.cosmocraft.trading_cells.feature.farmer.domain.model.FarmerYield;
import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.TradingCells;
import com.cosmocraft.trading_cells.platform.neoforge.catalog.SafeDynamicCatalog;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BeetrootBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public final class FarmerCropStackAdapter {
    private static final List<Option> VANILLA_VILLAGER_OPTIONS = List.of(
            new Option(Items.WHEAT_SEEDS, Blocks.WHEAT, FarmerCrop.WHEAT),
            new Option(Items.CARROT, Blocks.CARROTS, FarmerCrop.CARROT),
            new Option(Items.POTATO, Blocks.POTATOES, FarmerCrop.POTATO),
            new Option(Items.BEETROOT_SEEDS, Blocks.BEETROOTS, FarmerCrop.BEETROOT),
            new Option(Items.TORCHFLOWER_SEEDS, Blocks.TORCHFLOWER_CROP, FarmerCrop.NONE),
            new Option(Items.PITCHER_POD, Blocks.PITCHER_CROP, FarmerCrop.NONE)
    );
    private static final Catalog VANILLA_VILLAGER_CATALOG = Catalog.create(VANILLA_VILLAGER_OPTIONS);
    private static final AtomicReference<Catalog> VILLAGER_CATALOG = new AtomicReference<>();
    private static final Set<Item> REPORTED_HARVEST_FAILURES = ConcurrentHashMap.newKeySet();

    private FarmerCropStackAdapter() {
    }

    public static FarmerCrop from(FarmerKind kind, ItemStack stack) {
        FarmerCrop crop = switch (kind) {
            case VILLAGER -> villagerCrop(stack);
            case PIGLIN -> piglinCrop(stack);
        };
        return crop.isSupportedBy(kind) ? crop : FarmerCrop.NONE;
    }

    public static boolean isSupported(FarmerKind kind, ItemStack stack) {
        if (kind == FarmerKind.VILLAGER) {
            return villagerOption(stack) != null;
        }
        return from(kind, stack) != FarmerCrop.NONE;
    }

    public static boolean isDynamicVillagerCrop(ItemStack stack) {
        Option option = villagerOption(stack);
        return option != null && option.crop() == FarmerCrop.NONE;
    }

    public static List<Option> villagerOptions() {
        return villagerCatalog().options();
    }

    public static ItemStack previewOutput(Option option) {
        if (option.item() == Items.PITCHER_POD) {
            return new ItemStack(Items.PITCHER_PLANT);
        }
        return new ItemStack(option.item());
    }

    public static ItemStack input(FarmerCrop crop) {
        return new ItemStack(switch (crop) {
            case WHEAT -> Items.WHEAT_SEEDS;
            case CARROT -> Items.CARROT;
            case POTATO -> Items.POTATO;
            case BEETROOT -> Items.BEETROOT_SEEDS;
            case CRIMSON_FUNGUS -> Items.CRIMSON_FUNGUS;
            case WARPED_FUNGUS -> Items.WARPED_FUNGUS;
            case CRIMSON_ROOTS -> Items.CRIMSON_ROOTS;
            case NETHER_WART -> Items.NETHER_WART;
            case WEEPING_VINES -> Items.WEEPING_VINES;
            case NETHER_SPROUTS -> Items.NETHER_SPROUTS;
            case WARPED_ROOTS -> Items.WARPED_ROOTS;
            case TWISTING_VINES -> Items.TWISTING_VINES;
            case NONE -> Items.AIR;
        });
    }

    public static ItemStack output(FarmerYield yield) {
        return output(yield.product(), yield.count());
    }

    public static ItemStack output(FarmerProduct product, int count) {
        return new ItemStack(switch (product) {
            case WHEAT -> Items.WHEAT;
            case WHEAT_SEEDS -> Items.WHEAT_SEEDS;
            case CARROT -> Items.CARROT;
            case POTATO -> Items.POTATO;
            case BEETROOT -> Items.BEETROOT;
            case BEETROOT_SEEDS -> Items.BEETROOT_SEEDS;
            case CRIMSON_FUNGUS -> Items.CRIMSON_FUNGUS;
            case WARPED_FUNGUS -> Items.WARPED_FUNGUS;
            case CRIMSON_ROOTS -> Items.CRIMSON_ROOTS;
            case NETHER_WART -> Items.NETHER_WART;
            case WEEPING_VINES -> Items.WEEPING_VINES;
            case NETHER_WART_BLOCK -> Items.NETHER_WART_BLOCK;
            case CRIMSON_STEM -> Items.CRIMSON_STEM;
            case NETHER_SPROUTS -> Items.NETHER_SPROUTS;
            case WARPED_ROOTS -> Items.WARPED_ROOTS;
            case TWISTING_VINES -> Items.TWISTING_VINES;
            case WARPED_WART_BLOCK -> Items.WARPED_WART_BLOCK;
            case WARPED_STEM -> Items.WARPED_STEM;
            case SHROOMLIGHT -> Items.SHROOMLIGHT;
        }, count);
    }

    public static BlockState cropState(FarmerCrop crop, int growthTicks, int maxGrowthTicks) {
        int duration = Math.max(0, maxGrowthTicks);
        int clampedTicks = Math.clamp(growthTicks, 0, duration);
        return switch (crop) {
            case WHEAT -> Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, stage(clampedTicks, duration, 7));
            case CARROT -> Blocks.CARROTS.defaultBlockState().setValue(CropBlock.AGE, stage(clampedTicks, duration, 7));
            case POTATO -> Blocks.POTATOES.defaultBlockState().setValue(CropBlock.AGE, stage(clampedTicks, duration, 7));
            case BEETROOT -> Blocks.BEETROOTS.defaultBlockState().setValue(BeetrootBlock.AGE, stage(clampedTicks, duration, 3));
            case CRIMSON_FUNGUS -> Blocks.CRIMSON_FUNGUS.defaultBlockState();
            case WARPED_FUNGUS -> Blocks.WARPED_FUNGUS.defaultBlockState();
            case CRIMSON_ROOTS -> Blocks.CRIMSON_ROOTS.defaultBlockState();
            case NETHER_WART -> Blocks.NETHER_WART.defaultBlockState().setValue(
                    NetherWartBlock.AGE,
                    stage(clampedTicks, duration, 3)
            );
            case WEEPING_VINES -> Blocks.WEEPING_VINES.defaultBlockState();
            case NETHER_SPROUTS -> Blocks.NETHER_SPROUTS.defaultBlockState();
            case WARPED_ROOTS -> Blocks.WARPED_ROOTS.defaultBlockState();
            case TWISTING_VINES -> Blocks.TWISTING_VINES.defaultBlockState();
            case NONE -> Blocks.AIR.defaultBlockState();
        };
    }

    public static BlockState cropState(
            FarmerKind kind,
            ItemStack cropStack,
            int growthTicks,
            int maxGrowthTicks
    ) {
        FarmerCrop crop = from(kind, cropStack);
        if (crop != FarmerCrop.NONE) {
            return cropState(crop, growthTicks, maxGrowthTicks);
        }
        Option option = kind == FarmerKind.VILLAGER ? villagerOption(cropStack) : null;
        return option == null
                ? Blocks.AIR.defaultBlockState()
                : stateAtProgress(option.block(), growthTicks, maxGrowthTicks);
    }

    public static List<ItemStack> dynamicVillagerHarvest(
            ServerLevel level,
            BlockPos pos,
            ItemStack cropStack,
            ItemStack hoe,
            int fortuneLevel
    ) {
        Option option = villagerOption(cropStack);
        if (option == null || option.crop() != FarmerCrop.NONE) {
            return List.of();
        }
        try {
            List<ItemStack> drops = Block.getDrops(
                    matureState(option.block()),
                    level,
                    pos,
                    null,
                    null,
                    hoe
            ).stream().filter(stack -> !stack.isEmpty()).map(ItemStack::copy).toList();
            return drops.isEmpty() ? fallbackDynamicHarvest(cropStack, fortuneLevel) : drops;
        } catch (RuntimeException exception) {
            if (REPORTED_HARVEST_FAILURES.add(option.item())) {
                TradingCells.LOGGER.warn(
                        "No se pudo obtener el botín del cultivo dinámico {}; se usará su semilla como salida.",
                        BuiltInRegistries.ITEM.getKey(option.item()),
                        exception
                );
            }
            return fallbackDynamicHarvest(cropStack, fortuneLevel);
        }
    }

    public static BlockState soilState(FarmerKind kind, FarmerCrop crop) {
        if (kind == FarmerKind.VILLAGER) {
            return Blocks.FARMLAND.defaultBlockState().setValue(
                    FarmlandBlock.MOISTURE,
                    FarmlandBlock.MAX_MOISTURE
            );
        }
        return switch (crop) {
            case WARPED_FUNGUS, NETHER_SPROUTS, WARPED_ROOTS, TWISTING_VINES ->
                    Blocks.WARPED_NYLIUM.defaultBlockState();
            case NETHER_WART -> Blocks.SOUL_SAND.defaultBlockState();
            default -> Blocks.CRIMSON_NYLIUM.defaultBlockState();
        };
    }

    private static FarmerCrop villagerCrop(ItemStack stack) {
        if (stack.is(Items.WHEAT_SEEDS)) {
            return FarmerCrop.WHEAT;
        }
        if (stack.is(Items.CARROT)) {
            return FarmerCrop.CARROT;
        }
        if (stack.is(Items.POTATO)) {
            return FarmerCrop.POTATO;
        }
        return stack.is(Items.BEETROOT_SEEDS) ? FarmerCrop.BEETROOT : FarmerCrop.NONE;
    }

    private static Catalog villagerCatalog() {
        Catalog cached = VILLAGER_CATALOG.get();
        if (cached != null) {
            return cached;
        }
        Catalog discovered = discoverVillagerCatalog();
        VILLAGER_CATALOG.compareAndSet(null, discovered);
        return VILLAGER_CATALOG.get();
    }

    private static Catalog discoverVillagerCatalog() {
        try {
            Set<Item> knownItems = new HashSet<>();
            VANILLA_VILLAGER_OPTIONS.forEach(option -> knownItems.add(option.item()));
            List<Option> additional = SafeDynamicCatalog.discover(
                    "villager crops",
                    () -> BuiltInRegistries.ITEM,
                    item -> dynamicVillagerOption(item, knownItems),
                    Comparator.comparing(option -> BuiltInRegistries.ITEM.getKey(option.item()).toString()),
                    item -> BuiltInRegistries.ITEM.getKey(item).toString()
            );
            if (additional.isEmpty()) {
                return VANILLA_VILLAGER_CATALOG;
            }
            ArrayList<Option> combined = new ArrayList<>(VANILLA_VILLAGER_OPTIONS);
            combined.addAll(additional);
            return Catalog.create(combined);
        } catch (RuntimeException | LinkageError exception) {
            TradingCells.LOGGER.warn(
                    "No se pudo ampliar la lista de cultivos de aldeano; se usará la lista vanilla.",
                    exception
            );
            return VANILLA_VILLAGER_CATALOG;
        }
    }

    private static Optional<Option> dynamicVillagerOption(Item item, Set<Item> knownItems) {
        if (knownItems.contains(item) || !(item instanceof BlockItem blockItem)) {
            return Optional.empty();
        }
        ItemStack stack = new ItemStack(item);
        if (!stack.is(ItemTags.VILLAGER_PLANTABLE_SEEDS)) {
            return Optional.empty();
        }
        Block block = blockItem.getBlock();
        block.defaultBlockState();
        if (BuiltInRegistries.ITEM.getKey(item) == null) {
            throw new IllegalArgumentException("Villager crop has no registered item identifier");
        }
        return Optional.of(new Option(item, block, FarmerCrop.NONE));
    }

    private static Option villagerOption(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        return villagerCatalog().byItem().get(stack.getItem());
    }

    private static BlockState stateAtProgress(Block block, int growthTicks, int maxGrowthTicks) {
        BlockState state = block.defaultBlockState();
        IntegerProperty age = ageProperty(state);
        if (age == null) {
            return state;
        }
        int maximumAge = age.getPossibleValues().getLast();
        int currentAge = stage(Math.max(0, growthTicks), Math.max(0, maxGrowthTicks), maximumAge);
        return state.setValue(age, currentAge);
    }

    private static BlockState matureState(Block block) {
        BlockState state = block.defaultBlockState();
        IntegerProperty age = ageProperty(state);
        return age == null ? state : state.setValue(age, age.getPossibleValues().getLast());
    }

    private static IntegerProperty ageProperty(BlockState state) {
        return state.getProperties().stream()
                .filter(property -> property instanceof IntegerProperty)
                .map(property -> (IntegerProperty) property)
                .filter(property -> "age".equals(property.getName()))
                .findFirst()
                .orElse(null);
    }

    private static List<ItemStack> fallbackDynamicHarvest(ItemStack cropStack, int fortuneLevel) {
        ItemStack result = cropStack.copyWithCount(Math.max(1, 2 + fortuneLevel));
        return List.of(result);
    }

    private static FarmerCrop piglinCrop(ItemStack stack) {
        if (stack.is(Items.CRIMSON_FUNGUS)) {
            return FarmerCrop.CRIMSON_FUNGUS;
        }
        if (stack.is(Items.WARPED_FUNGUS)) {
            return FarmerCrop.WARPED_FUNGUS;
        }
        if (stack.is(Items.CRIMSON_ROOTS)) {
            return FarmerCrop.CRIMSON_ROOTS;
        }
        if (stack.is(Items.NETHER_WART)) {
            return FarmerCrop.NETHER_WART;
        }
        if (stack.is(Items.WEEPING_VINES)) {
            return FarmerCrop.WEEPING_VINES;
        }
        if (stack.is(Items.NETHER_SPROUTS)) {
            return FarmerCrop.NETHER_SPROUTS;
        }
        if (stack.is(Items.WARPED_ROOTS)) {
            return FarmerCrop.WARPED_ROOTS;
        }
        return stack.is(Items.TWISTING_VINES) ? FarmerCrop.TWISTING_VINES : FarmerCrop.NONE;
    }

    private static int stage(int ticks, int maxTicks, int maxStage) {
        return maxTicks <= 0 ? 0 : Math.min(maxStage, ticks * (maxStage + 1) / maxTicks);
    }

    public record Option(Item item, Block block, FarmerCrop crop) {
    }

    private record Catalog(List<Option> options, Map<Item, Option> byItem) {
        private static Catalog create(List<Option> options) {
            List<Option> immutableOptions = List.copyOf(options);
            Map<Item, Option> byItem = new HashMap<>();
            for (Option option : immutableOptions) {
                if (byItem.put(option.item(), option) != null) {
                    throw new IllegalArgumentException("Duplicate villager crop item");
                }
            }
            return new Catalog(immutableOptions, Map.copyOf(byItem));
        }
    }
}
