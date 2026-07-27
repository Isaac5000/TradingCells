package com.cosmocraft.trading_cells.feature.farmer.adapters.input;

import com.cosmocraft.trading_cells.feature.farmer.domain.model.FarmerCrop;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BeetrootBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class FarmerCropStackAdapter {
    private FarmerCropStackAdapter() {
    }

    public static FarmerCrop from(ItemStack stack) {
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
        return FarmerCrop.NONE;
    }

    public static boolean isSupported(ItemStack stack) {
        return from(stack) != FarmerCrop.NONE;
    }

    public static ItemStack produce(FarmerCrop crop, int count) {
        return switch (crop) {
            case WHEAT -> new ItemStack(Items.WHEAT, count);
            case CARROT -> new ItemStack(Items.CARROT, count);
            case POTATO -> new ItemStack(Items.POTATO, count);
            case BEETROOT -> new ItemStack(Items.BEETROOT, count);
            case NONE -> ItemStack.EMPTY;
        };
    }

    public static ItemStack seeds(FarmerCrop crop, int count) {
        return switch (crop) {
            case WHEAT -> new ItemStack(Items.WHEAT_SEEDS, count);
            case BEETROOT -> new ItemStack(Items.BEETROOT_SEEDS, count);
            default -> ItemStack.EMPTY;
        };
    }

    public static BlockState cropState(FarmerCrop crop, int growthTicks, int maxGrowthTicks) {
        int duration = Math.max(0, maxGrowthTicks);
        int clampedTicks = Math.clamp(growthTicks, 0, duration);
        return switch (crop) {
            case WHEAT -> Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, stage(clampedTicks, duration, 7));
            case CARROT -> Blocks.CARROTS.defaultBlockState().setValue(CropBlock.AGE, stage(clampedTicks, duration, 7));
            case POTATO -> Blocks.POTATOES.defaultBlockState().setValue(CropBlock.AGE, stage(clampedTicks, duration, 7));
            case BEETROOT -> Blocks.BEETROOTS.defaultBlockState().setValue(BeetrootBlock.AGE, stage(clampedTicks, duration, 3));
            case NONE -> Blocks.AIR.defaultBlockState();
        };
    }

    private static int stage(int ticks, int maxTicks, int maxStage) {
        return maxTicks <= 0 ? 0 : Math.min(maxStage, ticks * (maxStage + 1) / maxTicks);
    }
}
