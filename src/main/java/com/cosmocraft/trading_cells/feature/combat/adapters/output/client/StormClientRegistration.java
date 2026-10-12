package com.cosmocraft.trading_cells.feature.combat.adapters.output.client;

import com.cosmocraft.trading_cells.feature.combat.adapters.minecraft.StormChargeData;
import com.cosmocraft.trading_cells.feature.combat.adapters.minecraft.StormWeaponItem;
import com.cosmocraft.trading_cells.feature.combat.adapters.minecraft.VoltaicCapacitorItem;
import com.cosmocraft.trading_cells.feature.combat.adapters.output.CombatRegistrationAdapter;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterRangeSelectItemModelPropertyEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

@EventBusSubscriber(modid = "trading_cells", value = Dist.CLIENT)
public final class StormClientRegistration {
    private StormClientRegistration() { }
    @SubscribeEvent public static void extensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            @Override public net.minecraft.client.model.HumanoidModel.ArmPose getArmPose(
                    net.minecraft.world.entity.LivingEntity user, net.minecraft.world.InteractionHand hand,
                    net.minecraft.world.item.ItemStack stack) {
                return null;
            }
            @Override public boolean applyForgeHandTransform(com.mojang.blaze3d.vertex.PoseStack pose,
                    net.minecraft.client.player.LocalPlayer player, net.minecraft.world.entity.HumanoidArm arm,
                    net.minecraft.world.item.ItemStack stack, float partial, float equip, float swing) {
                return StormArmAnimation.firstPerson(pose, player, arm, stack, partial);
            }
        }, CombatRegistrationAdapter.WELDER.get(), CombatRegistrationAdapter.BATON.get());
    }
    @SubscribeEvent public static void arms(net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent event) {
        StormArmAnimation.register(event);
    }
    @SubscribeEvent public static void models(net.neoforged.neoforge.client.event.RegisterItemModelsEvent event) {
        event.register(Identifier.fromNamespaceAndPath("trading_cells", "storm_beam"), StormBeamItemModel.Unbaked.CODEC);
    }
    @SubscribeEvent public static void screens(RegisterMenuScreensEvent event) {
        event.register(CombatRegistrationAdapter.STATION_MENU.get(), StormStationScreen::new);
    }
    @SubscribeEvent public static void properties(RegisterRangeSelectItemModelPropertyEvent event) {
        event.register(Identifier.fromNamespaceAndPath("trading_cells", "storm_weapon"), StormWeaponProperty.CODEC);
    }
    @SubscribeEvent public static void tooltip(ItemTooltipEvent event) {
        var stack = event.getItemStack();
        boolean weapon = stack.getItem() instanceof StormWeaponItem;
        if (!weapon && !(stack.getItem() instanceof VoltaicCapacitorItem)) { return; }
        if (weapon) {
            var battery = StormWeaponItem.battery(stack);
            event.getToolTip().add((battery.isEmpty()
                    ? Component.translatable("tooltip.trading_cells.storm.unloaded")
                    : Component.translatable("tooltip.trading_cells.storm.charge", StormChargeData.charge(battery), StormChargeData.capacity(battery)))
                    .withStyle(ChatFormatting.AQUA));
            event.getToolTip().add(Component.translatable(stack.is(CombatRegistrationAdapter.WELDER.get())
                    ? "tooltip.trading_cells.storm.welder" : "tooltip.trading_cells.storm.baton").withStyle(ChatFormatting.GRAY));
            event.getToolTip().add(Component.translatable("tooltip.trading_cells.storm.reload", StormReloadKey.RELOAD.getTranslatedKeyMessage()).withStyle(ChatFormatting.GRAY));
        } else {
            event.getToolTip().add(Component.translatable("tooltip.trading_cells.storm.charge", StormChargeData.charge(stack),
                    StormChargeData.capacity(stack)).withStyle(ChatFormatting.AQUA));
        }
    }
}
