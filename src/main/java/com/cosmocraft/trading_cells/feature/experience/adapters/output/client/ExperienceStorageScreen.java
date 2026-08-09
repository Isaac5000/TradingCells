package com.cosmocraft.trading_cells.feature.experience.adapters.output.client;

import com.cosmocraft.trading_cells.feature.experience.adapters.input.ExperienceStorageMenu;
import com.cosmocraft.trading_cells.feature.experience.domain.model.ExperienceMath;
import com.cosmocraft.trading_cells.feature.experience.domain.model.ExperienceTransferAction;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.MachineScreenLayout;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.MachineScreenTheme;
import com.cosmocraft.trading_cells.platform.neoforge.network.ExperienceStorageTransferPayload;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public final class ExperienceStorageScreen extends AbstractContainerScreen<ExperienceStorageMenu> {
    private static final Identifier SURFACE =
            Identifier.withDefaultNamespace("textures/block/smooth_stone.png");
    private static final MachineScreenTheme THEME = MachineScreenTheme.IRON_FARM;
    private static final int STORAGE_BOX_X = 8;
    private static final int PLAYER_BOX_X = 104;
    private static final int SUMMARY_BOX_Y = 28;
    private static final int SUMMARY_BOX_WIDTH = 92;
    private static final int SUMMARY_BOX_HEIGHT = 38;
    private static final int FIELD_X = 28;
    private static final int FIELD_Y = 72;
    private static final int FIELD_WIDTH = 148;
    private static final int FIELD_HEIGHT = 18;
    private static final int BUTTON_Y = 95;
    private static final int BUTTON_WIDTH = 72;
    private static final int DETAIL_TEXT_COLOR = 0xFF80FF20;
    private static final float DETAIL_TEXT_SCALE = 0.78F;
    private static final int PANEL_INTERIOR = 0xED202628;

    private EditBox amountField;
    private Button depositButton;
    private Button withdrawButton;

    public ExperienceStorageScreen(ExperienceStorageMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, MachineScreenLayout.WIDTH, MachineScreenLayout.HEIGHT);
        titleLabelY = MachineScreenLayout.TITLE_Y;
        inventoryLabelX = MachineScreenLayout.PLAYER_INVENTORY_LABEL_X;
        inventoryLabelY = MachineScreenLayout.PLAYER_INVENTORY_LABEL_Y;
    }

    @Override
    protected void init() {
        super.init();
        amountField = addRenderableWidget(new EditBox(
                font,
                leftPos + FIELD_X,
                topPos + FIELD_Y,
                FIELD_WIDTH,
                FIELD_HEIGHT,
                Component.translatable("gui.trading_cells.experience_amount")
        ));
        amountField.setMaxLength(10);
        amountField.setFilter(value -> value.isEmpty() || value.chars().allMatch(Character::isDigit));
        amountField.setHint(Component.translatable("gui.trading_cells.experience_amount_hint"));
        amountField.setResponder(value -> updateButtonStates());

        depositButton = addRenderableWidget(Button.builder(
                Component.translatable("button.trading_cells.deposit_xp"),
                button -> send(true)
        ).bounds(leftPos + FIELD_X, topPos + BUTTON_Y, BUTTON_WIDTH, 18).build());
        withdrawButton = addRenderableWidget(Button.builder(
                Component.translatable("button.trading_cells.withdraw_xp"),
                button -> send(false)
        ).bounds(leftPos + FIELD_X + BUTTON_WIDTH + 4, topPos + BUTTON_Y, BUTTON_WIDTH, 18).build());
        updateButtonStates();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateButtonStates();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int x = leftPos;
        int y = topPos;
        MachineScreenLayout.drawBackground(graphics, x, y, SURFACE, THEME);
        drawExperienceSummaries(graphics, x, y);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        MachineScreenLayout.drawLabels(graphics, font, title, playerInventoryTitle, THEME);
    }

    private void drawExperienceSummaries(GuiGraphicsExtractor graphics, int x, int y) {
        int storedPoints = menu.storedExperience();
        int playerPoints = playerExperiencePoints();
        drawSummaryBox(
                graphics,
                x + STORAGE_BOX_X,
                y + SUMMARY_BOX_Y,
                Component.translatable("gui.trading_cells.storage_summary"),
                storedPoints,
                ExperienceMath.levelForTotalPoints(storedPoints)
        );
        drawSummaryBox(
                graphics,
                x + PLAYER_BOX_X,
                y + SUMMARY_BOX_Y,
                Component.translatable("gui.trading_cells.player_summary"),
                playerPoints,
                playerLevel()
        );
    }

    private void drawSummaryBox(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            Component heading,
            int experience,
            int levels
    ) {
        graphics.fill(x, y, x + SUMMARY_BOX_WIDTH, y + SUMMARY_BOX_HEIGHT, THEME.frameDark());
        graphics.fill(x + 1, y + 1, x + SUMMARY_BOX_WIDTH - 1, y + SUMMARY_BOX_HEIGHT - 1, THEME.frameLight());
        graphics.fill(x + 2, y + 2, x + SUMMARY_BOX_WIDTH - 2, y + SUMMARY_BOX_HEIGHT - 2, PANEL_INTERIOR);
        graphics.text(
                font,
                heading,
                x + (SUMMARY_BOX_WIDTH - font.width(heading)) / 2,
                y + 4,
                THEME.titleText(),
                true
        );
        drawScaledCenteredText(
                graphics,
                Component.translatable("gui.trading_cells.experience_value", experience),
                x + SUMMARY_BOX_WIDTH / 2,
                y + 15
        );
        drawScaledCenteredText(
                graphics,
                Component.translatable("gui.trading_cells.levels_value", levels),
                x + SUMMARY_BOX_WIDTH / 2,
                y + 25
        );
    }

    private void drawScaledCenteredText(
            GuiGraphicsExtractor graphics,
            Component text,
            int centerX,
            int y
    ) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(centerX, y);
        graphics.pose().scale(DETAIL_TEXT_SCALE, DETAIL_TEXT_SCALE);
        graphics.text(font, text, -font.width(text) / 2, 0, DETAIL_TEXT_COLOR, true);
        graphics.pose().popMatrix();
    }

    private void updateButtonStates() {
        if (depositButton == null || withdrawButton == null) {
            return;
        }
        int storedPoints = menu.storedExperience();
        int playerPoints = playerExperiencePoints();
        long freeCapacity = (long) menu.capacity() - storedPoints;
        boolean transferAll = amountField.getValue().isEmpty();
        if (transferAll) {
            depositButton.active = playerPoints > 0 && freeCapacity > 0;
            withdrawButton.active = storedPoints > 0 && playerPoints < Integer.MAX_VALUE;
            return;
        }

        int requestedLevels = requestedLevels();
        int playerLevel = playerLevel();
        int depositPoints = pointsRemovedByLevels(playerLevel, playerProgress(), requestedLevels);
        depositButton.active = requestedLevels > 0
                && requestedLevels <= playerLevel
                && depositPoints > 0
                && depositPoints <= freeCapacity;
        int withdrawableLevels = ExperienceMath.maximumAdditionalLevels(
                playerLevel,
                playerProgress(),
                storedPoints
        );
        withdrawButton.active = requestedLevels > 0 && requestedLevels <= withdrawableLevels;
    }

    private int requestedLevels() {
        if (amountField == null || amountField.getValue().isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(amountField.getValue());
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private int playerExperiencePoints() {
        return ExperienceMath.totalPoints(playerLevel(), playerProgress());
    }

    private int playerLevel() {
        return minecraft == null || minecraft.player == null ? 0 : minecraft.player.experienceLevel;
    }

    private float playerProgress() {
        return minecraft == null || minecraft.player == null ? 0.0F : minecraft.player.experienceProgress;
    }

    private static int pointsRemovedByLevels(
            int playerLevel,
            float playerProgress,
            int requestedLevels
    ) {
        if (requestedLevels <= 0 || requestedLevels > playerLevel) {
            return 0;
        }
        int currentPoints = ExperienceMath.totalPoints(playerLevel, playerProgress);
        int targetPoints = requestedLevels == playerLevel
                ? 0
                : ExperienceMath.pointsAtLevelWithProgress(playerLevel - requestedLevels, playerProgress);
        return Math.max(0, currentPoints - targetPoints);
    }

    private void send(boolean deposit) {
        boolean transferAll = amountField.getValue().isEmpty();
        ExperienceTransferAction action;
        if (deposit) {
            action = transferAll ? ExperienceTransferAction.DEPOSIT_ALL : ExperienceTransferAction.DEPOSIT;
        } else {
            action = transferAll ? ExperienceTransferAction.WITHDRAW_ALL : ExperienceTransferAction.WITHDRAW;
        }
        ClientPacketDistributor.sendToServer(new ExperienceStorageTransferPayload(
                menu.containerId,
                action.id(),
                transferAll ? 0 : requestedLevels()
        ));
    }
}
