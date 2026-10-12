package com.cosmocraft.trading_cells.feature.combat.adapters.output.client;

import com.cosmocraft.trading_cells.feature.combat.adapters.minecraft.ElectricArcWelderItem;
import com.cosmocraft.trading_cells.feature.combat.adapters.minecraft.StormWeaponItem;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.*;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4fc;
import org.jspecify.annotations.Nullable;

public record StormBeamItemModel(ItemModel delegate, ModelRenderProperties properties, Matrix4fc transformation) implements ItemModel {
    @Override public void update(ItemStackRenderState output, ItemStack stack, ItemModelResolver resolver,
            ItemDisplayContext display, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
        delegate.update(output, stack, resolver, display, level, owner, seed);
        output.setAnimated();
        var entity = owner == null ? null : owner.asLivingEntity();
        if (entity == null || level == null || !display.firstPerson() && display != ItemDisplayContext.THIRD_PERSON_LEFT_HAND
                && display != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND || !entity.isUsingItem()
                || entity.getUseItem() != stack || StormWeaponItem.reloading(stack)) { return; }
        var client = Minecraft.getInstance();
        float partial = client.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        var target = ElectricArcWelderItem.trace(level, entity, partial).end().subtract(client.gameRenderer.mainCamera().position());
        var layer = output.newLayer();
        properties.applyToLayer(layer, display);
        layer.setLocalTransform(transformation);
        layer.setupSpecialModel(StormArcRenderer.INSTANCE, new StormArcRenderer.Arc(target, level.getGameTime() / 2 + entity.getId() * 31L));
    }
    public record Unbaked(ItemModel.Unbaked model) implements ItemModel.Unbaked {
        private static final Identifier BASE = Identifier.fromNamespaceAndPath("trading_cells", "item/storm/electric_arc_welder");
        public static final MapCodec<Unbaked> CODEC = ItemModels.CODEC.fieldOf("model").xmap(Unbaked::new, Unbaked::model);
        @Override public MapCodec<Unbaked> type() { return CODEC; }
        @Override public void resolveDependencies(ResolvableModel.Resolver resolver) { model.resolveDependencies(resolver); resolver.markDependency(BASE); }
        @Override public ItemModel bake(ItemModel.BakingContext context, Matrix4fc transform) {
            var baker = context.blockModelBaker();
            var base = baker.getModel(BASE);
            return new StormBeamItemModel(model.bake(context, transform), ModelRenderProperties.fromResolvedModel(baker, base, base.getTopTextureSlots()), transform);
        }
    }
}
