package com.cosmocraft.trading_cells.feature.mobfarm.adapters.output.client;

import com.cosmocraft.trading_cells.feature.mobfarm.adapters.input.EssenceExtractorItem;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public record SyringeExtractionProperty() implements RangeSelectItemModelProperty {
    public static final MapCodec<SyringeExtractionProperty> CODEC = MapCodec.unit(new SyringeExtractionProperty());
    @Override public float get(ItemStack stack, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
        int tier = EssenceExtractorItem.extractionTier(stack);
        var entity = owner == null ? null : owner.asLivingEntity();
        if (tier < 1 || tier > 4 || entity == null || !entity.isUsingItem() || entity.getUseItem() != stack) { return -1; }
        float partial = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
        int duration = EssenceExtractorItem.extractionTicks(tier);
        return (tier - 1) * 2 + com.cosmocraft.trading_cells.feature.mobfarm.domain.model.SyringeAnimation.modelProgress(
                tier, duration - entity.getUseItemRemainingTicks() + partial);
    }
    @Override public MapCodec<SyringeExtractionProperty> type() { return CODEC; }
}
