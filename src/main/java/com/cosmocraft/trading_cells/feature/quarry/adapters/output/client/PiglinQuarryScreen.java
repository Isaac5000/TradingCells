package com.cosmocraft.trading_cells.feature.quarry.adapters.output.client;

import com.cosmocraft.trading_cells.feature.quarry.adapters.input.QuarryBlockEntity;
import com.cosmocraft.trading_cells.feature.quarry.adapters.input.QuarryMenu;
import com.cosmocraft.trading_cells.feature.quarry.adapters.input.PiglinQuarryMenuLayout;
import com.cosmocraft.trading_cells.feature.quarry.adapters.output.QuarryRegistrationAdapter;
import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.TradingCells;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.FittedTextRenderer;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.MachineScreenUtil;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.MachineSlotSprites;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.SlotRenderer;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.TiledSurfaceRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class PiglinQuarryScreen extends AbstractContainerScreen<QuarryMenu> {
    public static final int RECIPE_VIEWER_X = 199;
    public static final int RECIPE_VIEWER_Y = 30;
    public static final int RECIPE_VIEWER_WIDTH = 69;
    public static final int RECIPE_VIEWER_HEIGHT = 13;
    private static final Identifier BACKGROUND = Identifier.fromNamespaceAndPath(
            TradingCells.MOD_ID,
            "textures/gui/quarry/piglin_background.png"
    );
    private static final Identifier SURFACE = Identifier.fromNamespaceAndPath(
            "minecraft",
            "textures/block/netherrack.png"
    );
    private static final Identifier SURFACE_OVERLAY = Identifier.fromNamespaceAndPath(
            TradingCells.MOD_ID,
            "textures/gui/quarry/piglin_surface_overlay.png"
    );
    private static final SlotRenderer.Palette SLOT_PALETTE = new SlotRenderer.Palette(
            0xFF2F2F2F,
            0xFF575757,
            0xFFBEBEBE,
            0xFFE2E2E2
    );
    private static final int FRAME_DARK = 0xFF551526;
    private static final int FRAME_LIGHT = 0xFFDC6679;
    private static final int LEFT_SURFACE_X = 6;
    private static final int LEFT_SURFACE_Y = 26;
    private static final int LEFT_SURFACE_WIDTH = 112;
    private static final int LEFT_SURFACE_HEIGHT = 179;
    private static final int MACHINE_SURFACE_X = 124;
    private static final int MACHINE_SURFACE_Y = 26;
    private static final int MACHINE_SURFACE_WIDTH = 218;
    private static final int MACHINE_SURFACE_HEIGHT = 75;
    private static final int[] INPUT_ROW_Y = {29, 64, 99};
    private static final int INPUT_ROW_X = 10;
    private static final int INPUT_ROW_WIDTH = 103;
    private static final int INPUT_ROW_HEIGHT = 32;
    private static final int OUTPUT_HEADER_X = 151;
    private static final int OUTPUT_HEADER_Y = 51;
    private static final int OUTPUT_HEADER_WIDTH = 162;
    private static final int OUTPUT_HEADER_HEIGHT = 11;
    private static final int OUTPUT_LABEL_HEIGHT = 13;
    private static final int INVENTORY_HEADER_X = 162;
    private static final int INVENTORY_HEADER_Y = 105;
    private static final int INVENTORY_HEADER_WIDTH = 162;
    private static final int INVENTORY_HEADER_HEIGHT = 11;
    private static final int INVENTORY_LABEL_HEIGHT = 13;
    private static final int CATALOG_BUTTON_X = 134;
    private static final int CATALOG_BUTTON_Y = 63;
    private static final int CATALOG_BUTTON_SIZE = 12;
    private static final int TEXT_DARK = 0xFF303436;
    private static final int PANEL_DARK = 0xFF4A4E50;
    private static final int PANEL_LIGHT = 0xFFAEB3B5;
    private static final int ACCENT = 0xFF8D4652;
    private static final int OFFSCREEN_MOUSE_COORDINATE = -10_000;

    private final QuarryCatalogPanel catalog = new QuarryCatalogPanel();
    private Button catalogButton;

    public PiglinQuarryScreen(QuarryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, PiglinQuarryMenuLayout.WIDTH, PiglinQuarryMenuLayout.HEIGHT);
        titleLabelY = 8;
        inventoryLabelX = 164;
        inventoryLabelY = 108;
    }

    @Override
    protected void init() {
        super.init();
        catalogButton = addRenderableWidget(Button.builder(Component.literal("?"), button -> catalog.toggle(menu))
                .bounds(leftPos + CATALOG_BUTTON_X, topPos + CATALOG_BUTTON_Y,
                        CATALOG_BUTTON_SIZE, CATALOG_BUTTON_SIZE)
                .build());
        catalogButton.setTooltip(Tooltip.create(Component.translatable("button.trading_cells.quarry_catalog")));
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        catalog.tick(menu);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        drawBackground(graphics);
        drawSurfaces(graphics);
        drawSurfaceOverlay(graphics);
        drawOutputHeader(graphics);
        drawInventoryHeader(graphics);
        drawInputRows(graphics);
        drawInventorySlots(graphics);
        drawMachineSlots(graphics);
        drawProgress(graphics);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        drawLabels(graphics);
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int contentMouseX = catalog.isOpen() ? OFFSCREEN_MOUSE_COORDINATE : mouseX;
        int contentMouseY = catalog.isOpen() ? OFFSCREEN_MOUSE_COORDINATE : mouseY;
        super.extractContents(graphics, contentMouseX, contentMouseY, partialTick);
        if (catalog.isOpen()) {
            graphics.nextStratum();
            catalog.draw(graphics, font, menu, leftPos, topPos, mouseX, mouseY, FRAME_DARK, FRAME_LIGHT);
        }
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (catalog.mouseScrolled(x, y, scrollY, leftPos, topPos, menu)) {
            return true;
        }
        return catalog.isOpen() || super.mouseScrolled(x, y, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        boolean overCatalogButton = catalogButton != null && catalogButton.isMouseOver(event.x(), event.y());
        if (catalog.isOpen() && !overCatalogButton) {
            catalog.close();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void drawBackground(GuiGraphicsExtractor graphics) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos, topPos, 0.0F, 0.0F,
                PiglinQuarryMenuLayout.WIDTH, PiglinQuarryMenuLayout.HEIGHT,
                PiglinQuarryMenuLayout.ATLAS_WIDTH, PiglinQuarryMenuLayout.ATLAS_HEIGHT);
    }

    private void drawSurfaces(GuiGraphicsExtractor graphics) {
        TiledSurfaceRenderer.draw(graphics, SURFACE, leftPos + LEFT_SURFACE_X, topPos + LEFT_SURFACE_Y,
                LEFT_SURFACE_WIDTH, LEFT_SURFACE_HEIGHT, 0x00000000);
        TiledSurfaceRenderer.draw(graphics, SURFACE, leftPos + MACHINE_SURFACE_X, topPos + MACHINE_SURFACE_Y,
                MACHINE_SURFACE_WIDTH, MACHINE_SURFACE_HEIGHT, 0x00000000);
    }

    private void drawSurfaceOverlay(GuiGraphicsExtractor graphics) {
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                SURFACE_OVERLAY,
                leftPos,
                topPos,
                0.0F,
                0.0F,
                PiglinQuarryMenuLayout.WIDTH,
                PiglinQuarryMenuLayout.HEIGHT,
                PiglinQuarryMenuLayout.WIDTH,
                PiglinQuarryMenuLayout.HEIGHT
        );
    }

    private void drawInputRows(GuiGraphicsExtractor graphics) {
        for (int rowY : INPUT_ROW_Y) {
            int x = leftPos + INPUT_ROW_X;
            int y = topPos + rowY;
            graphics.fill(x, y, x + INPUT_ROW_WIDTH, y + INPUT_ROW_HEIGHT, PANEL_DARK);
            graphics.fill(x + 1, y + 1, x + INPUT_ROW_WIDTH - 1, y + INPUT_ROW_HEIGHT - 1, PANEL_LIGHT);
            graphics.fill(x + 1, y + 1, x + 4, y + INPUT_ROW_HEIGHT - 1, ACCENT);
        }
    }

    private void drawOutputHeader(GuiGraphicsExtractor graphics) {
        int x = leftPos + OUTPUT_HEADER_X;
        int y = topPos + OUTPUT_HEADER_Y;
        graphics.fill(x, y, x + OUTPUT_HEADER_WIDTH, y + OUTPUT_HEADER_HEIGHT, PANEL_DARK);
        graphics.fill(x + 1, y + 1, x + OUTPUT_HEADER_WIDTH - 1, y + OUTPUT_HEADER_HEIGHT - 1, PANEL_LIGHT);
    }

    private void drawInventoryHeader(GuiGraphicsExtractor graphics) {
        int x = leftPos + INVENTORY_HEADER_X;
        int y = topPos + INVENTORY_HEADER_Y;
        graphics.fill(x, y, x + INVENTORY_HEADER_WIDTH, y + INVENTORY_HEADER_HEIGHT, PANEL_DARK);
        graphics.fill(x + 1, y + 1, x + INVENTORY_HEADER_WIDTH - 1, y + INVENTORY_HEADER_HEIGHT - 1, PANEL_LIGHT);
    }

    private void drawInventorySlots(GuiGraphicsExtractor graphics) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                drawSlot(graphics, PiglinQuarryMenuLayout.PLAYER_INVENTORY_X + column * 18,
                        PiglinQuarryMenuLayout.PLAYER_INVENTORY_Y + row * 18);
            }
        }
        for (int column = 0; column < 9; column++) {
            drawSlot(graphics, PiglinQuarryMenuLayout.PLAYER_INVENTORY_X + column * 18,
                    PiglinQuarryMenuLayout.PLAYER_HOTBAR_Y);
        }
        drawSlot(graphics, PiglinQuarryMenuLayout.EQUIPMENT_X, PiglinQuarryMenuLayout.EQUIPMENT_HEAD_Y);
        drawSlot(graphics, PiglinQuarryMenuLayout.EQUIPMENT_X, PiglinQuarryMenuLayout.EQUIPMENT_CHEST_Y);
        drawSlot(graphics, PiglinQuarryMenuLayout.EQUIPMENT_X, PiglinQuarryMenuLayout.EQUIPMENT_LEGS_Y);
        drawSlot(graphics, PiglinQuarryMenuLayout.EQUIPMENT_X, PiglinQuarryMenuLayout.EQUIPMENT_FEET_Y);
        drawSlot(graphics, PiglinQuarryMenuLayout.EQUIPMENT_X, PiglinQuarryMenuLayout.EQUIPMENT_OFFHAND_Y);
    }

    private void drawMachineSlots(GuiGraphicsExtractor graphics) {
        drawSlot(graphics, PiglinQuarryMenuLayout.WORKER_SLOT_X, PiglinQuarryMenuLayout.WORKER_SLOT_Y);
        drawSlot(graphics, PiglinQuarryMenuLayout.PICKAXE_SLOT_X, PiglinQuarryMenuLayout.PICKAXE_SLOT_Y);
        drawSlot(graphics, PiglinQuarryMenuLayout.UPGRADE_SLOT_X, PiglinQuarryMenuLayout.UPGRADE_SLOT_Y);
        if (!menu.getSlot(QuarryBlockEntity.WORKER_SLOT).hasItem()) {
            drawEmptySprite(graphics, PiglinQuarryMenuLayout.WORKER_SLOT_X,
                    PiglinQuarryMenuLayout.WORKER_SLOT_Y, MachineSlotSprites.PIGLIN_HEAD);
        }
        if (!menu.getSlot(QuarryBlockEntity.PICKAXE_SLOT).hasItem()) {
            drawGhostItem(graphics, new ItemStack(Items.IRON_PICKAXE),
                    PiglinQuarryMenuLayout.PICKAXE_SLOT_X, PiglinQuarryMenuLayout.PICKAXE_SLOT_Y);
        }
        if (!menu.getSlot(QuarryBlockEntity.UPGRADE_SLOT).hasItem()) {
            drawGhostItem(graphics, QuarryRegistrationAdapter.QUARRY_COPPER_UPGRADE_ITEM.get().getDefaultInstance(),
                    PiglinQuarryMenuLayout.UPGRADE_SLOT_X, PiglinQuarryMenuLayout.UPGRADE_SLOT_Y);
        }
        for (int index = 0; index < QuarryBlockEntity.OUTPUT_SLOT_COUNT; index++) {
            drawSlot(graphics, menu.outputSlotX(index), menu.outputSlotY(index));
        }
    }

    private void drawProgress(GuiGraphicsExtractor graphics) {
        int x = leftPos + RECIPE_VIEWER_X;
        int y = topPos + RECIPE_VIEWER_Y;
        drawProgressFrame(graphics, x, y);
        int fill = Math.min(RECIPE_VIEWER_WIDTH - 4,
                menu.cycleTicks() * (RECIPE_VIEWER_WIDTH - 4) / menu.maximumCycleTicks());
        if (fill > 0) {
            graphics.fill(x + 2, y + 2, x + 2 + fill, y + RECIPE_VIEWER_HEIGHT - 2, 0xFFD06A3A);
        }
        if (menu.cycleTicks() > 0) {
            MachineScreenUtil.drawCenteredCountdown(graphics, font, x + RECIPE_VIEWER_WIDTH / 2, y + 1,
                    menu.cycleTicks(), menu.maximumCycleTicks());
        }
    }

    private void drawLabels(GuiGraphicsExtractor graphics) {
        FittedTextRenderer.centered(graphics, font, Component.translatable("gui.trading_cells.inputs"),
                9, 115, 8, 18, TEXT_DARK, false);
        FittedTextRenderer.centered(graphics, font, title, 126, 340, 8, 18, TEXT_DARK, false);
        FittedTextRenderer.centered(graphics, font, playerInventoryTitle,
                INVENTORY_HEADER_X, INVENTORY_HEADER_X + INVENTORY_HEADER_WIDTH,
                INVENTORY_HEADER_Y, INVENTORY_LABEL_HEIGHT, TEXT_DARK, false);
        FittedTextRenderer.centered(graphics, font, Component.translatable("gui.trading_cells.outputs"),
                OUTPUT_HEADER_X, OUTPUT_HEADER_X + OUTPUT_HEADER_WIDTH,
                OUTPUT_HEADER_Y, OUTPUT_LABEL_HEIGHT, TEXT_DARK, false);
        drawInputLabel(graphics, "gui.trading_cells.input.piglin", INPUT_ROW_Y[0]);
        drawInputLabel(graphics, "gui.trading_cells.input.pickaxe", INPUT_ROW_Y[1]);
        drawInputLabel(graphics, "gui.trading_cells.input.upgrade", INPUT_ROW_Y[2]);
    }

    private void drawInputLabel(GuiGraphicsExtractor graphics, String key, int rowY) {
        FittedTextRenderer.centered(graphics, font, Component.translatable(key),
                39, 109, rowY, INPUT_ROW_HEIGHT, TEXT_DARK, false);
    }

    private void drawSlot(GuiGraphicsExtractor graphics, int x, int y) {
        SlotRenderer.drawAtItemPosition(graphics, leftPos, topPos, x, y, SLOT_PALETTE);
    }

    private void drawEmptySprite(GuiGraphicsExtractor graphics, int x, int y, Identifier sprite) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, leftPos + x, topPos + y, 16, 16);
    }

    private void drawGhostItem(GuiGraphicsExtractor graphics, ItemStack stack, int x, int y) {
        graphics.fakeItem(stack, leftPos + x, topPos + y);
        graphics.fill(leftPos + x, topPos + y, leftPos + x + 16, topPos + y + 16, 0x80606060);
    }

    private boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= leftPos + x && mouseX < leftPos + x + width
                && mouseY >= topPos + y && mouseY < topPos + y + height;
    }

    private static void drawProgressFrame(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.fill(x, y, x + RECIPE_VIEWER_WIDTH, y + RECIPE_VIEWER_HEIGHT, 0xFF303436);
        graphics.fill(x + 1, y + 1, x + RECIPE_VIEWER_WIDTH - 1, y + RECIPE_VIEWER_HEIGHT - 1, 0xFF8A9093);
        graphics.fill(x + 2, y + 2, x + RECIPE_VIEWER_WIDTH - 2, y + RECIPE_VIEWER_HEIGHT - 2, 0xFF555B5E);
    }
}
