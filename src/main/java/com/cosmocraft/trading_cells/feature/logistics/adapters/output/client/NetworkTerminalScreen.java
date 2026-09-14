package com.cosmocraft.trading_cells.feature.logistics.adapters.output.client;

import com.cosmocraft.trading_cells.feature.logistics.adapters.input.NetworkTerminalMenu;
import com.cosmocraft.trading_cells.feature.logistics.domain.model.LogisticsResourceType;
import com.cosmocraft.trading_cells.feature.logistics.domain.model.NetworkAmountFormat;
import com.cosmocraft.trading_cells.feature.logistics.adapters.neoforge.LogisticsComponentData;
import com.cosmocraft.trading_cells.platform.neoforge.network.NetworkTerminalActionPayload;
import com.cosmocraft.trading_cells.platform.neoforge.network.NetworkTerminalSyncPayload;
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public final class NetworkTerminalScreen extends AbstractContainerScreen<NetworkTerminalMenu> {
    public static final int WIDTH = 374;
    public static final int HEIGHT = 238;
    private static final int GRID_X = 198, GRID_Y = 71, GRID_WIDTH = 160, GRID_HEIGHT = 156, CELL = 20;
    private static final int WHITE = 0xFFF5F5F5, MUTED = 0xFFB8C1C4;
    private String query = "";
    private boolean recipes;
    private int debounce;
    private double scroll, targetScroll;
    private long lastFrame;
    private int requestedRow;
    private boolean draggingScroll;
    private boolean pressedNetwork;
    private boolean virtualClick;
    private Button depositContainer, withdrawContainer;
    private NetworkTerminalSyncPayload.Entry selectedContainerEntry;
    private ItemStack lastQuickMoveItem = ItemStack.EMPTY;
    private Slot lastQuickMoveSlot, lastPlayerClick;

    public NetworkTerminalScreen(NetworkTerminalMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, WIDTH, HEIGHT);
        inventoryLabelY = 141;
        inventoryLabelX = 12;
    }

    @Override
    protected void init() {
        super.init();
        int index = 0;
        for (LogisticsResourceType type : LogisticsResourceType.values()) {
            addRenderableWidget(Button.builder(label("resource." + type.serializedName()), ignored -> {
                recipes = false;
                resetScroll();
                ClientPacketDistributor.sendToServer(NetworkTerminalActionPayload.view(menu.containerId, type, 0, query));
            }).bounds(leftPos + 12 + index++ * 70, topPos + 25, 68, 18).build());
        }
        if (menu.crafting()) {
            addRenderableWidget(Button.builder(label("recipes"), ignored -> {
                recipes = !recipes;
                resetScroll();
                sendView(0);
            }).bounds(leftPos + 294, topPos + 25, 68, 18).build());
        }
        EditBox search = addRenderableWidget(new EditBox(font, leftPos + GRID_X, topPos + 48, GRID_WIDTH, 18, label("search")));
        search.setMaxLength(64);
        search.setHint(label("search"));
        search.setValue(query);
        search.setResponder(value -> { query = value; debounce = 5; });
        depositContainer = arrowButton(112, 77, false, "insert", () ->
                sendAction(NetworkTerminalActionPayload.Action.CONTAINER_DEPOSIT, 0, Long.MAX_VALUE, null));
        withdrawContainer = arrowButton(112, 104, true, "withdraw", () -> {
            if (selectedContainerEntry != null) {
                sendAction(NetworkTerminalActionPayload.Action.CONTAINER_WITHDRAW, 0, Long.MAX_VALUE, selectedContainerEntry);
            }
        });
        updateContainerButton();
        lastFrame = System.nanoTime();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateContainerButton();
        if (debounce > 0 && --debounce == 0) {
            resetScroll();
            sendView(0);
        } else {
            int row = Math.max(0, (int) scroll / CELL - 1);
            if (row != requestedRow) { sendView(row); }
        }
    }

    private void updateContainerButton() {
        depositContainer.visible = menu.selectedType() != LogisticsResourceType.ITEM;
        withdrawContainer.visible = depositContainer.visible;
        if (selectedContainerEntry != null) {
            selectedContainerEntry = menu.entries().stream().filter(entry -> entry.key().equals(selectedContainerEntry.key()))
                    .findFirst().orElse(null);
        }
        if (selectedContainerEntry == null && !menu.entries().isEmpty()) { selectedContainerEntry = menu.entries().getFirst(); }
        depositContainer.active = menu.getSlot(36).hasItem();
        withdrawContainer.active = depositContainer.active && selectedContainerEntry != null;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (!event.isEscape() && event.key() != 258 && getFocused() instanceof EditBox field && field.isFocused()) {
            field.keyPressed(event);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        long now = System.nanoTime();
        double elapsed = Math.min(0.1, (now - lastFrame) / 1_000_000_000.0);
        lastFrame = now;
        targetScroll = Math.clamp(targetScroll, 0, maximumScroll());
        scroll += (targetScroll - scroll) * (1 - Math.exp(-18 * elapsed));
        if (Math.abs(scroll - targetScroll) < 0.05) { scroll = targetScroll; }
        graphics.fill(leftPos, topPos, leftPos + WIDTH, topPos + HEIGHT, 0xFF899296);
        graphics.fill(leftPos + 2, topPos + 2, leftPos + WIDTH - 2, topPos + HEIGHT - 2, 0xF0272D30);
        graphics.enableScissor(leftPos + GRID_X, topPos + GRID_Y, leftPos + GRID_X + GRID_WIDTH, topPos + GRID_Y + GRID_HEIGHT);
        List<NetworkTerminalSyncPayload.Entry> entries = menu.entries();
        for (int index = 0; index < NetworkTerminalMenu.PAGE_SIZE; index++) {
            int x = leftPos + GRID_X + index % NetworkTerminalMenu.NETWORK_COLUMNS * CELL;
            int y = entryY(index);
            slot(graphics, x, y, CELL, 0xFF5D6568);
            if (index < entries.size()) {
                var entry = entries.get(index);
                graphics.fakeItem(entry.icon(), x + 2, y + 1);
                if (!recipes) {
                    drawAmount(graphics, entry.amount(), x, y);
                }
                if (overGrid(mouseX, mouseY) && inside(mouseX, mouseY, x, y, CELL, CELL)) {
                    graphics.setComponentTooltipForNextFrame(font, entryTooltip(entry), mouseX, mouseY,
                            menu.selectedType() == LogisticsResourceType.ITEM ? entry.icon() : ItemStack.EMPTY);
                }
            }
        }
        graphics.disableScissor();
        if (menu.selectedType() != LogisticsResourceType.ITEM && selectedContainerEntry != null) {
            slot(graphics, leftPos + 145, topPos + 90, 18, 0xFF65DDE2);
            graphics.fakeItem(selectedContainerEntry.icon(), leftPos + 146, topPos + 91);
            if (inside(mouseX, mouseY, leftPos + 145, topPos + 90, 18, 18)) {
                graphics.setComponentTooltipForNextFrame(font, entryTooltip(selectedContainerEntry), mouseX, mouseY, ItemStack.EMPTY);
            }
        }
        graphics.fill(leftPos + 361, topPos + GRID_Y, leftPos + 367, topPos + GRID_Y + GRID_HEIGHT, 0xFF15191B);
        int thumbHeight = Math.max(12, (int) (GRID_HEIGHT * (double) GRID_HEIGHT / Math.max(GRID_HEIGHT, menu.totalPages() * CELL)));
        int thumbY = topPos + GRID_Y + (int) (maximumScroll() == 0 ? 0 : scroll / maximumScroll() * (GRID_HEIGHT - thumbHeight));
        graphics.fill(leftPos + 361, thumbY, leftPos + 367, thumbY + thumbHeight, MUTED);
        for (var inventorySlot : menu.slots) {
            if (inventorySlot.isActive()) {
                slot(graphics, leftPos + inventorySlot.x - 1, topPos + inventorySlot.y - 1, 18, 0xFF727D82);
            }
        }
        if (menu.crafting() && menu.selectedType() == LogisticsResourceType.ITEM) {
            for (int index = 0; index < 9; index++) {
                int x = leftPos + 23 + index % 3 * 18, y = topPos + 76 + index / 3 * 18;
                slot(graphics, x, y, 18, 0xFF727D82);
                graphics.fakeItem(menu.craftingGrid().get(index), x + 1, y + 1);
            }
            drawArrow(graphics, leftPos + 95, topPos + 97, false, WHITE);
            slot(graphics, leftPos + 132, topPos + 93, 22, 0xFF96907D);
            graphics.fakeItem(menu.craftingResult(), leftPos + 135, topPos + 96);
            graphics.itemDecorations(font, menu.craftingResult(), leftPos + 135, topPos + 96);
            if (!menu.craftingResult().isEmpty() && inside(mouseX, mouseY, leftPos + 132, topPos + 93, 22, 22)) {
                graphics.setComponentTooltipForNextFrame(font, getTooltipFromItem(minecraft, menu.craftingResult()), mouseX, mouseY, menu.craftingResult());
            }
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, title, 12, 9, WHITE, true);
        graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, MUTED, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        pressedNetwork = overGrid(event.x(), event.y());
        virtualClick = false;
        Slot playerSlot = menu.slots.stream().limit(36).filter(candidate ->
                inside(event.x(), event.y(), leftPos + candidate.x, topPos + candidate.y, 16, 16)).findFirst().orElse(null);
        boolean repeatedSlot = playerSlot != null && playerSlot == lastPlayerClick && playerSlot == lastQuickMoveSlot;
        lastPlayerClick = playerSlot;
        if (!recipes && menu.selectedType() == LogisticsResourceType.ITEM && event.button() == 0
                && event.hasShiftDown() && doubleClick && repeatedSlot) {
            ItemStack matching = playerSlot.hasItem() ? playerSlot.getItem() : lastQuickMoveItem;
            if (!matching.isEmpty()) {
                ClientPacketDistributor.sendToServer(new NetworkTerminalActionPayload(menu.containerId,
                        NetworkTerminalActionPayload.Action.INSERT_INVENTORY, LogisticsResourceType.ITEM, 0, "",
                        InteractionHand.MAIN_HAND, null, BuiltInRegistries.ITEM.getKey(matching.getItem()),
                        LogisticsComponentData.fingerprint(matching.getComponentsPatch()), Long.MAX_VALUE));
                isQuickCrafting = false; quickCraftSlots.clear(); virtualClick = true;
                return true;
            }
        }
        if (event.button() == 0 && inside(event.x(), event.y(), leftPos + 360, topPos + GRID_Y, 8, GRID_HEIGHT)) {
            draggingScroll = true;
            dragScrollbar(event.y());
            return true;
        }
        if (overGrid(event.x(), event.y()) && (event.button() == 0 || event.button() == 1)) {
            virtualClick = true;
            if (!recipes && menu.selectedType() == LogisticsResourceType.ITEM && !menu.getCarried().isEmpty()) {
                if (doubleClick && event.button() == 0 && !event.hasShiftDown()) {
                    var matching = menu.entries().stream().filter(entry ->
                            ItemStack.isSameItemSameComponents(entry.icon(), menu.getCarried())).findFirst().orElse(null);
                    if (matching != null) {
                        sendAction(NetworkTerminalActionPayload.Action.CURSOR_WITHDRAW, 0, menu.getCarried().getMaxStackSize(), matching);
                    }
                    return true;
                }
                sendAction(NetworkTerminalActionPayload.Action.CURSOR_DEPOSIT, 0,
                        event.button() == 1 ? 1 : menu.getCarried().getCount(), null);
                return true;
            }
            int row = (int) Math.floor((event.y() - topPos - GRID_Y + scroll) / CELL);
            int column = (int) (event.x() - leftPos - GRID_X) / CELL;
            int index = (row - menu.page()) * NetworkTerminalMenu.NETWORK_COLUMNS + column;
            if (index >= 0 && index < menu.entries().size()) {
                var entry = menu.entries().get(index);
                if (recipes) {
                    sendAction(NetworkTerminalActionPayload.Action.SELECT_RECIPE, 0, 0, entry);
                } else if (menu.selectedType() == LogisticsResourceType.ITEM) {
                    long count = Math.min(entry.amount(), entry.icon().isEmpty() ? 64 : entry.icon().getMaxStackSize());
                    sendAction(NetworkTerminalActionPayload.Action.CURSOR_WITHDRAW, event.hasShiftDown() ? 1 : 0,
                            event.button() == 1 ? (count + 1) / 2 : count, entry);
                } else {
                    selectedContainerEntry = entry;
                    sendAction(NetworkTerminalActionPayload.Action.CONTAINER_WITHDRAW, 0, Long.MAX_VALUE, entry);
                }
            }
            return true;
        }
        if (menu.crafting() && menu.selectedType() == LogisticsResourceType.ITEM) {
            for (int index = 0; index < 9; index++) {
                if (inside(event.x(), event.y(), leftPos + 23 + index % 3 * 18, topPos + 76 + index / 3 * 18, 18, 18)) {
                    virtualClick = true;
                    sendAction(NetworkTerminalActionPayload.Action.GHOST, index, event.button() == 1 ? 0 : 1, null);
                    return true;
                }
            }
            if (inside(event.x(), event.y(), leftPos + 132, topPos + 93, 22, 22)) {
                virtualClick = true;
                sendAction(event.hasShiftDown() ? NetworkTerminalActionPayload.Action.CRAFT_STACK
                        : NetworkTerminalActionPayload.Action.CRAFT_CURSOR, 0, 1, null);
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    protected void slotClicked(Slot slot, int slotId, int button, ContainerInput input) {
        if (input == ContainerInput.QUICK_MOVE && slot != null && slot.hasItem()) {
            lastQuickMoveItem = slot.getItem().copy();
            lastQuickMoveSlot = slot;
        }
        super.slotClicked(slot, slotId, button, input);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (overGrid(x, y)) {
            targetScroll = Math.clamp(targetScroll - scrollY * 16, 0, maximumScroll());
            return true;
        }
        return super.mouseScrolled(x, y, scrollX, scrollY);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        if (draggingScroll) { dragScrollbar(event.y()); return true; }
        return super.mouseDragged(event, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (draggingScroll) { draggingScroll = false; return true; }
        if (virtualClick) {
            virtualClick = false;
            return true;
        }
        if (!pressedNetwork && !recipes && menu.selectedType() == LogisticsResourceType.ITEM
                && overGrid(event.x(), event.y()) && !menu.getCarried().isEmpty()
                && (event.button() == 0 || event.button() == 1)) {
            isQuickCrafting = false;
            quickCraftSlots.clear();
            sendAction(NetworkTerminalActionPayload.Action.CURSOR_DEPOSIT, 0,
                    event.button() == 1 ? 1 : menu.getCarried().getCount(), null);
            return true;
        }
        return super.mouseReleased(event);
    }

    private void dragScrollbar(double y) {
        targetScroll = Math.clamp((y - topPos - GRID_Y) / GRID_HEIGHT, 0, 1) * maximumScroll();
        scroll = targetScroll;
    }

    private int entryY(int index) {
        return topPos + GRID_Y + (menu.page() + index / NetworkTerminalMenu.NETWORK_COLUMNS) * CELL - (int) scroll;
    }

    private double maximumScroll() { return Math.max(0, menu.totalPages() * CELL - GRID_HEIGHT); }
    private boolean overGrid(double x, double y) { return inside(x, y, leftPos + GRID_X, topPos + GRID_Y, GRID_WIDTH, GRID_HEIGHT); }
    private void resetScroll() { scroll = 0; targetScroll = 0; requestedRow = 0; }

    private void sendView(int row) {
        requestedRow = Math.max(0, row);
        if (recipes) { sendAction(NetworkTerminalActionPayload.Action.RECIPES, requestedRow, 0, null); }
        else { ClientPacketDistributor.sendToServer(NetworkTerminalActionPayload.view(menu.containerId, menu.selectedType(), requestedRow, query)); }
    }

    private void sendAction(NetworkTerminalActionPayload.Action action, int page, long quantity, NetworkTerminalSyncPayload.Entry entry) {
        ClientPacketDistributor.sendToServer(new NetworkTerminalActionPayload(menu.containerId, action,
                menu.selectedType(), page, query, InteractionHand.MAIN_HAND, entry == null ? null : entry.adapterId(),
                entry == null ? null : entry.resourceId(), entry == null ? "" : entry.componentFingerprint(), quantity));
    }

    private void drawAmount(GuiGraphicsExtractor graphics, long amount, int x, int y) {
        String count = NetworkAmountFormat.compact(amount, minecraft.options.languageCode);
        int width = font.width(count);
        float scale = NetworkAmountFormat.scaleToFit(width + 1, CELL - 2);
        graphics.pose().pushMatrix();
        graphics.pose().translate(x + CELL - 1 - (width + 1) * scale, y + CELL - 1 - (font.lineHeight + 1) * scale);
        graphics.pose().scale(scale, scale);
        graphics.text(font, count, 0, 0, WHITE, true);
        graphics.pose().popMatrix();
    }

    private List<Component> entryTooltip(NetworkTerminalSyncPayload.Entry entry) {
        var lines = new ArrayList<Component>();
        if (menu.selectedType() == LogisticsResourceType.ITEM && !entry.icon().isEmpty()) {
            lines.addAll(getTooltipFromItem(minecraft, entry.icon()));
        } else if (menu.selectedType() == LogisticsResourceType.FLUID || menu.selectedType() == LogisticsResourceType.GAS) {
            lines.add(BuiltInRegistries.FLUID.get(entry.resourceId()).map(holder ->
                    net.neoforged.neoforge.transfer.fluid.FluidResource.of(holder.value()).getHoverName())
                    .orElse(Component.literal(entry.displayName())));
        } else { lines.add(Component.literal(entry.displayName())); }
        if (!recipes) {
            var locale = Locale.forLanguageTag(minecraft.options.languageCode.replace('_', '-'));
            lines.add(1, Component.literal(java.text.NumberFormat.getIntegerInstance(locale).format(entry.amount()))
                    .withStyle(ChatFormatting.AQUA));
        }
        if (minecraft.options.advancedItemTooltips && menu.selectedType() != LogisticsResourceType.ITEM) {
            lines.add(Component.literal(entry.resourceId().toString()).withStyle(ChatFormatting.DARK_GRAY));
        }
        return lines;
    }

    private Button arrowButton(int x, int y, boolean left, String tooltip, Runnable press) {
        var button = new Button(leftPos + x, topPos + y, 26, 20, label(tooltip), ignored -> press.run(), supplier -> supplier.get()) {
            @Override
            protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
                extractDefaultSprite(graphics);
                drawArrow(graphics, getX() + 2, getY() + 3, left, active ? WHITE : MUTED);
            }
        };
        button.setTooltip(Tooltip.create(label(tooltip)));
        return addRenderableWidget(button);
    }

    private static void drawArrow(GuiGraphicsExtractor graphics, int x, int y, boolean left, int color) {
        for (int column = 0; column < 22; column++) {
            int halfHeight = column < 13 ? 2 : 10 - (column - 12);
            halfHeight = Math.min(7, halfHeight);
            int at = x + (left ? 21 - column : column);
            graphics.fill(at, y + 7 - halfHeight, at + 1, y + 8 + halfHeight, 0xFF41484B);
            if (halfHeight > 0) { graphics.fill(at, y + 8 - halfHeight, at + 1, y + 7 + halfHeight, color); }
        }
    }

    private static Component label(String suffix) { return Component.translatable("gui.trading_cells.pipe." + suffix); }
    private static boolean inside(double x, double y, int left, int top, int width, int height) {
        return x >= left && x < left + width && y >= top && y < top + height;
    }
    private static void slot(GuiGraphicsExtractor graphics, int x, int y, int size, int border) {
        graphics.fill(x, y, x + size, y + size, border);
        graphics.fill(x + 1, y + 1, x + size - 1, y + size - 1, 0xFF343C40);
    }
}
