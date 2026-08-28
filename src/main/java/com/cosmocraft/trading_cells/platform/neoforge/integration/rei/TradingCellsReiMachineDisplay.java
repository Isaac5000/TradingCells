package com.cosmocraft.trading_cells.platform.neoforge.integration.rei;

import com.cosmocraft.trading_cells.feature.breeders.adapters.input.BreederMenu;
import com.cosmocraft.trading_cells.feature.farmer.adapters.input.FarmerBlockEntity;
import com.cosmocraft.trading_cells.feature.ironfarm.adapters.input.IronFarmBlockEntity;
import com.cosmocraft.trading_cells.feature.ironfarm.adapters.input.IronFarmMenu;
import com.cosmocraft.trading_cells.feature.infusion.adapters.input.ArcaneInfuserBlockEntity;
import com.cosmocraft.trading_cells.feature.infusion.adapters.input.ArcaneInfuserMenu;
import com.cosmocraft.trading_cells.feature.infusion.adapters.output.client.ArcaneInfuserScreen;
import com.cosmocraft.trading_cells.feature.quarry.adapters.input.QuarryBlockEntity;
import com.cosmocraft.trading_cells.feature.trader.adapters.input.NetheritePiglinBarteringCellBlockEntity;
import com.cosmocraft.trading_cells.feature.trader.adapters.input.NetheritePiglinBarteringCellMenu;
import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.FeatureComposition;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.MachineScreenLayout;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.MachineScreenTheme;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.MachineScreenUtil;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.SlotRenderer;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.trader.VillagerTradeScreenCommon;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.trader.VillagerTradeSprites;
import java.util.ArrayList;
import java.util.List;
import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.widgets.Slot;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

final class TradingCellsReiMachineDisplay {
    private static final int PANEL_FRAME_SIZE = 2;
    private static final int ENTRY_CONTENT_OFFSET = -1;

    static final int WIDTH = MachineScreenLayout.MACHINE_PANEL_WIDTH + PANEL_FRAME_SIZE * 2;
    static final int HEIGHT = MachineScreenLayout.MACHINE_PANEL_HEIGHT + PANEL_FRAME_SIZE * 2;

    private static final int CONVERTER_ENTITY_INPUT_X = 20;
    private static final int CONVERTER_ENTITY_OUTPUT_X = 166;
    private static final int CONVERTER_ENTITY_Y = 49;
    private static final int BARTER_PIGLIN_X = 26;
    private static final int BARTER_GOLD_X = 56;
    private static final int BARTER_OUTPUT_X = 168;
    private static final int BARTER_SLOT_Y = 51;
    private static final int NETHERITE_PIGLIN_X = 166;
    private static final int NETHERITE_PIGLIN_Y = 57;
    private static final int FARMER_WORKER_X = 64;
    private static final int FARMER_HOE_X = 94;
    private static final int FARMER_CROP_X = 124;
    private static final int FARMER_INPUT_Y = 30;
    private static final int FARMER_OUTPUT_FIRST_X = 22;
    private static final int FARMER_OUTPUT_FIRST_Y = 82;
    private static final int QUARRY_WORKER_X = 58;
    private static final int QUARRY_PICKAXE_X = 94;
    private static final int QUARRY_UPGRADE_X = 130;
    private static final int QUARRY_INPUT_Y = 24;
    private static final int QUARRY_OUTPUT_FIRST_X = 22;
    private static final int QUARRY_OUTPUT_FIRST_Y = 81;
    private static final int COMPACT_OUTPUT_COLUMN_COUNT = 9;
    private static final int COMPACT_OUTPUT_SLOT_SPACING = 18;
    private static final int SKELETON_INPUT_X = 38;
    private static final int SKELETON_WORKER_Y = 32;
    private static final int SKELETON_SWORD_Y = 53;
    private static final int SKELETON_TARGET_Y = 74;
    private static final int SKELETON_PROGRESS_X = 68;
    private static final int SKELETON_PROGRESS_Y = 53;
    private static final int SKELETON_OUTPUT_X = 148;
    private static final int SKELETON_OUTPUT_Y = SKELETON_PROGRESS_Y;
    private static final int SMITHING_BASE_X = 39;
    private static final int SMITHING_ADDITION_X = 72;
    private static final int SMITHING_OUTPUT_X = 146;
    private static final int SMITHING_SLOT_Y = 55;
    private static final int SMITHING_ARROW_X = 105;
    private static final int REDSTONE_SPAWNER_X = 31;
    private static final int REDSTONE_COMPARATOR_X = 78;
    private static final int REDSTONE_OUTPUT_X = 145;
    private static final int REDSTONE_SLOT_Y = 39;
    private static final int REDSTONE_ARROW_X = 112;

    private TradingCellsReiMachineDisplay() {
    }

    static List<Widget> setup(TradingCellsReiDisplay display, Rectangle suppliedBounds) {
        Rectangle bounds = new Rectangle(
                suppliedBounds.getCenterX() - WIDTH / 2,
                suppliedBounds.y,
                WIDTH,
                HEIGHT
        );
        List<Widget> widgets = new ArrayList<>();
        widgets.add(createBackground(display, bounds));
        switch (display.layout().kind()) {
            case BREEDING -> addBreeding(widgets, display, bounds);
            case INCUBATION -> addIncubation(widgets, display, bounds);
            case FARMING -> addFarming(widgets, display, bounds);
            case CONVERSION -> addConversion(widgets, display, bounds);
            case IRON_FARM -> addIronFarm(widgets, display, bounds);
            case SKELETON_FARM, ZOMBIE_FARM -> addSkeletonFarm(widgets, display, bounds);
            case DECAPITATION_SMITHING -> addDecapitationSmithing(widgets, display, bounds);
            case PIGLIN_BARTERING -> addPiglinBartering(widgets, display, bounds);
            case NETHERITE_PIGLIN_BARTERING -> addNetheritePiglinBartering(widgets, display, bounds);
            case QUARRY -> addQuarry(widgets, display, bounds);
            case ARCANE_INFUSION -> addArcaneInfusion(widgets, display, bounds);
            case SPAWNER_REDSTONE_CONTROL -> addSpawnerRedstoneControl(widgets, display, bounds);
        }
        if (display.layout().kind() == TradingCellsReiLayout.Kind.ARCANE_INFUSION
                || display.layout().kind() == TradingCellsReiLayout.Kind.DECAPITATION_SMITHING
                || display.layout().kind() == TradingCellsReiLayout.Kind.SPAWNER_REDSTONE_CONTROL) {
            return widgets;
        } else if (isInstantBartering(display)) {
            addOutputAmountTooltip(widgets, display, bounds);
        } else {
            addProgressTooltip(widgets, display, bounds);
        }
        return widgets;
    }

    private static Widget createBackground(TradingCellsReiDisplay display, Rectangle bounds) {
        return Widgets.createDrawableWidget((graphics, mouseX, mouseY, delta) -> {
            TradingCellsReiLayout layout = display.layout();
            MachineScreenTheme theme = layout.theme();
            graphics.fill(
                    bounds.x,
                    bounds.y,
                    bounds.x + bounds.width,
                    bounds.y + bounds.height,
                    theme.frameDark()
            );
            graphics.fill(
                    bounds.x + 1,
                    bounds.y + 1,
                    bounds.x + bounds.width - 1,
                    bounds.y + bounds.height - 1,
                    theme.frameLight()
            );
            MachineScreenLayout.drawMachinePanelSurface(
                    graphics,
                    bounds.x + PANEL_FRAME_SIZE,
                    bounds.y + PANEL_FRAME_SIZE,
                    layout.surface()
            );
            drawSlotFrames(graphics, display, bounds);
            if (layout.kind() == TradingCellsReiLayout.Kind.ARCANE_INFUSION) {
                drawArcaneInfusionInfo(graphics, display, bounds);
            } else if (layout.kind() == TradingCellsReiLayout.Kind.DECAPITATION_SMITHING) {
                drawDecapitationSmithingInfo(graphics, bounds);
            } else if (layout.kind() == TradingCellsReiLayout.Kind.SPAWNER_REDSTONE_CONTROL) {
                drawSpawnerRedstoneControlInfo(graphics, display, bounds);
            } else if (isInstantBartering(display)) {
                drawOutputAmount(graphics, display, bounds);
            } else {
                drawProgress(graphics, display, bounds);
            }
            if (layout.kind() == TradingCellsReiLayout.Kind.IRON_FARM) {
                drawIronFarmInfo(graphics, display, bounds);
            }
        });
    }

    private static void drawSlotFrames(
            me.shedaniel.rei.api.client.gui.compat.GuiGraphics graphics,
            TradingCellsReiDisplay display,
            Rectangle bounds
    ) {
        switch (display.layout().kind()) {
            case BREEDING -> {
                drawSlot(graphics, bounds, BreederMenu.FOOD_SLOT_X, BreederMenu.FOOD_SLOT_Y, display);
                drawSlot(graphics, bounds, BreederMenu.PARENT_A_SLOT_X, BreederMenu.PARENT_SLOT_Y, display);
                drawSlot(graphics, bounds, BreederMenu.PARENT_B_SLOT_X, BreederMenu.PARENT_SLOT_Y, display);
                drawSlot(graphics, bounds, BreederMenu.BABY_PREVIEW_SLOT_X, BreederMenu.BABY_PREVIEW_SLOT_Y, display);
                drawSlot(graphics, bounds, BreederMenu.EMPTY_CAPTURER_SLOT_X, BreederMenu.CAPTURER_SLOT_Y, display);
                drawSlot(graphics, bounds, BreederMenu.FILLED_CAPTURER_SLOT_X, BreederMenu.CAPTURER_SLOT_Y, display);
            }
            case INCUBATION -> {
                drawSlot(graphics, bounds, MachineScreenLayout.machineX(45), 48, display);
                drawSlot(graphics, bounds, MachineScreenLayout.machineX(115), 48, display);
            }
            case FARMING -> {
                drawSlot(graphics, bounds, FARMER_WORKER_X, FARMER_INPUT_Y, display);
                drawSlot(graphics, bounds, FARMER_HOE_X, FARMER_INPUT_Y, display);
                drawSlot(graphics, bounds, FARMER_CROP_X, FARMER_INPUT_Y, display);
                for (int index = 0; index < FarmerBlockEntity.OUTPUT_SLOT_COUNT; index++) {
                    drawSlot(
                            graphics,
                            bounds,
                            farmerOutputSlotX(index),
                            farmerOutputSlotY(index),
                            display
                    );
                }
            }
            case CONVERSION -> {
                drawSlot(graphics, bounds, CONVERTER_ENTITY_INPUT_X, CONVERTER_ENTITY_Y, display);
                drawSlot(graphics, bounds, CONVERTER_ENTITY_OUTPUT_X, CONVERTER_ENTITY_Y, display);
                for (int index = 0; index < 4; index++) {
                    int slotX = MachineScreenLayout.machineX(43) + index * 24;
                    drawSlot(graphics, bounds, slotX, 35, display);
                    drawSlot(graphics, bounds, slotX, 63, display);
                }
            }
            case IRON_FARM -> {
                for (int index = 0; index < IronFarmBlockEntity.VILLAGER_SLOT_COUNT; index++) {
                    drawSlot(
                            graphics,
                            bounds,
                            IronFarmMenu.VILLAGER_ROW_X + index * 24,
                            IronFarmMenu.VILLAGER_ROW_Y,
                            display
                    );
                }
                for (int index = 0; index < IronFarmBlockEntity.OUTPUT_SLOT_COUNT; index++) {
                    drawSlot(
                            graphics,
                            bounds,
                            IronFarmMenu.outputSlotX(index),
                            IronFarmMenu.OUTPUT_ROW_Y,
                            display
                    );
                }
            }
            case SKELETON_FARM, ZOMBIE_FARM -> {
                drawSlot(graphics, bounds, SKELETON_INPUT_X, SKELETON_WORKER_Y, display);
                drawSlot(graphics, bounds, SKELETON_INPUT_X, SKELETON_SWORD_Y, display);
                drawSlot(graphics, bounds, SKELETON_INPUT_X, SKELETON_TARGET_Y, display);
                if (!display.getOutputEntries().isEmpty()) {
                    drawSlot(graphics, bounds, SKELETON_OUTPUT_X, SKELETON_OUTPUT_Y, display);
                }
            }
            case DECAPITATION_SMITHING -> {
                drawSlot(graphics, bounds, SMITHING_BASE_X, SMITHING_SLOT_Y, display);
                drawSlot(graphics, bounds, SMITHING_ADDITION_X, SMITHING_SLOT_Y, display);
                drawSlot(graphics, bounds, SMITHING_OUTPUT_X, SMITHING_SLOT_Y, display);
            }
            case PIGLIN_BARTERING -> {
                drawSlot(graphics, bounds, BARTER_PIGLIN_X, BARTER_SLOT_Y, display);
                drawSlot(graphics, bounds, BARTER_GOLD_X, BARTER_SLOT_Y, display);
                drawSlot(graphics, bounds, BARTER_OUTPUT_X, BARTER_SLOT_Y, display);
            }
            case NETHERITE_PIGLIN_BARTERING -> {
                drawSlot(
                        graphics,
                        bounds,
                        NetheritePiglinBarteringCellMenu.CONTROL_SLOT_X,
                        NetheritePiglinBarteringCellMenu.UPGRADE_SLOT_Y,
                        display
                );
                drawSlot(
                        graphics,
                        bounds,
                        NetheritePiglinBarteringCellMenu.CONTROL_SLOT_X,
                        NetheritePiglinBarteringCellMenu.FILTER_SLOT_Y,
                        display
                );
                for (int lane = 0;
                     lane < NetheritePiglinBarteringCellBlockEntity.GOLD_SLOT_COUNT;
                     lane++) {
                    drawSlot(
                            graphics,
                            bounds,
                            NetheritePiglinBarteringCellMenu.GOLD_ROW_X
                                    + lane * NetheritePiglinBarteringCellMenu.LANE_SPACING,
                            NetheritePiglinBarteringCellMenu.GOLD_ROW_Y,
                            display
                    );
                }
                for (int index = 0;
                     index < NetheritePiglinBarteringCellBlockEntity.OUTPUT_SLOT_COUNT;
                     index++) {
                    drawSlot(
                            graphics,
                            bounds,
                            NetheritePiglinBarteringCellMenu.outputSlotX(index),
                            NetheritePiglinBarteringCellMenu.outputSlotY(index),
                            display
                    );
                }
                drawSlot(graphics, bounds, NETHERITE_PIGLIN_X, NETHERITE_PIGLIN_Y, display);
            }
            case QUARRY -> {
                drawSlot(graphics, bounds, QUARRY_WORKER_X, QUARRY_INPUT_Y, display);
                drawSlot(graphics, bounds, QUARRY_PICKAXE_X, QUARRY_INPUT_Y, display);
                drawSlot(graphics, bounds, QUARRY_UPGRADE_X, QUARRY_INPUT_Y, display);
                for (int index = 0; index < QuarryBlockEntity.OUTPUT_SLOT_COUNT; index++) {
                    drawSlot(
                            graphics,
                            bounds,
                            quarryOutputSlotX(index),
                            quarryOutputSlotY(index),
                            display
                    );
                }
            }
            case ARCANE_INFUSION -> {
                for (int slot = 0; slot < ArcaneInfuserBlockEntity.INPUT_SLOT_COUNT; slot++) {
                    drawSlot(
                            graphics,
                            bounds,
                            ArcaneInfuserMenu.inputSlotX(slot),
                            ArcaneInfuserMenu.inputSlotY(slot),
                            display
                    );
                }
                drawSlot(graphics, bounds, ArcaneInfuserMenu.OUTPUT_SLOT_X, ArcaneInfuserMenu.OUTPUT_SLOT_Y, display);
            }
            case SPAWNER_REDSTONE_CONTROL -> {
                drawSlot(graphics, bounds, REDSTONE_SPAWNER_X, REDSTONE_SLOT_Y, display);
                drawSlot(graphics, bounds, REDSTONE_COMPARATOR_X, REDSTONE_SLOT_Y, display);
                drawSlot(graphics, bounds, REDSTONE_OUTPUT_X, REDSTONE_SLOT_Y, display);
            }
        }
    }

    private static void drawSlot(
            me.shedaniel.rei.api.client.gui.compat.GuiGraphics graphics,
            Rectangle bounds,
            int normalX,
            int normalY,
            TradingCellsReiDisplay display
    ) {
        MachineScreenLayout.drawSlot(
                graphics,
                screenX(bounds),
                screenY(bounds),
                normalX,
                normalY,
                display.layout().theme()
        );
    }

    private static void drawProgress(
            me.shedaniel.rei.api.client.gui.compat.GuiGraphics graphics,
            TradingCellsReiDisplay display,
            Rectangle bounds
    ) {
        Point point = progressPoint(display);
        int x = screenX(bounds) + point.x;
        int y = screenY(bounds) + point.y;
        MachineScreenTheme theme = display.layout().theme();
        MachineScreenLayout.drawProgressFrame(graphics, x, y, theme);

        int duration = Math.max(1, display.durationTicks());
        int elapsed = (int) ((System.currentTimeMillis() / 50L) % duration);
        int fill = Math.max(
                1,
                elapsed * MachineScreenLayout.PROGRESS_FILL_WIDTH / duration
        );
        graphics.fill(
                x + MachineScreenLayout.PROGRESS_FILL_X_OFFSET,
                y + MachineScreenLayout.PROGRESS_FILL_Y_OFFSET,
                x + MachineScreenLayout.PROGRESS_FILL_X_OFFSET + fill,
                y + MachineScreenLayout.PROGRESS_FILL_Y_OFFSET
                        + MachineScreenLayout.PROGRESS_FILL_HEIGHT,
                display.layout().progressColor()
        );

        String durationText = MachineScreenUtil.formatDuration(display.durationTicks());
        graphics.centeredText(
                Minecraft.getInstance().font,
                Component.literal(durationText),
                x + MachineScreenLayout.PROGRESS_FRAME_WIDTH / 2,
                y + 3,
                0xFFFFFFFF
        );
    }

    private static void drawOutputAmount(
            me.shedaniel.rei.api.client.gui.compat.GuiGraphics graphics,
            TradingCellsReiDisplay display,
            Rectangle bounds
    ) {
        display.outputAmount().ifPresent(amount -> {
            Point point = progressPoint(display);
            int centerX = outputAmountCenterX(display, bounds, point);
            int y = screenY(bounds) + point.y + 2;
            graphics.centeredText(
                    Minecraft.getInstance().font,
                    Component.literal(amount.label()),
                    centerX,
                    y,
                    display.layout().theme().titleText()
            );
        });
    }

    private static void drawIronFarmInfo(
            me.shedaniel.rei.api.client.gui.compat.GuiGraphics graphics,
            TradingCellsReiDisplay display,
            Rectangle bounds
    ) {
        int villagers = display.getInputEntries().size();
        int multiplier = FeatureComposition.ironFarm().cycle().multiplier(villagers);
        var font = Minecraft.getInstance().font;
        int x = screenX(bounds) + MachineScreenLayout.machineX(88);
        int y = screenY(bounds) + 98;
        graphics.text(
                font,
                Component.translatable("label.trading_cells.iron_villagers", villagers, 3),
                x,
                y,
                display.layout().theme().titleText(),
                true
        );
        graphics.text(
                font,
                Component.translatable("label.trading_cells.iron_current_efficiency", multiplier),
                x,
                y + 10,
                display.layout().theme().titleText(),
                true
        );
    }

    private static void drawArcaneInfusionInfo(
            me.shedaniel.rei.api.client.gui.compat.GuiGraphics graphics,
            TradingCellsReiDisplay display,
            Rectangle bounds
    ) {
        VillagerTradeScreenCommon.drawTradeArrow(
                graphics,
                screenX(bounds) + ArcaneInfuserScreen.RECIPE_VIEWER_X,
                screenY(bounds) + ArcaneInfuserScreen.RECIPE_VIEWER_Y,
                VillagerTradeSprites.State.NORMAL
        );
        if (!display.notes().isEmpty()) {
            graphics.centeredText(
                    Minecraft.getInstance().font,
                    display.notes().getFirst(),
                    bounds.getCenterX(),
                    screenY(bounds) + ArcaneInfuserScreen.RECIPE_EXPERIENCE_Y + 1,
                    display.layout().theme().titleText()
            );
        }
    }

    private static void drawDecapitationSmithingInfo(
            me.shedaniel.rei.api.client.gui.compat.GuiGraphics graphics,
            Rectangle bounds
    ) {
        graphics.centeredText(
                Minecraft.getInstance().font,
                Component.literal("+"),
                screenX(bounds) + 65,
                screenY(bounds) + SMITHING_SLOT_Y + 5,
                0xFFFFFFFF
        );
        VillagerTradeScreenCommon.drawTradeArrow(
                graphics,
                screenX(bounds) + SMITHING_ARROW_X,
                screenY(bounds) + SMITHING_SLOT_Y + 1,
                VillagerTradeSprites.State.NORMAL
        );
    }

    private static void drawSpawnerRedstoneControlInfo(
            me.shedaniel.rei.api.client.gui.compat.GuiGraphics graphics,
            TradingCellsReiDisplay display,
            Rectangle bounds
    ) {
        graphics.centeredText(
                Minecraft.getInstance().font,
                Component.literal("+"),
                screenX(bounds) + 64,
                screenY(bounds) + REDSTONE_SLOT_Y + 5,
                0xFFFFFFFF
        );
        VillagerTradeScreenCommon.drawTradeArrow(
                graphics,
                screenX(bounds) + REDSTONE_ARROW_X,
                screenY(bounds) + REDSTONE_SLOT_Y + 1,
                VillagerTradeSprites.State.NORMAL
        );
        int y = screenY(bounds) + 72;
        for (Component note : display.notes()) {
            graphics.centeredText(
                    Minecraft.getInstance().font,
                    note,
                    bounds.getCenterX(),
                    y,
                    display.layout().theme().titleText()
            );
            y += 9;
        }
    }

    private static void addBreeding(
            List<Widget> widgets,
            TradingCellsReiDisplay display,
            Rectangle bounds
    ) {
        addInput(widgets, bounds, BreederMenu.PARENT_A_SLOT_X, BreederMenu.PARENT_SLOT_Y, input(display, 0));
        addInput(widgets, bounds, BreederMenu.PARENT_B_SLOT_X, BreederMenu.PARENT_SLOT_Y, input(display, 1));
        addInput(widgets, bounds, BreederMenu.FOOD_SLOT_X, BreederMenu.FOOD_SLOT_Y, input(display, 2));
        addInput(
                widgets,
                bounds,
                BreederMenu.EMPTY_CAPTURER_SLOT_X,
                BreederMenu.CAPTURER_SLOT_Y,
                input(display, 3)
        );
        addOutput(
                widgets,
                bounds,
                BreederMenu.FILLED_CAPTURER_SLOT_X,
                BreederMenu.CAPTURER_SLOT_Y,
                output(display, 0)
        );
        addPreview(
                widgets,
                bounds,
                BreederMenu.BABY_PREVIEW_SLOT_X,
                BreederMenu.BABY_PREVIEW_SLOT_Y,
                output(display, 0)
        );

        boolean villager = display.layout() == TradingCellsReiLayout.VILLAGER_BREEDING;
        addDecoration(
                widgets,
                bounds,
                MachineScreenLayout.machineX(80),
                BreederMenu.PARENT_SLOT_Y,
                EntryIngredient.of(EntryStacks.of(new ItemStack(
                        villager ? Blocks.BED.yellow() : Blocks.BED.red()
                )))
        );
    }

    private static void addIncubation(
            List<Widget> widgets,
            TradingCellsReiDisplay display,
            Rectangle bounds
    ) {
        addInput(widgets, bounds, MachineScreenLayout.machineX(45), 48, input(display, 0));
        addOutput(widgets, bounds, MachineScreenLayout.machineX(115), 48, output(display, 0));
    }

    private static void addFarming(
            List<Widget> widgets,
            TradingCellsReiDisplay display,
            Rectangle bounds
    ) {
        addInput(widgets, bounds, FARMER_WORKER_X, FARMER_INPUT_Y, input(display, 0));
        addInput(widgets, bounds, FARMER_HOE_X, FARMER_INPUT_Y, input(display, 2));
        addInput(widgets, bounds, FARMER_CROP_X, FARMER_INPUT_Y, input(display, 1));
        for (int index = 0; index < display.getOutputEntries().size(); index++) {
            addOutput(
                    widgets,
                    bounds,
                    farmerOutputSlotX(index),
                    farmerOutputSlotY(index),
                    output(display, index)
            );
        }
    }

    private static void addConversion(
            List<Widget> widgets,
            TradingCellsReiDisplay display,
            Rectangle bounds
    ) {
        addInput(widgets, bounds, CONVERTER_ENTITY_INPUT_X, CONVERTER_ENTITY_Y, input(display, 0));
        addInput(widgets, bounds, MachineScreenLayout.machineX(43), 35, input(display, 1));
        addInput(widgets, bounds, MachineScreenLayout.machineX(43), 63, input(display, 2));
        addOutput(widgets, bounds, CONVERTER_ENTITY_OUTPUT_X, CONVERTER_ENTITY_Y, output(display, 0));
    }

    private static void addIronFarm(
            List<Widget> widgets,
            TradingCellsReiDisplay display,
            Rectangle bounds
    ) {
        for (int index = 0; index < display.getInputEntries().size(); index++) {
            addInput(
                    widgets,
                    bounds,
                    IronFarmMenu.VILLAGER_ROW_X + index * 24,
                    IronFarmMenu.VILLAGER_ROW_Y,
                    input(display, index)
            );
        }
        for (int index = 0; index < display.getOutputEntries().size(); index++) {
            addOutput(
                    widgets,
                    bounds,
                    IronFarmMenu.outputSlotX(index),
                    IronFarmMenu.OUTPUT_ROW_Y,
                    output(display, index)
            );
        }
    }

    private static void addSkeletonFarm(
            List<Widget> widgets,
            TradingCellsReiDisplay display,
            Rectangle bounds
    ) {
        addInput(widgets, bounds, SKELETON_INPUT_X, SKELETON_WORKER_Y, input(display, 0));
        addInput(widgets, bounds, SKELETON_INPUT_X, SKELETON_SWORD_Y, input(display, 1));
        addPreview(widgets, bounds, SKELETON_INPUT_X, SKELETON_TARGET_Y, input(display, 2));
        if (!display.getOutputEntries().isEmpty()) {
            addOutput(widgets, bounds, SKELETON_OUTPUT_X, SKELETON_OUTPUT_Y, output(display, 0));
        }
    }

    private static void addDecapitationSmithing(
            List<Widget> widgets,
            TradingCellsReiDisplay display,
            Rectangle bounds
    ) {
        addInput(widgets, bounds, SMITHING_BASE_X, SMITHING_SLOT_Y, input(display, 0));
        addInput(widgets, bounds, SMITHING_ADDITION_X, SMITHING_SLOT_Y, input(display, 1));
        addOutput(widgets, bounds, SMITHING_OUTPUT_X, SMITHING_SLOT_Y, output(display, 0));
    }

    private static void addSpawnerRedstoneControl(
            List<Widget> widgets,
            TradingCellsReiDisplay display,
            Rectangle bounds
    ) {
        addInput(widgets, bounds, REDSTONE_SPAWNER_X, REDSTONE_SLOT_Y, input(display, 0));
        addInput(widgets, bounds, REDSTONE_COMPARATOR_X, REDSTONE_SLOT_Y, input(display, 1));
        addOutput(widgets, bounds, REDSTONE_OUTPUT_X, REDSTONE_SLOT_Y, output(display, 0));
    }

    private static void addPiglinBartering(
            List<Widget> widgets,
            TradingCellsReiDisplay display,
            Rectangle bounds
    ) {
        addInput(widgets, bounds, BARTER_PIGLIN_X, BARTER_SLOT_Y, input(display, 0));
        addInput(widgets, bounds, BARTER_GOLD_X, BARTER_SLOT_Y, input(display, 1));
        addOutput(widgets, bounds, BARTER_OUTPUT_X, BARTER_SLOT_Y, output(display, 0));
    }

    private static void addNetheritePiglinBartering(
            List<Widget> widgets,
            TradingCellsReiDisplay display,
            Rectangle bounds
    ) {
        addInput(widgets, bounds, NETHERITE_PIGLIN_X, NETHERITE_PIGLIN_Y, input(display, 0));
        addInput(
                widgets,
                bounds,
                NetheritePiglinBarteringCellMenu.GOLD_ROW_X,
                NetheritePiglinBarteringCellMenu.GOLD_ROW_Y,
                input(display, 1)
        );
        addInput(
                widgets,
                bounds,
                NetheritePiglinBarteringCellMenu.CONTROL_SLOT_X,
                NetheritePiglinBarteringCellMenu.UPGRADE_SLOT_Y,
                input(display, 2)
        );
        addInput(
                widgets,
                bounds,
                NetheritePiglinBarteringCellMenu.CONTROL_SLOT_X,
                NetheritePiglinBarteringCellMenu.FILTER_SLOT_Y,
                input(display, 3)
        );
        addOutput(
                widgets,
                bounds,
                NetheritePiglinBarteringCellMenu.outputSlotX(0),
                NetheritePiglinBarteringCellMenu.outputSlotY(0),
                output(display, 0)
        );
    }

    private static void addQuarry(
            List<Widget> widgets,
            TradingCellsReiDisplay display,
            Rectangle bounds
    ) {
        addInput(widgets, bounds, QUARRY_WORKER_X, QUARRY_INPUT_Y, input(display, 0));
        addInput(widgets, bounds, QUARRY_PICKAXE_X, QUARRY_INPUT_Y, input(display, 1));
        if (display.getInputEntries().size() > 2 && !input(display, 2).isEmpty()) {
            addInput(widgets, bounds, QUARRY_UPGRADE_X, QUARRY_INPUT_Y, input(display, 2));
        }
        if (!display.getOutputEntries().isEmpty()) {
            addOutput(
                    widgets,
                    bounds,
                    quarryOutputSlotX(0),
                    quarryOutputSlotY(0),
                    output(display, 0)
            );
        }
    }

    private static void addArcaneInfusion(
            List<Widget> widgets,
            TradingCellsReiDisplay display,
            Rectangle bounds
    ) {
        for (int slot = 0; slot < ArcaneInfuserBlockEntity.INPUT_SLOT_COUNT; slot++) {
            addInput(
                    widgets,
                    bounds,
                    ArcaneInfuserMenu.inputSlotX(slot),
                    ArcaneInfuserMenu.inputSlotY(slot),
                    input(display, slot)
            );
        }
        addOutput(widgets, bounds, ArcaneInfuserMenu.OUTPUT_SLOT_X, ArcaneInfuserMenu.OUTPUT_SLOT_Y, output(display, 0));
    }

    private static void addProgressTooltip(
            List<Widget> widgets,
            TradingCellsReiDisplay display,
            Rectangle bounds
    ) {
        Point point = progressPoint(display);
        Rectangle progressBounds = new Rectangle(
                screenX(bounds) + point.x,
                screenY(bounds) + point.y,
                MachineScreenLayout.PROGRESS_FRAME_WIDTH,
                MachineScreenLayout.PROGRESS_FRAME_HEIGHT
        );
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(Component.translatable(
                "rei.trading_cells.duration",
                MachineScreenUtil.formatDuration(display.durationTicks())
        ));
        tooltip.addAll(display.notes());
        widgets.add(Widgets.createTooltip(progressBounds, tooltip));
    }

    private static void addOutputAmountTooltip(
            List<Widget> widgets,
            TradingCellsReiDisplay display,
            Rectangle bounds
    ) {
        display.outputAmount().ifPresent(amount -> {
            Point point = progressPoint(display);
            int centerX = outputAmountCenterX(display, bounds, point);
            Rectangle amountBounds = new Rectangle(
                    centerX - MachineScreenLayout.PROGRESS_FRAME_WIDTH / 2,
                    screenY(bounds) + point.y,
                    MachineScreenLayout.PROGRESS_FRAME_WIDTH,
                    MachineScreenLayout.PROGRESS_FRAME_HEIGHT
            );
            Component amountTooltip = amount.minimum() == amount.maximum()
                    ? Component.translatable("rei.trading_cells.amount_exact", amount.maximum())
                    : Component.translatable(
                            "rei.trading_cells.amount_range",
                            amount.minimum(),
                            amount.maximum()
                    );
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(amountTooltip);
            tooltip.addAll(display.notes());
            widgets.add(Widgets.createTooltip(amountBounds, tooltip));
        });
    }

    private static int outputAmountCenterX(
            TradingCellsReiDisplay display,
            Rectangle bounds,
            Point progressPoint
    ) {
        if (display.layout().kind() == TradingCellsReiLayout.Kind.NETHERITE_PIGLIN_BARTERING) {
            return bounds.getCenterX();
        }
        return screenX(bounds) + progressPoint.x + MachineScreenLayout.PROGRESS_FRAME_WIDTH / 2;
    }

    private static boolean isInstantBartering(TradingCellsReiDisplay display) {
        return switch (display.layout().kind()) {
            case PIGLIN_BARTERING, NETHERITE_PIGLIN_BARTERING -> true;
            default -> false;
        };
    }

    private static int farmerOutputSlotX(int index) {
        return FARMER_OUTPUT_FIRST_X
                + index % COMPACT_OUTPUT_COLUMN_COUNT * COMPACT_OUTPUT_SLOT_SPACING;
    }

    private static int farmerOutputSlotY(int index) {
        return FARMER_OUTPUT_FIRST_Y
                + index / COMPACT_OUTPUT_COLUMN_COUNT * COMPACT_OUTPUT_SLOT_SPACING;
    }

    private static int quarryOutputSlotX(int index) {
        return QUARRY_OUTPUT_FIRST_X
                + index % COMPACT_OUTPUT_COLUMN_COUNT * COMPACT_OUTPUT_SLOT_SPACING;
    }

    private static int quarryOutputSlotY(int index) {
        return QUARRY_OUTPUT_FIRST_Y
                + index / COMPACT_OUTPUT_COLUMN_COUNT * COMPACT_OUTPUT_SLOT_SPACING;
    }

    private static Point progressPoint(TradingCellsReiDisplay display) {
        return switch (display.layout().kind()) {
            case BREEDING -> new Point(MachineScreenLayout.machineX(54), 65);
            case INCUBATION -> new Point(MachineScreenLayout.machineX(54), 77);
            case FARMING -> new Point(MachineScreenLayout.machineX(54), 66);
            case CONVERSION -> new Point(MachineScreenLayout.machineX(54), 95);
            case IRON_FARM -> new Point(MachineScreenLayout.machineX(54), 47);
            case SKELETON_FARM, ZOMBIE_FARM -> new Point(SKELETON_PROGRESS_X, SKELETON_PROGRESS_Y);
            case DECAPITATION_SMITHING -> new Point(SMITHING_ARROW_X, SMITHING_SLOT_Y);
            case PIGLIN_BARTERING -> new Point(88, 53);
            case NETHERITE_PIGLIN_BARTERING -> new Point(88, 58);
            case QUARRY -> new Point(
                    (MachineScreenLayout.WIDTH - MachineScreenLayout.PROGRESS_FRAME_WIDTH) / 2,
                    64
            );
            case ARCANE_INFUSION -> new Point(MachineScreenLayout.machineX(54), 77);
            case SPAWNER_REDSTONE_CONTROL -> new Point(REDSTONE_ARROW_X, REDSTONE_SLOT_Y);
        };
    }

    private static void addInput(
            List<Widget> widgets,
            Rectangle bounds,
            int normalX,
            int normalY,
            EntryIngredient entries
    ) {
        addEntry(widgets, bounds, normalX, normalY, entries, Slot.INPUT);
    }

    private static void addOutput(
            List<Widget> widgets,
            Rectangle bounds,
            int normalX,
            int normalY,
            EntryIngredient entries
    ) {
        addEntry(widgets, bounds, normalX, normalY, entries, Slot.OUTPUT);
    }

    private static void addPreview(
            List<Widget> widgets,
            Rectangle bounds,
            int normalX,
            int normalY,
            EntryIngredient entries
    ) {
        addEntry(widgets, bounds, normalX, normalY, entries, Slot.UN_MARKED);
    }

    private static void addDecoration(
            List<Widget> widgets,
            Rectangle bounds,
            int normalX,
            int normalY,
            EntryIngredient entries
    ) {
        Slot slot = createSlot(bounds, normalX, normalY)
                .entries(entries)
                .disableBackground()
                .disableHighlight()
                .disableTooltips()
                .noInteractable();
        widgets.add(slot);
    }

    private static void addEntry(
            List<Widget> widgets,
            Rectangle bounds,
            int normalX,
            int normalY,
            EntryIngredient entries,
            byte mark
    ) {
        Slot slot = createSlot(bounds, normalX, normalY)
                .entries(entries)
                .disableBackground();
        slot.setNoticeMark(mark);
        widgets.add(slot);
    }

    private static Slot createSlot(Rectangle bounds, int normalX, int normalY) {
        return Widgets.createSlot(new Rectangle(
                screenX(bounds) + normalX + ENTRY_CONTENT_OFFSET,
                screenY(bounds) + normalY + ENTRY_CONTENT_OFFSET,
                SlotRenderer.FRAME_SIZE,
                SlotRenderer.FRAME_SIZE
        ));
    }

    private static EntryIngredient input(TradingCellsReiDisplay display, int index) {
        return display.getInputEntries().get(index);
    }

    private static EntryIngredient output(TradingCellsReiDisplay display, int index) {
        return display.getOutputEntries().get(index);
    }

    private static int screenX(Rectangle bounds) {
        return bounds.x + PANEL_FRAME_SIZE - MachineScreenLayout.MACHINE_PANEL_X;
    }

    private static int screenY(Rectangle bounds) {
        return bounds.y + PANEL_FRAME_SIZE - MachineScreenLayout.MACHINE_PANEL_Y;
    }

}
