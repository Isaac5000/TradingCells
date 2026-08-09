package com.cosmocraft.trading_cells.feature.quarry.adapters.input;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/** Produces one complete cycle result without ever returning a partial reward. */
public final class QuarryResultFactory {
    private QuarryResultFactory() {
    }

    public static List<ItemStack> create(
            QuarryMaterialDefinition definition,
            ServerLevel level,
            BlockPos pos,
            ItemStack pickaxe,
            boolean deepMining
    ) {
        int fortuneLevel = QuarryEnchantments.fortuneLevel(pickaxe, level.registryAccess());
        boolean silkTouch = QuarryEnchantments.hasSilkTouch(pickaxe, level.registryAccess());
        if (definition.useBlockLoot()) {
            return blockLoot(definition, level, pos, pickaxe, deepMining, silkTouch, fortuneLevel);
        }

        int amount = silkTouch
                ? 1
                : level.getRandom().nextIntBetweenInclusive(
                        definition.minimumAmount(),
                        definition.maximumAmount()
                );
        if (definition.fortuneCompatible()) {
            amount *= fortuneMultiplier(level, fortuneLevel);
        }
        return stacks(
                silkTouch ? definition.silkResult(deepMining) : definition.normalResult(),
                amount
        );
    }

    private static List<ItemStack> blockLoot(
            QuarryMaterialDefinition definition,
            ServerLevel level,
            BlockPos pos,
            ItemStack pickaxe,
            boolean deepMining,
            boolean silkTouch,
            int fortuneLevel
    ) {
        Block block = BuiltInRegistries.BLOCK.getOptional(definition.silkResult(deepMining)).orElse(null);
        if (block == null) {
            return List.of();
        }
        List<ItemStack> drops = Block.getDrops(block.defaultBlockState(), level, pos, null, null, pickaxe).stream()
                .filter(stack -> !stack.isEmpty())
                .map(ItemStack::copy)
                .toList();
        List<ItemStack> result = drops.isEmpty()
                ? stacks(silkTouch ? definition.silkResult(deepMining) : definition.normalResult(), 1)
                : drops;
        if (silkTouch && definition.fortuneCompatible()) {
            return multiply(result, fortuneMultiplier(level, fortuneLevel));
        }
        return result;
    }

    private static int fortuneMultiplier(ServerLevel level, int fortuneLevel) {
        if (fortuneLevel <= 0) {
            return 1;
        }
        int bonus = Math.max(0, level.getRandom().nextInt(fortuneLevel + 2) - 1);
        return bonus + 1;
    }

    private static List<ItemStack> multiply(List<ItemStack> source, int multiplier) {
        if (multiplier <= 1) {
            return source;
        }
        List<ItemStack> result = new ArrayList<>();
        for (ItemStack stack : source) {
            int remaining = Math.multiplyExact(stack.getCount(), multiplier);
            while (remaining > 0) {
                ItemStack copy = stack.copy();
                int count = Math.min(remaining, copy.getMaxStackSize());
                copy.setCount(count);
                result.add(copy);
                remaining -= count;
            }
        }
        return List.copyOf(result);
    }

    private static List<ItemStack> stacks(net.minecraft.resources.Identifier itemId, int amount) {
        Item item = BuiltInRegistries.ITEM.getOptional(itemId).orElse(null);
        if (item == null || amount <= 0) {
            return List.of();
        }
        List<ItemStack> stacks = new ArrayList<>();
        int remaining = amount;
        while (remaining > 0) {
            ItemStack stack = new ItemStack(item);
            int count = Math.min(remaining, stack.getMaxStackSize());
            stack.setCount(count);
            stacks.add(stack);
            remaining -= count;
        }
        return List.copyOf(stacks);
    }
}
