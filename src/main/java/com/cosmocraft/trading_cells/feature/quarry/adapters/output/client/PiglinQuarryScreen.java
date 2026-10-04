package com.cosmocraft.trading_cells.feature.quarry.adapters.output.client;

import com.cosmocraft.trading_cells.feature.quarry.adapters.input.QuarryBlockEntity;
import com.cosmocraft.trading_cells.feature.quarry.adapters.input.QuarryMenu;
import com.cosmocraft.trading_cells.feature.quarry.adapters.input.PiglinQuarryMenuLayout;
import com.cosmocraft.trading_cells.feature.quarry.adapters.output.QuarryRegistrationAdapter;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.FittedTextRenderer;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.MachineScreenUtil;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.MachineSlotSprites;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.SlotRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.MachineContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class PiglinQuarryScreen extends MachineContainerScreen<QuarryMenu> {
    public static final int RECIPE_VIEWER_X = 199;
    public static final int RECIPE_VIEWER_Y = 21;
    public static final int RECIPE_VIEWER_WIDTH = 69;
    public static final int RECIPE_VIEWER_HEIGHT = 13;
    private static final Identifier SURFACE = Identifier.fromNamespaceAndPath(
            "minecraft",
            "textures/block/netherrack.png"
    );
    private static final SlotRenderer.Palette SLOT_PALETTE = new SlotRenderer.Palette(
            0xFF2F2F2F,
            0xFF575757,
            0xFF343D41,
            0xFF869397
    );
    private static final int FRAME_DARK = 0xFF551526;
    private static final int FRAME_LIGHT = 0xFFDC6679;
    private static final int[] INPUT_ROW_Y = {29, 64, 99, 134};
    private static final int INPUT_ROW_X = 10;
    private static final int INPUT_ROW_WIDTH = 103;
    private static final int INPUT_ROW_HEIGHT = 32;
    private static final int OUTPUT_HEADER_X = 151;
    private static final int OUTPUT_HEADER_Y = 36;
    private static final int OUTPUT_HEADER_WIDTH = 162;
    private static final int OUTPUT_HEADER_HEIGHT = 11;
    private static final int OUTPUT_LABEL_HEIGHT = 13;
    private static final int INVENTORY_HEADER_X = 162;
    private static final int INVENTORY_HEADER_Y = 111;
    private static final int INVENTORY_HEADER_WIDTH = 162;
    private static final int INVENTORY_HEADER_HEIGHT = 11;
    private static final int INVENTORY_LABEL_HEIGHT = 13;
    private static final int CATALOG_BUTTON_X = 134;
    private static final int CATALOG_BUTTON_Y = 49;
    private static final int CATALOG_BUTTON_SIZE = 12;
    private static final int TEXT_DARK = 0xFFE0E5E6;
    private static final int PANEL_DARK = 0xFF566166;
    private static final int PANEL_LIGHT = 0xFF343D41;
    private static final int ACCENT = 0xFF8D4652;
    private static final int OFFSCREEN_MOUSE_COORDINATE = -10_000;

    private final QuarryCatalogPanel catalog = new QuarryCatalogPanel();
    private Button catalogButton;

    public PiglinQuarryScreen(QuarryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, PiglinQuarryMenuLayout.WIDTH, PiglinQuarryMenuLayout.HEIGHT);
        titleLabelY = 8;
        inventoryLabelX = 164;
        inventoryLabelY = 112;
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
        com.cosmocraft.trading_cells.platform.neoforge.client.screen.MachineGuiSkin.PIGLIN_QUARRY.draw(
                graphics, leftPos, topPos, imageWidth, imageHeight);
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
        drawSlot(graphics, 17, 142);
        com.cosmocraft.trading_cells.platform.neoforge.client.screen.MachineSlotGhosts.upgrade(
                graphics, menu.getSlot(QuarryBlockEntity.SPEED_SLOT), leftPos, topPos, false);
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
        FittedTextRenderer.centered(graphics, font, title, 126, 340, 7, 12, TEXT_DARK, false);
        FittedTextRenderer.centered(graphics, font, playerInventoryTitle,
                INVENTORY_HEADER_X, INVENTORY_HEADER_X + INVENTORY_HEADER_WIDTH,
                INVENTORY_HEADER_Y, INVENTORY_LABEL_HEIGHT, TEXT_DARK, false);
        FittedTextRenderer.centered(graphics, font, Component.translatable("gui.trading_cells.outputs"),
                OUTPUT_HEADER_X, OUTPUT_HEADER_X + OUTPUT_HEADER_WIDTH,
                OUTPUT_HEADER_Y, OUTPUT_LABEL_HEIGHT, TEXT_DARK, false);
        drawInputLabel(graphics, "gui.trading_cells.input.piglin", INPUT_ROW_Y[0]);
        drawInputLabel(graphics, "gui.trading_cells.input.pickaxe", INPUT_ROW_Y[1]);
        drawInputLabel(graphics, "gui.trading_cells.input.upgrade", INPUT_ROW_Y[2]);
        drawInputLabel(graphics, "gui.trading_cells.interaction.speed", INPUT_ROW_Y[3]);
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
