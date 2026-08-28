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
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BeetrootBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class FarmerCropStackAdapter {
    private static final TagKey<Item> VILLAGER_FARMER_PLANTS = TagKey.create(
            Registries.ITEM,
            Identifier.fromNamespaceAndPath(TradingCells.MOD_ID, "villager_farmer_plants")
    );
    private static final TagKey<Item> FARMER_WALL_PLANTS = farmerPlantTag("farmer_wall_plants");
    private static final TagKey<Item> FARMER_WATER_PLANTS = farmerPlantTag("farmer_water_plants");
    private static final TagKey<Item> FARMER_CEILING_PLANTS = farmerPlantTag("farmer_ceiling_plants");
    private static final TagKey<Item> FARMER_JUNGLE_SUPPORT = farmerPlantTag("farmer_jungle_support");
    private static final TagKey<Item> FARMER_ROOTED_DIRT_SUPPORT = farmerPlantTag("farmer_rooted_dirt_support");
    private static final TagKey<Item> FARMER_PALE_MOSS_SUPPORT = farmerPlantTag("farmer_pale_moss_support");
    private static final int VISUAL_GROWTH_STAGES = 8;
    private static final int PIGLIN_VISUAL_GROWTH_STAGES = 4;
    private static final List<Option> BASE_VILLAGER_OPTIONS = List.of(
            new Option(Items.WHEAT_SEEDS, Blocks.WHEAT, FarmerCrop.WHEAT),
            new Option(Items.CARROT, Blocks.CARROTS, FarmerCrop.CARROT),
            new Option(Items.POTATO, Blocks.POTATOES, FarmerCrop.POTATO),
            new Option(Items.BEETROOT_SEEDS, Blocks.BEETROOTS, FarmerCrop.BEETROOT),
            new Option(Items.PUMPKIN_SEEDS, Blocks.PUMPKIN_STEM, FarmerCrop.PUMPKIN),
            new Option(Items.MELON_SEEDS, Blocks.MELON_STEM, FarmerCrop.MELON),
            new Option(Items.SUGAR_CANE, Blocks.SUGAR_CANE, FarmerCrop.SUGAR_CANE),
            new Option(Items.COCOA_BEANS, Blocks.COCOA, FarmerCrop.COCOA),
            new Option(Items.TORCHFLOWER_SEEDS, Blocks.TORCHFLOWER_CROP, FarmerCrop.TORCHFLOWER),
            new Option(Items.PITCHER_POD, Blocks.PITCHER_CROP, FarmerCrop.PITCHER_PLANT)
    );
    private static final List<Option> VANILLA_VILLAGER_OPTIONS = createVanillaVillagerOptions();
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

    public static List<PreviewYield> previewYields(Option option) {
        if (option.harvestRules().isEmpty()) {
            return List.of();
        }
        return option.harvestRules().stream()
                .map(rule -> new PreviewYield(
                        new ItemStack(rule.item(), rule.count(0)),
                        rule.chance(0),
                        rule.requirement() == ToolRequirement.SILK_TOUCH
                ))
                .toList();
    }

    public static float visualGrowthScale(
            FarmerKind kind,
            ItemStack cropStack,
            int growthTicks,
            int maxGrowthTicks
    ) {
        FarmerCrop crop = from(kind, cropStack);
        if (crop == FarmerCrop.PUMPKIN || crop == FarmerCrop.MELON) {
            return continuousGrowthScale(growthTicks, maxGrowthTicks);
        }
        if (kind == FarmerKind.PIGLIN && crop != FarmerCrop.NONE && crop != FarmerCrop.NETHER_WART) {
            return stagedGrowthScale(
                    growthTicks,
                    maxGrowthTicks,
                    PIGLIN_VISUAL_GROWTH_STAGES
            );
        }
        Option option = kind == FarmerKind.VILLAGER ? villagerOption(cropStack) : null;
        if (option == null || option.growthStyle() != GrowthStyle.SCALED) {
            return 1.0F;
        }
        return stagedGrowthScale(growthTicks, maxGrowthTicks, VISUAL_GROWTH_STAGES);
    }

    public static ItemStack input(FarmerCrop crop) {
        return new ItemStack(switch (crop) {
            case WHEAT -> Items.WHEAT_SEEDS;
            case CARROT -> Items.CARROT;
            case POTATO -> Items.POTATO;
            case BEETROOT -> Items.BEETROOT_SEEDS;
            case PUMPKIN -> Items.PUMPKIN_SEEDS;
            case MELON -> Items.MELON_SEEDS;
            case SUGAR_CANE -> Items.SUGAR_CANE;
            case COCOA -> Items.COCOA_BEANS;
            case TORCHFLOWER -> Items.TORCHFLOWER_SEEDS;
            case PITCHER_PLANT -> Items.PITCHER_POD;
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
            case PUMPKIN -> Items.PUMPKIN;
            case MELON -> Items.MELON;
            case MELON_SLICE -> Items.MELON_SLICE;
            case SUGAR_CANE -> Items.SUGAR_CANE;
            case COCOA_BEANS -> Items.COCOA_BEANS;
            case TORCHFLOWER -> Items.TORCHFLOWER;
            case TORCHFLOWER_SEEDS -> Items.TORCHFLOWER_SEEDS;
            case PITCHER_PLANT -> Items.PITCHER_PLANT;
            case PITCHER_POD -> Items.PITCHER_POD;
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
            case PUMPKIN -> Blocks.PUMPKIN.defaultBlockState();
            case MELON -> Blocks.MELON.defaultBlockState();
            case SUGAR_CANE -> Blocks.SUGAR_CANE.defaultBlockState();
            case COCOA -> Blocks.COCOA.defaultBlockState().setValue(
                    CocoaBlock.AGE,
                    stage(clampedTicks, duration, 2)
            );
            case TORCHFLOWER -> stateAtProgress(Blocks.TORCHFLOWER_CROP, clampedTicks, duration);
            case PITCHER_PLANT -> stateAtProgress(Blocks.PITCHER_CROP, clampedTicks, duration);
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
            int fortuneLevel,
            boolean silkTouch
    ) {
        Option option = villagerOption(cropStack);
        if (option == null || option.crop() != FarmerCrop.NONE) {
            return List.of();
        }
        if (!option.harvestRules().isEmpty()) {
            return customVillagerHarvest(level, option, fortuneLevel, silkTouch);
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

    public static BlockState soilState(FarmerKind kind, ItemStack cropStack) {
        FarmerCrop crop = from(kind, cropStack);
        if (kind == FarmerKind.VILLAGER && crop == FarmerCrop.NONE) {
            Option option = villagerOption(cropStack);
            if (option != null) {
                return option.soil().state();
            }
        }
        return soilState(kind, crop);
    }

    public static RenderSupport renderSupport(FarmerKind kind, ItemStack cropStack) {
        if (cropStack.isEmpty()) {
            return RenderSupport.FLOOR;
        }
        if (cropStack.is(FARMER_WATER_PLANTS) || hasFluidMedium(kind, cropStack)) {
            return RenderSupport.WATER;
        }
        if (cropStack.is(FARMER_WALL_PLANTS)) {
            return RenderSupport.WALL;
        }
        if (cropStack.is(FARMER_CEILING_PLANTS)) {
            return RenderSupport.CEILING;
        }
        Option option = kind == FarmerKind.VILLAGER ? villagerOption(cropStack) : null;
        if (option != null && option.wallSupported()) {
            return RenderSupport.WALL;
        }
        String path = BuiltInRegistries.ITEM.getKey(cropStack.getItem()).getPath();
        if (path.endsWith("_coral_block")
                || path.endsWith("_coral")
                || path.endsWith("_coral_fan")
                || path.equals("kelp")
                || path.equals("seagrass")
                || path.equals("sea_pickle")
                || path.equals("lily_pad")
                || path.equals("small_dripleaf")) {
            return RenderSupport.WATER;
        }
        if (path.equals("vine") || path.equals("glow_lichen")) {
            return RenderSupport.WALL;
        }
        if (path.equals("glow_berries")
                || path.equals("hanging_roots")
                || path.equals("pale_hanging_moss")
                || path.equals("spore_blossom")) {
            return RenderSupport.CEILING;
        }
        return RenderSupport.FLOOR;
    }

    private static boolean inferWallSupport(Block block) {
        BlockState state = block.defaultBlockState();
        if (block instanceof VineBlock || block instanceof MultifaceBlock) {
            return true;
        }

        Direction facing;
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        } else if (state.hasProperty(BlockStateProperties.FACING)) {
            facing = state.getValue(BlockStateProperties.FACING);
        } else {
            return false;
        }
        if (facing.getAxis() == Direction.Axis.Y) {
            return false;
        }

        try {
            VoxelShape shape = state.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
            if (shape.isEmpty()) {
                return true;
            }
            Direction.Axis axis = facing.getAxis();
            return shape.max(axis) - shape.min(axis) < 0.75D;
        } catch (RuntimeException | LinkageError ignored) {
            return true;
        }
    }

    public static BlockState renderSupportState(
            FarmerKind kind,
            ItemStack cropStack,
            RenderSupport support
    ) {
        if (support == RenderSupport.WALL) {
            return cropStack.is(FARMER_JUNGLE_SUPPORT)
                    ? Blocks.JUNGLE_LOG.defaultBlockState()
                    : Blocks.OAK_LOG.defaultBlockState();
        }
        if (support == RenderSupport.CEILING) {
            if (kind == FarmerKind.PIGLIN) {
                return Blocks.CRIMSON_NYLIUM.defaultBlockState();
            }
            if (cropStack.is(FARMER_ROOTED_DIRT_SUPPORT)) {
                return Blocks.ROOTED_DIRT.defaultBlockState();
            }
            return cropStack.is(FARMER_PALE_MOSS_SUPPORT)
                    ? Blocks.PALE_MOSS_BLOCK.defaultBlockState()
                    : Blocks.OAK_LOG.defaultBlockState();
        }
        return soilState(kind, cropStack);
    }

    public static BlockState renderFluidVisualState(
            FarmerKind kind,
            ItemStack cropStack,
            RenderSupport support
    ) {
        if (support != RenderSupport.WATER) {
            return Blocks.AIR.defaultBlockState();
        }
        FluidState fluidState = cropFluidState(kind, cropStack);
        if (fluidState.isEmpty()) {
            fluidState = Fluids.WATER.defaultFluidState();
        }
        if (fluidState.is(FluidTags.WATER)) {
            return Blocks.STAINED_GLASS.pick(DyeColor.LIGHT_BLUE).defaultBlockState();
        }
        if (fluidState.is(FluidTags.LAVA)) {
            return Blocks.STAINED_GLASS.pick(DyeColor.ORANGE).defaultBlockState();
        }
        DyeColor nearestColor = nearestDyeColor(
                fluidState.createLegacyBlock().getBlock().defaultMapColor().col
        );
        return Blocks.STAINED_GLASS.pick(nearestColor).defaultBlockState();
    }

    private static boolean hasFluidMedium(FarmerKind kind, ItemStack cropStack) {
        return !cropFluidState(kind, cropStack).isEmpty();
    }

    private static FluidState cropFluidState(FarmerKind kind, ItemStack cropStack) {
        FarmerCrop crop = from(kind, cropStack);
        if (crop != FarmerCrop.NONE) {
            return cropState(crop, 0, 1).getFluidState();
        }
        Option option = kind == FarmerKind.VILLAGER ? villagerOption(cropStack) : null;
        return option == null
                ? Fluids.EMPTY.defaultFluidState()
                : option.block().defaultBlockState().getFluidState();
    }

    private static DyeColor nearestDyeColor(int rgb) {
        int red = rgb >> 16 & 255;
        int green = rgb >> 8 & 255;
        int blue = rgb & 255;
        DyeColor nearest = DyeColor.LIGHT_BLUE;
        int nearestDistance = Integer.MAX_VALUE;
        for (DyeColor color : DyeColor.values()) {
            int candidate = color.getTextureDiffuseColor();
            int redDifference = red - (candidate >> 16 & 255);
            int greenDifference = green - (candidate >> 8 & 255);
            int blueDifference = blue - (candidate & 255);
            int distance = redDifference * redDifference
                    + greenDifference * greenDifference
                    + blueDifference * blueDifference;
            if (distance < nearestDistance) {
                nearest = color;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    private static TagKey<Item> farmerPlantTag(String path) {
        return TagKey.create(
                Registries.ITEM,
                Identifier.fromNamespaceAndPath(TradingCells.MOD_ID, path)
        );
    }

    public static BlockState soilState(FarmerKind kind, FarmerCrop crop) {
        if (kind == FarmerKind.VILLAGER) {
            if (crop == FarmerCrop.SUGAR_CANE) {
                return Blocks.SAND.defaultBlockState();
            }
            if (crop == FarmerCrop.COCOA) {
                return Blocks.JUNGLE_LOG.defaultBlockState();
            }
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

    private static List<ItemStack> customVillagerHarvest(
            ServerLevel level,
            Option option,
            int fortuneLevel,
            boolean silkTouch
    ) {
        int fortune = Math.max(0, fortuneLevel);
        ArrayList<ItemStack> drops = new ArrayList<>(option.harvestRules().size());
        for (HarvestRule rule : option.harvestRules()) {
            if (!rule.requirement().allows(silkTouch)) {
                continue;
            }
            int chance = rule.chance(fortune);
            if (chance == FarmerYield.CHANCE_SCALE
                    || level.getRandom().nextInt(FarmerYield.CHANCE_SCALE) < chance) {
                drops.add(new ItemStack(rule.item(), rule.count(fortune)));
            }
        }
        return List.copyOf(drops);
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
        if (stack.is(Items.BEETROOT_SEEDS)) {
            return FarmerCrop.BEETROOT;
        }
        if (stack.is(Items.PUMPKIN_SEEDS)) {
            return FarmerCrop.PUMPKIN;
        }
        if (stack.is(Items.MELON_SEEDS)) {
            return FarmerCrop.MELON;
        }
        if (stack.is(Items.SUGAR_CANE)) {
            return FarmerCrop.SUGAR_CANE;
        }
        if (stack.is(Items.COCOA_BEANS)) {
            return FarmerCrop.COCOA;
        }
        if (stack.is(Items.TORCHFLOWER_SEEDS)) {
            return FarmerCrop.TORCHFLOWER;
        }
        return stack.is(Items.PITCHER_POD) ? FarmerCrop.PITCHER_PLANT : FarmerCrop.NONE;
    }

    private static List<Option> createVanillaVillagerOptions() {
        ArrayList<Option> options = new ArrayList<>(BASE_VILLAGER_OPTIONS);
        addPoisonousPotato(options);

        addTree(options, "oak_sapling", "oak_log", "oak_leaves", true);
        addTree(options, "spruce_sapling", "spruce_log", "spruce_leaves", false);
        addTree(options, "birch_sapling", "birch_log", "birch_leaves", false);
        addTree(options, "jungle_sapling", "jungle_log", "jungle_leaves", false);
        addTree(options, "acacia_sapling", "acacia_log", "acacia_leaves", false);
        addTree(options, "dark_oak_sapling", "dark_oak_log", "dark_oak_leaves", true);
        addTree(options, "cherry_sapling", "cherry_log", "cherry_leaves", false);
        addTree(options, "pale_oak_sapling", "pale_oak_log", "pale_oak_leaves", false);
        addTree(
                options,
                "mangrove_propagule",
                "mangrove_log",
                "mangrove_leaves",
                false,
                "mangrove_roots"
        );
        addTree(options, "azalea", "oak_log", "azalea_leaves", false);
        addTree(options, "flowering_azalea", "oak_log", "flowering_azalea_leaves", false);

        addMushroom(options, "brown_mushroom", "brown_mushroom_block");
        addMushroom(options, "red_mushroom", "red_mushroom_block");
        addCactus(options);
        addMoss(options, false);
        addMoss(options, true);
        addCoral(options, "tube");
        addCoral(options, "brain");
        addCoral(options, "bubble");
        addCoral(options, "fire");
        addCoral(options, "horn");
        addChorus(options);

        addSimplePlants(options, SoilKind.DIRT,
                "bamboo",
                "vine",
                "glow_berries:cave_vines",
                "sweet_berries:sweet_berry_bush",
                "small_dripleaf",
                "big_dripleaf",
                "hanging_roots",
                "lily_pad"
        );
        addSimplePlants(options, SoilKind.SAND,
                "kelp",
                "seagrass",
                "sea_pickle"
        );
        addSimplePlants(options, SoilKind.STONE,
                "glow_lichen"
        );
        addSimplePlants(options, SoilKind.GRASS,
                "short_grass",
                "fern",
                "bush",
                "firefly_bush",
                "dead_bush",
                "leaf_litter",
                "pale_hanging_moss",
                "spore_blossom",
                "pink_petals",
                "wildflowers",
                "short_dry_grass",
                "tall_dry_grass"
        );
        addSimplePlants(options, SoilKind.GRASS,
                "tall_grass",
                "large_fern",
                "sunflower",
                "lilac",
                "rose_bush",
                "peony"
        );
        addSimplePlants(options, SoilKind.GRASS,
                "dandelion",
                "golden_dandelion",
                "poppy",
                "blue_orchid",
                "allium",
                "azure_bluet",
                "red_tulip",
                "orange_tulip",
                "white_tulip",
                "pink_tulip",
                "oxeye_daisy",
                "cornflower",
                "lily_of_the_valley",
                "wither_rose",
                "closed_eyeblossom",
                "open_eyeblossom"
        );
        return List.copyOf(options);
    }

    private static void addPoisonousPotato(List<Option> options) {
        options.add(new Option(
                Items.POISONOUS_POTATO,
                Blocks.POTATOES,
                FarmerCrop.NONE,
                SoilKind.FARMLAND,
                GrowthStyle.NATURAL,
                List.of(new HarvestRule(
                        Items.POISONOUS_POTATO,
                        1,
                        1,
                        FarmerYield.CHANCE_SCALE,
                        0,
                        FarmerYield.CHANCE_SCALE
                ))
        ));
    }

    private static void addTree(
            List<Option> options,
            String saplingPath,
            String logPath,
            String leavesPath,
            boolean dropsApples,
            String... additionalDrops
    ) {
        Item sapling = registeredItem(saplingPath);
        Block saplingBlock = registeredBlock(saplingPath);
        if (sapling == null || saplingBlock == null) {
            return;
        }
        ArrayList<HarvestRule> rules = new ArrayList<>(4 + additionalDrops.length);
        addRule(rules, logPath, 4, 1, FarmerYield.CHANCE_SCALE, 0, FarmerYield.CHANCE_SCALE);
        addRule(rules, leavesPath, 2, 1, 7_500, 0, 7_500);
        addRule(rules, saplingPath, 1, 0, 3_500, 500, 8_500);
        if (dropsApples) {
            addRule(rules, "apple", 1, 0, 1_000, 250, 4_000);
        }
        for (String additionalDrop : additionalDrops) {
            addRule(
                    rules,
                    additionalDrop,
                    2,
                    1,
                    FarmerYield.CHANCE_SCALE,
                    0,
                    FarmerYield.CHANCE_SCALE
            );
        }
        options.add(new Option(
                sapling,
                saplingBlock,
                FarmerCrop.NONE,
                SoilKind.DIRT,
                GrowthStyle.SCALED,
                rules
        ));
    }

    private static void addMushroom(List<Option> options, String mushroomPath, String blockPath) {
        Item mushroom = registeredItem(mushroomPath);
        Block mushroomBlock = registeredBlock(mushroomPath);
        if (mushroom == null || mushroomBlock == null) {
            return;
        }
        ArrayList<HarvestRule> rules = new ArrayList<>(3);
        addSilkTouchRule(
                rules,
                "mushroom_stem",
                2,
                1,
                6_500,
                500,
                9_000
        );
        addSilkTouchRule(rules, blockPath, 2, 1, 6_500, 500, 9_000);
        addRule(
                rules,
                mushroomPath,
                1,
                1,
                FarmerYield.CHANCE_SCALE,
                0,
                FarmerYield.CHANCE_SCALE
        );
        options.add(new Option(
                mushroom,
                mushroomBlock,
                FarmerCrop.NONE,
                SoilKind.PODZOL,
                GrowthStyle.SCALED,
                rules
        ));
    }

    private static void addCactus(List<Option> options) {
        Item cactus = registeredItem("cactus");
        Block cactusBlock = registeredBlock("cactus");
        if (cactus == null || cactusBlock == null) {
            return;
        }
        ArrayList<HarvestRule> rules = new ArrayList<>(2);
        addRule(rules, "cactus", 2, 1, FarmerYield.CHANCE_SCALE, 0, FarmerYield.CHANCE_SCALE);
        addRule(rules, "cactus_flower", 1, 0, 2_500, 500, 7_500);
        options.add(new Option(
                cactus,
                cactusBlock,
                FarmerCrop.NONE,
                SoilKind.SAND,
                GrowthStyle.SCALED,
                rules
        ));
    }

    private static void addMoss(List<Option> options, boolean pale) {
        String mossPath = pale ? "pale_moss_block" : "moss_block";
        Item moss = registeredItem(mossPath);
        Block mossBlock = registeredBlock(mossPath);
        if (moss == null || mossBlock == null) {
            return;
        }
        ArrayList<HarvestRule> rules = new ArrayList<>(1);
        addRule(rules, mossPath, 1, 1, FarmerYield.CHANCE_SCALE, 0, FarmerYield.CHANCE_SCALE);
        options.add(new Option(
                moss,
                mossBlock,
                FarmerCrop.NONE,
                SoilKind.GRASS,
                GrowthStyle.SCALED,
                rules
        ));
        addSimplePlants(options, SoilKind.GRASS, pale ? "pale_moss_carpet" : "moss_carpet");
    }

    private static void addCoral(List<Option> options, String color) {
        String coralPath = color + "_coral";
        String coralBlockPath = coralPath + "_block";
        addSimplePlants(
                options,
                SoilKind.SAND,
                coralBlockPath,
                coralPath,
                coralPath + "_fan"
        );
    }

    private static void addChorus(List<Option> options) {
        Item chorusFlower = registeredItem("chorus_flower");
        Block chorusFlowerBlock = registeredBlock("chorus_flower");
        if (chorusFlower == null || chorusFlowerBlock == null) {
            return;
        }
        ArrayList<HarvestRule> rules = new ArrayList<>(2);
        addRule(rules, "chorus_fruit", 2, 1, 8_000, 250, FarmerYield.CHANCE_SCALE);
        addRule(rules, "chorus_flower", 1, 0, 3_000, 500, 8_500);
        options.add(new Option(
                chorusFlower,
                chorusFlowerBlock,
                FarmerCrop.NONE,
                SoilKind.END_STONE,
                GrowthStyle.SCALED,
                rules
        ));
    }

    private static void addSimplePlants(
            List<Option> options,
            SoilKind soil,
            String... definitions
    ) {
        for (String definition : definitions) {
            String[] paths = definition.split(":", 2);
            String itemPath = paths[0];
            String blockPath = paths.length == 2 ? paths[1] : itemPath;
            Item item = registeredItem(itemPath);
            Block block = registeredBlock(blockPath);
            if (item == null || block == null) {
                continue;
            }
            options.add(new Option(
                    item,
                    block,
                    FarmerCrop.NONE,
                    soil,
                    GrowthStyle.SCALED,
                    List.of(new HarvestRule(
                            item,
                            1,
                            1,
                            FarmerYield.CHANCE_SCALE,
                            0,
                            FarmerYield.CHANCE_SCALE
                    ))
            ));
        }
    }

    private static void addRule(
            List<HarvestRule> rules,
            String itemPath,
            int baseCount,
            int fortuneCount,
            int baseChance,
            int fortuneChance,
            int maximumChance
    ) {
        Item item = registeredItem(itemPath);
        if (item != null) {
            rules.add(new HarvestRule(
                    item,
                    baseCount,
                    fortuneCount,
                    baseChance,
                    fortuneChance,
                    maximumChance
            ));
        }
    }

    private static void addSilkTouchRule(
            List<HarvestRule> rules,
            String itemPath,
            int baseCount,
            int fortuneCount,
            int baseChance,
            int fortuneChance,
            int maximumChance
    ) {
        Item item = registeredItem(itemPath);
        if (item != null) {
            rules.add(new HarvestRule(
                    item,
                    baseCount,
                    fortuneCount,
                    baseChance,
                    fortuneChance,
                    maximumChance,
                    ToolRequirement.SILK_TOUCH
            ));
        }
    }

    private static Item registeredItem(String path) {
        return BuiltInRegistries.ITEM.getOptional(minecraftId(path)).orElse(null);
    }

    private static Block registeredBlock(String path) {
        return BuiltInRegistries.BLOCK.getOptional(minecraftId(path)).orElse(null);
    }

    private static Identifier minecraftId(String path) {
        return Identifier.fromNamespaceAndPath("minecraft", path);
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
        boolean plantableSeed = stack.is(ItemTags.VILLAGER_PLANTABLE_SEEDS);
        boolean simplePlant = stack.is(VILLAGER_FARMER_PLANTS);
        if (!plantableSeed && !simplePlant) {
            return Optional.empty();
        }
        Block block = blockItem.getBlock();
        block.defaultBlockState();
        if (BuiltInRegistries.ITEM.getKey(item) == null) {
            throw new IllegalArgumentException("Villager crop has no registered item identifier");
        }
        return Optional.of(new Option(
                item,
                block,
                FarmerCrop.NONE,
                SoilKind.FARMLAND,
                GrowthStyle.SCALED,
                simplePlant && !plantableSeed
                        ? List.of(new HarvestRule(
                                item,
                                1,
                                1,
                                FarmerYield.CHANCE_SCALE,
                                0,
                                FarmerYield.CHANCE_SCALE
                        ))
                        : List.of()
        ));
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
        ItemStack result = cropStack.copyWithCount(Math.max(1, 1 + fortuneLevel));
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

    private static float continuousGrowthScale(int growthTicks, int maxGrowthTicks) {
        if (maxGrowthTicks <= 0) {
            return 1.0F;
        }
        float progress = Math.clamp(growthTicks / (float) maxGrowthTicks, 0.0F, 1.0F);
        return 0.20F + 0.80F * progress;
    }

    private static float stagedGrowthScale(int growthTicks, int maxGrowthTicks, int stages) {
        int maximumStage = Math.max(1, stages - 1);
        int currentStage = stage(
                Math.max(0, growthTicks),
                Math.max(0, maxGrowthTicks),
                maximumStage
        );
        float progress = currentStage / (float) maximumStage;
        return 0.20F + 0.80F * progress;
    }

    public record PreviewYield(ItemStack stack, int chanceBasisPoints, boolean requiresSilkTouch) {
        public PreviewYield {
            stack = stack.copy();
            if (stack.isEmpty()) {
                throw new IllegalArgumentException("A preview yield cannot be empty");
            }
            if (chanceBasisPoints < 1 || chanceBasisPoints > FarmerYield.CHANCE_SCALE) {
                throw new IllegalArgumentException("Invalid preview yield chance");
            }
        }

        public boolean isGuaranteed() {
            return chanceBasisPoints == FarmerYield.CHANCE_SCALE;
        }

        @Override
        public ItemStack stack() {
            return stack.copy();
        }
    }

    public enum RenderSupport {
        FLOOR,
        WALL,
        WATER,
        CEILING
    }

    public static final class Option {
        private final Item item;
        private final Block block;
        private final FarmerCrop crop;
        private final SoilKind soil;
        private final GrowthStyle growthStyle;
        private final List<HarvestRule> harvestRules;
        private final boolean wallSupported;

        private Option(Item item, Block block, FarmerCrop crop) {
            this(
                    item,
                    block,
                    crop,
                    SoilKind.FARMLAND,
                    GrowthStyle.NATURAL,
                    List.of()
            );
        }

        private Option(
                Item item,
                Block block,
                FarmerCrop crop,
                SoilKind soil,
                GrowthStyle growthStyle,
                List<HarvestRule> harvestRules
        ) {
            this.item = java.util.Objects.requireNonNull(item);
            this.block = java.util.Objects.requireNonNull(block);
            this.crop = java.util.Objects.requireNonNull(crop);
            this.soil = java.util.Objects.requireNonNull(soil);
            this.growthStyle = java.util.Objects.requireNonNull(growthStyle);
            this.harvestRules = List.copyOf(harvestRules);
            this.wallSupported = inferWallSupport(block);
        }

        public Item item() {
            return item;
        }

        public Block block() {
            return block;
        }

        public FarmerCrop crop() {
            return crop;
        }

        private SoilKind soil() {
            return soil;
        }

        private GrowthStyle growthStyle() {
            return growthStyle;
        }

        private List<HarvestRule> harvestRules() {
            return harvestRules;
        }

        private boolean wallSupported() {
            return wallSupported;
        }
    }

    private enum SoilKind {
        FARMLAND,
        DIRT,
        GRASS,
        SAND,
        PODZOL,
        STONE,
        END_STONE;

        private BlockState state() {
            return switch (this) {
                case FARMLAND -> Blocks.FARMLAND.defaultBlockState().setValue(
                        FarmlandBlock.MOISTURE,
                        FarmlandBlock.MAX_MOISTURE
                );
                case DIRT -> Blocks.DIRT.defaultBlockState();
                case GRASS -> Blocks.GRASS_BLOCK.defaultBlockState();
                case SAND -> Blocks.SAND.defaultBlockState();
                case PODZOL -> Blocks.PODZOL.defaultBlockState();
                case STONE -> Blocks.STONE.defaultBlockState();
                case END_STONE -> Blocks.END_STONE.defaultBlockState();
            };
        }
    }

    private enum GrowthStyle {
        NATURAL,
        SCALED
    }

    private enum ToolRequirement {
        NONE,
        SILK_TOUCH;

        private boolean allows(boolean silkTouch) {
            return this == NONE || silkTouch;
        }
    }

    private record HarvestRule(
            Item item,
            int baseCount,
            int fortuneCount,
            int baseChance,
            int fortuneChance,
            int maximumChance,
            ToolRequirement requirement
    ) {
        private HarvestRule(
                Item item,
                int baseCount,
                int fortuneCount,
                int baseChance,
                int fortuneChance,
                int maximumChance
        ) {
            this(
                    item,
                    baseCount,
                    fortuneCount,
                    baseChance,
                    fortuneChance,
                    maximumChance,
                    ToolRequirement.NONE
            );
        }

        private HarvestRule {
            java.util.Objects.requireNonNull(item);
            java.util.Objects.requireNonNull(requirement);
            if (baseCount < 1 || fortuneCount < 0) {
                throw new IllegalArgumentException("Invalid villager crop output count");
            }
            if (baseChance < 1
                    || baseChance > FarmerYield.CHANCE_SCALE
                    || fortuneChance < 0
                    || maximumChance < baseChance
                    || maximumChance > FarmerYield.CHANCE_SCALE) {
                throw new IllegalArgumentException("Invalid villager crop output chance");
            }
        }

        private int count(int fortuneLevel) {
            long count = baseCount + (long) Math.max(0, fortuneLevel) * fortuneCount;
            return (int) Math.min(Integer.MAX_VALUE, count);
        }

        private int chance(int fortuneLevel) {
            long chance = baseChance + (long) Math.max(0, fortuneLevel) * fortuneChance;
            return (int) Math.min(maximumChance, chance);
        }
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
