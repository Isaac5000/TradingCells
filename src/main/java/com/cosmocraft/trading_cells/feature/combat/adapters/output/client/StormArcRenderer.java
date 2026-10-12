package com.cosmocraft.trading_cells.feature.combat.adapters.output.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Random;
import java.util.function.Consumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/** The beam shares the weapon's actual baked hand transform, including camera bob. */
public final class StormArcRenderer implements SpecialModelRenderer<StormArcRenderer.Arc> {
    public static final StormArcRenderer INSTANCE = new StormArcRenderer();
    public record Arc(Vec3 target, long seed) { }
    @Override public Arc extractArgument(ItemStack stack) { return null; }
    @Override public void getExtents(Consumer<Vector3fc> output) { output.accept(new Vector3f(0.5F, 9.5F / 16, -12.4F / 16)); }
    @Override public void submit(Arc arc, PoseStack pose, SubmitNodeCollector collector, int light, int overlay, boolean foil, int outline) {
        if (arc == null) { return; }
        Vector3f local = new Matrix4f(pose.last().pose()).invert().transformPosition(arc.target().toVector3f());
        Vec3 start = new Vec3(0.5, 9.5 / 16, -12.4 / 16), end = new Vec3(local);
        collector.submitCustomGeometry(pose, RenderTypes.lightning(), (matrix, vertices) -> {
            Random random = new Random(arc.seed());
            Vec3 axis = end.subtract(start), direction = axis.normalize();
            Vec3 right = direction.cross(Math.abs(direction.y) > 0.9 ? new Vec3(1,0,0) : new Vec3(0,1,0)).normalize();
            Vec3 up = direction.cross(right);
            Vec3 previous = start;
            for (int i = 1; i <= 24; i++) {
                Vec3 point = start.add(axis.scale(i / 24D));
                if (i < 24) { point = point.add(right.scale((random.nextDouble() - 0.5) * 0.14)).add(up.scale((random.nextDouble() - 0.5) * 0.14)); }
                draw(matrix, vertices, previous, point, 0.03, 0x5046CFFF);
                draw(matrix, vertices, previous, point, 0.01, 0xFFF0FFFF);
                if (i % 4 == 0 && i < 24) {
                    double side = random.nextBoolean() ? 1 : -1;
                    Vec3 fork = point.add(right.scale(side * 0.35)).add(up.scale(random.nextDouble() * 0.25)).add(direction.scale(0.3));
                    draw(matrix, vertices, point, fork, 0.007, 0xC080E9FF);
                    draw(matrix, vertices, fork, fork.add(right.scale(side * 0.25)).add(direction.scale(0.45)), 0.003, 0x804AD6FF);
                }
                previous = point;
            }
        });
    }
    private static void draw(PoseStack.Pose pose, VertexConsumer vertices, Vec3 start, Vec3 end, double width, int color) {
        Vec3 direction = end.subtract(start).normalize();
        Vec3 side = direction.cross(Math.abs(direction.y) > 0.9 ? new Vec3(1,0,0) : new Vec3(0,1,0)).normalize().scale(width);
        Vec3 up = direction.cross(side).normalize().scale(width);
        Vec3[] corners = {side.add(up), side.subtract(up), side.scale(-1).subtract(up), up.subtract(side)};
        for (int i = 0; i < 4; i++) {
            vertex(pose, vertices, start.add(corners[i]), color);
            vertex(pose, vertices, end.add(corners[i]), color);
            vertex(pose, vertices, end.add(corners[(i+1)%4]), color);
            vertex(pose, vertices, start.add(corners[(i+1)%4]), color);
        }
    }
    private static void vertex(PoseStack.Pose pose, VertexConsumer vertices, Vec3 p, int color) {
        vertices.addVertex(pose.pose(), (float) p.x, (float) p.y, (float) p.z).setColor(color);
    }
}
