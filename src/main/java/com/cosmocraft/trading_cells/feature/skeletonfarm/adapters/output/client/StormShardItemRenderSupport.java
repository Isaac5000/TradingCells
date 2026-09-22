package com.cosmocraft.trading_cells.feature.skeletonfarm.adapters.output.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.MapCodec;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.NoDataSpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.Identifier;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.NonNull;

/** Draws the vanilla charged-Creeper energy layer over the Storm Shard model. */
public final class StormShardItemRenderSupport {
    private static final Identifier POWER_TEXTURE = Identifier.withDefaultNamespace(
            "textures/entity/creeper/creeper_armor.png"
    );
    private static final int POWER_COLOR = 0xFF808080;
    private static final float UV_SCROLL_PER_TICK = 0.01F;
    private static final float FRONT_Z = 8.6F / 16.0F;
    private static final float BACK_Z = 7.4F / 16.0F;
    private static final float SIDE_OFFSET = 0.02F / 16.0F;
    private static final float SIDE_UV_DEPTH = 1.0F / 16.0F;

    private static final Identifier SHARD_TEXTURE = Identifier.fromNamespaceAndPath(
            "trading_cells", "textures/item/storm_shard.png");

    private StormShardItemRenderSupport() {
    }

    private static float animationTicks() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null) {
            return minecraft.level.getGameTime()
                    + minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        }
        return (float) (System.nanoTime() / 50_000_000.0D);
    }

    private static OverlayVertex[] buildShardMesh(SpriteContents sprite) {
        List<OverlayVertex> vertices = new ArrayList<>();
        addFrontAndBackFaces(vertices, sprite);
        addOutlineFaces(vertices, sprite);
        return vertices.toArray(OverlayVertex[]::new);
    }

    private static void addFrontAndBackFaces(List<OverlayVertex> vertices, SpriteContents sprite) {
        for (int y = 0; y < sprite.height(); y++) {
            int x = 0;
            while (x < sprite.width()) {
                while (x < sprite.width() && !isOpaque(sprite, x, y)) {
                    x++;
                }
                int start = x;
                while (x < sprite.width() && isOpaque(sprite, x, y)) {
                    x++;
                }
                if (start < x) {
                    addFrontAndBackQuad(vertices, sprite, start, x, y);
                }
            }
        }
    }

    private static void addFrontAndBackQuad(List<OverlayVertex> vertices, SpriteContents sprite, int startX, int endX, int y) {
        float left = pixel(startX, sprite.width());
        float right = pixel(endX, sprite.width());
        float top = 1.0F - pixel(y, sprite.height());
        float bottom = 1.0F - pixel(y + 1, sprite.height());
        float u0 = left;
        float u1 = right;
        float v0 = pixel(y, sprite.height());
        float v1 = pixel(y + 1, sprite.height());

        addQuad(vertices,
                vertex(left, bottom, FRONT_Z, u0, v1, 0.0F, 0.0F, 1.0F),
                vertex(right, bottom, FRONT_Z, u1, v1, 0.0F, 0.0F, 1.0F),
                vertex(right, top, FRONT_Z, u1, v0, 0.0F, 0.0F, 1.0F),
                vertex(left, top, FRONT_Z, u0, v0, 0.0F, 0.0F, 1.0F));
        addQuad(vertices,
                vertex(right, bottom, BACK_Z, u1, v1, 0.0F, 0.0F, -1.0F),
                vertex(left, bottom, BACK_Z, u0, v1, 0.0F, 0.0F, -1.0F),
                vertex(left, top, BACK_Z, u0, v0, 0.0F, 0.0F, -1.0F),
                vertex(right, top, BACK_Z, u1, v0, 0.0F, 0.0F, -1.0F));
    }

    private static void addOutlineFaces(List<OverlayVertex> vertices, SpriteContents sprite) {
        for (int y = 0; y < sprite.height(); y++) {
            for (int x = 0; x < sprite.width(); x++) {
                if (!isOpaque(sprite, x, y)) {
                    continue;
                }
                if (!isOpaque(sprite, x - 1, y)) {
                    addLeftFace(vertices, sprite, x, y);
                }
                if (!isOpaque(sprite, x + 1, y)) {
                    addRightFace(vertices, sprite, x, y);
                }
                if (!isOpaque(sprite, x, y - 1)) {
                    addTopFace(vertices, sprite, x, y);
                }
                if (!isOpaque(sprite, x, y + 1)) {
                    addBottomFace(vertices, sprite, x, y);
                }
            }
        }
    }

    private static void addLeftFace(List<OverlayVertex> vertices, SpriteContents sprite, int x, int y) {
        float edgeX = pixel(x, sprite.width()) - SIDE_OFFSET;
        float top = 1.0F - pixel(y, sprite.height());
        float bottom = 1.0F - pixel(y + 1, sprite.height());
        float u = pixel(x, sprite.width());
        float v0 = pixel(y, sprite.height());
        float v1 = pixel(y + 1, sprite.height());
        addQuad(vertices,
                vertex(edgeX, bottom, BACK_Z, u - SIDE_UV_DEPTH, v1, -1.0F, 0.0F, 0.0F),
                vertex(edgeX, bottom, FRONT_Z, u, v1, -1.0F, 0.0F, 0.0F),
                vertex(edgeX, top, FRONT_Z, u, v0, -1.0F, 0.0F, 0.0F),
                vertex(edgeX, top, BACK_Z, u - SIDE_UV_DEPTH, v0, -1.0F, 0.0F, 0.0F));
    }

    private static void addRightFace(List<OverlayVertex> vertices, SpriteContents sprite, int x, int y) {
        float edgeX = pixel(x + 1, sprite.width()) + SIDE_OFFSET;
        float top = 1.0F - pixel(y, sprite.height());
        float bottom = 1.0F - pixel(y + 1, sprite.height());
        float u = pixel(x + 1, sprite.width());
        float v0 = pixel(y, sprite.height());
        float v1 = pixel(y + 1, sprite.height());
        addQuad(vertices,
                vertex(edgeX, bottom, FRONT_Z, u, v1, 1.0F, 0.0F, 0.0F),
                vertex(edgeX, bottom, BACK_Z, u + SIDE_UV_DEPTH, v1, 1.0F, 0.0F, 0.0F),
                vertex(edgeX, top, BACK_Z, u + SIDE_UV_DEPTH, v0, 1.0F, 0.0F, 0.0F),
                vertex(edgeX, top, FRONT_Z, u, v0, 1.0F, 0.0F, 0.0F));
    }

    private static void addTopFace(List<OverlayVertex> vertices, SpriteContents sprite, int x, int y) {
        float left = pixel(x, sprite.width());
        float right = pixel(x + 1, sprite.width());
        float edgeY = 1.0F - pixel(y, sprite.height()) + SIDE_OFFSET;
        float u0 = left;
        float u1 = right;
        float v = pixel(y, sprite.height());
        addQuad(vertices,
                vertex(left, edgeY, FRONT_Z, u0, v, 0.0F, 1.0F, 0.0F),
                vertex(right, edgeY, FRONT_Z, u1, v, 0.0F, 1.0F, 0.0F),
                vertex(right, edgeY, BACK_Z, u1, v - SIDE_UV_DEPTH, 0.0F, 1.0F, 0.0F),
                vertex(left, edgeY, BACK_Z, u0, v - SIDE_UV_DEPTH, 0.0F, 1.0F, 0.0F));
    }

    private static void addBottomFace(List<OverlayVertex> vertices, SpriteContents sprite, int x, int y) {
        float left = pixel(x, sprite.width());
        float right = pixel(x + 1, sprite.width());
        float edgeY = 1.0F - pixel(y + 1, sprite.height()) - SIDE_OFFSET;
        float u0 = left;
        float u1 = right;
        float v = pixel(y + 1, sprite.height());
        addQuad(vertices,
                vertex(left, edgeY, BACK_Z, u0, v + SIDE_UV_DEPTH, 0.0F, -1.0F, 0.0F),
                vertex(right, edgeY, BACK_Z, u1, v + SIDE_UV_DEPTH, 0.0F, -1.0F, 0.0F),
                vertex(right, edgeY, FRONT_Z, u1, v, 0.0F, -1.0F, 0.0F),
                vertex(left, edgeY, FRONT_Z, u0, v, 0.0F, -1.0F, 0.0F));
    }

    private static boolean isOpaque(SpriteContents sprite, int x, int y) {
        return x >= 0 && x < sprite.width() && y >= 0 && y < sprite.height()
                && !sprite.isTransparent(0, x, y);
    }

    private static float pixel(int coordinate, int size) {
        return (float) coordinate / size;
    }

    private static OverlayVertex vertex(
            float x,
            float y,
            float z,
            float u,
            float v,
            float normalX,
            float normalY,
            float normalZ
    ) {
        return new OverlayVertex(x, y, z, u, v, normalX, normalY, normalZ);
    }

    private static void addQuad(
            List<OverlayVertex> vertices,
            OverlayVertex first,
            OverlayVertex second,
            OverlayVertex third,
            OverlayVertex fourth
    ) {
        vertices.add(first);
        vertices.add(second);
        vertices.add(third);
        vertices.add(fourth);
    }

    private static void drawMesh(OverlayVertex[] mesh, PoseStack.Pose pose, VertexConsumer vertices, int packedLight) {
        for (OverlayVertex meshVertex : mesh) {
            vertices.addVertex(pose, meshVertex.x(), meshVertex.y(), meshVertex.z())
                    .setColor(POWER_COLOR)
                    .setUv(meshVertex.u(), meshVertex.v())
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(packedLight)
                    .setNormal(pose, meshVertex.normalX(), meshVertex.normalY(), meshVertex.normalZ());
        }
    }

    public static final class Renderer implements NoDataSpecialModelRenderer {
        private final OverlayVertex[] mesh;

        private Renderer(SpriteContents sprite) {
            // Rebuilt only on resource reload; rendering retains no image or fixed silhouette.
            mesh = buildShardMesh(sprite);
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
            float offset = animationTicks() * UV_SCROLL_PER_TICK % 1.0F;
            RenderType powerLayer = RenderTypes.energySwirl(POWER_TEXTURE, offset, offset);
            collector.order(1).submitCustomGeometry(
                    poseStack,
                    powerLayer,
                    (pose, vertices) -> drawMesh(mesh, pose, vertices, packedLight)
            );
        }

        @Override
        public void getExtents(Consumer<Vector3fc> extents) {
            extents.accept(new Vector3f(-SIDE_OFFSET, -SIDE_OFFSET, BACK_Z));
            extents.accept(new Vector3f(1.0F + SIDE_OFFSET, 1.0F + SIDE_OFFSET, FRONT_Z));
        }
    }

    private record OverlayVertex(
            float x,
            float y,
            float z,
            float u,
            float v,
            float normalX,
            float normalY,
            float normalZ
    ) {
    }

    public record Unbaked() implements NoDataSpecialModelRenderer.Unbaked {
        public static final Unbaked INSTANCE = new Unbaked();
        public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(INSTANCE);

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }

        @Override
        public SpecialModelRenderer<Void> bake(SpecialModelRenderer.@NonNull BakingContext context) {
            // Special models bake before atlas upload; use the current resource pack directly.
            try (var input = Minecraft.getInstance().getResourceManager().open(SHARD_TEXTURE)) {
                NativeImage image = NativeImage.read(input);
                try (var sprite = new SpriteContents(SHARD_TEXTURE,
                        new FrameSize(image.getWidth(), image.getHeight()), image)) {
                    return new Renderer(sprite);
                }
            } catch (IOException exception) {
                throw new IllegalStateException("Cannot load Storm Shard energy silhouette", exception);
            }
        }
    }
}
