package com.cosmocraft.trading_cells.feature.combat.adapters.output.client;

import com.cosmocraft.trading_cells.feature.combat.adapters.minecraft.ElectricArcWelderItem;
import com.cosmocraft.trading_cells.feature.combat.adapters.minecraft.StormWeaponItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.renderstate.AvatarRenderStateModifier;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class StormArmAnimation {
    private static final ContextKey<Use> USE = new ContextKey<>(Identifier.fromNamespaceAndPath("trading_cells", "storm_arms"));
    private record Use(HumanoidArm arm, boolean welder, boolean loading, float progress) { }
    private StormArmAnimation() { }
    public static float smooth(float value) { float p = Math.clamp(value, 0, 1); return p*p*(3-2*p); }
    public static float insertion(float p) { return smooth((p - 0.2F) / 0.55F); }
    public static float lift(float p) { return smooth(Math.min(p / 0.2F, (1-p) / 0.2F)); }

    public static void register(RegisterRenderStateModifiersEvent event) {
        event.registerAvatarEntityModifier(new AvatarRenderStateModifier() {
            @Override public <T extends Avatar & ClientAvatarEntity> void accept(T avatar, AvatarRenderState state) {
                Use use = null;
                for (var hand : InteractionHand.values()) {
                    var stack = avatar.getItemInHand(hand);
                    boolean welder = stack.getItem() instanceof ElectricArcWelderItem;
                    boolean loading = stack.getItem() instanceof StormWeaponItem && StormWeaponItem.animating(stack);
                    if (welder || loading) {
                        var arm = hand == InteractionHand.MAIN_HAND ? avatar.getMainArm() : avatar.getMainArm().getOpposite();
                        float partial = net.minecraft.client.Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
                        use = new Use(arm, welder, loading, loading ? StormWeaponItem.animationProgress(stack, avatar.level(), partial) : 0);
                        break;
                    }
                }
                state.setRenderData(USE, use);
            }
        });
    }

    public static void apply(HumanoidModel<?> model, HumanoidRenderState state) {
        Use use = state.getRenderData(USE);
        if (use == null) { return; }
        int side = use.arm() == HumanoidArm.RIGHT ? 1 : -1;
        var holding = model.getArm(use.arm());
        float lift = use.loading() ? lift(use.progress()) : 0;
        holding.xRot = Mth.lerp(lift, -Mth.HALF_PI + model.head.xRot * 0.85F, -1.35F);
        holding.yRot = Mth.lerp(lift, model.head.yRot * 0.7F - side * 0.3F, -side * 0.35F);
        holding.zRot = 0;
        if (!use.loading()) { return; }
        var opposite = model.getArm(use.arm().getOpposite());
        float approach = 1 - insertion(use.progress());
        float transfer = lift * (1 - smooth((use.progress()-0.78F)/0.2F));
        var target = use.welder() ? new Vector3f(4.3F - 7*approach, 9.5F, 9.2F) : new Vector3f(5.5F - 7*approach, 3.5F, 8);
        float scale = use.welder() ? 0.6F : 0.75F;
        target.sub(8,8,8).mul(scale).add(0, use.welder() ? 3 : 3.5F, use.welder() ? -2 : 0);
        if (use.welder()) { target.rotateX(75 * lift * Mth.DEG_TO_RAD); }
        target.add(side, 2, -10).rotateY(Mth.PI).rotateX(-Mth.HALF_PI);
        boolean slim = state instanceof AvatarRenderState avatar && avatar.skin.model() == net.minecraft.world.entity.player.PlayerModelType.SLIM;
        var shoulders = new Vector3f(holding.x-opposite.x + (slim ? side*0.5F : 0), holding.y-opposite.y, holding.z-opposite.z);
        var wrist = new Vector3f(side * (slim ? 0.5F : 1), 10, -2);
        // Turn the loaded hand towards the other hand's reachable sphere. Both shoulders stay fixed.
        holding.yRot = Mth.rotLerpRad(transfer, holding.yRot,
                reachableYaw(new Vector3f(target).rotateX(holding.xRot), shoulders, wrist.lengthSquared(), holding.yRot));
        var holdingRotation = new Quaternionf().rotationZYX(holding.zRot, holding.yRot, holding.xRot);
        target.rotate(holdingRotation).add(shoulders);
        var direction = new Vector3f(target).normalize();
        // Inherit the weapon's roll before aiming the supporting wrist at its actual grip.
        var orientation = new Quaternionf().rotationTo(new Vector3f(wrist).normalize().rotate(holdingRotation), direction)
                .mul(holdingRotation);
        var blended = new Quaternionf().rotationZYX(opposite.zRot, opposite.yRot, opposite.xRot)
                .slerp(orientation, transfer).getEulerAnglesZYX(new Vector3f());
        opposite.setRotation(blended.x, blended.y, blended.z);
    }

    private static float reachableYaw(Vector3f cell, Vector3f shoulders, float reachSquared, float previous) {
        float a = shoulders.x*cell.x + shoulders.z*cell.z;
        float b = shoulders.x*cell.z - shoulders.z*cell.x;
        double radius = Math.hypot(a, b);
        if (radius < 1.0e-6) { return previous; }
        double distance = (reachSquared-cell.lengthSquared()-shoulders.lengthSquared())/2 - shoulders.y*cell.y;
        float center = (float) Math.atan2(b, a);
        float angle = (float) Math.acos(Math.clamp(distance/radius, -1, 1));
        float left = Mth.wrapDegrees((center-angle-previous)*Mth.RAD_TO_DEG)*Mth.DEG_TO_RAD;
        float right = Mth.wrapDegrees((center+angle-previous)*Mth.RAD_TO_DEG)*Mth.DEG_TO_RAD;
        return previous + (Math.abs(left) < Math.abs(right) ? left : right);
    }

    public static boolean firstPerson(PoseStack pose, net.minecraft.client.player.LocalPlayer player,
            HumanoidArm arm, ItemStack stack, float partial) {
        boolean welder = stack.getItem() instanceof ElectricArcWelderItem;
        boolean loading = StormWeaponItem.animating(stack);
        if (!welder && !loading) { return false; }
        int side = arm == HumanoidArm.RIGHT ? 1 : -1;
        pose.translate(side * 0.3, -0.28, -0.85);
        if (loading) {
            float p = StormWeaponItem.animationProgress(stack, player.level(), partial);
            pose.translate(-side * 0.15 * lift(p), 0.22 * lift(p), -0.12 * lift(p));
            if (welder) {
                pose.translate(0, -0.203125, 0.171875);
                pose.mulPose(Axis.XP.rotationDegrees(75 * lift(p)));
                pose.mulPose(Axis.ZP.rotationDegrees(35 * lift(p)));
                pose.translate(0, 0.203125, -0.171875);
            }
        }
        return true;
    }
    public static void thirdPersonItem(PoseStack pose, net.minecraft.client.renderer.entity.state.ArmedEntityRenderState state,
            HumanoidArm arm, ItemStack stack) {
        Use use = state.getRenderData(USE);
        if (use != null && use.arm() == arm && use.loading() && use.welder()) {
            pose.mulPose(Axis.XP.rotationDegrees(75 * lift(use.progress())));
        }
    }
}
