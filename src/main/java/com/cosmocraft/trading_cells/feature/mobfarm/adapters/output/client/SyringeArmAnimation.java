package com.cosmocraft.trading_cells.feature.mobfarm.adapters.output.client;

import com.cosmocraft.trading_cells.feature.mobfarm.adapters.input.EssenceExtractorItem;
import com.cosmocraft.trading_cells.feature.mobfarm.domain.model.SyringeAnimation;
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
import net.neoforged.neoforge.client.renderstate.AvatarRenderStateModifier;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class SyringeArmAnimation {
    private static final ContextKey<Use> USE = new ContextKey<>(Identifier.fromNamespaceAndPath("trading_cells", "syringe_use"));
    private record Use(HumanoidArm arm, boolean extracting, int duration) { }

    private SyringeArmAnimation() { }

    public static void register(RegisterRenderStateModifiersEvent event) {
        event.registerAvatarEntityModifier(new AvatarRenderStateModifier() {
            @Override public <T extends Avatar & ClientAvatarEntity> void accept(T avatar, AvatarRenderState state) {
                var stack = avatar.getUseItem();
                Use use = null;
                if (avatar.isUsingItem() && stack.getItem() instanceof EssenceExtractorItem) {
                    int tier = EssenceExtractorItem.extractionTier(stack);
                    HumanoidArm arm = avatar.getUsedItemHand() == InteractionHand.MAIN_HAND
                            ? avatar.getMainArm() : avatar.getMainArm().getOpposite();
                    use = new Use(arm, tier > 0, tier > 0 ? EssenceExtractorItem.extractionTicks(tier) : EssenceExtractorItem.RELOAD_TICKS);
                }
                state.setRenderData(USE, use);
            }
        });
    }

    public static void apply(HumanoidModel<?> model, HumanoidRenderState state) {
        Use use = state.getRenderData(USE);
        if (use == null) { return; }
        float progress = Math.clamp(state.ticksUsingItem / use.duration(), 0, 1);
        float envelope = use.extracting() ? SyringeAnimation.armExtension(state.ticksUsingItem)
                : smooth(Math.min(state.ticksUsingItem / 3, (use.duration() - state.ticksUsingItem) / 3));
        int side = use.arm() == HumanoidArm.RIGHT ? 1 : -1;
        var holding = model.getArm(use.arm());
        holding.xRot = Mth.lerp(envelope, holding.xRot, use.extracting() ? -Mth.HALF_PI + model.head.xRot * 0.5F : -1.1F);
        holding.yRot = Mth.lerp(envelope, holding.yRot, use.extracting() ? model.head.yRot * 0.5F : -side * 0.65F);
        holding.zRot = Mth.lerp(envelope, holding.zRot, 0);
        if (use.extracting()) { return; }

        // Match the baked vial path and the third-person grip, in model pixels.
        float approach = 1 - smooth(progress / 0.75F);
        var opposite = model.getArm(use.arm().getOpposite());
        var target = new Vector3f(side * (-1 + 5.1F * approach), 10.255F, -8.12F - 1.7F * approach)
                .rotate(new Quaternionf().rotationZYX(holding.zRot, holding.yRot, holding.xRot))
                .add(holding.x - opposite.x, holding.y - opposite.y, holding.z - opposite.z);
        var angles = new Quaternionf().rotationTo(new Vector3f(side, 10, -2).normalize(), target.normalize())
                .getEulerAnglesZYX(new Vector3f());
        float placing = envelope * (1 - smooth((progress - 0.78F) / 0.22F));
        opposite.xRot = Mth.lerp(placing, opposite.xRot, angles.x);
        opposite.yRot = Mth.lerp(placing, opposite.yRot, angles.y);
        opposite.zRot = Mth.lerp(placing, opposite.zRot, angles.z);
    }

    private static float smooth(float value) {
        float clamped = Math.clamp(value, 0, 1);
        return clamped * clamped * (3 - 2 * clamped);
    }
}
