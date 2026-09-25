package com.cosmocraft.trading_cells.feature.mobfarm.adapters.output.client;

import com.cosmocraft.trading_cells.feature.mobfarm.adapters.input.EssenceExtractorItem;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public record SyringeLoadProperty() implements RangeSelectItemModelProperty {
    public static final MapCodec<SyringeLoadProperty> CODEC = MapCodec.unit(new SyringeLoadProperty());
    @Override public float get(ItemStack stack, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
        if (EssenceExtractorItem.isLoaded(stack)) { return 1; }
        var entity = owner == null ? null : owner.asLivingEntity();
        if (entity == null || !entity.isUsingItem() || entity.getUseItem() != stack) { return -1; }
        float partial = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
        return Math.clamp((EssenceExtractorItem.RELOAD_TICKS - entity.getUseItemRemainingTicks() + partial)
                / EssenceExtractorItem.RELOAD_TICKS, 0, 1);
    }
    @Override public MapCodec<SyringeLoadProperty> type() { return CODEC; }
}
