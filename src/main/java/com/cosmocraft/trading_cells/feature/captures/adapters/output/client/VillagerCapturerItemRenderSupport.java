package com.cosmocraft.trading_cells.feature.captures.adapters.output.client;

import com.cosmocraft.trading_cells.platform.neoforge.client.render.PreviewEntityRenderUtil;

import com.cosmocraft.trading_cells.feature.captures.adapters.input.VillagerCapturerItem;
import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.TradingCells;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import com.mojang.math.Axis;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.NonNull;

public final class VillagerCapturerItemRenderSupport {
    private static final float GUI_ADULT_SCALE = 0.38F;
    private static final float GUI_BABY_SCALE = 0.48F;

    // Use the concrete backend directly to reduce indirection; interface was unnecessary
    // for the current codebase and removed to simplify the class count.
    private static final MinecraftVillagerCapturerRenderBackend RENDER_BACKEND = new MinecraftVillagerCapturerRenderBackend();

    public enum SpecialProfile {
        DEFAULT,
        GUI,
        FIXED,
        ON_SHELF,
        THIRD_PERSON,
        FIRST_PERSON
    }

    private VillagerCapturerItemRenderSupport() {
    }

    // Minimal renderer: no runtime flags or fallback. Only required code remains.

    // Transform descriptor grouped by profile so values are easy to find and change.
    // Use a compact record for the transform; accessors are .scale(), .x(), .y(), .z(), .rotX(), .rotY(), .rotZ()
    public record Transform(float scale, double x, double y, double z, float rotX, float rotY, float rotZ) {}

    // ---------------------------------------------------------------------
    // TRANSFORMS: central place for all scale/translation/rotation values
    // Edit these values to change how captured villagers are scaled/positioned
    // in each display context (GUI, first/third person, fixed/item-frame, on_shelf).
    // This is the easiest place to find and tune scale & rotation.
    // ---------------------------------------------------------------------
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
                new Transform(0.36F,
                        1.25D, 0.6D, 1.7D,
                        0.0F, 0.0F, 0.0F));

        ADULT_TRANSFORMS.put(SpecialProfile.GUI,
                new Transform(GUI_ADULT_SCALE,
                        CapturedEntityGuiTransform.translationX(GUI_ADULT_SCALE), 0.18D, 0.0D,
                        0.0F, 0.0F, 0.0F));

        BABY_TRANSFORMS.put(SpecialProfile.GUI,
                new Transform(GUI_BABY_SCALE,
                        CapturedEntityGuiTransform.translationX(GUI_BABY_SCALE), 0.23D, 0.0D,
                        0.0F, 0.0F, 0.0F));

        ADULT_TRANSFORMS.put(SpecialProfile.FIXED,
                new Transform(0.4F,
                        1.2D, 0.0D, 1.2D,
                        0.0F, 0.0F, 0.0F));

        BABY_TRANSFORMS.put(SpecialProfile.FIXED,
                new Transform(0.52F,
                        0.95D, 0.0D, 0.95D,
                        0.0F, 0.0F, 0.0F));

        ADULT_TRANSFORMS.put(SpecialProfile.ON_SHELF,
                new Transform(0.80F,
                        0.6D, -0.4D, 0.5D,
                        0.0F, 0.0F, 0.0F));

        BABY_TRANSFORMS.put(SpecialProfile.ON_SHELF,
                new Transform(1.0F,
                        0.48D, -0.25D, 0.42D,
                        0.0F, 0.0F, 0.0F));

        ADULT_TRANSFORMS.put(SpecialProfile.DEFAULT,
                new Transform(0.45F,
                        1.1D, 0.0D, 1.2D,
                        0.0F, 180.0F, 0.0F));

        BABY_TRANSFORMS.put(SpecialProfile.DEFAULT,
                new Transform(0.58F,
                        0.85D, 0.0D, 0.95D,
                        0.0F, 180.0F, 0.0F));
    }

    // getProfileTransform intentionally removed to reduce public surface; use TRANSFORMS directly

    // Runtime tuning helper removed to keep the API minimal. Reintroduce if you need live tweaking.


    // Convenience overloads removed to reduce unused public surface area. Use
    // renderVillager(..., ItemDisplayContext, ...) or renderVillager(..., ItemDisplayContext, PoseStack, MultiBufferSource, ...) instead.

    // Public convenience method removed; internal render entry via renderVillagerInternal and
    // renderVillagerSpecial remains. If external callers require this exact signature,
    // we can restore it, but currently it's unused.

    /**
     * Render usado por los item models "minecraft:special".
     * Permite perfilar el ajuste base (por ejemplo, 1ª persona vs el resto).
     */
    public static boolean renderVillagerSpecial(ItemStack stack, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int packedLight, int packedOverlay, float partialTick, SpecialProfile profile) {
        RenderParams params = new RenderParams(null, poseStack, submitNodeCollector, packedLight, packedOverlay, partialTick, profile);
        return renderVillagerInternal(stack, params);
    }

    // Parameter object to avoid long parameter lists and group rendering parameters.
        private record RenderParams(ItemDisplayContext displayContext,
                                    PoseStack poseStack,
                                    SubmitNodeCollector submitNodeCollector,
                                    int packedLight,
                                    int packedOverlay,
                                    float partialTick,
                                    SpecialProfile specialProfile) {
    }

    private static boolean renderVillagerInternal(ItemStack stack, RenderParams params) {
        ItemDisplayContext displayContext = params.displayContext;
        PoseStack poseStack = params.poseStack;
        SubmitNodeCollector submitNodeCollector = params.submitNodeCollector;
        float partialTick = params.partialTick;
        SpecialProfile specialProfile = params.specialProfile;
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            return false;
        }
        if (!VillagerCapturerItem.hasCapturedVillager(stack)) {
            return false;
        }

        Villager villager = VillagerCapturerItem.createCapturedVillager(level, stack, new BlockPos(0, 0, 0));
        if (villager == null) {
            TradingCells.LOGGER.warn("[VillagerCapturer] No se pudo crear la entidad aldeano desde el item. Comprobar NBT del item.");
            return false;
        }

        poseStack.pushPose();
        try {
            // Normalizamos siempre a un perfil (podemos recibir displayContext cuando se llama manualmente
            // o null cuando se invoca desde el model "minecraft:special"). Mapear a un perfil asegura
            // que aplicamos la misma lógica base en todos los caminos.
            SpecialProfile profileToUse = (displayContext == null) ? specialProfile : mapDisplayContextToProfile(displayContext);
            // Delegate adult vs baby rendering to separate helpers for clarity.
            // Render both adults and babies identically: use the same render path
            return renderVillagerEntity(villager, poseStack, submitNodeCollector, partialTick, profileToUse, params.packedLight());
        } catch (Exception _) {
            return false;
        } finally {
            poseStack.popPose();
        }
    }

    // no reflection fallback required in minimal implementation

    // MultiBufferSource convenience overload removed to reduce unused APIs; callers should use the Object-based variant

    // applyDisplayTransform removed - we always use applySpecialModelBaseTransform which
    // centralizes scale/translation/rotation per SpecialProfile. This avoids duplicate
    // transform logic and potential inconsistencies between code paths.

    // applySpecialModelBaseTransform was removed to reduce dead code; we keep a single applyTransform implementation.

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

    private static boolean renderVillagerEntity(Villager villager, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, float partialTick, SpecialProfile profile, int packedLight) {
        Transform transform = getTransformForEntity(profile, villager.isBaby());
        applyTransform(poseStack, transform);
        return orientationFixer(villager, poseStack, submitNodeCollector, partialTick, profile, packedLight);
    }

    private static boolean orientationFixer(Villager villager, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, float partialTick, SpecialProfile profile, int packedLight) {
        // Save current rotation-related state that is accessible
        float prevYHeadRot = villager.yHeadRot;
        float prevYBodyRot = villager.yBodyRot;
        float prevYRotO = villager.yRotO;
        float prevXRotO = villager.xRotO;
        float prevYHeadRotO = villager.yHeadRotO;
        float prevYBodyRotO = villager.yBodyRotO;
        try {
            // Zero out accessible rotation fields so rendered entity faces default orientation
            villager.yHeadRot = 0.0F;
            villager.yBodyRot = 0.0F;
            villager.yRotO = 0.0F;
            villager.xRotO = 0.0F;
            villager.yHeadRotO = 0.0F;
            villager.yBodyRotO = 0.0F;
            // Ignore any stored lighting or pose; render a neutral texture-only preview.
            return RENDER_BACKEND.render(villager, poseStack, submitNodeCollector, partialTick, profile, packedLight);
        } finally {
            // Restore saved state exactly to the same fields we modified
            villager.yHeadRot = prevYHeadRot;
            villager.yBodyRot = prevYBodyRot;
            villager.yRotO = prevYRotO;
            villager.xRotO = prevXRotO;
            villager.yHeadRotO = prevYHeadRotO;
            villager.yBodyRotO = prevYBodyRotO;
        }
    }

    /*
     * Small pose adjustments applied only to baby entities after the base transform.
     * Kept minimal but separate so baby rendering can be tuned without touching adult code.
     */
    // No baby-specific adjustments in minimal implementation

    // ---------------------------------------------------------------------
    // Consolidated special renderer classes and an internal backend so all
    // rendering code lives in this single file (user preference).
    // ---------------------------------------------------------------------

    /**
     * Base special renderer used by concrete profile renderers.
     */
    static class VillagerCapturerProfiledSpecialRenderer implements SpecialModelRenderer<ItemStack> {
        private final SpecialProfile profile;

        protected VillagerCapturerProfiledSpecialRenderer(SpecialProfile profile) {
            this.profile = profile;
        }

        public ItemStack extractArgument(@NonNull ItemStack stack) {
            return stack;
        }

        @Override
        public void submit(ItemStack stack, @NonNull PoseStack poseStack, @NonNull SubmitNodeCollector submitNodeCollector, int packedLight, int packedOverlay, boolean hasFoil, int seed) {
            if (stack == null) return;
            // Intentionally silent here: no logging to avoid spam in normal play.
            try {
                renderVillagerSpecial(
                        stack,
                        poseStack,
                        submitNodeCollector,
                        packedLight,
                        packedOverlay,
                        0.0F,
                        profile
                );
            } catch (Exception _) {
                // Item previews fail closed; logging every frame would flood the client log.
            }
        }

        @Override
        public void getExtents( java.util.function.@NonNull Consumer<Vector3fc> extents) {
            switch (profile) {
                case GUI -> {
                    extents.accept(new Vector3f(-0.5F, 0.0F, -0.5F));
                    extents.accept(new Vector3f(0.5F, 1.0F, 0.5F));
                }
                case FIRST_PERSON -> {
                    extents.accept(new Vector3f(-2.0F, -2.0F, -2.0F));
                    extents.accept(new Vector3f(2.0F, 2.0F, 2.0F));
                }
                case THIRD_PERSON -> {
                    extents.accept(new Vector3f(-0.45F, 0.0F, -0.45F));
                    extents.accept(new Vector3f(0.45F, 1.4F, 0.45F));
                }
                case FIXED, ON_SHELF -> {
                    extents.accept(new Vector3f(-0.45F, 0.0F, -0.45F));
                    extents.accept(new Vector3f(0.45F, 1.0F, 0.45F));
                }
                default -> {
                    extents.accept(new Vector3f(-0.4F, 0.0F, -0.4F));
                    extents.accept(new Vector3f(0.4F, 1.0F, 0.4F));
                }
            }
        }
    }

    public static final class Default extends VillagerCapturerProfiledSpecialRenderer {
        public Default() { super(SpecialProfile.DEFAULT); }
        @SuppressWarnings({"rawtypes"})
        public static final class Unbaked {
            private static final ConfigurableUnbaked INSTANCE = new ConfigurableUnbaked(Default::new);
            public static final MapCodec<? extends SpecialModelRenderer.Unbaked<SpecialModelRenderer>> MAP_CODEC = INSTANCE.type();
            private Unbaked() {}
        }
    }

    public static final class Gui extends VillagerCapturerProfiledSpecialRenderer {
        public Gui() { super(SpecialProfile.GUI); }
        @SuppressWarnings({"rawtypes"})
        public static final class Unbaked {
            private static final ConfigurableUnbaked INSTANCE = new ConfigurableUnbaked(Gui::new);
            public static final MapCodec<? extends SpecialModelRenderer.Unbaked<SpecialModelRenderer>> MAP_CODEC = INSTANCE.type();
            private Unbaked() {}
        }
    }

    public static final class Fixed extends VillagerCapturerProfiledSpecialRenderer {
        public Fixed() { super(SpecialProfile.FIXED); }
        @SuppressWarnings({"rawtypes"})
        public static final class Unbaked {
            private static final ConfigurableUnbaked INSTANCE = new ConfigurableUnbaked(Fixed::new);
            public static final MapCodec<? extends SpecialModelRenderer.Unbaked<SpecialModelRenderer>> MAP_CODEC = INSTANCE.type();
            private Unbaked() {}
        }
    }

    public static final class OnShelf extends VillagerCapturerProfiledSpecialRenderer {
        public OnShelf() { super(SpecialProfile.ON_SHELF); }
        @SuppressWarnings({"rawtypes"})
        public static final class Unbaked {
            private static final ConfigurableUnbaked INSTANCE = new ConfigurableUnbaked(OnShelf::new);
            public static final MapCodec<? extends SpecialModelRenderer.Unbaked<SpecialModelRenderer>> MAP_CODEC = INSTANCE.type();
            private Unbaked() {}
        }
    }

    public static final class ThirdPerson extends VillagerCapturerProfiledSpecialRenderer {
        public ThirdPerson() { super(SpecialProfile.THIRD_PERSON); }
        @SuppressWarnings({"rawtypes"})
        public static final class Unbaked {
            private static final ConfigurableUnbaked INSTANCE = new ConfigurableUnbaked(ThirdPerson::new);
            public static final MapCodec<? extends SpecialModelRenderer.Unbaked<SpecialModelRenderer>> MAP_CODEC = INSTANCE.type();
            private Unbaked() {}
        }
    }

    public static final class FirstPerson extends VillagerCapturerProfiledSpecialRenderer {
        public FirstPerson() { super(SpecialProfile.FIRST_PERSON); }
        @SuppressWarnings({"rawtypes"})
        public static final class Unbaked {
            private static final ConfigurableUnbaked INSTANCE = new ConfigurableUnbaked(FirstPerson::new);
            public static final MapCodec<? extends SpecialModelRenderer.Unbaked<SpecialModelRenderer>> MAP_CODEC = INSTANCE.type();
            private Unbaked() {}
        }
    }

    // Small helper to avoid repeating the same Unbaked boilerplate for each profile
    @SuppressWarnings({"rawtypes","unchecked"})
    private static final class ConfigurableUnbaked implements SpecialModelRenderer.Unbaked<SpecialModelRenderer> {
        private final java.util.function.Supplier<SpecialModelRenderer> factory;
        private final MapCodec<? extends SpecialModelRenderer.Unbaked<SpecialModelRenderer>> codec;

        ConfigurableUnbaked(java.util.function.Supplier<SpecialModelRenderer> factory) {
            this.factory = factory;
            this.codec = MapCodec.unit(this);
        }

        @Override
        public @NonNull MapCodec<? extends SpecialModelRenderer.Unbaked<SpecialModelRenderer>> type() {
            return this.codec;
        }

        @Override
        public SpecialModelRenderer bake(SpecialModelRenderer.@NonNull BakingContext bakingContext) {
            return this.factory.get();
        }
    }

    /**
     * Internal backend adapter moved into this file so nobody has to jump between files.
     */
    public static final class MinecraftVillagerCapturerRenderBackend {
        public boolean render(Villager villager, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, float partialTick, SpecialProfile profile, int packedLight) {
            Minecraft mc = Minecraft.getInstance();
            net.minecraft.client.renderer.state.level.CameraRenderState cameraState = new net.minecraft.client.renderer.state.level.CameraRenderState();
            cameraState.initialized = profile == SpecialProfile.GUI;

            EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
            PreviewEntityRenderUtil.prepare(villager);
            EntityRenderState state = dispatcher.extractEntity(villager, partialTick);
            PreviewEntityRenderUtil.applyLight(state, packedLight);
            PreviewEntityRenderUtil.suppressWorldEffects(state);
            dispatcher.submit(state, cameraState, 0.0D, 0.0D, 0.0D, poseStack, submitNodeCollector);
            return true;
        }
    }

}
