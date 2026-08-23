package com.cosmocraft.trading_cells.platform.neoforge.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.NoDataSpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.NonNull;

import java.util.function.Consumer;

/** Adds a small machine-specific badge over a reusable upgrade base model. */
public final class UpgradeBadgeItemRenderSupport {
    private static final float LEFT = 8.0F / 16.0F;
    private static final float RIGHT = 1.0F;
    private static final float BOTTOM = 0.0F;
    private static final float TOP = 8.0F / 16.0F;
    private static final float FRONT_Z = 8.7F / 16.0F;
    private static final float BACK_Z = 7.3F / 16.0F;

    private UpgradeBadgeItemRenderSupport() {
    }

    private static void drawBadge(PoseStack.Pose pose, VertexConsumer vertices, int packedLight) {
        addVertex(vertices, pose, LEFT, BOTTOM, FRONT_Z, 0.0F, 1.0F, packedLight, 1.0F);
        addVertex(vertices, pose, RIGHT, BOTTOM, FRONT_Z, 1.0F, 1.0F, packedLight, 1.0F);
        addVertex(vertices, pose, RIGHT, TOP, FRONT_Z, 1.0F, 0.0F, packedLight, 1.0F);
        addVertex(vertices, pose, LEFT, TOP, FRONT_Z, 0.0F, 0.0F, packedLight, 1.0F);

        addVertex(vertices, pose, RIGHT, BOTTOM, BACK_Z, 1.0F, 1.0F, packedLight, -1.0F);
        addVertex(vertices, pose, LEFT, BOTTOM, BACK_Z, 0.0F, 1.0F, packedLight, -1.0F);
        addVertex(vertices, pose, LEFT, TOP, BACK_Z, 0.0F, 0.0F, packedLight, -1.0F);
        addVertex(vertices, pose, RIGHT, TOP, BACK_Z, 1.0F, 0.0F, packedLight, -1.0F);
    }

    private static void addVertex(
            VertexConsumer vertices,
            PoseStack.Pose pose,
            float x,
            float y,
            float z,
            float u,
            float v,
            int packedLight,
            float normalZ
    ) {
        vertices.addVertex(pose, x, y, z)
                .setColor(0xFFFFFFFF)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(pose, 0.0F, 0.0F, normalZ);
    }

    public static final class Renderer implements NoDataSpecialModelRenderer {
        private final Identifier texture;

        private Renderer(Identifier texture) {
            this.texture = texture;
        }

        @Override
        public void submit(
                @NonNull PoseStack poseStack,
                @NonNull SubmitNodeCollector collector,
                int packedLight,
                int packedOverlay,
                boolean hasFoil,
                int seed
        ) {
            RenderType renderType = RenderTypes.entityTranslucentCullItemTarget(texture);
            collector.order(1).submitCustomGeometry(
                    poseStack,
                    renderType,
                    (pose, vertices) -> drawBadge(pose, vertices, packedLight)
            );
        }

        @Override
        public void getExtents(Consumer<Vector3fc> extents) {
            extents.accept(new Vector3f(LEFT, BOTTOM, BACK_Z));
            extents.accept(new Vector3f(RIGHT, TOP, FRONT_Z));
        }
    }

    public record Unbaked(Badge badge) implements NoDataSpecialModelRenderer.Unbaked {
        public static final MapCodec<Unbaked> MAP_CODEC = Badge.CODEC.fieldOf("badge")
                .xmap(Unbaked::new, Unbaked::badge);

        @Override
        public @NonNull MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }

        @Override
        public SpecialModelRenderer<Void> bake(SpecialModelRenderer.@NonNull BakingContext context) {
            return new Renderer(badge.texture());
        }
    }

    public enum Badge {
        QUARRY("quarry", "textures/item/iron_pickaxe.png"),
        BARTER("barter", "textures/item/gold_ingot.png");

        private static final Codec<Badge> CODEC = Codec.STRING.comapFlatMap(
                Badge::decode,
                Badge::serializedName
        );

        private final String serializedName;
        private final Identifier texture;

        Badge(String serializedName, String texturePath) {
            this.serializedName = serializedName;
            this.texture = Identifier.withDefaultNamespace(texturePath);
        }

        private static DataResult<Badge> decode(String name) {
            for (Badge badge : values()) {
                if (badge.serializedName.equals(name)) {
                    return DataResult.success(badge);
                }
            }
            return DataResult.error(() -> "Unknown upgrade badge: " + name);
        }

        private String serializedName() {
            return serializedName;
        }

        private Identifier texture() {
            return texture;
        }
    }
}
