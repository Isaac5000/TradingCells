package com.cosmocraft.trading_cells.platform.neoforge.integration.rei.mixin;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/** Applies the narrowly scoped compatibility patches required by REI on NeoForge 26.2. */
public final class TradingCellsReiMixinPlugin implements IMixinConfigPlugin {
    private static final String ARCHITECTURY_HANDLER =
            "dev.architectury.event.neoforge.EventHandlerImplClient";
    private static final String REI_DISPLAY_SCREEN =
            "me.shedaniel.rei.impl.client.gui.screen.DefaultDisplayViewingScreen";
    private static final String HANDLER_NAME = "eventContainerScreenEvent";
    private static final String COMPATIBILITY_HANDLER_NAME =
            "trading_cells$renderContainerForeground";
    private static final String REMOVED_EVENT_DESCRIPTOR =
            "(Lnet/neoforged/neoforge/client/event/ContainerScreenEvent$Render$Foreground;)V";
    private static final String CURRENT_EVENT_DESCRIPTOR =
            "(Lnet/neoforged/neoforge/client/event/ScreenEvent$Render$Foreground;)V";

    private final Set<String> patchedTargets = new HashSet<>();

    @Override
    public void onLoad(String mixinPackage) {
        // No setup is required before inspecting the target bytecode.
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return ARCHITECTURY_HANDLER.equals(targetClassName)
                || REI_DISPLAY_SCREEN.equals(targetClassName);
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
        // This compatibility mixin has one fixed target.
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(
            String targetClassName,
            ClassNode targetClass,
            String mixinClassName,
            IMixinInfo mixinInfo
    ) {
        if (!ARCHITECTURY_HANDLER.equals(targetClassName)) {
            return;
        }
        boolean hasCurrentHandler = targetClass.methods.stream().anyMatch(this::isCurrentHandler);
        boolean removedStaleHandler = targetClass.methods.removeIf(this::isStaleHandler);
        if (removedStaleHandler && !hasCurrentHandler) {
            patchedTargets.add(targetClassName);
        }
    }

    @Override
    public void postApply(
            String targetClassName,
            ClassNode targetClass,
            String mixinClassName,
            IMixinInfo mixinInfo
    ) {
        if (!ARCHITECTURY_HANDLER.equals(targetClassName)) {
            return;
        }
        if (!patchedTargets.remove(targetClassName)) {
            targetClass.methods.removeIf(this::isCompatibilityHandler);
        }
    }

    private boolean isStaleHandler(MethodNode method) {
        return HANDLER_NAME.equals(method.name) && REMOVED_EVENT_DESCRIPTOR.equals(method.desc);
    }

    private boolean isCurrentHandler(MethodNode method) {
        return HANDLER_NAME.equals(method.name) && CURRENT_EVENT_DESCRIPTOR.equals(method.desc);
    }

    private boolean isCompatibilityHandler(MethodNode method) {
        return COMPATIBILITY_HANDLER_NAME.equals(method.name)
                && CURRENT_EVENT_DESCRIPTOR.equals(method.desc);
    }
}
