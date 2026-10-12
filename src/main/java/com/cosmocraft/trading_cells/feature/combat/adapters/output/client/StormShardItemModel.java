package com.cosmocraft.trading_cells.feature.combat.adapters.output.client;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemModels;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4fc;
import org.jspecify.annotations.Nullable;

/** Prevents the GUI item atlas from freezing the charged layer between frames. */
public record StormShardItemModel(ItemModel delegate) implements ItemModel {
    @Override public void update(ItemStackRenderState output, ItemStack stack, ItemModelResolver resolver,
            ItemDisplayContext display, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
        delegate.update(output, stack, resolver, display, level, owner, seed);
        output.setAnimated();
    }

    public record Unbaked(ItemModel.Unbaked model) implements ItemModel.Unbaked {
        public static final MapCodec<Unbaked> CODEC = ItemModels.CODEC.fieldOf("model").xmap(Unbaked::new, Unbaked::model);
        @Override public MapCodec<Unbaked> type() { return CODEC; }
        @Override public void resolveDependencies(ResolvableModel.Resolver resolver) { model.resolveDependencies(resolver); }
        @Override public ItemModel bake(ItemModel.BakingContext context, Matrix4fc transformation) {
            return new StormShardItemModel(model.bake(context, transformation));
        }
    }
}
