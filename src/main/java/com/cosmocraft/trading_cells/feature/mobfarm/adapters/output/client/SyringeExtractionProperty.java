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
        return (tier - 1) * 2 + Math.clamp((EssenceExtractorItem.EXTRACTION_TICKS - entity.getUseItemRemainingTicks() + partial)
                / EssenceExtractorItem.EXTRACTION_TICKS, 0, 1);
    }
    @Override public MapCodec<SyringeExtractionProperty> type() { return CODEC; }
}
