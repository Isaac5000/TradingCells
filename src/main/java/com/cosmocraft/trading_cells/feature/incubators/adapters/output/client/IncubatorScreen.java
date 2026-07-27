package com.cosmocraft.trading_cells.feature.incubators.adapters.output.client;

import com.cosmocraft.trading_cells.feature.incubators.adapters.input.IncubatorBlockEntity;
import com.cosmocraft.trading_cells.feature.incubators.adapters.input.IncubatorMenu;
import com.cosmocraft.trading_cells.feature.captures.domain.model.CapturedMobKind;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.MachineScreenLayout;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.MachineScreenTheme;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.MachineScreenUtil;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.MachineSlotSprites;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public final class IncubatorScreen extends AbstractContainerScreen<IncubatorMenu> {
    private static final Identifier YELLOW_WOOL = Identifier.fromNamespaceAndPath(
            "minecraft",
            "textures/block/yellow_wool.png"
    );
    private static final Identifier RED_WOOL = Identifier.fromNamespaceAndPath(
            "minecraft",
            "textures/block/red_wool.png"
    );
    private static final int PROGRESS_FRAME_X = MachineScreenLayout.machineX(54);
    private static final int PROGRESS_FRAME_Y = 77;
    private static final int PROGRESS_X = PROGRESS_FRAME_X + MachineScreenLayout.PROGRESS_FILL_X_OFFSET;
    private static final int PROGRESS_Y = PROGRESS_FRAME_Y + MachineScreenLayout.PROGRESS_FILL_Y_OFFSET;
    private static final int PROGRESS_WIDTH = MachineScreenLayout.PROGRESS_FILL_WIDTH;
    private static final int PROGRESS_HEIGHT = MachineScreenLayout.PROGRESS_FILL_HEIGHT;

    public IncubatorScreen(IncubatorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, MachineScreenLayout.WIDTH, MachineScreenLayout.HEIGHT);
        titleLabelY = MachineScreenLayout.TITLE_Y;
        inventoryLabelX = MachineScreenLayout.PLAYER_INVENTORY_LABEL_X;
        inventoryLabelY = MachineScreenLayout.PLAYER_INVENTORY_LABEL_Y;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int x = leftPos;
        int y = topPos;
        boolean villager = menu.kind() == CapturedMobKind.VILLAGER;
        Identifier surface = villager ? YELLOW_WOOL : RED_WOOL;
        MachineScreenTheme theme = villager
                ? MachineScreenTheme.VILLAGER_INCUBATOR
                : MachineScreenTheme.PIGLIN_INCUBATOR;

        MachineScreenLayout.drawBackground(graphics, x, y, surface, theme);
        int inputSlotX = MachineScreenLayout.machineX(45);
        MachineScreenLayout.drawSlot(graphics, x, y, inputSlotX, 48, theme);
        MachineScreenLayout.drawSlot(graphics, x, y, MachineScreenLayout.machineX(115), 48, theme);
        if (!menu.getSlot(IncubatorBlockEntity.INPUT_SLOT).hasItem()) {
            MachineScreenLayout.drawEmptySlotSprite(
                    graphics,
                    x,
                    y,
                    inputSlotX,
                    48,
                    villager ? MachineSlotSprites.VILLAGER_HEAD : MachineSlotSprites.PIGLIN_HEAD
            );
        }
        MachineScreenLayout.drawProgressFrame(graphics, x + PROGRESS_FRAME_X, y + PROGRESS_FRAME_Y, theme);

        int progress = menu.maxIncubationTicks() <= 0
                ? 0
                : Math.min(PROGRESS_WIDTH, menu.incubationTicks() * PROGRESS_WIDTH / menu.maxIncubationTicks());
        if (progress > 0) {
            int color = villager ? 0xFFF0C934 : 0xFFD84B45;
            graphics.fill(
                    x + PROGRESS_X,
                    y + PROGRESS_Y,
                    x + PROGRESS_X + progress,
                    y + PROGRESS_Y + PROGRESS_HEIGHT,
                    color
            );
        }
        if (menu.incubationTicks() > 0) {
            MachineScreenUtil.drawCenteredCountdown(
                    graphics,
                    font,
                    x + PROGRESS_X + PROGRESS_WIDTH / 2,
                    y + PROGRESS_Y - 1,
                    menu.incubationTicks(),
                    menu.maxIncubationTicks()
            );
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        MachineScreenTheme theme = menu.kind() == CapturedMobKind.VILLAGER
                ? MachineScreenTheme.VILLAGER_INCUBATOR
                : MachineScreenTheme.PIGLIN_INCUBATOR;
        MachineScreenLayout.drawLabels(graphics, font, title, playerInventoryTitle, theme);
    }
}
