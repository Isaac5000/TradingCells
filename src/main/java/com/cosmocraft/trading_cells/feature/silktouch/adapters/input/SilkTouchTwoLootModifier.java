package com.cosmocraft.trading_cells.feature.silktouch.adapters.input;

import com.cosmocraft.trading_cells.feature.silktouch.adapters.output.SilkTouchTwoRegistrationAdapter;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

public final class SilkTouchTwoLootModifier extends LootModifier {
    public static final MapCodec<SilkTouchTwoLootModifier> CODEC = RecordCodecBuilder.mapCodec(instance ->
            codecStart(instance).apply(instance, SilkTouchTwoLootModifier::new)
    );

    public SilkTouchTwoLootModifier(LootItemCondition[] conditions, int priority) {
        super(conditions, priority);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        BlockState state = context.getOptionalParameter(LootContextParams.BLOCK_STATE);
        ItemInstance tool = context.getOptionalParameter(LootContextParams.TOOL);
        if (state == null || tool == null) {
            return generatedLoot;
        }

        BlockEntity blockEntity = context.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        ItemStack drop = SilkTouchTwoDropAdapter.createDrop(
                state,
                tool,
                context.getLevel().registryAccess(),
                blockEntity
        );
        if (!drop.isEmpty() && generatedLoot.stream().noneMatch(stack -> stack.is(drop.getItem()))) {
            generatedLoot.add(drop);
        }
        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return SilkTouchTwoRegistrationAdapter.LOOT_MODIFIER.get();
    }
}
