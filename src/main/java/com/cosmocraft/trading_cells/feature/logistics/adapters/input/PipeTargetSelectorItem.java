package com.cosmocraft.trading_cells.feature.logistics.adapters.input;

import com.cosmocraft.trading_cells.feature.logistics.adapters.api.LogisticsResourceAdapters;
import com.cosmocraft.trading_cells.feature.logistics.domain.model.PipeRuleTarget;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import org.jspecify.annotations.Nullable;

public final class PipeTargetSelectorItem extends Item {
    private static final String ROOT = "TradingCellsPipeTarget";

    public PipeTargetSelectorItem(Properties properties) { super(properties); }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        var player = context.getPlayer();
        var level = context.getLevel();
        var pos = context.getClickedPos();
        if (player == null || player.isSpectator() || !level.mayInteract(player, pos)
                || !player.mayUseItemAt(pos, context.getClickedFace(), stack)) { return InteractionResult.FAIL; }
        if (!(level instanceof ServerLevel serverLevel)) { return InteractionResult.SUCCESS; }
        if (!isCompatibleTarget(serverLevel, pos)) { return InteractionResult.FAIL; }
        setTarget(stack, new PipeRuleTarget(level.dimension().identifier().toString(), pos.getX(), pos.getY(), pos.getZ()));
        player.sendOverlayMessage(Component.translatable("item.trading_cells.pipe_target_selector.saved", pos.getX(), pos.getY(), pos.getZ()));
        return InteractionResult.CONSUME;
    }

    private static boolean isCompatibleTarget(ServerLevel level, BlockPos pos) {
        for (var adapter : LogisticsResourceAdapters.all()) {
            if (LogisticsNetworkManager.findHandler(adapter, level, pos, null) != null) { return true; }
            for (Direction side : Direction.values()) {
                if (LogisticsNetworkManager.findHandler(adapter, level, pos, side) != null) { return true; }
            }
        }
        return false;
    }

    public static void setTarget(ItemStack stack, PipeRuleTarget target) {
        if (!(stack.getItem() instanceof PipeTargetSelectorItem)) { return; }
        var data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        var tag = new CompoundTag();
        tag.putInt("SchemaVersion", 1);
        tag.putString("Dimension", target.dimension());
        tag.putInt("X", target.x()); tag.putInt("Y", target.y()); tag.putInt("Z", target.z());
        data.put(ROOT, tag);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
    }

    public static @Nullable PipeRuleTarget target(ItemStack stack) {
        if (!(stack.getItem() instanceof PipeTargetSelectorItem)) { return null; }
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getCompound(ROOT).map(tag -> {
            if (tag.getIntOr("SchemaVersion", 0) != 1 || tag.getInt("X").isEmpty()
                    || tag.getInt("Y").isEmpty() || tag.getInt("Z").isEmpty()) { return null; }
            String dimension = tag.getStringOr("Dimension", "");
            if (Identifier.tryParse(dimension) == null) { return null; }
            try { return new PipeRuleTarget(dimension, tag.getIntOr("X", 0), tag.getIntOr("Y", 0), tag.getIntOr("Z", 0)); }
            catch (IllegalArgumentException ignored) { return null; }
        }).orElse(null);
    }

    public static void appendTooltip(ItemStack stack, Consumer<Component> builder) {
        var target = target(stack);
        if (target == null) { return; }
        builder.accept(Component.literal("x=").append(Component.literal(Integer.toString(target.x())).withStyle(ChatFormatting.AQUA))
                .append(" y=").append(Component.literal(Integer.toString(target.y())).withStyle(ChatFormatting.AQUA))
                .append(" z=").append(Component.literal(Integer.toString(target.z())).withStyle(ChatFormatting.AQUA)));
        builder.accept(Component.literal(target.dimension()).withStyle(ChatFormatting.GRAY));
    }
}
