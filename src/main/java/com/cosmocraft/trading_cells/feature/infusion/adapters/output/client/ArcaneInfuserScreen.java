package com.cosmocraft.trading_cells.feature.infusion.adapters.output.client;

import com.cosmocraft.trading_cells.feature.infusion.adapters.input.ArcaneInfuserBlockEntity;
import com.cosmocraft.trading_cells.feature.infusion.adapters.input.ArcaneInfuserMenu;
import com.cosmocraft.trading_cells.feature.infusion.adapters.minecraft.ArcaneInfusionRecipeDisplay;
import com.cosmocraft.trading_cells.feature.infusion.domain.model.ArcaneInfusionTransferAction;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.MachineScreenLayout;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.MachineScreenTheme;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.NonNegativeIntegerEditBox;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.trader.VillagerTradeScreenCommon;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.trader.VillagerTradeSprites;
import com.cosmocraft.trading_cells.platform.neoforge.network.ArcaneInfuserTransferPayload;
import com.cosmocraft.trading_cells.shared.machines.domain.model.MinecraftExperience;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public final class ArcaneInfuserScreen extends AbstractContainerScreen<ArcaneInfuserMenu>
        implements RecipeUpdateListener {
    private static final Identifier SURFACE =
            Identifier.withDefaultNamespace("textures/block/end_stone.png");
    private static final WidgetSprites RECIPE_BOOK_BUTTON_SPRITES = new WidgetSprites(
            Identifier.fromNamespaceAndPath("trading_cells", "arcane_infuser/recipe_book_button"),
            Identifier.fromNamespaceAndPath("trading_cells", "arcane_infuser/recipe_book_button_highlighted")
    );
    private static final MachineScreenTheme THEME = MachineScreenTheme.IRON_FARM;
    private static final int RECIPE_PANEL_X = 7;
    private static final int RECIPE_PANEL_Y = 25;
    private static final int RECIPE_PANEL_WIDTH = 104;
    private static final int RECIPE_PANEL_HEIGHT = 91;
    private static final int CONTROL_PANEL_X = 114;
    private static final int CONTROL_PANEL_Y = 25;
    private static final int CONTROL_PANEL_WIDTH = 86;
    private static final int CONTROL_PANEL_HEIGHT = 91;
    private static final int CONTROL_X = 117;
    private static final int CONTROL_WIDTH = 80;
    private static final int SUMMARY_Y = 28;
    private static final int SUMMARY_WIDTH = 40;
    private static final int SUMMARY_HEIGHT = 29;
    private static final int STORAGE_SUMMARY_X = 117;
    private static final int PLAYER_SUMMARY_X = 158;
    private static final int AMOUNT_LABEL_Y = 59;
    private static final int FIELD_Y = 66;
    private static final int FIELD_HEIGHT = 13;
    private static final int BUTTON_HEIGHT = 15;
    private static final int DEPOSIT_BUTTON_Y = 81;
    private static final int WITHDRAW_BUTTON_Y = 98;
    private static final int RECIPE_EXPERIENCE_X = 10;
    public static final int RECIPE_EXPERIENCE_Y = 102;
    private static final int RECIPE_EXPERIENCE_WIDTH = 98;
    private static final int RECIPE_EXPERIENCE_HEIGHT = 12;
    private static final int RECIPE_BOOK_WIDE_VIEWPORT_TRIM = 26;
    public static final int RECIPE_VIEWER_X = MachineScreenLayout.machineX(56);
    public static final int RECIPE_VIEWER_Y = ArcaneInfuserMenu.OUTPUT_SLOT_Y + 2;
    public static final int RECIPE_VIEWER_WIDTH = VillagerTradeSprites.ARROW_WIDTH;
    public static final int RECIPE_VIEWER_HEIGHT = VillagerTradeSprites.ARROW_HEIGHT;
    private static final int PANEL_INTERIOR = 0xE825202C;
    private static final int SUMMARY_INTERIOR = 0xED202628;
    private static final int VALUE_TEXT_COLOR = 0xFF80FF20;
    private static final int GHOST_RESULT_OVERLAY = 0x9825202C;
    private static final float SUMMARY_HEADING_SCALE = 0.56F;
    private static final float SUMMARY_VALUE_SCALE = 0.55F;
    private static final float AMOUNT_LABEL_SCALE = 0.54F;

    private EditBox amountField;
    private Button depositButton;
    private Button withdrawButton;
    private InfuserControlButton xpModeButton;
    private InfuserControlButton lockButton;
    private final ArcaneInfusionRecipeBookComponent recipeBookComponent;
    private boolean widthTooNarrow;
    private int selectedRecipeExperience = -1;
    private boolean displayedLockedGhost;

    public ArcaneInfuserScreen(ArcaneInfuserMenu menu, Inventory inventory, Component title) {
        this(menu, inventory, title, new ArcaneInfusionRecipeBookComponent(menu));
    }

    private ArcaneInfuserScreen(
            ArcaneInfuserMenu menu,
            Inventory inventory,
            Component title,
            ArcaneInfusionRecipeBookComponent recipeBookComponent
    ) {
        super(menu, inventory, title, MachineScreenLayout.WIDTH, MachineScreenLayout.HEIGHT);
        this.recipeBookComponent = recipeBookComponent;
        titleLabelY = MachineScreenLayout.TITLE_Y;
        inventoryLabelX = MachineScreenLayout.PLAYER_INVENTORY_LABEL_X;
        inventoryLabelY = MachineScreenLayout.PLAYER_INVENTORY_LABEL_Y;
    }

    @Override
    protected void init() {
        super.init();
        widthTooNarrow = width < 379;
        int recipeBookViewportWidth = width - (widthTooNarrow ? 0 : RECIPE_BOOK_WIDE_VIEWPORT_TRIM);
        recipeBookComponent.init(recipeBookViewportWidth, height, minecraft, widthTooNarrow);
        leftPos = recipeBookComponent.updateScreenPosition(width, imageWidth);
        initRecipeBookButton();
        addWidget(recipeBookComponent);
        amountField = addRenderableWidget(new NonNegativeIntegerEditBox(
                font,
                leftPos + CONTROL_X,
                topPos + FIELD_Y,
                CONTROL_WIDTH,
                FIELD_HEIGHT,
                Component.translatable("gui.trading_cells.experience_amount")
        ));
        amountField.setMaxLength(10);
        amountField.setHint(Component.translatable("gui.trading_cells.experience_amount_hint_short"));
        amountField.setTooltip(Tooltip.create(
                Component.translatable("gui.trading_cells.experience_amount_hint")
        ));
        amountField.setResponder(value -> updateButtonStates());

        depositButton = addRenderableWidget(Button.builder(
                Component.translatable("button.trading_cells.deposit_xp"),
                button -> send(true)
        ).bounds(leftPos + CONTROL_X, topPos + DEPOSIT_BUTTON_Y, CONTROL_WIDTH, BUTTON_HEIGHT).build());
        withdrawButton = addRenderableWidget(Button.builder(
                Component.translatable("button.trading_cells.withdraw_xp"),
                button -> send(false)
        ).bounds(leftPos + CONTROL_X, topPos + WITHDRAW_BUTTON_Y, CONTROL_WIDTH, BUTTON_HEIGHT).build());
        xpModeButton = addRenderableWidget(new InfuserControlButton(leftPos + 69, topPos + 81,
                false, () -> menu.fillStorage() ? 1 : 0, button -> automationAction(0)));
        lockButton = addRenderableWidget(new InfuserControlButton(leftPos + 89, topPos + 81,
                true, menu::lockState, button -> automationAction(1)));
        updateButtonStates();
    }

    @Override
    public void containerTick() {
        super.containerTick();
        recipeBookComponent.tick();
        if (menu.lockState() != 0 && menu.automationDisplay() != null) {
            recipeBookComponent.fillGhostRecipe(menu.automationDisplay());
            selectedRecipeExperience = menu.automationDisplay().experience();
            displayedLockedGhost = true;
        } else if (displayedLockedGhost) {
            recipeBookComponent.slotClicked(menu.slots.get(0));
            selectedRecipeExperience = -1;
            displayedLockedGhost = false;
        }
        updateButtonStates();
    }

    protected ScreenPosition getRecipeBookButtonPosition() {
        return new ScreenPosition(
                leftPos + ArcaneInfuserMenu.OUTPUT_SLOT_X - 1,
                topPos + RECIPE_PANEL_Y + 3
        );
    }

    protected void onRecipeBookButtonClick() {
        repositionTransferWidgets();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int x = leftPos;
        int y = topPos;
        MachineScreenLayout.drawBackground(graphics, x, y, SURFACE, THEME);
        drawPanel(graphics, x + RECIPE_PANEL_X, y + RECIPE_PANEL_Y, RECIPE_PANEL_WIDTH, RECIPE_PANEL_HEIGHT);
        drawPanel(graphics, x + CONTROL_PANEL_X, y + CONTROL_PANEL_Y, CONTROL_PANEL_WIDTH, CONTROL_PANEL_HEIGHT);
        drawMachineSlots(graphics, x, y);
        VillagerTradeScreenCommon.drawTradeArrow(
                graphics,
                x + RECIPE_VIEWER_X,
                y + RECIPE_VIEWER_Y,
                isRecipeViewerHovered(mouseX, mouseY)
                        ? VillagerTradeSprites.State.HOVERED
                        : VillagerTradeSprites.State.NORMAL
        );
        drawRecipeExperience(graphics, x, y);
        drawExperienceSummaries(graphics, x, y);
        drawAmountLabel(graphics, x, y);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (recipeBookComponent.isVisible() && widthTooNarrow) {
            extractBackground(graphics, mouseX, mouseY, partialTick);
        } else {
            super.extractContents(graphics, mouseX, mouseY, partialTick);
        }
        graphics.nextStratum();
        recipeBookComponent.extractRenderState(graphics, mouseX, mouseY, partialTick);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(
                new net.neoforged.neoforge.client.event.ScreenEvent.Render.Foreground(
                        this,
                        graphics,
                        mouseX,
                        mouseY,
                        partialTick
                )
        );
        graphics.nextStratum();
        extractCarriedItem(graphics, mouseX, mouseY);
        extractTooltip(graphics, mouseX, mouseY);
        recipeBookComponent.extractTooltip(graphics, mouseX, mouseY, hoveredSlot);
    }

    @Override
    protected void extractSlots(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractSlots(graphics, mouseX, mouseY);
        recipeBookComponent.extractGhostRecipe(graphics, false);
    }

    @Override
    protected void extractSlot(GuiGraphicsExtractor graphics, Slot slot, int mouseX, int mouseY) {
        super.extractSlot(graphics, slot, mouseX, mouseY);
        if (slot.index == ArcaneInfuserBlockEntity.OUTPUT_SLOT
                && slot.hasItem()
                && menu.insufficientRecipeExperience()) {
            graphics.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, GHOST_RESULT_OVERLAY);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        MachineScreenLayout.drawLabels(graphics, font, title, playerInventoryTitle, THEME);
    }

    private void drawMachineSlots(GuiGraphicsExtractor graphics, int x, int y) {
        for (int slot = 0; slot < ArcaneInfuserBlockEntity.INPUT_SLOT_COUNT; slot++) {
            drawSlot(graphics, x, y, ArcaneInfuserMenu.inputSlotX(slot), ArcaneInfuserMenu.inputSlotY(slot));
        }
        drawSlot(graphics, x, y, ArcaneInfuserMenu.OUTPUT_SLOT_X, ArcaneInfuserMenu.OUTPUT_SLOT_Y);
    }

    private void drawSlot(GuiGraphicsExtractor graphics, int x, int y, int slotX, int slotY) {
        MachineScreenLayout.drawSlot(graphics, x, y, slotX, slotY, THEME);
    }

    private void drawExperienceSummaries(GuiGraphicsExtractor graphics, int x, int y) {
        int storedPoints = menu.storedExperience();
        Component storedExperience = Component.translatable(
                "gui.trading_cells.arcane_experience",
                storedPoints
        );
        Component playerExperience = Component.translatable(
                "gui.trading_cells.arcane_player_experience",
                playerExperiencePoints()
        );
        Component storedLevels = Component.translatable(
                "gui.trading_cells.arcane_levels",
                MinecraftExperience.levelForTotalPoints(storedPoints)
        );
        Component playerLevels = Component.translatable(
                "gui.trading_cells.arcane_levels",
                playerLevel()
        );
        float experienceScale = commonFittedScale(storedExperience, playerExperience, SUMMARY_VALUE_SCALE);
        float levelsScale = commonFittedScale(storedLevels, playerLevels, SUMMARY_VALUE_SCALE);
        drawCompactSummary(
                graphics,
                x + STORAGE_SUMMARY_X,
                y + SUMMARY_Y,
                Component.translatable("gui.trading_cells.storage_summary"),
                storedExperience,
                storedLevels,
                experienceScale,
                levelsScale
        );
        drawCompactSummary(
                graphics,
                x + PLAYER_SUMMARY_X,
                y + SUMMARY_Y,
                Component.translatable("gui.trading_cells.player_summary"),
                playerExperience,
                playerLevels,
                experienceScale,
                levelsScale
        );
    }

    private void drawCompactSummary(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            Component heading,
            Component experience,
            Component levels,
            float experienceScale,
            float levelsScale
    ) {
        graphics.fill(x, y, x + SUMMARY_WIDTH, y + SUMMARY_HEIGHT, THEME.frameDark());
        graphics.fill(x + 1, y + 1, x + SUMMARY_WIDTH - 1, y + SUMMARY_HEIGHT - 1, THEME.frame());
        graphics.fill(x + 2, y + 2, x + SUMMARY_WIDTH - 2, y + SUMMARY_HEIGHT - 2, SUMMARY_INTERIOR);
        drawFittedCenteredText(
                graphics,
                heading,
                x + SUMMARY_WIDTH / 2,
                y + 3,
                SUMMARY_WIDTH - 4,
                SUMMARY_HEADING_SCALE,
                THEME.titleText()
        );
        drawCenteredScaledText(
                graphics,
                experience,
                x + SUMMARY_WIDTH / 2,
                y + 12,
                experienceScale,
                VALUE_TEXT_COLOR
        );
        drawCenteredScaledText(
                graphics,
                levels,
                x + SUMMARY_WIDTH / 2,
                y + 20,
                levelsScale,
                VALUE_TEXT_COLOR
        );
    }

    private void drawAmountLabel(GuiGraphicsExtractor graphics, int x, int y) {
        drawFittedCenteredText(
                graphics,
                Component.translatable("gui.trading_cells.arcane_amount_label"),
                x + CONTROL_X + CONTROL_WIDTH / 2,
                y + AMOUNT_LABEL_Y,
                CONTROL_WIDTH,
                AMOUNT_LABEL_SCALE,
                THEME.titleText()
        );
    }

    private void drawRecipeExperience(GuiGraphicsExtractor graphics, int x, int y) {
        int required = menu.requiredExperience();
        if (required == 0 && selectedRecipeExperience >= 0) {
            required = selectedRecipeExperience;
        }
        int available = required == 0 ? 0 : Math.min(menu.storedExperience(), required);
        int panelX = x + RECIPE_EXPERIENCE_X;
        int panelY = y + RECIPE_EXPERIENCE_Y;
        graphics.fill(
                panelX,
                panelY,
                panelX + RECIPE_EXPERIENCE_WIDTH,
                panelY + RECIPE_EXPERIENCE_HEIGHT,
                THEME.frameDark()
        );
        graphics.fill(
                panelX + 1,
                panelY + 1,
                panelX + RECIPE_EXPERIENCE_WIDTH - 1,
                panelY + RECIPE_EXPERIENCE_HEIGHT - 1,
                THEME.frame()
        );
        graphics.fill(
                panelX + 2,
                panelY + 2,
                panelX + RECIPE_EXPERIENCE_WIDTH - 2,
                panelY + RECIPE_EXPERIENCE_HEIGHT - 2,
                SUMMARY_INTERIOR
        );
        if (required > 0) {
            drawFittedCenteredText(
                    graphics,
                    Component.translatable(
                            "gui.trading_cells.arcane_recipe_experience",
                            available,
                            required
                    ),
                    panelX + RECIPE_EXPERIENCE_WIDTH / 2,
                    panelY + 4,
                    RECIPE_EXPERIENCE_WIDTH - 6,
                    0.62F,
                    VALUE_TEXT_COLOR
            );
        }
    }

    private void drawPanel(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, THEME.frameDark());
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, THEME.frameLight());
        graphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, PANEL_INTERIOR);
    }

    private void drawFittedCenteredText(
            GuiGraphicsExtractor graphics,
            Component text,
            int centerX,
            int y,
            int maximumWidth,
            float maximumScale,
            int color
    ) {
        drawCenteredScaledText(
                graphics,
                text,
                centerX,
                y,
                fittedScale(text, maximumWidth, maximumScale),
                color
        );
    }

    private void drawCenteredScaledText(
            GuiGraphicsExtractor graphics,
            Component text,
            int centerX,
            int y,
            float scale,
            int color
    ) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(centerX, y);
        graphics.pose().scale(scale, scale);
        graphics.text(font, text, -font.width(text) / 2, 0, color, true);
        graphics.pose().popMatrix();
    }

    private float commonFittedScale(Component first, Component second, float maximumScale) {
        return Math.min(
                fittedScale(first, SUMMARY_WIDTH - 4, maximumScale),
                fittedScale(second, SUMMARY_WIDTH - 4, maximumScale)
        );
    }

    private float fittedScale(Component text, int maximumWidth, float maximumScale) {
        int textWidth = font.width(text);
        return textWidth <= 0
                ? maximumScale
                : Math.min(maximumScale, (float) maximumWidth / textWidth);
    }

    private void updateButtonStates() {
        if (xpModeButton != null && lockButton != null) {
            Component mode = Component.translatable(menu.fillStorage()
                    ? "gui.trading_cells.xp.fill_storage" : "gui.trading_cells.xp.active_recipe");
            xpModeButton.setMessage(mode);
            xpModeButton.setTooltip(Tooltip.create(mode));
            Component recipe = menu.lockedRecipeResult().isEmpty() ? Component.literal(menu.lockedRecipeName())
                    : menu.lockedRecipeResult().getHoverName();
            Component lock = Component.translatable(switch (menu.lockState()) {
                case 1 -> "gui.trading_cells.infuser.unlock";
                case 2 -> "gui.trading_cells.infuser.missing_recipe";
                default -> "gui.trading_cells.infuser.lock";
            }, recipe);
            lockButton.setMessage(lock);
            lockButton.setTooltip(Tooltip.create(lock));
            lockButton.active = menu.lockState() != 0 || menu.requiredExperience() > 0 || menu.automationDisplay() != null;
        }
        if (depositButton == null || withdrawButton == null) {
            return;
        }
        int storedPoints = menu.storedExperience();
        int playerPoints = playerExperiencePoints();
        int freeCapacity = menu.capacity() - storedPoints;
        if (amountField.getValue().isEmpty()) {
            depositButton.active = playerPoints > 0 && freeCapacity > 0;
            withdrawButton.active = storedPoints > 0 && playerPoints < Integer.MAX_VALUE;
            return;
        }

        int requestedLevels = requestedLevels();
        int depositPoints = pointsRemovedByLevels(playerLevel(), playerProgress(), requestedLevels);
        depositButton.active = requestedLevels > 0
                && requestedLevels <= playerLevel()
                && depositPoints > 0
                && depositPoints <= freeCapacity;
        int withdrawableLevels = MinecraftExperience.maximumAdditionalLevels(
                playerLevel(),
                playerProgress(),
                storedPoints
        );
        withdrawButton.active = requestedLevels > 0 && requestedLevels <= withdrawableLevels;
    }

    private void initRecipeBookButton() {
        ScreenPosition position = getRecipeBookButtonPosition();
        addRenderableWidget(new ImageButton(
                position.x(),
                position.y(),
                20,
                18,
                RECIPE_BOOK_BUTTON_SPRITES,
                button -> {
                    recipeBookComponent.toggleVisibility();
                    leftPos = recipeBookComponent.updateScreenPosition(width, imageWidth);
                    ScreenPosition updated = getRecipeBookButtonPosition();
                    button.setPosition(updated.x(), updated.y());
                    onRecipeBookButtonClick();
                }
        ));
    }

    private void repositionTransferWidgets() {
        if (xpModeButton != null) { xpModeButton.setPosition(leftPos + 69, topPos + 81); }
        if (lockButton != null) { lockButton.setPosition(leftPos + 89, topPos + 81); }
        if (amountField != null) {
            amountField.setPosition(leftPos + CONTROL_X, topPos + FIELD_Y);
        }
        if (depositButton != null) {
            depositButton.setPosition(leftPos + CONTROL_X, topPos + DEPOSIT_BUTTON_Y);
        }
        if (withdrawButton != null) {
            withdrawButton.setPosition(leftPos + CONTROL_X, topPos + WITHDRAW_BUTTON_Y);
        }
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        return recipeBookComponent.charTyped(event) || super.charTyped(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        return recipeBookComponent.keyPressed(event) || super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (recipeBookComponent.mouseClicked(event, doubleClick)) {
            setFocused(recipeBookComponent);
            return true;
        }
        return widthTooNarrow && recipeBookComponent.isVisible()
                || super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        return recipeBookComponent.mouseDragged(event, deltaX, deltaY)
                || super.mouseDragged(event, deltaX, deltaY);
    }

    @Override
    protected boolean isHovering(int left, int top, int width, int height, double mouseX, double mouseY) {
        return (!widthTooNarrow || !recipeBookComponent.isVisible())
                && super.isHovering(left, top, width, height, mouseX, mouseY);
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int left, int top) {
        boolean outside = mouseX < left
                || mouseY < top
                || mouseX >= left + imageWidth
                || mouseY >= top + imageHeight;
        return recipeBookComponent.hasClickedOutside(
                mouseX,
                mouseY,
                leftPos,
                topPos,
                imageWidth,
                imageHeight
        ) && outside;
    }

    @Override
    protected void slotClicked(Slot slot, int slotId, int button, ContainerInput input) {
        super.slotClicked(slot, slotId, button, input);
        recipeBookComponent.slotClicked(slot);
        if (slotId >= 0 && slotId <= ArcaneInfuserBlockEntity.OUTPUT_SLOT) {
            selectedRecipeExperience = -1;
        }
    }

    @Override
    public void recipesUpdated() {
        recipeBookComponent.recipesUpdated();
    }

    @Override
    public void fillGhostRecipe(RecipeDisplay display) {
        selectedRecipeExperience = display instanceof ArcaneInfusionRecipeDisplay infusion
                ? infusion.experience()
                : -1;
        recipeBookComponent.fillGhostRecipe(display);
    }

    private boolean isRecipeViewerHovered(int mouseX, int mouseY) {
        int x = leftPos + RECIPE_VIEWER_X;
        int y = topPos + RECIPE_VIEWER_Y;
        return mouseX >= x && mouseX < x + RECIPE_VIEWER_WIDTH
                && mouseY >= y && mouseY < y + RECIPE_VIEWER_HEIGHT;
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
        return MinecraftExperience.totalPoints(playerLevel(), playerProgress());
    }

    private int playerLevel() {
        return minecraft == null || minecraft.player == null ? 0 : minecraft.player.experienceLevel;
    }

    private float playerProgress() {
        return minecraft == null || minecraft.player == null ? 0.0F : minecraft.player.experienceProgress;
    }

    private static int pointsRemovedByLevels(int playerLevel, float playerProgress, int requestedLevels) {
        if (requestedLevels <= 0 || requestedLevels > playerLevel) {
            return 0;
        }
        int currentPoints = MinecraftExperience.totalPoints(playerLevel, playerProgress);
        int targetPoints = requestedLevels == playerLevel
                ? 0
                : MinecraftExperience.pointsAtLevelWithProgress(playerLevel - requestedLevels, playerProgress);
        return Math.max(0, currentPoints - targetPoints);
    }

    private void send(boolean deposit) {
        boolean transferAll = amountField.getValue().isEmpty();
        ArcaneInfusionTransferAction action;
        if (deposit) {
            action = transferAll
                    ? ArcaneInfusionTransferAction.DEPOSIT_ALL
                    : ArcaneInfusionTransferAction.DEPOSIT;
        } else {
            action = transferAll
                    ? ArcaneInfusionTransferAction.WITHDRAW_ALL
                    : ArcaneInfusionTransferAction.WITHDRAW;
        }
        ClientPacketDistributor.sendToServer(new ArcaneInfuserTransferPayload(
                menu.containerId,
                action.id(),
                transferAll ? 0 : requestedLevels()
        ));
    }

    private void automationAction(int button) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, button);
        }
    }
}
