package com.cosmocraft.trading_cells.feature.mobfarm.adapters.output.client;

import com.cosmocraft.trading_cells.feature.mobfarm.adapters.input.EntityEssenceData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import java.lang.ref.WeakReference;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class EntityModuleItemRenderer implements SpecialModelRenderer<SimulationEntityPreview> {
    private static final int MAX_PREVIEWS = 64;
    private final Map<CustomData, SimulationEntityPreview> previews = new LinkedHashMap<>(16, 0.75F, true);
    private WeakReference<Level> previewLevel = new WeakReference<>(null);

    @Override
    public @Nullable SimulationEntityPreview extractArgument(@NonNull ItemStack stack) {
        Minecraft minecraft = Minecraft.getInstance();
        if (previewLevel.get() != minecraft.level) {
            previews.clear();
            previewLevel = new WeakReference<>(minecraft.level);
        }
        if (minecraft.level == null) { return null; }
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        if (!previews.containsKey(data)) {
            if (previews.size() >= MAX_PREVIEWS) { previews.remove(previews.keySet().iterator().next()); }
            previews.put(data, SimulationEntityPreview.create(EntityEssenceData.createEntity(minecraft.level, stack),
                    minecraft.getEntityRenderDispatcher(), 180.0F, 0.55F, 0.64F));
        }
        return previews.get(data);
    }

    @Override
    public void submit(@Nullable SimulationEntityPreview preview, @NonNull PoseStack poseStack,
            @NonNull SubmitNodeCollector collector, int packedLight, int packedOverlay, boolean hasFoil, int outlineColor) {
        if (preview == null) { return; }
        CameraRenderState camera = new CameraRenderState();
        camera.initialized = true;
        preview.submit(Minecraft.getInstance().getEntityRenderDispatcher(), 0.5D, 0.21875D, 0.5D,
                packedLight, poseStack, collector, camera);
    }

    @Override
    public void getExtents(Consumer<Vector3fc> extents) {
        extents.accept(new Vector3f(0.0F, 0.0F, 0.0F));
        extents.accept(new Vector3f(1.0F, 1.0F, 1.0F));
    }

    public static final class Unbaked implements SpecialModelRenderer.Unbaked<SimulationEntityPreview> {
        public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(new Unbaked());
        private Unbaked() { }
        @Override public @NonNull MapCodec<Unbaked> type() { return MAP_CODEC; }
        @Override public SpecialModelRenderer<SimulationEntityPreview> bake(SpecialModelRenderer.@NonNull BakingContext context) {
            return new EntityModuleItemRenderer();
        }
    }
}
