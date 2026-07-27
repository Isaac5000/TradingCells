package com.cosmocraft.trading_cells.feature.captures.adapters.output.client;

import com.cosmocraft.trading_cells.platform.neoforge.client.render.PreviewEntityRenderUtil;

import com.cosmocraft.trading_cells.feature.captures.adapters.input.PiglinCapturerItem;
import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.TradingCells;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import com.mojang.math.Axis;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.NonNull;
// removed unused Nullable import

public final class PiglinCapturerItemRenderSupport {
    private static final float GUI_ADULT_SCALE = 0.38F;
    private static final float GUI_BABY_SCALE = 0.55F;

    private static final MinecraftPiglinCapturerRenderBackend RENDER_BACKEND = new MinecraftPiglinCapturerRenderBackend();

    public enum SpecialProfile {
        DEFAULT,
        GUI,
        FIXED,
        ON_SHELF,
        THIRD_PERSON,
        FIRST_PERSON
    }

    private PiglinCapturerItemRenderSupport() {}

    public record Transform(float scale, double x, double y, double z, float rotX, float rotY, float rotZ) {}

    private static final Map<SpecialProfile, Transform> ADULT_TRANSFORMS = new EnumMap<>(SpecialProfile.class);
    private static final Map<SpecialProfile, Transform> BABY_TRANSFORMS = new EnumMap<>(SpecialProfile.class);

    static {
        // Primera persona: la entidad se renderiza sobre el icono del capturador.
        // El ajuste anterior quedaba fuera del centro visible de la mano: demasiado bajo y a la derecha.
        // Adulto y bebé comparten posición/rotación; solo cambia la escala para compensar la altura real del modelo.
        ADULT_TRANSFORMS.put(SpecialProfile.FIRST_PERSON,
                new Transform(0.32F,
                        -0.80D, 0.15D, -0.35D,
                        0.0F, 25.0F, 0.0F));

        BABY_TRANSFORMS.put(SpecialProfile.FIRST_PERSON,
                new Transform(0.32F,
                        -0.80D, 0.15D, -0.35D,
                        0.0F, 25.0F, 0.0F));

        ADULT_TRANSFORMS.put(SpecialProfile.THIRD_PERSON,
                new Transform(0.26F,
                        1.7D, 0.6D, 2.2D,
                        0.0F, 0.0F, 0.0F));

        BABY_TRANSFORMS.put(SpecialProfile.THIRD_PERSON,
                new Transform(0.38F,
                        1.2D, 0.6D, 1.65D,
                        0.0F, 0.0F, 0.0F));

        ADULT_TRANSFORMS.put(SpecialProfile.GUI,
                new Transform(GUI_ADULT_SCALE,
                        CapturedEntityGuiTransform.translationX(GUI_ADULT_SCALE), 0.18D, 0.0D,
                        0.0F, 0.0F, 0.0F));

        BABY_TRANSFORMS.put(SpecialProfile.GUI,
                new Transform(GUI_BABY_SCALE,
                        CapturedEntityGuiTransform.translationX(GUI_BABY_SCALE), 0.30D, 0.0D,
                        0.0F, 0.0F, 0.0F));

        ADULT_TRANSFORMS.put(SpecialProfile.FIXED,
                new Transform(0.4F,
                        1.2D, 0.0D, 1.2D,
                        0.0F, 0.0F, 0.0F));

        BABY_TRANSFORMS.put(SpecialProfile.FIXED,
                new Transform(0.55F,
                        0.90D, 0.0D, 0.90D,
                        0.0F, 0.0F, 0.0F));

        ADULT_TRANSFORMS.put(SpecialProfile.ON_SHELF,
                new Transform(0.80F,
                        0.6D, -0.4D, 0.5D,
                        0.0F, 0.0F, 0.0F));

        BABY_TRANSFORMS.put(SpecialProfile.ON_SHELF,
                new Transform(1.05F,
                        0.45D, -0.20D, 0.40D,
                        0.0F, 0.0F, 0.0F));

        ADULT_TRANSFORMS.put(SpecialProfile.DEFAULT,
                new Transform(0.45F,
                        1.1D, 0.0D, 1.2D,
                        0.0F, 180.0F, 0.0F));

        BABY_TRANSFORMS.put(SpecialProfile.DEFAULT,
                new Transform(0.60F,
                        0.82D, 0.0D, 0.90D,
                        0.0F, 180.0F, 0.0F));
    }

    // getProfileTransform intentionally removed to reduce public surface; use TRANSFORMS directly

    private record RenderParams(ItemDisplayContext displayContext,
                                PoseStack poseStack,
                                SubmitNodeCollector submitNodeCollector,
                                int packedLight,
                                int packedOverlay,
                                float partialTick,
                                SpecialProfile specialProfile) {}

    // Convenience overloads removed; use renderPiglinSpecial(...) or internal entrypoint.

    public static boolean renderPiglinSpecial(ItemStack stack, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int packedLight, int packedOverlay, float partialTick, SpecialProfile profile) {
        RenderParams params = new RenderParams(null, poseStack, submitNodeCollector, packedLight, packedOverlay, partialTick, profile);
        return renderPiglinInternal(stack, params);
    }

    private static boolean renderPiglinInternal(ItemStack stack, RenderParams params) {
        ItemDisplayContext displayContext = params.displayContext;
        PoseStack poseStack = params.poseStack;
        SubmitNodeCollector submitNodeCollector = params.submitNodeCollector;
        float partialTick = params.partialTick;
        SpecialProfile specialProfile = params.specialProfile;
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            return false;
        }
        if (!PiglinCapturerItem.hasCapturedPiglin(stack)) {
            return false;
        }

        Piglin piglin = PiglinCapturerItem.createCapturedPiglin(level, stack, new BlockPos(0, 0, 0));
        if (piglin == null) {
            TradingCells.LOGGER.warn("[PiglinCapturer] No se pudo crear la entidad piglin desde el item. Comprobar NBT del item.");
            return false;
        }

        poseStack.pushPose();
        try {
            SpecialProfile profileToUse = (displayContext == null) ? specialProfile : mapDisplayContextToProfile(displayContext);
            return renderPiglinEntity(piglin, poseStack, submitNodeCollector, partialTick, profileToUse, params.packedLight());
        } catch (Exception _) {
            return false;
        } finally {
            poseStack.popPose();
        }
    }

    private static SpecialProfile mapDisplayContextToProfile(ItemDisplayContext displayContext) {
        return switch (displayContext) {
            case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND -> SpecialProfile.FIRST_PERSON;
            case THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND -> SpecialProfile.THIRD_PERSON;
            case FIXED -> SpecialProfile.FIXED;
            case GUI -> SpecialProfile.GUI;
            default -> SpecialProfile.DEFAULT;
        };
    }

    private static Transform getTransformForEntity(SpecialProfile profile, boolean baby) {
        Map<SpecialProfile, Transform> transforms = baby ? BABY_TRANSFORMS : ADULT_TRANSFORMS;
        return transforms.getOrDefault(profile, transforms.get(SpecialProfile.DEFAULT));
    }

    private static void applyTransform(PoseStack poseStack, Transform t) {
        poseStack.scale(t.scale(), t.scale(), t.scale());
        poseStack.translate(t.x(), t.y(), t.z());
        if (t.rotX() != 0.0F) poseStack.mulPose(Axis.XP.rotationDegrees(t.rotX()));
        if (t.rotY() != 0.0F) poseStack.mulPose(Axis.YP.rotationDegrees(t.rotY()));
        if (t.rotZ() != 0.0F) poseStack.mulPose(Axis.ZP.rotationDegrees(t.rotZ()));
    }

    private static boolean renderPiglinEntity(Piglin piglin, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, float partialTick, SpecialProfile profile, int packedLight) {
        Transform transform = getTransformForEntity(profile, piglin.isBaby());
        applyTransform(poseStack, transform);
        return orientationFixer(piglin, poseStack, submitNodeCollector, partialTick, profile, packedLight);
    }

    private static boolean orientationFixer(Piglin piglin, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, float partialTick, SpecialProfile profile, int packedLight) {
        // Save accessible rotation-related fields
        float prevYHeadRot = piglin.yHeadRot;
        float prevYBodyRot = piglin.yBodyRot;
        float prevYRotO = piglin.yRotO;
        float prevXRotO = piglin.xRotO;
        float prevYHeadRotO = piglin.yHeadRotO;
        float prevYBodyRotO = piglin.yBodyRotO;
        try {
            // Zero out accessible rotation fields so entity faces default orientation
            piglin.yHeadRot = 0.0F;
            piglin.yBodyRot = 0.0F;
            piglin.yRotO = 0.0F;
            piglin.xRotO = 0.0F;
            piglin.yHeadRotO = 0.0F;
            piglin.yBodyRotO = 0.0F;
            // We ignore any stored lighting or pose. Backend renders a neutral, texture-only preview.
            return RENDER_BACKEND.render(piglin, poseStack, submitNodeCollector, partialTick, profile, packedLight);
        } finally {
            piglin.yHeadRot = prevYHeadRot;
            piglin.yBodyRot = prevYBodyRot;
            piglin.yRotO = prevYRotO;
            piglin.xRotO = prevXRotO;
            piglin.yHeadRotO = prevYHeadRotO;
            piglin.yBodyRotO = prevYBodyRotO;
        }
    }

    static class PiglinCapturerProfiledSpecialRenderer implements SpecialModelRenderer<ItemStack> {
        private final SpecialProfile profile;

        protected PiglinCapturerProfiledSpecialRenderer(SpecialProfile profile) { this.profile = profile; }

        public ItemStack extractArgument(@NonNull ItemStack stack) { return stack; }

        @Override
        public void submit(ItemStack stack, @NonNull PoseStack poseStack, @NonNull SubmitNodeCollector submitNodeCollector, int packedLight, int packedOverlay, boolean hasFoil, int seed) {
            if (stack == null) return;
            try {
                renderPiglinSpecial(stack, poseStack, submitNodeCollector, packedLight, packedOverlay, 0.0F, profile);
            } catch (Exception _) {
                // Item previews fail closed; logging every frame would flood the client log.
            }
        }

        @Override
        public void getExtents(java.util.function.@NonNull Consumer<Vector3fc> extents) {
            switch (profile) {
                case GUI -> { extents.accept(new Vector3f(-0.5F, 0.0F, -0.5F)); extents.accept(new Vector3f(0.5F, 1.0F, 0.5F)); }
                case FIRST_PERSON -> { extents.accept(new Vector3f(-2.0F, -2.0F, -2.0F)); extents.accept(new Vector3f(2.0F, 2.0F, 2.0F)); }
                case THIRD_PERSON -> { extents.accept(new Vector3f(-0.45F, 0.0F, -0.45F)); extents.accept(new Vector3f(0.45F, 1.4F, 0.45F)); }
                case FIXED, ON_SHELF -> { extents.accept(new Vector3f(-0.45F, 0.0F, -0.45F)); extents.accept(new Vector3f(0.45F, 1.0F, 0.45F)); }
                default -> { extents.accept(new Vector3f(-0.4F, 0.0F, -0.4F)); extents.accept(new Vector3f(0.4F, 1.0F, 0.4F)); }
            }
        }
    }

    public static final class Default extends PiglinCapturerProfiledSpecialRenderer {
        public Default() { super(SpecialProfile.DEFAULT); }
        @SuppressWarnings({"rawtypes"})
        public static final class Unbaked {
            private static final ConfigurableUnbaked INSTANCE = new ConfigurableUnbaked(Default::new);
            public static final MapCodec<? extends SpecialModelRenderer.Unbaked<SpecialModelRenderer>> MAP_CODEC = INSTANCE.type();
            private Unbaked() {}
        }
    }

    public static final class Gui extends PiglinCapturerProfiledSpecialRenderer {
        public Gui() { super(SpecialProfile.GUI); }
        @SuppressWarnings({"rawtypes"})
        public static final class Unbaked {
            private static final ConfigurableUnbaked INSTANCE = new ConfigurableUnbaked(Gui::new);
            public static final MapCodec<? extends SpecialModelRenderer.Unbaked<SpecialModelRenderer>> MAP_CODEC = INSTANCE.type();
            private Unbaked() {}
        }
    }

    public static final class Fixed extends PiglinCapturerProfiledSpecialRenderer {
        public Fixed() { super(SpecialProfile.FIXED); }
        @SuppressWarnings({"rawtypes"})
        public static final class Unbaked {
            private static final ConfigurableUnbaked INSTANCE = new ConfigurableUnbaked(Fixed::new);
            public static final MapCodec<? extends SpecialModelRenderer.Unbaked<SpecialModelRenderer>> MAP_CODEC = INSTANCE.type();
            private Unbaked() {}
        }
    }

    public static final class OnShelf extends PiglinCapturerProfiledSpecialRenderer {
        public OnShelf() { super(SpecialProfile.ON_SHELF); }
        @SuppressWarnings({"rawtypes"})
        public static final class Unbaked {
            private static final ConfigurableUnbaked INSTANCE = new ConfigurableUnbaked(OnShelf::new);
            public static final MapCodec<? extends SpecialModelRenderer.Unbaked<SpecialModelRenderer>> MAP_CODEC = INSTANCE.type();
            private Unbaked() {}
        }
    }

    public static final class ThirdPerson extends PiglinCapturerProfiledSpecialRenderer {
        public ThirdPerson() { super(SpecialProfile.THIRD_PERSON); }
        @SuppressWarnings({"rawtypes"})
        public static final class Unbaked {
            private static final ConfigurableUnbaked INSTANCE = new ConfigurableUnbaked(ThirdPerson::new);
            public static final MapCodec<? extends SpecialModelRenderer.Unbaked<SpecialModelRenderer>> MAP_CODEC = INSTANCE.type();
            private Unbaked() {}
        }
    }

    public static final class FirstPerson extends PiglinCapturerProfiledSpecialRenderer {
        public FirstPerson() { super(SpecialProfile.FIRST_PERSON); }
        @SuppressWarnings({"rawtypes"})
        public static final class Unbaked {
            private static final ConfigurableUnbaked INSTANCE = new ConfigurableUnbaked(FirstPerson::new);
            public static final MapCodec<? extends SpecialModelRenderer.Unbaked<SpecialModelRenderer>> MAP_CODEC = INSTANCE.type();
            private Unbaked() {}
        }
    }

    @SuppressWarnings({"rawtypes","unchecked"})
    private static final class ConfigurableUnbaked implements SpecialModelRenderer.Unbaked<SpecialModelRenderer> {
        private final java.util.function.Supplier<SpecialModelRenderer> factory;
        private final MapCodec<? extends SpecialModelRenderer.Unbaked<SpecialModelRenderer>> codec;
        ConfigurableUnbaked(java.util.function.Supplier<SpecialModelRenderer> factory) { this.factory = factory; this.codec = MapCodec.unit(this); }
        @Override public @NonNull MapCodec<? extends SpecialModelRenderer.Unbaked<SpecialModelRenderer>> type() { return this.codec; }
        @Override public SpecialModelRenderer bake(SpecialModelRenderer.@NonNull BakingContext bakingContext) { return this.factory.get(); }
    }

    public static final class MinecraftPiglinCapturerRenderBackend {
        public boolean render(Piglin piglin, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, float partialTick, SpecialProfile profile, int packedLight) {
            Minecraft mc = Minecraft.getInstance();
            net.minecraft.client.renderer.state.level.CameraRenderState cameraState = new net.minecraft.client.renderer.state.level.CameraRenderState();
            cameraState.initialized = profile == SpecialProfile.GUI;
            EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
            PreviewEntityRenderUtil.prepare(piglin);
            EntityRenderState state = dispatcher.extractEntity(piglin, partialTick);
            PreviewEntityRenderUtil.applyLight(state, packedLight);
            PreviewEntityRenderUtil.suppressWorldEffects(state);
            dispatcher.submit(state, cameraState, 0.0D, 0.0D, 0.0D, poseStack, submitNodeCollector);
            return true;
        }
    }
}
