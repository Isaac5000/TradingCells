package com.cosmocraft.trading_cells.feature.trader.adapters.output.client;

import com.cosmocraft.trading_cells.feature.trader.adapters.input.VillagerTradingCellMenu;
import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.TradingCells;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.TradeExperienceDisplay;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.trader.VillagerGuiThemeColors;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.trader.VillagerGuiTextures;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.trader.VillagerTradeScreenCommon;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.trader.VillagerTradeScreenLayout;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.trader.VillagerTradeSprites;
import com.cosmocraft.trading_cells.platform.neoforge.menu.VillagerTradeMenuLayout;
import com.cosmocraft.trading_cells.platform.neoforge.network.ExtractTradingCellExperiencePayload;
import com.cosmocraft.trading_cells.platform.neoforge.network.ResetTradesPayload;
import com.cosmocraft.trading_cells.platform.neoforge.network.SelectTradingCellOfferPayload;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.List;
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
import net.minecraft.world.item.trading.MerchantOffers;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.lwjgl.glfw.GLFW;

public final class VillagerTradingCellScreen extends AbstractContainerScreen<VillagerTradingCellMenu> { // NOSONAR - Minecraft fixes the screen inheritance hierarchy.
    private static final Identifier RESET_NORMAL =
            Identifier.fromNamespaceAndPath(TradingCells.MOD_ID, "trader/reset/reset_trades");
    private static final Identifier RESET_HOVERED =
            Identifier.fromNamespaceAndPath(TradingCells.MOD_ID, "trader/reset/reset_trades_hovered");
    private static final WidgetSprites RESET_SPRITES = new WidgetSprites(RESET_NORMAL, RESET_HOVERED);

    private static final int ROW_X = VillagerTradeScreenLayout.MANUAL_ROW_X;
    private static final int ROW_Y = VillagerTradeScreenLayout.MANUAL_ROW_Y;
    private static final int ROW_WIDTH = VillagerTradeScreenLayout.MANUAL_ROW_WIDTH;
    private static final int ROW_HEIGHT = VillagerTradeScreenLayout.MANUAL_ROW_HEIGHT;
    private static final int SCROLL_X = VillagerTradeScreenLayout.MANUAL_SCROLL_X;
    private static final int SCROLL_Y = VillagerTradeScreenLayout.MANUAL_SCROLL_Y;
    private static final int SCROLL_HEIGHT = VillagerTradeScreenLayout.MANUAL_SCROLL_HEIGHT;
    private final List<OfferButton> offerButtons = new ArrayList<>();
    private TransparentButton xpButton;
    private SilentImageButton resetButton;
    private int firstVisibleOffer;
    private int selectedOffer;
    private int knownOfferCount = -1;
    private int knownOffersRevision = -1;
    private int pendingResetTicks;
    private int xpButtonPressTicks;
    private boolean draggingScroll;
    private double scrollDragOffset;

    public VillagerTradingCellScreen(VillagerTradingCellMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, VillagerTradeMenuLayout.WIDTH, VillagerTradeMenuLayout.HEIGHT);
        titleLabelX = -10_000;
        titleLabelY = -10_000;
        inventoryLabelX = -10_000;
        inventoryLabelY = -10_000;
    }

    @Override
    protected void init() {
        super.init();
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
        rebuildOfferButtons();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        int offerCount = menu.getOffers().size();
        if (offerCount != knownOfferCount) {
            selectedOffer = Mth.clamp(selectedOffer, 0, Math.max(0, offerCount - 1));
            firstVisibleOffer = Mth.clamp(firstVisibleOffer, 0, Math.max(0, offerCount - VillagerTradeScreenLayout.MANUAL_VISIBLE_ROWS));
            rebuildOfferButtons();
        }
        int synchronizedSelection = Mth.clamp(menu.selectedOfferIndex(), 0, Math.max(0, offerCount - 1));
        if (synchronizedSelection != selectedOffer) {
            selectedOffer = synchronizedSelection;
            if (selectedOffer < firstVisibleOffer
                    || selectedOffer >= firstVisibleOffer + VillagerTradeScreenLayout.MANUAL_VISIBLE_ROWS) {
                firstVisibleOffer = Mth.clamp(
                        selectedOffer,
                        0,
                        Math.max(0, offerCount - VillagerTradeScreenLayout.MANUAL_VISIBLE_ROWS)
                );
                rebuildOfferButtons();
            }
        }
        int revision = menu.offersRevision();
        if (revision != knownOffersRevision) {
            selectedOffer = Mth.clamp(menu.selectedOfferIndex(), 0, Math.max(0, offerCount - 1));
            if (selectedOffer < firstVisibleOffer
                    || selectedOffer >= firstVisibleOffer + VillagerTradeScreenLayout.MANUAL_VISIBLE_ROWS) {
                firstVisibleOffer = Mth.clamp(
                        selectedOffer,
                        0,
                        Math.max(0, offerCount - VillagerTradeScreenLayout.MANUAL_VISIBLE_ROWS)
                );
                rebuildOfferButtons();
            }
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
        xpButton.active = menu.storedExperience() > 0;
        resetButton.visible = menu.canResetTrades();
        resetButton.active = menu.canResetTrades();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        VillagerTradeScreenLayout.drawBackground(graphics, currentTexture(), leftPos, topPos);
        VillagerTradeScreenLayout.drawCommonSlots(
                graphics,
                leftPos,
                topPos,
                VillagerGuiThemeColors.resolve()
        );
        drawProfessionProgress(graphics);
        drawOffers(graphics, mouseX, mouseY);
        drawSelectedOfferPreview(graphics);
        drawXpPanelDecorations(graphics, mouseX, mouseY);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        drawCommonText(graphics);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (isInsideTradeList(mouseX, mouseY) && canScroll()) {
            firstVisibleOffer = Mth.clamp(
                    (int)(firstVisibleOffer - scrollY),
                    0,
                    Math.max(0, menu.getOffers().size() - VillagerTradeScreenLayout.MANUAL_VISIBLE_ROWS)
            );
            rebuildOfferButtons();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (canScroll()
                && event.x() >= leftPos + SCROLL_X
                && event.x() < leftPos + SCROLL_X + 5
                && event.y() >= topPos + SCROLL_Y
                && event.y() < topPos + SCROLL_Y + SCROLL_HEIGHT) {
            int thumbHeight = scrollThumbHeight(menu.getOffers().size());
            int thumbY = scrollThumbY(menu.getOffers().size(), thumbHeight);
            double localY = event.y() - topPos;
            if (localY >= thumbY && localY < thumbY + thumbHeight) {
                draggingScroll = true;
                scrollDragOffset = localY - thumbY;
            } else {
                int page = VillagerTradeScreenLayout.MANUAL_VISIBLE_ROWS;
                firstVisibleOffer = Mth.clamp(
                        firstVisibleOffer + (localY < thumbY ? -page : page),
                        0,
                        Math.max(0, menu.getOffers().size() - page)
                );
                rebuildOfferButtons();
            }
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        if (draggingScroll) {
            updateScrollFromMouse(event.y());
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
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (key == GLFW.GLFW_KEY_C && menu.canResetTrades()) {
            resetTrades();
            return true;
        }
        int offerCount = menu.getOffers().size();
        if (offerCount == 0) {
            return super.keyPressed(event);
        }
        int target = switch (key) {
            case GLFW.GLFW_KEY_UP -> selectedOffer - 1;
            case GLFW.GLFW_KEY_DOWN -> selectedOffer + 1;
            case GLFW.GLFW_KEY_PAGE_UP -> selectedOffer - VillagerTradeScreenLayout.MANUAL_VISIBLE_ROWS;
            case GLFW.GLFW_KEY_PAGE_DOWN -> selectedOffer + VillagerTradeScreenLayout.MANUAL_VISIBLE_ROWS;
            case GLFW.GLFW_KEY_HOME -> 0;
            case GLFW.GLFW_KEY_END -> offerCount - 1;
            default -> Integer.MIN_VALUE;
        };
        if (target == Integer.MIN_VALUE) {
            return super.keyPressed(event);
        }
        selectOffer(Mth.clamp(target, 0, offerCount - 1));
        return true;
    }

    private Identifier currentTexture() {
        return VillagerGuiTextures.resolve();
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
                Component.translatable("gui.trading_cells.inventory"),
                VillagerTradeScreenLayout.INVENTORY_LABEL_X,
                VillagerTradeScreenLayout.INVENTORY_LABEL_Y,
                VillagerTradeScreenCommon.TEXT_DARK,
                false
        );
        graphics.text(
                font,
                Component.translatable(
                        "gui.trading_cells.level",
                        menu.merchantLevel(),
                        VillagerTradeScreenCommon.levelName(menu.merchantLevel())
                ),
                VillagerTradeScreenLayout.LEVEL_TEXT_X,
                VillagerTradeScreenLayout.PROFESSION_TEXT_Y,
                VillagerTradeScreenCommon.TEXT_DARK,
                false
        );
        Component xpText = VillagerTradeScreenCommon.professionExperienceText(
                menu.merchantLevel(),
                menu.merchantXp()
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
    }

    private void drawProfessionProgress(GuiGraphicsExtractor graphics) {
        VillagerTradeScreenCommon.drawProfessionProgress(
                graphics,
                leftPos + VillagerTradeScreenLayout.PROFESSION_PROGRESS_X,
                topPos + VillagerTradeScreenLayout.PROFESSION_PROGRESS_Y,
                VillagerTradeScreenLayout.PROFESSION_PROGRESS_WIDTH,
                menu.merchantLevel(),
                menu.merchantXp()
        );
    }

    private void drawSelectedOfferPreview(GuiGraphicsExtractor graphics) {
        MerchantOffer offer = menu.selectedOffer();
        if (offer == null) {
            return;
        }
        VillagerTradeScreenCommon.drawTradeArrow(
                graphics,
                leftPos + VillagerTradeScreenLayout.PREVIEW_ARROW_X,
                topPos + VillagerTradeMenuLayout.MANUAL_TRADER_SLOT_Y + 4,
                offer.isOutOfStock()
                        ? VillagerTradeSprites.State.DISABLED
                        : VillagerTradeSprites.State.NORMAL
        );
    }

    private void drawOffers(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        MerchantOffers offers = menu.getOffers();
        int visible = Math.clamp(
                offers.size() - firstVisibleOffer,
                0,
                VillagerTradeScreenLayout.MANUAL_VISIBLE_ROWS
        );
        graphics.enableScissor(leftPos + ROW_X, topPos + ROW_Y, leftPos + ROW_X + ROW_WIDTH, topPos + ROW_Y + ROW_HEIGHT * VillagerTradeScreenLayout.MANUAL_VISIBLE_ROWS);
        for (int row = 0; row < visible; row++) {
            int index = firstVisibleOffer + row;
            MerchantOffer offer = offers.get(index);
            int x = leftPos + ROW_X;
            int y = topPos + ROW_Y + row * ROW_HEIGHT;
            boolean hovered = mouseX >= x && mouseX < x + ROW_WIDTH && mouseY >= y && mouseY < y + ROW_HEIGHT;
            VillagerTradeSprites.State state = VillagerTradeSprites.state(
                    offer.isOutOfStock(),
                    index == selectedOffer,
                    hovered
            );
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    VillagerTradeSprites.row(state),
                    x,
                    y,
                    0,
                    0,
                    ROW_WIDTH,
                    ROW_HEIGHT,
                    VillagerTradeSprites.ROW_WIDTH,
                    VillagerTradeSprites.ROW_HEIGHT
            );
            drawOfferItems(graphics, offer, x, y, mouseX, mouseY, state);
        }
        graphics.disableScissor();
        drawScrollbar(graphics, offers.size(), mouseX, mouseY);
    }

    private void drawOfferItems(
            GuiGraphicsExtractor graphics,
            MerchantOffer offer,
            int x,
            int y,
            int mouseX,
            int mouseY,
            VillagerTradeSprites.State state
    ) {
        ItemStack first = offer.getCostA();
        ItemStack second = offer.getCostB();
        ItemStack result = offer.getResult();
        int firstX = x + 2;
        int secondX = x + 37;
        int arrowX = x + 58;
        int resultX = x + 80;
        int itemY = y + (ROW_HEIGHT - 16) / 2;

        VillagerTradeScreenCommon.drawOfferCostA(
                graphics,
                font,
                offer,
                firstX,
                itemY
        );

        if (!second.isEmpty()) {
            graphics.fakeItem(second, secondX, itemY);
            graphics.itemDecorations(font, second, secondX, itemY);
        }

        VillagerTradeScreenCommon.drawTradeArrow(
                graphics,
                arrowX,
                y + 7,
                state
        );
        graphics.fakeItem(result, resultX, itemY);
        graphics.itemDecorations(font, result, resultX, itemY);

        if (mouseY >= y && mouseY < y + ROW_HEIGHT) {
            if (mouseX >= firstX && mouseX < firstX + 18) {
                graphics.setTooltipForNextFrame(font, first, mouseX, mouseY);
            } else if (!second.isEmpty() && mouseX >= secondX && mouseX < secondX + 18) {
                graphics.setTooltipForNextFrame(font, second, mouseX, mouseY);
            } else if (mouseX >= resultX && mouseX < resultX + 18) {
                graphics.setTooltipForNextFrame(font, result, mouseX, mouseY);
            }
        }
    }


    private void drawScrollbar(GuiGraphicsExtractor graphics, int offerCount, int mouseX, int mouseY) {
        if (offerCount <= VillagerTradeScreenLayout.MANUAL_VISIBLE_ROWS) {
            return;
        }
        VillagerGuiThemeColors colors = VillagerGuiThemeColors.resolve();
        int x = leftPos + SCROLL_X;
        int y = topPos + SCROLL_Y;
        graphics.fill(x, y, x + 5, y + SCROLL_HEIGHT, colors.scrollBorder());
        graphics.fill(x + 1, y + 1, x + 4, y + SCROLL_HEIGHT - 1, colors.scrollTrack());
        int thumbHeight = scrollThumbHeight(offerCount);
        int thumbY = topPos + scrollThumbY(offerCount, thumbHeight);
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
    }


    private void selectOffer(int index) {
        ClientPacketDistributor.sendToServer(new SelectTradingCellOfferPayload(
                menu.containerId,
                index,
                menu.offersRevision()
        ));
    }

    private void withdrawExperience() {
        if (menu.storedExperience() <= 0) {
            return;
        }
        xpButtonPressTicks = 3;
        byte mode = isShiftPressed() ? ExtractTradingCellExperiencePayload.NEXT_LEVEL : ExtractTradingCellExperiencePayload.ALL;
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

    private void rebuildOfferButtons() {
        for (OfferButton button : offerButtons) {
            removeWidget(button);
        }
        offerButtons.clear();
        knownOfferCount = menu.getOffers().size();
        int visible = Math.clamp(
                knownOfferCount - firstVisibleOffer,
                0,
                VillagerTradeScreenLayout.MANUAL_VISIBLE_ROWS
        );
        for (int row = 0; row < visible; row++) {
            OfferButton button = new OfferButton(leftPos + ROW_X, topPos + ROW_Y + row * ROW_HEIGHT, row);
            offerButtons.add(addRenderableWidget(button));
        }
    }

    private void updateScrollFromMouse(double mouseY) {
        int offerCount = menu.getOffers().size();
        int maxScroll = Math.max(0, offerCount - VillagerTradeScreenLayout.MANUAL_VISIBLE_ROWS);
        if (maxScroll == 0) {
            return;
        }
        int thumbHeight = scrollThumbHeight(offerCount);
        int travel = SCROLL_HEIGHT - thumbHeight;
        double localThumbTop = mouseY - topPos - scrollDragOffset;
        float normalized = travel <= 0 ? 0.0F : (float)((localThumbTop - SCROLL_Y) / travel);
        firstVisibleOffer = Mth.clamp(Math.round(Mth.clamp(normalized, 0.0F, 1.0F) * maxScroll), 0, maxScroll);
        rebuildOfferButtons();
    }

    private int scrollThumbHeight(int offerCount) {
        return Math.max(18, SCROLL_HEIGHT * VillagerTradeScreenLayout.MANUAL_VISIBLE_ROWS / Math.max(1, offerCount));
    }

    private int scrollThumbY(int offerCount, int thumbHeight) {
        int maxScroll = Math.max(1, offerCount - VillagerTradeScreenLayout.MANUAL_VISIBLE_ROWS);
        return SCROLL_Y + (SCROLL_HEIGHT - thumbHeight) * firstVisibleOffer / maxScroll;
    }

    private boolean canScroll() {
        return menu.getOffers().size() > VillagerTradeScreenLayout.MANUAL_VISIBLE_ROWS;
    }

    private boolean isInsideTradeList(double mouseX, double mouseY) {
        return mouseX >= leftPos + ROW_X && mouseX < leftPos + ROW_X + 105
                && mouseY >= topPos + ROW_Y && mouseY < topPos + ROW_Y + SCROLL_HEIGHT;
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

    private final class OfferButton extends Button.Plain { // NOSONAR - Minecraft widget inheritance is framework-defined.
        private OfferButton(int x, int y, int row) {
            super(x, y, ROW_WIDTH, ROW_HEIGHT, CommonComponents.EMPTY, button -> selectOffer(firstVisibleOffer + row), DEFAULT_NARRATION);
        }

        @Override
        protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            // Visuals are rendered by the screen to preserve clipping and z-order.
        }
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
