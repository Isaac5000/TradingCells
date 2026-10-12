package com.cosmocraft.trading_cells.feature.combat.adapters.output.client;

import com.cosmocraft.trading_cells.feature.combat.adapters.minecraft.ElectricArcWelderItem;
import com.cosmocraft.trading_cells.feature.combat.adapters.minecraft.StormWeaponItem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@EventBusSubscriber(modid = "trading_cells", value = Dist.CLIENT)
public final class StormFirstPersonHands {
    private StormFirstPersonHands() { }
    @SubscribeEvent public static void render(RenderHandEvent event) {
        var player = Minecraft.getInstance().player;
        if (player == null || player.isInvisible()) { return; }
        InteractionHand hand = null;
        for (var candidate : InteractionHand.values()) {
            var stack = player.getItemInHand(candidate);
            if (stack.getItem() instanceof ElectricArcWelderItem
                    || stack.getItem() instanceof StormWeaponItem && StormWeaponItem.animating(stack)) { hand = candidate; break; }
        }
        if (hand == null) { return; }
        var stack = player.getItemInHand(hand);
        boolean loading = StormWeaponItem.animating(stack);
        if (event.getHand() != hand) { if (loading) { event.setCanceled(true); } return; }
        var arm = hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        boolean welder = stack.getItem() instanceof ElectricArcWelderItem;
        float p = loading ? StormWeaponItem.animationProgress(stack, player.level(), event.getPartialTick()) : 0;
        var local = new PoseStack();
        StormArmAnimation.firstPerson(local, player, arm, stack, event.getPartialTick());
        // Match the first-person display transform; both wrists follow actual model points.
        if (welder) { local.translate(0, -1F/16, 1F/16); local.scale(0.5F,0.5F,0.5F); }
        else { local.translate(0, -1F/16, 1F/16); local.scale(0.7F,0.7F,0.7F); }
        local.translate(-0.5,-0.5,-0.5);
        Vector3f grip = new Vector3f(8,3.5F,welder ? 11.5F : 8).div(16).mulPosition(local.last().pose());
        draw(event, arm, grip, 0);
        if (loading) {
            var support = new Vector3f((welder ? 4.3F : 5.5F) - 7*(1-StormArmAnimation.insertion(p)), welder ? 9.5F : 3.5F, welder ? 9.2F : 8);
            support.div(16).mulPosition(local.last().pose());
            draw(event, arm.getOpposite(), support, 2.8F);
        }
    }
    private static void draw(RenderHandEvent event, HumanoidArm arm, Vector3f wrist, float palmFace) {
        var client = Minecraft.getInstance();
        var player = client.player;
        int side = arm == HumanoidArm.RIGHT ? 1 : -1;
        var direction = new Vector3f(wrist).sub(side*0.65F, -0.75F, 0.35F).normalize();
        var pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(wrist.x, wrist.y, wrist.z);
        pose.mulPose(new Quaternionf().rotationTo(new Vector3f(0,1,0), direction));
        pose.scale(0.65F, 0.65F, 0.65F);
        var renderer = client.getEntityRenderDispatcher().getPlayerRenderer(player);
        var initial = renderer.getModel().getArm(arm).getInitialPose();
        // AvatarRenderer adds its own wrist roll after resetting the arm pose.
        // The cell rests on the palm surface instead of inside the opaque hand.
        float centerX = player.getSkin().model() == net.minecraft.world.entity.player.PlayerModelType.SLIM ? 0.5F : 1;
        var tip = new Vector3f(-side * centerX, palmFace != 0 ? 8 : 10, palmFace)
                .rotateZ(side * 0.1F).add(initial.x(), initial.y(), initial.z());
        pose.translate(-tip.x/16, -tip.y/16, -tip.z/16);
        var skin = player.getSkin().body().texturePath();
        if (arm == HumanoidArm.RIGHT) {
            renderer.renderRightHand(pose, event.getSubmitNodeCollector(), event.getPackedLight(), skin, player.isModelPartShown(PlayerModelPart.RIGHT_SLEEVE), player);
        } else {
            renderer.renderLeftHand(pose, event.getSubmitNodeCollector(), event.getPackedLight(), skin, player.isModelPartShown(PlayerModelPart.LEFT_SLEEVE), player);
        }
        pose.popPose();
    }
}
