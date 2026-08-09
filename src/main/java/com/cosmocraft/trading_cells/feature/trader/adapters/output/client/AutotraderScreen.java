package com.cosmocraft.trading_cells.feature.trader.adapters.output.client;

import com.cosmocraft.trading_cells.feature.trader.adapters.input.AutotraderMenu;
import com.cosmocraft.trading_cells.feature.trader.domain.model.AutotraderPolicy;
import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.TradingCells;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.TradeExperienceDisplay;
import com.mojang.blaze3d.platform.InputConstants;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.trader.VillagerGuiThemeColors;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.trader.VillagerGuiTextures;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.trader.VillagerTradeScreenCommon;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.trader.VillagerTradeScreenLayout;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.trader.VillagerTradeSprites;
import com.cosmocraft.trading_cells.platform.neoforge.menu.VillagerTradeMenuLayout;
import com.cosmocraft.trading_cells.platform.neoforge.network.ExtractTradingCellExperiencePayload;
import com.cosmocraft.trading_cells.platform.neoforge.network.ResetTradesPayload;
import com.cosmocraft.trading_cells.platform.neoforge.network.SelectAutotraderOfferPayload;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.lwjgl.glfw.GLFW;

public final class AutotraderScreen extends AbstractContainerScreen<AutotraderMenu> { // NOSONAR - Minecraft fixes the screen inheritance hierarchy.
    private static final int OFFSCREEN_MOUSE_COORDINATE = -10_000;
    private static final Identifier RESET_NORMAL =
            Identifier.fromNamespaceAndPath(TradingCells.MOD_ID, "trader/reset/reset_trades");
    private static final Identifier RESET_HOVERED =
            Identifier.fromNamespaceAndPath(TradingCells.MOD_ID, "trader/reset/reset_trades_hovered");
    private static final WidgetSprites RESET_SPRITES = new WidgetSprites(RESET_NORMAL, RESET_HOVERED);

    private static final int SELECTOR_X = VillagerTradeMenuLayout.AUTOTRADER_SELECTOR_X;
    private static final int SELECTOR_Y = VillagerTradeMenuLayout.AUTOTRADER_SELECTOR_Y;
    private static final int SELECTOR_W = VillagerTradeMenuLayout.AUTOTRADER_SELECTOR_WIDTH;
    private static final int SELECTOR_H = VillagerTradeMenuLayout.AUTOTRADER_SELECTOR_HEIGHT;
    private static final int TRADE_COST_A_OFFSET = 3;
    private static final int TRADE_COST_B_OFFSET = 20;
    private static final int TRADE_ARROW_OFFSET = 50;
    private static final int TRADE_RESULT_OFFSET = 69;
    private static final int DROPDOWN_X = VillagerTradeScreenLayout.AUTOTRADER_DROPDOWN_X;
    private static final int DROPDOWN_Y = VillagerTradeScreenLayout.AUTOTRADER_DROPDOWN_Y;
    private static final int DROPDOWN_VIEW_X = VillagerTradeScreenLayout.AUTOTRADER_DROPDOWN_VIEW_X;
    private static final int DROPDOWN_VIEW_Y = VillagerTradeScreenLayout.AUTOTRADER_DROPDOWN_VIEW_Y;
    private static final int DROPDOWN_VIEW_W = VillagerTradeScreenLayout.AUTOTRADER_DROPDOWN_VIEW_WIDTH;
    private static final int DROPDOWN_ROW_H = VillagerTradeScreenLayout.DROPDOWN_ROW_HEIGHT;
    private static final int DROPDOWN_TRACK_X = VillagerTradeScreenLayout.AUTOTRADER_DROPDOWN_TRACK_X;
    private TransparentButton selectorButton;
    private TransparentButton xpButton;
    private SilentImageButton resetButton;
    private boolean dropdownOpen;
    private int firstVisibleOffer;
    private int focusedOffer;
    private int knownOfferCount = -1;
    private int knownOffersRevision = -1;
    private int pendingResetTicks;
    private int xpButtonPressTicks;
    private boolean draggingScroll;
    private double scrollDragOffset;

    public AutotraderScreen(AutotraderMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, VillagerTradeMenuLayout.WIDTH, VillagerTradeMenuLayout.HEIGHT);
        titleLabelX = -10_000;
        titleLabelY = -10_000;
        inventoryLabelX = -10_000;
        inventoryLabelY = -10_000;
    }

    @Override
    protected void init() {
        super.init();
        selectorButton = addRenderableWidget(new TransparentButton(
                leftPos + SELECTOR_X,
                topPos + SELECTOR_Y,
                SELECTOR_W,
                SELECTOR_H,
                button -> toggleDropdown()
        ));
        xpButton = addRenderableWidget(new TransparentButton(
                leftPos + VillagerTradeScreenLayout.XP_BUTTON_X,
                topPos + VillagerTradeScreenLayout.XP_BUTTON_Y,
                VillagerTradeScreenLayout.XP_BUTTON_WIDTH,
                VillagerTradeScreenLayout.XP_BUTTON_HEIGHT,
                button -> withdrawExperience()
        ));
        resetButton = addRenderableWidget(new SilentImageButton(
                leftPos + VillagerTradeMenuLayout.RESET_BUTTON_X,
                topPos + VillagerTradeMenuLayout.RESET_BUTTON_Y,
                VillagerTradeMenuLayout.RESET_BUTTON_WIDTH,
                VillagerTradeMenuLayout.RESET_BUTTON_HEIGHT,
                RESET_SPRITES,
                button -> resetTrades(),
                Component.empty()
        ));
        resetButton.setTooltip(Tooltip.create(Component.translatable("button.trading_cells.reset_trades")));
        knownOffersRevision = menu.offersRevision();
        refreshControls();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        int count = menu.offers().size();
        int revision = menu.offersRevision();
        if (count != knownOfferCount || revision != knownOffersRevision) {
            focusedOffer = Mth.clamp(menu.selectedOfferIndex(), 0, Math.max(0, count - 1));
            firstVisibleOffer = Mth.clamp(firstVisibleOffer, 0, Math.max(0, count - VillagerTradeScreenLayout.DROPDOWN_VISIBLE_ROWS));
            if (dropdownOpen) {
                keepFocusedOfferVisible();
            }
            knownOfferCount = count;
        }
        if (knownOffersRevision >= 0 && revision > knownOffersRevision && pendingResetTicks > 0) {
            playButtonSound();
            pendingResetTicks = 0;
        } else if (pendingResetTicks > 0) {
            pendingResetTicks--;
        }
        if (xpButtonPressTicks > 0) {
            xpButtonPressTicks--;
        }
        knownOffersRevision = revision;
        refreshControls();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        VillagerTradeScreenLayout.drawBackground(
                graphics,
                VillagerGuiTextures.resolve(),
                leftPos,
                topPos
        );
        VillagerTradeScreenLayout.drawCommonSlots(
                graphics,
                leftPos,
                topPos,
                VillagerGuiThemeColors.resolve()
        );
        drawProfessionProgress(graphics);
        drawSelectionAreaPanels(graphics);
        drawSelector(graphics, mouseX, mouseY);
        drawBufferSlots(graphics);
        drawSelectedOfferPreview(graphics);
        drawXpPanelDecorations(graphics, mouseX, mouseY);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        drawCommonText(graphics);
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        boolean dropdownHovered = dropdownOpen && isInsideDropdown(mouseX, mouseY);
        int contentMouseX = dropdownHovered ? OFFSCREEN_MOUSE_COORDINATE : mouseX;
        int contentMouseY = dropdownHovered ? OFFSCREEN_MOUSE_COORDINATE : mouseY;
        super.extractContents(graphics, contentMouseX, contentMouseY, partialTick);
        MerchantOffer selected = menu.selectedOffer();
        if (!dropdownOpen) {
            setSelectedOfferTooltip(graphics, selected, mouseX, mouseY);
            setMissingSecondIngredientTooltip(graphics, selected, mouseX, mouseY);
        }
        if (dropdownOpen) {
            graphics.nextStratum();
            drawDropdown(graphics, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (!dropdownOpen) {
            return super.mouseClicked(event, doubleClick);
        }
        if (resetButton != null && resetButton.isMouseOver(event.x(), event.y())) {
            return super.mouseClicked(event, doubleClick);
        }
        if (handleDropdownScrollbarClick(event) || handleDropdownOfferClick(event)) {
            return true;
        }
        boolean insideDropdown = isInsideDropdown(event.x(), event.y());
        if (!insideDropdown && !isInsideSelector(event.x(), event.y())) {
            closeDropdown();
            return true;
        }
        return insideDropdown || super.mouseClicked(event, doubleClick);
    }

    private boolean handleDropdownScrollbarClick(MouseButtonEvent event) {
        int offerCount = menu.offers().size();
        if (offerCount <= VillagerTradeScreenLayout.DROPDOWN_VISIBLE_ROWS
                || event.x() < leftPos + DROPDOWN_TRACK_X
                || event.x() >= leftPos + DROPDOWN_TRACK_X + 5
                || event.y() < topPos + DROPDOWN_VIEW_Y
                || event.y() >= topPos + DROPDOWN_VIEW_Y + dropdownContentHeight()) {
            return false;
        }
        int thumbHeight = dropdownThumbHeight(offerCount);
        int thumbY = dropdownThumbY(offerCount, thumbHeight);
        double localY = event.y() - topPos;
        if (localY >= thumbY && localY < thumbY + thumbHeight) {
            draggingScroll = true;
            scrollDragOffset = localY - thumbY;
            return true;
        }

        int page = VillagerTradeScreenLayout.DROPDOWN_VISIBLE_ROWS;
        int direction = localY < thumbY ? -page : page;
        firstVisibleOffer = Mth.clamp(
                firstVisibleOffer + direction,
                0,
                Math.max(0, offerCount - page)
        );
        return true;
    }

    private boolean handleDropdownOfferClick(MouseButtonEvent event) {
        if (event.x() < leftPos + DROPDOWN_VIEW_X
                || event.x() >= leftPos + DROPDOWN_VIEW_X + DROPDOWN_VIEW_W
                || event.y() < topPos + DROPDOWN_VIEW_Y
                || event.y() >= topPos + DROPDOWN_VIEW_Y + dropdownContentHeight()) {
            return false;
        }
        int row = (int) ((event.y() - (topPos + DROPDOWN_VIEW_Y)) / DROPDOWN_ROW_H);
        int offerIndex = firstVisibleOffer + row;
        if (row < 0
                || row >= VillagerTradeScreenLayout.DROPDOWN_VISIBLE_ROWS
                || offerIndex < 0
                || offerIndex >= menu.offers().size()) {
            return false;
        }
        selectOffer(offerIndex);
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        if (draggingScroll) {
            updateDropdownScrollFromMouse(event.y());
            return true;
        }
        return super.mouseDragged(event, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        draggingScroll = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (dropdownOpen && isInsideDropdown(mouseX, mouseY)) {
            firstVisibleOffer = Mth.clamp(
                    (int)(firstVisibleOffer - scrollY),
                    0,
                    Math.max(0, menu.offers().size() - VillagerTradeScreenLayout.DROPDOWN_VISIBLE_ROWS)
            );
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (!dropdownOpen) {
            return super.keyPressed(event);
        }
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            closeDropdown();
            return true;
        }
        int count = menu.offers().size();
        if (count == 0) {
            return super.keyPressed(event);
        }
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            selectOffer(focusedOffer);
            return true;
        }
        int target = switch (key) {
            case GLFW.GLFW_KEY_UP -> focusedOffer - 1;
            case GLFW.GLFW_KEY_DOWN -> focusedOffer + 1;
            case GLFW.GLFW_KEY_PAGE_UP ->
                    focusedOffer - VillagerTradeScreenLayout.DROPDOWN_VISIBLE_ROWS;
            case GLFW.GLFW_KEY_PAGE_DOWN ->
                    focusedOffer + VillagerTradeScreenLayout.DROPDOWN_VISIBLE_ROWS;
            case GLFW.GLFW_KEY_HOME -> 0;
            case GLFW.GLFW_KEY_END -> count - 1;
            default -> Integer.MIN_VALUE;
        };
        if (target == Integer.MIN_VALUE) {
            return super.keyPressed(event);
        }
        focusedOffer = Mth.clamp(target, 0, count - 1);
        keepFocusedOfferVisible();
        return true;
    }

    public boolean resetTradesFromShortcut() {
        if (!menu.canResetTrades()) {
            return false;
        }
        resetTrades();
        return true;
    }

    private void drawCommonText(GuiGraphicsExtractor graphics) {
        VillagerTradeScreenCommon.drawCenteredText(
                graphics,
                font,
                Component.translatable("gui.trading_cells.trades"),
                VillagerTradeScreenLayout.TRADES_TITLE_CENTER_X,
                VillagerTradeScreenLayout.HEADER_TEXT_Y,
                VillagerTradeScreenCommon.TEXT_DARK
        );
        VillagerTradeScreenCommon.drawCenteredText(
                graphics,
                font,
                VillagerTradeScreenCommon.professionDisplayName(menu.villagerData()),
                VillagerTradeScreenLayout.PROFESSION_TITLE_CENTER_X,
                VillagerTradeScreenLayout.HEADER_TEXT_Y,
                VillagerTradeScreenCommon.TEXT_DARK
        );
        graphics.text(
                font,
                Component.translatable(
                        "gui.trading_cells.level",
                        menu.villagerData().level(),
                        VillagerTradeScreenCommon.levelName(menu.villagerData().level())
                ),
                VillagerTradeScreenLayout.LEVEL_TEXT_X,
                VillagerTradeScreenLayout.PROFESSION_TEXT_Y,
                VillagerTradeScreenCommon.TEXT_DARK,
                false
        );
        Component xpText = VillagerTradeScreenCommon.professionExperienceText(
                menu.villagerData().level(),
                menu.villagerXp()
        );
        graphics.text(
                font,
                xpText,
                VillagerTradeScreenLayout.PROFESSION_XP_RIGHT_X - font.width(xpText),
                VillagerTradeScreenLayout.PROFESSION_TEXT_Y,
                VillagerTradeScreenCommon.TEXT_DARK,
                false
        );
        VillagerTradeScreenCommon.drawStoredExperienceText(
                graphics,
                font,
                TradeExperienceDisplay.storedExperience(menu.storedExperience()),
                TradeExperienceDisplay.equivalentLevel(menu.storedExperience()),
                VillagerTradeScreenLayout.XP_TEXT_X,
                VillagerTradeScreenLayout.XP_TEXT_Y,
                VillagerTradeScreenLayout.XP_LEVEL_TEXT_Y
        );
        Component withdrawText = Component.translatable("gui.trading_cells.withdraw_xp");
        VillagerTradeScreenCommon.drawCenteredText(
                graphics,
                font,
                withdrawText,
                VillagerTradeScreenLayout.XP_BUTTON_X + VillagerTradeScreenLayout.XP_BUTTON_WIDTH / 2,
                VillagerTradeScreenLayout.XP_BUTTON_Y + 6,
                VillagerTradeScreenCommon.buttonTextColor(menu.storedExperience() > 0)
        );
        graphics.text(
                font,
                Component.translatable("gui.trading_cells.inventory"),
                VillagerTradeScreenLayout.INVENTORY_LABEL_X,
                VillagerTradeScreenLayout.INVENTORY_LABEL_Y,
                VillagerTradeScreenCommon.TEXT_DARK,
                false
        );
        graphics.text(
                font,
                Component.translatable("gui.trading_cells.input_1"),
                14,
                VillagerTradeMenuLayout.AUTOTRADER_INPUT_A_TITLE_Y,
                VillagerTradeScreenCommon.TEXT_DARK,
                false
        );
        graphics.text(
                font,
                Component.translatable("gui.trading_cells.input_2"),
                14,
                VillagerTradeMenuLayout.AUTOTRADER_INPUT_B_TITLE_Y,
                VillagerTradeScreenCommon.TEXT_DARK,
                false
        );
        graphics.text(
                font,
                Component.translatable("gui.trading_cells.output"),
                14,
                VillagerTradeMenuLayout.AUTOTRADER_OUTPUT_TITLE_Y,
                VillagerTradeScreenCommon.TEXT_DARK,
                false
        );
    }

    private void drawProfessionProgress(GuiGraphicsExtractor graphics) {
        VillagerTradeScreenCommon.drawProfessionProgress(
                graphics,
                leftPos + VillagerTradeScreenLayout.PROFESSION_PROGRESS_X,
                topPos + VillagerTradeScreenLayout.PROFESSION_PROGRESS_Y,
                VillagerTradeScreenLayout.PROFESSION_PROGRESS_WIDTH,
                menu.villagerData().level(),
                menu.villagerXp()
        );
    }

    private void drawSelectionAreaPanels(GuiGraphicsExtractor graphics) {
        VillagerGuiThemeColors colors = VillagerGuiThemeColors.resolve();
        drawSelectionPanel(
                graphics,
                VillagerTradeMenuLayout.AUTOTRADER_PANEL_X,
                VillagerTradeMenuLayout.AUTOTRADER_INPUT_A_PANEL_Y,
                VillagerTradeMenuLayout.AUTOTRADER_PANEL_WIDTH,
                VillagerTradeMenuLayout.AUTOTRADER_PANEL_HEIGHT,
                colors
        );
        drawSelectionPanel(
                graphics,
                VillagerTradeMenuLayout.AUTOTRADER_PANEL_X,
                VillagerTradeMenuLayout.AUTOTRADER_INPUT_B_PANEL_Y,
                VillagerTradeMenuLayout.AUTOTRADER_PANEL_WIDTH,
                VillagerTradeMenuLayout.AUTOTRADER_PANEL_HEIGHT,
                colors
        );
        drawSelectionPanel(
                graphics,
                VillagerTradeMenuLayout.AUTOTRADER_PANEL_X,
                VillagerTradeMenuLayout.AUTOTRADER_OUTPUT_PANEL_Y,
                VillagerTradeMenuLayout.AUTOTRADER_PANEL_WIDTH,
                VillagerTradeMenuLayout.AUTOTRADER_PANEL_HEIGHT,
                colors
        );
    }

    private void drawSelectionPanel(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int width,
            int height,
            VillagerGuiThemeColors colors
    ) {
        int left = leftPos + x;
        int top = topPos + y;
        graphics.fill(left, top, left + width, top + height, colors.slotOuter());
        graphics.fill(left + 1, top + 1, left + width - 1, top + height - 1, colors.slotInner());
        graphics.fill(left + 1, top + 1, left + width - 1, top + 2, colors.slotLight());
        graphics.fill(left + 1, top + 1, left + 2, top + height - 1, colors.slotLight());
        graphics.fill(left + 1, top + height - 2, left + width - 1, top + height - 1, colors.slotDark());
        graphics.fill(left + width - 2, top + 1, left + width - 1, top + height - 1, colors.slotDark());
    }

    private void drawSelector(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                VillagerTradeSprites.DROPDOWN,
                leftPos + SELECTOR_X,
                topPos + SELECTOR_Y,
                0,
                0,
                SELECTOR_W,
                SELECTOR_H,
                102,
                18
        );
        MerchantOffer offer = menu.selectedOffer();
        if (offer == null) {
            graphics.text(
                    font,
                    Component.translatable("label.trading_cells.no_trades"),
                    leftPos + SELECTOR_X + 4,
                    topPos + SELECTOR_Y + 5,
                    0xFF3A3A3A,
                    false
            );
            return;
        }
        int itemY = topPos + SELECTOR_Y + 1;
        int costAX = leftPos + SELECTOR_X + TRADE_COST_A_OFFSET;
        int costBX = leftPos + SELECTOR_X + TRADE_COST_B_OFFSET;
        int arrowX = leftPos + SELECTOR_X + TRADE_ARROW_OFFSET;
        int resultX = leftPos + SELECTOR_X + TRADE_RESULT_OFFSET;
        boolean selectorHovered = mouseX >= leftPos + SELECTOR_X
                && mouseX < leftPos + SELECTOR_X + SELECTOR_W
                && mouseY >= topPos + SELECTOR_Y
                && mouseY < topPos + SELECTOR_Y + SELECTOR_H;
        VillagerTradeScreenCommon.drawOfferCostA(graphics, font, offer, costAX, itemY);
        if (!offer.getCostB().isEmpty()) {
            graphics.fakeItem(offer.getCostB(), costBX, itemY);
            graphics.itemDecorations(font, offer.getCostB(), costBX, itemY);
        }
        VillagerTradeScreenCommon.drawTradeArrow(
                graphics,
                arrowX,
                topPos + SELECTOR_Y + 4,
                VillagerTradeSprites.state(offer.isOutOfStock(), dropdownOpen, selectorHovered)
        );
        graphics.fakeItem(offer.getResult(), resultX, itemY);
        graphics.itemDecorations(font, offer.getResult(), resultX, itemY);
        VillagerTradeScreenCommon.drawDropdownChevron(
                graphics,
                leftPos + SELECTOR_X + 94,
                topPos + SELECTOR_Y + 8,
                true,
                selectorHovered
        );
    }

    private void drawBufferSlots(GuiGraphicsExtractor graphics) {
        for (int row = 0; row < 3; row++) {
            int y = switch (row) {
                case 0 -> VillagerTradeMenuLayout.AUTOTRADER_INPUT_A_Y;
                case 1 -> VillagerTradeMenuLayout.AUTOTRADER_INPUT_B_Y;
                default -> VillagerTradeMenuLayout.AUTOTRADER_OUTPUT_Y;
            };
            for (int index = 0; index < AutotraderPolicy.INPUT_SLOTS_PER_COST; index++) {
                VillagerTradeScreenLayout.drawSlotAtFramePosition(
                        graphics,
                        leftPos,
                        topPos,
                        VillagerTradeMenuLayout.AUTOTRADER_ROW_X + index * 18,
                        y,
                        VillagerGuiThemeColors.resolve()
                );
            }
        }
        MerchantOffer offer = menu.selectedOffer();
        if (offer == null || offer.getItemCostB().isEmpty()) {
            for (int index = 0; index < AutotraderPolicy.INPUT_SLOTS_PER_COST; index++) {
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        VillagerTradeSprites.DISABLED_SLOT_OVERLAY,
                        leftPos + VillagerTradeMenuLayout.AUTOTRADER_ROW_X + index * 18,
                        topPos + VillagerTradeMenuLayout.AUTOTRADER_INPUT_B_Y,
                        0,
                        0,
                        18,
                        18,
                        18,
                        18
                );
            }
        }
    }

    private void drawSelectedOfferPreview(GuiGraphicsExtractor graphics) {
        MerchantOffer offer = menu.selectedOffer();
        if (offer == null) {
            return;
        }
        VillagerTradeScreenCommon.drawOfferCostA(
                graphics,
                font,
                offer,
                leftPos + VillagerTradeMenuLayout.MANUAL_PAYMENT_A_X + 1,
                topPos + VillagerTradeMenuLayout.MANUAL_TRADER_SLOT_Y + 1
        );
        if (!offer.getCostB().isEmpty()) {
            drawItem(
                    graphics,
                    offer.getCostB(),
                    VillagerTradeMenuLayout.MANUAL_PAYMENT_B_X,
                    VillagerTradeMenuLayout.MANUAL_TRADER_SLOT_Y
            );
        }
        VillagerTradeScreenCommon.drawTradeArrow(
                graphics,
                leftPos + VillagerTradeScreenLayout.PREVIEW_ARROW_X,
                topPos
                        + VillagerTradeMenuLayout.MANUAL_TRADER_SLOT_Y
                        + VillagerTradeScreenLayout.AUTOTRADER_PREVIEW_ARROW_Y_OFFSET,
                offer.isOutOfStock()
                        ? VillagerTradeSprites.State.DISABLED
                        : VillagerTradeSprites.State.NORMAL
        );
        drawItem(
                graphics,
                offer.getResult(),
                VillagerTradeMenuLayout.MANUAL_RESULT_X,
                VillagerTradeMenuLayout.MANUAL_TRADER_SLOT_Y
        );
    }


    private void drawDropdown(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int count = menu.offers().size();
        int visible = Math.clamp(
                count - firstVisibleOffer,
                0,
                VillagerTradeScreenLayout.DROPDOWN_VISIBLE_ROWS
        );
        int contentHeight = dropdownContentHeight();
        drawDropdownFrame(graphics, contentHeight);
        graphics.enableScissor(
                leftPos + DROPDOWN_VIEW_X,
                topPos + DROPDOWN_VIEW_Y,
                leftPos + DROPDOWN_VIEW_X + DROPDOWN_VIEW_W,
                topPos + DROPDOWN_VIEW_Y + contentHeight
        );
        for (int row = 0; row < visible; row++) {
            int index = firstVisibleOffer + row;
            drawDropdownRow(graphics, menu.offers().get(index), index, row, mouseX, mouseY);
        }
        graphics.disableScissor();
        drawDropdownScrollbar(graphics, count, mouseX, mouseY);
    }

    private void drawDropdownFrame(GuiGraphicsExtractor graphics, int contentHeight) {
        graphics.fill(
                leftPos + DROPDOWN_X,
                topPos + DROPDOWN_Y,
                leftPos + DROPDOWN_X + SELECTOR_W,
                topPos + DROPDOWN_Y + contentHeight + 4,
                0xFF3A3A3A
        );
        graphics.fill(
                leftPos + DROPDOWN_X + 1,
                topPos + DROPDOWN_Y + 1,
                leftPos + DROPDOWN_X + SELECTOR_W - 1,
                topPos + DROPDOWN_Y + contentHeight + 3,
                0xB3000000
        );
    }

    private void drawDropdownRow(
            GuiGraphicsExtractor graphics,
            MerchantOffer offer,
            int index,
            int row,
            int mouseX,
            int mouseY
    ) {
        int x = leftPos + DROPDOWN_VIEW_X;
        int y = topPos + DROPDOWN_VIEW_Y + row * DROPDOWN_ROW_H;
        boolean hovered = mouseX >= x
                && mouseX < x + DROPDOWN_VIEW_W
                && mouseY >= y
                && mouseY < y + DROPDOWN_ROW_H;
        VillagerTradeSprites.State state = VillagerTradeSprites.state(
                offer.isOutOfStock(),
                index == focusedOffer,
                hovered
        );
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                VillagerTradeSprites.dropdownRow(state),
                x,
                y,
                0,
                0,
                DROPDOWN_VIEW_W,
                DROPDOWN_ROW_H,
                VillagerTradeSprites.DROPDOWN_ROW_WIDTH,
                VillagerTradeSprites.DROPDOWN_ROW_HEIGHT
        );
        int costAX = x + TRADE_COST_A_OFFSET;
        int costBX = x + TRADE_COST_B_OFFSET;
        int resultX = x + TRADE_RESULT_OFFSET;
        VillagerTradeScreenCommon.drawOfferCostA(graphics, font, offer, costAX, y + 1);
        if (!offer.getCostB().isEmpty()) {
            graphics.fakeItem(offer.getCostB(), costBX, y + 1);
            graphics.itemDecorations(font, offer.getCostB(), costBX, y + 1);
        }
        VillagerTradeScreenCommon.drawTradeArrow(
                graphics,
                x + TRADE_ARROW_OFFSET,
                y + 4,
                state
        );
        graphics.fakeItem(offer.getResult(), resultX, y + 1);
        graphics.itemDecorations(font, offer.getResult(), resultX, y + 1);
        setDropdownOfferTooltip(graphics, offer, x, y, mouseX, mouseY);
    }

    private void setDropdownOfferTooltip(
            GuiGraphicsExtractor graphics,
            MerchantOffer offer,
            int x,
            int y,
            int mouseX,
            int mouseY
    ) {
        if (mouseY < y || mouseY >= y + DROPDOWN_ROW_H) {
            return;
        }
        int costAX = x + TRADE_COST_A_OFFSET;
        int costBX = x + TRADE_COST_B_OFFSET;
        int resultX = x + TRADE_RESULT_OFFSET;
        if (mouseX >= costAX && mouseX < costAX + 16) {
            graphics.setTooltipForNextFrame(font, offer.getCostA(), mouseX, mouseY);
        } else if (!offer.getCostB().isEmpty() && mouseX >= costBX && mouseX < costBX + 16) {
            graphics.setTooltipForNextFrame(font, offer.getCostB(), mouseX, mouseY);
        } else if (mouseX >= resultX && mouseX < resultX + 16) {
            graphics.setTooltipForNextFrame(font, offer.getResult(), mouseX, mouseY);
        }
    }

    private void updateDropdownScrollFromMouse(double mouseY) {
        int count = menu.offers().size();
        int maxScroll = Math.max(0, count - VillagerTradeScreenLayout.DROPDOWN_VISIBLE_ROWS);
        if (maxScroll == 0) {
            return;
        }
        int thumbHeight = dropdownThumbHeight(count);
        int travel = dropdownContentHeight() - thumbHeight;
        double localThumbTop = mouseY - topPos - scrollDragOffset;
        float normalized = travel <= 0 ? 0.0F : (float)((localThumbTop - DROPDOWN_VIEW_Y) / travel);
        firstVisibleOffer = Mth.clamp(Math.round(Mth.clamp(normalized, 0.0F, 1.0F) * maxScroll), 0, maxScroll);
    }

    private int dropdownThumbHeight(int offerCount) {
        return Math.max(
                12,
                dropdownContentHeight()
                        * VillagerTradeScreenLayout.DROPDOWN_VISIBLE_ROWS
                        / Math.max(1, offerCount)
        );
    }

    private int dropdownThumbY(int offerCount, int thumbHeight) {
        int maxScroll = Math.max(1, offerCount - VillagerTradeScreenLayout.DROPDOWN_VISIBLE_ROWS);
        return DROPDOWN_VIEW_Y
                + (dropdownContentHeight() - thumbHeight) * firstVisibleOffer / maxScroll;
    }

    private void toggleDropdown() {
        if (dropdownOpen) {
            closeDropdown();
            return;
        }
        if (menu.offers().isEmpty()) {
            return;
        }
        dropdownOpen = true;
        focusedOffer = menu.selectedOfferIndex();
        keepFocusedOfferVisible();
    }

    private void closeDropdown() {
        dropdownOpen = false;
    }

    private void keepFocusedOfferVisible() {
        if (focusedOffer < firstVisibleOffer) {
            firstVisibleOffer = focusedOffer;
        } else if (focusedOffer >= firstVisibleOffer + VillagerTradeScreenLayout.DROPDOWN_VISIBLE_ROWS) {
            firstVisibleOffer = focusedOffer - VillagerTradeScreenLayout.DROPDOWN_VISIBLE_ROWS + 1;
        }
        firstVisibleOffer = Mth.clamp(
                firstVisibleOffer,
                0,
                Math.max(0, menu.offers().size() - VillagerTradeScreenLayout.DROPDOWN_VISIBLE_ROWS)
        );
    }

    private void selectOffer(int index) {
        ClientPacketDistributor.sendToServer(new SelectAutotraderOfferPayload(
                menu.containerId,
                index,
                menu.offersRevision()
        ));
        focusedOffer = index;
        closeDropdown();
    }

    private void withdrawExperience() {
        if (menu.storedExperience() <= 0) {
            return;
        }
        xpButtonPressTicks = 3;
        byte mode = isShiftPressed()
                ? ExtractTradingCellExperiencePayload.NEXT_LEVEL
                : ExtractTradingCellExperiencePayload.ALL;
        ClientPacketDistributor.sendToServer(new ExtractTradingCellExperiencePayload(menu.containerId, mode));
    }


    private boolean isShiftPressed() {
        return minecraft != null
                && (InputConstants.isKeyDown(minecraft.getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT)
                || InputConstants.isKeyDown(minecraft.getWindow(), GLFW.GLFW_KEY_RIGHT_SHIFT));
    }

    private void resetTrades() {
        if (menu.canResetTrades()) {
            pendingResetTicks = 40;
            ClientPacketDistributor.sendToServer(new ResetTradesPayload(menu.containerId, menu.offersRevision()));
        }
    }


    private void playButtonSound() {
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }

    private void refreshControls() {
        selectorButton.visible = menu.hasVillager();
        selectorButton.active = !menu.offers().isEmpty();
        xpButton.active = menu.storedExperience() > 0;
        resetButton.visible = menu.canResetTrades();
        resetButton.active = menu.canResetTrades();
    }



    private void drawDropdownScrollbar(
            GuiGraphicsExtractor graphics,
            int offerCount,
            int mouseX,
            int mouseY
    ) {
        if (offerCount <= VillagerTradeScreenLayout.DROPDOWN_VISIBLE_ROWS) {
            return;
        }
        VillagerGuiThemeColors colors = VillagerGuiThemeColors.resolve();
        int x = leftPos + DROPDOWN_TRACK_X;
        int y = topPos + DROPDOWN_VIEW_Y;
        int contentHeight = dropdownContentHeight();
        graphics.fill(x, y, x + 5, y + contentHeight, colors.scrollBorder());
        graphics.fill(x + 1, y + 1, x + 4, y + contentHeight - 1, colors.scrollTrack());
        int thumbHeight = dropdownThumbHeight(offerCount);
        int thumbY = topPos + dropdownThumbY(offerCount, thumbHeight);
        boolean thumbHovered = draggingScroll
                || mouseX >= x && mouseX < x + 5
                && mouseY >= thumbY && mouseY < thumbY + thumbHeight;
        graphics.fill(x, thumbY, x + 5, thumbY + thumbHeight, colors.slotDark());
        graphics.fill(
                x + 1,
                thumbY + 1,
                x + 4,
                thumbY + thumbHeight - 1,
                thumbHovered ? colors.slotLight() : colors.scrollThumb()
        );
        graphics.fill(x + 1, thumbY + 1, x + 4, thumbY + 2, colors.scrollThumbLight());
        if (firstVisibleOffer > 0) {
            drawScrollIndicator(graphics, x, y + 2, true, colors.scrollThumbLight());
        }
        if (firstVisibleOffer + VillagerTradeScreenLayout.DROPDOWN_VISIBLE_ROWS < offerCount) {
            drawScrollIndicator(
                    graphics,
                    x,
                    y + contentHeight - 4,
                    false,
                    colors.scrollThumbLight()
            );
        }
    }

    private void drawScrollIndicator(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            boolean pointsUp,
            int color
    ) {
        int tipY = pointsUp ? y : y + 2;
        int baseY = pointsUp ? y + 2 : y;
        graphics.fill(x + 2, tipY, x + 3, tipY + 1, color);
        graphics.fill(x + 1, baseY, x + 4, baseY + 1, color);
    }




    private void drawItem(GuiGraphicsExtractor graphics, ItemStack stack, int frameX, int frameY) {
        if (stack.isEmpty()) {
            return;
        }
        graphics.fakeItem(stack, leftPos + frameX + 1, topPos + frameY + 1);
        graphics.itemDecorations(font, stack, leftPos + frameX + 1, topPos + frameY + 1);
    }

    private void setSelectedOfferTooltip(
            GuiGraphicsExtractor graphics,
            MerchantOffer offer,
            int mouseX,
            int mouseY
    ) {
        if (offer == null) {
            return;
        }
        if (isHoveringPreviewSlot(
                mouseX,
                mouseY,
                VillagerTradeMenuLayout.MANUAL_PAYMENT_A_X,
                VillagerTradeMenuLayout.MANUAL_TRADER_SLOT_Y
        )) {
            graphics.setTooltipForNextFrame(font, offer.getCostA(), mouseX, mouseY);
        } else if (!offer.getCostB().isEmpty() && isHoveringPreviewSlot(
                mouseX,
                mouseY,
                VillagerTradeMenuLayout.MANUAL_PAYMENT_B_X,
                VillagerTradeMenuLayout.MANUAL_TRADER_SLOT_Y
        )) {
            graphics.setTooltipForNextFrame(font, offer.getCostB(), mouseX, mouseY);
        } else if (isHoveringPreviewSlot(
                mouseX,
                mouseY,
                VillagerTradeMenuLayout.MANUAL_RESULT_X,
                VillagerTradeMenuLayout.MANUAL_TRADER_SLOT_Y
        )) {
            graphics.setTooltipForNextFrame(font, offer.getResult(), mouseX, mouseY);
        }
    }

    private void setMissingSecondIngredientTooltip(
            GuiGraphicsExtractor graphics,
            MerchantOffer selected,
            int mouseX,
            int mouseY
    ) {
        if (selected != null && selected.getItemCostB().isPresent()) {
            return;
        }
        for (int index = 0; index < AutotraderPolicy.INPUT_SLOTS_PER_COST; index++) {
            int slotX = leftPos + VillagerTradeMenuLayout.AUTOTRADER_ROW_X + index * 18;
            int slotY = topPos + VillagerTradeMenuLayout.AUTOTRADER_INPUT_B_Y;
            if (mouseX < slotX || mouseX >= slotX + 18 || mouseY < slotY || mouseY >= slotY + 18) {
                continue;
            }
            graphics.setTooltipForNextFrame(
                    font,
                    Component.translatable("gui.trading_cells.no_second_ingredient"),
                    mouseX,
                    mouseY
            );
            return;
        }
    }

    private boolean isHoveringPreviewSlot(int mouseX, int mouseY, int frameX, int frameY) {
        int x = leftPos + frameX;
        int y = topPos + frameY;
        return mouseX >= x && mouseX < x + 18 && mouseY >= y && mouseY < y + 18;
    }


    private void drawXpPanelDecorations(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        boolean active = menu.storedExperience() > 0;
        boolean hovered = active
                && mouseX >= leftPos + VillagerTradeScreenLayout.XP_BUTTON_X
                && mouseX < leftPos + VillagerTradeScreenLayout.XP_BUTTON_X + VillagerTradeScreenLayout.XP_BUTTON_WIDTH
                && mouseY >= topPos + VillagerTradeScreenLayout.XP_BUTTON_Y
                && mouseY < topPos + VillagerTradeScreenLayout.XP_BUTTON_Y + VillagerTradeScreenLayout.XP_BUTTON_HEIGHT;
        VillagerTradeScreenCommon.drawBeveledButton(
                graphics,
                leftPos + VillagerTradeScreenLayout.XP_BUTTON_X,
                topPos + VillagerTradeScreenLayout.XP_BUTTON_Y,
                VillagerTradeScreenLayout.XP_BUTTON_WIDTH,
                VillagerTradeScreenLayout.XP_BUTTON_HEIGHT,
                active,
                hovered,
                xpButtonPressTicks > 0
        );
        VillagerTradeScreenCommon.drawStoredExperiencePanel(
                graphics,
                leftPos + VillagerTradeScreenLayout.XP_DISPLAY_X,
                topPos + VillagerTradeScreenLayout.XP_DISPLAY_Y,
                VillagerTradeScreenLayout.XP_DISPLAY_WIDTH,
                VillagerTradeScreenLayout.XP_DISPLAY_HEIGHT,
                leftPos + VillagerTradeScreenLayout.XP_ICON_X,
                topPos + VillagerTradeScreenLayout.XP_ICON_Y
        );
    }

    private boolean isInsideDropdown(double mouseX, double mouseY) {
        return mouseX >= leftPos + DROPDOWN_X
                && mouseX < leftPos + DROPDOWN_X + SELECTOR_W
                && mouseY >= topPos + DROPDOWN_Y
                && mouseY < topPos + DROPDOWN_Y + dropdownContentHeight() + 4;
    }

    private boolean isInsideSelector(double mouseX, double mouseY) {
        return mouseX >= leftPos + SELECTOR_X
                && mouseX < leftPos + SELECTOR_X + SELECTOR_W
                && mouseY >= topPos + SELECTOR_Y
                && mouseY < topPos + SELECTOR_Y + SELECTOR_H;
    }

    private int dropdownContentHeight() {
        return VillagerTradeScreenLayout.dropdownContentHeight(menu.offers().size());
    }

    private static final class SilentImageButton extends ImageButton { // NOSONAR - Minecraft widget inheritance is framework-defined.
        private SilentImageButton(
                int x,
                int y,
                int width,
                int height,
                WidgetSprites sprites,
                Button.OnPress onPress,
                Component message
        ) {
            super(x, y, width, height, sprites, onPress, message);
        }

        @Override
        public void playDownSound(SoundManager soundManager) {
            // Success sound is emitted only after the server increments offersRevision.
        }
    }

    private static final class TransparentButton extends Button.Plain { // NOSONAR - Minecraft widget inheritance is framework-defined.
        private TransparentButton(int x, int y, int width, int height, OnPress onPress) {
            super(x, y, width, height, CommonComponents.EMPTY, onPress, DEFAULT_NARRATION);
        }

        @Override
        protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            // The screen draws this hit target so it stays visually transparent.
        }
    }
}
