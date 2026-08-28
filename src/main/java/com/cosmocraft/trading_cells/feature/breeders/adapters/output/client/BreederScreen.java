package com.cosmocraft.trading_cells.feature.breeders.adapters.output.client;

import com.cosmocraft.trading_cells.feature.breeders.adapters.input.BreederBlockEntity;
import com.cosmocraft.trading_cells.feature.breeders.adapters.input.BreederMenu;
import com.cosmocraft.trading_cells.feature.breeders.domain.model.BreederKind;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.MachineScreenLayout;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.MachineScreenTheme;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.MachineScreenUtil;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.MachineSlotSprites;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

public final class BreederScreen extends AbstractContainerScreen<BreederMenu> { // NOSONAR - Minecraft fixes the screen inheritance hierarchy.
    private static final Identifier OAK_PLANKS = Identifier.fromNamespaceAndPath(
            "minecraft",
            "textures/block/oak_planks.png"
    );
    private static final Identifier CRIMSON_PLANKS = Identifier.fromNamespaceAndPath(
            "minecraft",
            "textures/block/crimson_planks.png"
    );
    private static final int PROGRESS_FRAME_X = MachineScreenLayout.machineX(54);
    private static final int PROGRESS_FRAME_Y = 65;
    private static final int PROGRESS_X = PROGRESS_FRAME_X + MachineScreenLayout.PROGRESS_FILL_X_OFFSET;
    private static final int PROGRESS_Y = PROGRESS_FRAME_Y + MachineScreenLayout.PROGRESS_FILL_Y_OFFSET;
    private static final int PROGRESS_WIDTH = MachineScreenLayout.PROGRESS_FILL_WIDTH;
    private static final int PROGRESS_HEIGHT = MachineScreenLayout.PROGRESS_FILL_HEIGHT;

    private static final int OFFSCREEN_MOUSE_COORDINATE = -10_000;
    private static final int MAX_VISIBLE_VARIANTS = 4;
    private static final int VARIANT_BUTTON_X = MachineScreenLayout.machineX(101);
    private static final int VARIANT_BUTTON_Y = 23;
    private static final int VARIANT_LIST_X = VARIANT_BUTTON_X;
    private static final int VARIANT_LIST_Y = 42;
    private static final int VARIANT_LIST_WIDTH = 67;
    private static final int VARIANT_ROW_HEIGHT = 16;

    private final List<VariantButton> variantButtons = new ArrayList<>();
    private Button variantSelector;
    private boolean variantListOpen;
    private int variantScroll;

    public BreederScreen(BreederMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, MachineScreenLayout.WIDTH, MachineScreenLayout.HEIGHT);
        titleLabelY = MachineScreenLayout.TITLE_Y;
        inventoryLabelX = MachineScreenLayout.PLAYER_INVENTORY_LABEL_X;
        inventoryLabelY = MachineScreenLayout.PLAYER_INVENTORY_LABEL_Y;
    }

    @Override
    protected void init() {
        super.init();
        if (menu.kind() != BreederKind.VILLAGER) {
            return;
        }
        variantSelector = addRenderableWidget(new VariantSelectorButton(
                leftPos + VARIANT_BUTTON_X,
                topPos + VARIANT_BUTTON_Y
        ));
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (variantSelector != null) {
            variantSelector.setMessage(variantSelectorLabel());
        }
        if (variantListOpen
                && variantButtons.size() != Math.min(MAX_VISIBLE_VARIANTS, menu.villagerVariantCount())) {
            closeVariantList();
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int x = leftPos;
        int y = topPos;
        boolean villager = menu.kind() == BreederKind.VILLAGER;
        MachineScreenTheme theme = villager
                ? MachineScreenTheme.VILLAGER_BREEDER
                : MachineScreenTheme.PIGLIN_BREEDER;
        Identifier surface = villager ? OAK_PLANKS : CRIMSON_PLANKS;
        MachineScreenLayout.drawBackground(graphics, x, y, surface, theme);

        MachineScreenLayout.drawSlot(graphics, x, y, BreederMenu.FOOD_SLOT_X, BreederMenu.FOOD_SLOT_Y, theme);
        if (!menu.getSlot(BreederBlockEntity.FOOD_SLOT).hasItem()) {
            MachineScreenLayout.drawEmptySlotSprite(
                    graphics,
                    x,
                    y,
                    BreederMenu.FOOD_SLOT_X,
                    BreederMenu.FOOD_SLOT_Y,
                    villager ? MachineSlotSprites.BREAD : MachineSlotSprites.PORKCHOP
            );
        }
        MachineScreenLayout.drawSlot(graphics, x, y, BreederMenu.PARENT_A_SLOT_X, BreederMenu.PARENT_SLOT_Y, theme);
        MachineScreenLayout.drawSlot(graphics, x, y, BreederMenu.PARENT_B_SLOT_X, BreederMenu.PARENT_SLOT_Y, theme);

        Identifier parentPlaceholder = villager ? MachineSlotSprites.VILLAGER_HEAD : MachineSlotSprites.PIGLIN_HEAD;
        if (!menu.getSlot(BreederBlockEntity.PARENT_A_SLOT).hasItem()) {
            MachineScreenLayout.drawEmptySlotSprite(
                    graphics,
                    x,
                    y,
                    BreederMenu.PARENT_A_SLOT_X,
                    BreederMenu.PARENT_SLOT_Y,
                    parentPlaceholder
            );
        }
        if (!menu.getSlot(BreederBlockEntity.PARENT_B_SLOT).hasItem()) {
            MachineScreenLayout.drawEmptySlotSprite(
                    graphics,
                    x,
                    y,
                    BreederMenu.PARENT_B_SLOT_X,
                    BreederMenu.PARENT_SLOT_Y,
                    parentPlaceholder
            );
        }

        graphics.fakeItem(
                new ItemStack(villager ? Blocks.BED.yellow() : Blocks.BED.red()),
                x + MachineScreenLayout.machineX(80),
                y + BreederMenu.PARENT_SLOT_Y
        );

        MachineScreenLayout.drawProgressFrame(graphics, x + PROGRESS_FRAME_X, y + PROGRESS_FRAME_Y, theme);
        MachineScreenLayout.drawSlot(
                graphics,
                x,
                y,
                BreederMenu.BABY_PREVIEW_SLOT_X,
                BreederMenu.BABY_PREVIEW_SLOT_Y,
                theme
        );
        MachineScreenLayout.drawSlot(
                graphics,
                x,
                y,
                BreederMenu.EMPTY_CAPTURER_SLOT_X,
                BreederMenu.CAPTURER_SLOT_Y,
                theme
        );
        if (!menu.getSlot(BreederBlockEntity.EMPTY_CAPTURER_SLOT).hasItem()) {
            MachineScreenLayout.drawEmptySlotSprite(
                    graphics,
                    x,
                    y,
                    BreederMenu.EMPTY_CAPTURER_SLOT_X,
                    BreederMenu.CAPTURER_SLOT_Y,
                    MachineSlotSprites.CAPTURER
            );
        }
        MachineScreenLayout.drawSlot(
                graphics,
                x,
                y,
                BreederMenu.FILLED_CAPTURER_SLOT_X,
                BreederMenu.CAPTURER_SLOT_Y,
                theme
        );

        int progress = menu.maxBreedTicks() <= 0
                ? 0
                : Math.min(PROGRESS_WIDTH, menu.breedTicks() * PROGRESS_WIDTH / menu.maxBreedTicks());
        if (progress > 0) {
            graphics.fill(
                    x + PROGRESS_X,
                    y + PROGRESS_Y,
                    x + PROGRESS_X + progress,
                    y + PROGRESS_Y + PROGRESS_HEIGHT,
                    villager ? 0xFFE2B23F : 0xFFE04A4A
            );
        }
        if (menu.breedTicks() > 0) {
            MachineScreenUtil.drawCenteredCountdown(
                    graphics,
                    font,
                    x + PROGRESS_X + PROGRESS_WIDTH / 2,
                    y + PROGRESS_Y - 1,
                    menu.breedTicks(),
                    menu.maxBreedTicks()
            );
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        MachineScreenTheme theme = menu.kind() == BreederKind.VILLAGER
                ? MachineScreenTheme.VILLAGER_BREEDER
                : MachineScreenTheme.PIGLIN_BREEDER;
        MachineScreenLayout.drawLabels(graphics, font, title, playerInventoryTitle, theme);
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        boolean overlayHovered = variantListOpen && isWithinVariantList(mouseX, mouseY);
        int contentMouseX = overlayHovered ? OFFSCREEN_MOUSE_COORDINATE : mouseX;
        int contentMouseY = overlayHovered ? OFFSCREEN_MOUSE_COORDINATE : mouseY;
        super.extractContents(graphics, contentMouseX, contentMouseY, partialTick);
        if (!variantListOpen) {
            return;
        }
        graphics.nextStratum();
        int variantCount = menu.villagerVariantCount();
        int visible = Math.min(MAX_VISIBLE_VARIANTS, variantCount - variantScroll);
        for (int row = 0; row < visible; row++) {
            int variant = variantScroll + row;
            int rowX = leftPos + VARIANT_LIST_X;
            int rowY = topPos + VARIANT_LIST_Y + row * VARIANT_ROW_HEIGHT;
            boolean selected = variant == menu.selectedVillagerVariant();
            graphics.fill(
                    rowX,
                    rowY,
                    rowX + VARIANT_LIST_WIDTH,
                    rowY + VARIANT_ROW_HEIGHT - 1,
                    selected ? 0xF0A66D35 : 0xF04C321F
            );
            graphics.outline(
                    rowX,
                    rowY,
                    VARIANT_LIST_WIDTH,
                    VARIANT_ROW_HEIGHT - 1,
                    selected ? 0xFFFFD57A : 0xFFC49A68
            );
            graphics.centeredText(
                    font,
                    menu.villagerVariantName(variant),
                    rowX + VARIANT_LIST_WIDTH / 2,
                    rowY + 4,
                    0xFFFFFFFF
            );
        }
        drawVariantScrollbar(graphics, variantCount);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (variantListOpen && menu.villagerVariantCount() > MAX_VISIBLE_VARIANTS) {
            int maxScroll = menu.villagerVariantCount() - MAX_VISIBLE_VARIANTS;
            variantScroll = Mth.clamp((int) (variantScroll - scrollY), 0, maxScroll);
            return true;
        }
        return super.mouseScrolled(x, y, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (variantListOpen
                && !isWithinVariantList(event.x(), event.y())
                && (variantSelector == null || !variantSelector.isMouseOver(event.x(), event.y()))) {
            closeVariantList();
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void toggleVariantList() {
        if (variantListOpen) {
            closeVariantList();
            return;
        }
        variantListOpen = true;
        int variantCount = menu.villagerVariantCount();
        variantScroll = Mth.clamp(
                menu.selectedVillagerVariant() - MAX_VISIBLE_VARIANTS / 2,
                0,
                Math.max(0, variantCount - MAX_VISIBLE_VARIANTS)
        );
        int visible = Math.min(MAX_VISIBLE_VARIANTS, variantCount);
        for (int row = 0; row < visible; row++) {
            VariantButton button = new VariantButton(
                    leftPos + VARIANT_LIST_X,
                    topPos + VARIANT_LIST_Y + row * VARIANT_ROW_HEIGHT,
                    row
            );
            variantButtons.add(addRenderableWidget(button));
        }
    }

    private void closeVariantList() {
        for (VariantButton button : variantButtons) {
            removeWidget(button);
        }
        variantButtons.clear();
        variantListOpen = false;
    }

    private Component variantSelectorLabel() {
        return menu.selectedVillagerVariantName();
    }

    private boolean isWithinVariantList(double mouseX, double mouseY) {
        int visible = Math.min(MAX_VISIBLE_VARIANTS, menu.villagerVariantCount());
        int listX = leftPos + VARIANT_LIST_X;
        int listY = topPos + VARIANT_LIST_Y;
        return mouseX >= listX
                && mouseX < listX + VARIANT_LIST_WIDTH
                && mouseY >= listY
                && mouseY < listY + visible * VARIANT_ROW_HEIGHT;
    }

    private void drawVariantScrollbar(GuiGraphicsExtractor graphics, int variantCount) {
        if (variantCount <= MAX_VISIBLE_VARIANTS) {
            return;
        }
        int trackX = leftPos + VARIANT_LIST_X + VARIANT_LIST_WIDTH - 4;
        int trackY = topPos + VARIANT_LIST_Y + 2;
        int trackHeight = MAX_VISIBLE_VARIANTS * VARIANT_ROW_HEIGHT - 5;
        int thumbHeight = Math.max(9, trackHeight * MAX_VISIBLE_VARIANTS / variantCount);
        int maxScroll = variantCount - MAX_VISIBLE_VARIANTS;
        int thumbY = trackY + (trackHeight - thumbHeight) * variantScroll / maxScroll;
        graphics.fill(trackX, trackY, trackX + 2, trackY + trackHeight, 0xFF2A190E);
        graphics.fill(trackX, thumbY, trackX + 2, thumbY + thumbHeight, 0xFFFFD57A);
    }

    private final class VariantButton extends Button.Plain { // NOSONAR - Minecraft widget inheritance is framework-defined.
        private VariantButton(int x, int y, int row) {
            super(
                    x,
                    y,
                    VARIANT_LIST_WIDTH,
                    VARIANT_ROW_HEIGHT - 1,
                    CommonComponents.EMPTY,
                    ignored -> selectVariant(BreederScreen.this, row),
                    DEFAULT_NARRATION
            );
        }

        private static void selectVariant(BreederScreen screen, int row) {
            if (screen.minecraft != null && screen.minecraft.gameMode != null) {
                screen.minecraft.gameMode.handleInventoryButtonClick(
                        screen.menu.containerId,
                        BreederMenu.SELECT_VARIANT_BUTTON_BASE + screen.variantScroll + row
                );
            }
            screen.closeVariantList();
        }
    }

    private final class VariantSelectorButton extends Button.Plain { // NOSONAR - Minecraft widget inheritance is framework-defined.
        private static final int HEIGHT = 18;

        private VariantSelectorButton(int x, int y) {
            super(
                    x,
                    y,
                    VARIANT_LIST_WIDTH,
                    HEIGHT,
                    variantSelectorLabel(),
                    ignored -> toggleVariantList(),
                    DEFAULT_NARRATION
            );
        }

        @Override
        protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            MachineScreenTheme theme = MachineScreenTheme.VILLAGER_BREEDER;
            int x = getX();
            int y = getY();
            int width = getWidth();
            int height = getHeight();
            graphics.fill(x, y, x + width, y + height, theme.frameDark());
            graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, theme.frame());
            graphics.fill(
                    x + 2,
                    y + 2,
                    x + width - 2,
                    y + height - 2,
                    isMouseOver(mouseX, mouseY) ? theme.slotHighlight() : theme.slotInner()
            );

            Component message = getMessage();
            float scale = Math.min(1.0F, (float) (width - 6) / Math.max(1, font.width(message)));
            graphics.pose().pushMatrix();
            graphics.pose().translate(x + width / 2.0F, y + 5.0F);
            graphics.pose().scale(scale, scale);
            graphics.text(font, message, -font.width(message) / 2, 0, 0xFFFFFFFF, true);
            graphics.pose().popMatrix();
        }
    }
}
