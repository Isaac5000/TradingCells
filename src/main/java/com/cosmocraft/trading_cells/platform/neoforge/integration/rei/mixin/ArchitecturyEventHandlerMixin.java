package com.cosmocraft.trading_cells.platform.neoforge.integration.rei.mixin;

import dev.architectury.event.events.client.ClientGuiEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;

/**
 * Bridges Architectury's container foreground callback after NeoForge moved it
 * to {@link ScreenEvent.Render.Foreground} in 26.2.0.40-beta.
 */
@Pseudo
@Mixin(targets = "dev.architectury.event.neoforge.EventHandlerImplClient", remap = false)
abstract class ArchitecturyEventHandlerMixin {
    @Unique
    @SubscribeEvent(priority = EventPriority.HIGH)
    private static void trading_cells$renderContainerForeground(ScreenEvent.Render.Foreground event) {
        if (event.getScreen() instanceof AbstractContainerScreen<?> containerScreen) {
            ClientGuiEvent.RENDER_CONTAINER_FOREGROUND.invoker().render(
                    containerScreen,
                    event.getGuiGraphics(),
                    event.getMouseX(),
                    event.getMouseY(),
                    Minecraft.getInstance().getDeltaTracker().getRealtimeDeltaTicks()
            );
        }
    }
}
