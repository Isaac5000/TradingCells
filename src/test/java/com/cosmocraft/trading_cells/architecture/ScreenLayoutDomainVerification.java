package com.cosmocraft.trading_cells.architecture;

import com.cosmocraft.trading_cells.feature.trader.domain.model.AutotraderPolicy;
import com.cosmocraft.trading_cells.feature.captures.adapters.output.client.CapturedEntityGuiTransform;
import com.cosmocraft.trading_cells.feature.farmer.adapters.input.PiglinFarmerMenuLayout;
import com.cosmocraft.trading_cells.feature.farmer.adapters.input.VillagerFarmerMenuLayout;
import com.cosmocraft.trading_cells.feature.quarry.adapters.input.PiglinQuarryMenuLayout;
import com.cosmocraft.trading_cells.feature.quarry.adapters.input.VillagerQuarryMenuLayout;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.SlotRenderer;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.MachineScreenUtil;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.trader.VillagerTradeScreenLayout;
import com.cosmocraft.trading_cells.platform.neoforge.menu.VillagerTradeMenuLayout;

final class ScreenLayoutDomainVerification {
    private ScreenLayoutDomainVerification() {
    }

    static void verify() {
        verifyDurationFormatting();
        verifyWideMachineMenuGeometry();
        verifyCapturedEntityGuiCenter();
        verifyVillagerTradeGeometry();
    }

    private static void verifyDurationFormatting() {
        require("1m 56s".equals(MachineScreenUtil.formatDuration(2_334)),
                "Machine and REI durations must floor 116.7 seconds to 1m 56s");
        require("5s".equals(MachineScreenUtil.formatDuration(100)),
                "Durations below one minute must use the compact seconds format");
    }

    private static void verifyWideMachineMenuGeometry() {
        verifyWideLayout(
                "Villager Farmer",
                VillagerFarmerMenuLayout.WIDTH,
                VillagerFarmerMenuLayout.HEIGHT,
                new int[] {
                        VillagerFarmerMenuLayout.WORKER_SLOT_X,
                        VillagerFarmerMenuLayout.HOE_SLOT_X,
                        VillagerFarmerMenuLayout.CROP_SLOT_X
                },
                new int[] {
                        VillagerFarmerMenuLayout.WORKER_SLOT_Y,
                        VillagerFarmerMenuLayout.HOE_SLOT_Y,
                        VillagerFarmerMenuLayout.CROP_SLOT_Y
                },
                VillagerFarmerMenuLayout.OUTPUT_FIRST_X,
                VillagerFarmerMenuLayout.OUTPUT_FIRST_Y,
                VillagerFarmerMenuLayout.OUTPUT_COLUMNS,
                VillagerFarmerMenuLayout.OUTPUT_SPACING,
                VillagerFarmerMenuLayout.PLAYER_INVENTORY_X,
                VillagerFarmerMenuLayout.PLAYER_INVENTORY_Y,
                VillagerFarmerMenuLayout.PLAYER_HOTBAR_Y,
                VillagerFarmerMenuLayout.EQUIPMENT_X,
                VillagerFarmerMenuLayout.EQUIPMENT_OFFHAND_Y
        );
        verifyWideLayout(
                "Piglin Farmer",
                PiglinFarmerMenuLayout.WIDTH,
                PiglinFarmerMenuLayout.HEIGHT,
                new int[] {
                        PiglinFarmerMenuLayout.WORKER_SLOT_X,
                        PiglinFarmerMenuLayout.HOE_SLOT_X,
                        PiglinFarmerMenuLayout.CROP_SLOT_X
                },
                new int[] {
                        PiglinFarmerMenuLayout.WORKER_SLOT_Y,
                        PiglinFarmerMenuLayout.HOE_SLOT_Y,
                        PiglinFarmerMenuLayout.CROP_SLOT_Y
                },
                PiglinFarmerMenuLayout.OUTPUT_FIRST_X,
                PiglinFarmerMenuLayout.OUTPUT_FIRST_Y,
                PiglinFarmerMenuLayout.OUTPUT_COLUMNS,
                PiglinFarmerMenuLayout.OUTPUT_SPACING,
                PiglinFarmerMenuLayout.PLAYER_INVENTORY_X,
                PiglinFarmerMenuLayout.PLAYER_INVENTORY_Y,
                PiglinFarmerMenuLayout.PLAYER_HOTBAR_Y,
                PiglinFarmerMenuLayout.EQUIPMENT_X,
                PiglinFarmerMenuLayout.EQUIPMENT_OFFHAND_Y
        );
        verifyWideLayout(
                "Villager Quarry",
                VillagerQuarryMenuLayout.WIDTH,
                VillagerQuarryMenuLayout.HEIGHT,
                new int[] {
                        VillagerQuarryMenuLayout.WORKER_SLOT_X,
                        VillagerQuarryMenuLayout.PICKAXE_SLOT_X,
                        VillagerQuarryMenuLayout.UPGRADE_SLOT_X
                },
                new int[] {
                        VillagerQuarryMenuLayout.WORKER_SLOT_Y,
                        VillagerQuarryMenuLayout.PICKAXE_SLOT_Y,
                        VillagerQuarryMenuLayout.UPGRADE_SLOT_Y
                },
                VillagerQuarryMenuLayout.OUTPUT_FIRST_X,
                VillagerQuarryMenuLayout.OUTPUT_FIRST_Y,
                VillagerQuarryMenuLayout.OUTPUT_COLUMNS,
                VillagerQuarryMenuLayout.OUTPUT_SPACING,
                VillagerQuarryMenuLayout.PLAYER_INVENTORY_X,
                VillagerQuarryMenuLayout.PLAYER_INVENTORY_Y,
                VillagerQuarryMenuLayout.PLAYER_HOTBAR_Y,
                VillagerQuarryMenuLayout.EQUIPMENT_X,
                VillagerQuarryMenuLayout.EQUIPMENT_OFFHAND_Y
        );
        verifyWideLayout(
                "Piglin Quarry",
                PiglinQuarryMenuLayout.WIDTH,
                PiglinQuarryMenuLayout.HEIGHT,
                new int[] {
                        PiglinQuarryMenuLayout.WORKER_SLOT_X,
                        PiglinQuarryMenuLayout.PICKAXE_SLOT_X,
                        PiglinQuarryMenuLayout.UPGRADE_SLOT_X
                },
                new int[] {
                        PiglinQuarryMenuLayout.WORKER_SLOT_Y,
                        PiglinQuarryMenuLayout.PICKAXE_SLOT_Y,
                        PiglinQuarryMenuLayout.UPGRADE_SLOT_Y
                },
                PiglinQuarryMenuLayout.OUTPUT_FIRST_X,
                PiglinQuarryMenuLayout.OUTPUT_FIRST_Y,
                PiglinQuarryMenuLayout.OUTPUT_COLUMNS,
                PiglinQuarryMenuLayout.OUTPUT_SPACING,
                PiglinQuarryMenuLayout.PLAYER_INVENTORY_X,
                PiglinQuarryMenuLayout.PLAYER_INVENTORY_Y,
                PiglinQuarryMenuLayout.PLAYER_HOTBAR_Y,
                PiglinQuarryMenuLayout.EQUIPMENT_X,
                PiglinQuarryMenuLayout.EQUIPMENT_OFFHAND_Y
        );
    }

    private static void verifyWideLayout(
            String name,
            int width,
            int height,
            int[] inputX,
            int[] inputY,
            int outputFirstX,
            int outputFirstY,
            int outputColumns,
            int outputSpacing,
            int inventoryX,
            int inventoryY,
            int hotbarY,
            int equipmentX,
            int equipmentOffhandY
    ) {
        int slotSize = 16;
        require(width == 348 && height == 210, name + " must retain the 348x210 menu format");
        require(inputX.length == 3 && inputY.length == 3, name + " must expose exactly three inputs");
        for (int index = 0; index < inputX.length; index++) {
            require(inputX[index] >= 7 && inputX[index] + slotSize <= 116,
                    name + " input left the left panel horizontally");
            require(inputY[index] >= 27 && inputY[index] + slotSize <= 204,
                    name + " input left the left panel vertically");
        }
        int outputLastX = outputFirstX + (outputColumns - 1) * outputSpacing;
        int outputLastY = outputFirstY + outputSpacing;
        require(outputColumns == 9 && outputFirstX >= 124 && outputLastX + slotSize <= 342,
                name + " output matrix left the machine panel horizontally");
        require(outputFirstY >= 27 && outputLastY + slotSize <= 102,
                name + " output matrix left the machine panel vertically");
        require(inventoryX >= 124 && inventoryX + 8 * 18 + slotSize <= 342,
                name + " inventory left the lower panel horizontally");
        require(inventoryY >= 104 && inventoryY + 2 * 18 + slotSize <= 204,
                name + " inventory left the lower panel vertically");
        require(hotbarY >= inventoryY + 3 * 18 && hotbarY + slotSize <= 204,
                name + " hotbar overlaps the inventory or leaves the panel");
        require(equipmentX >= 124 && equipmentX + slotSize < inventoryX,
                name + " equipment column overlaps the inventory");
        require(equipmentOffhandY + slotSize <= 204,
                name + " equipment column leaves the lower panel");
    }

    private static void verifyCapturedEntityGuiCenter() {
        double adultCenter = CapturedEntityGuiTransform.effectiveCenterX(0.38F);
        require(close(adultCenter, CapturedEntityGuiTransform.effectiveCenterX(0.48F)),
                "Villager adults and babies must share the GUI X center");
        require(close(adultCenter, CapturedEntityGuiTransform.effectiveCenterX(0.55F)),
                "Piglin adults and babies must share the GUI X center");
    }

    private static void verifyVillagerTradeGeometry() {
        require(VillagerTradeMenuLayout.WIDTH == 348 && VillagerTradeMenuLayout.HEIGHT == 210,
                "Villager trade menus must keep their 348x210 visible bounds");
        require(SlotRenderer.FRAME_SIZE == 18 && SlotRenderer.ITEM_SIZE == 16,
                "Trade slots must use an 18x18 frame around a 16x16 item area");
        require(
                VillagerTradeMenuLayout.itemX(VillagerTradeMenuLayout.MANUAL_PAYMENT_A_X)
                        == VillagerTradeMenuLayout.MANUAL_PAYMENT_A_X + 1,
                "Menu item coordinates must match the visual slot inset"
        );
        require(
                VillagerTradeMenuLayout.EQUIPMENT_OFFHAND_Y + SlotRenderer.FRAME_SIZE
                        <= VillagerTradeMenuLayout.INVENTORY_PANEL_Y
                        + VillagerTradeMenuLayout.INVENTORY_PANEL_HEIGHT,
                "Five equipment slots must remain inside the inventory panel"
        );
        require(
                VillagerTradeScreenLayout.AUTOTRADER_DROPDOWN_Y
                        + VillagerTradeScreenLayout.AUTOTRADER_DROPDOWN_HEIGHT + 4
                        <= VillagerTradeMenuLayout.TRADES_CONTENT_Y
                        + VillagerTradeMenuLayout.TRADES_CONTENT_HEIGHT,
                "Autotrader dropdown must remain inside the menu"
        );
        require(
                VillagerTradeScreenLayout.MANUAL_SCROLL_X
                        == VillagerTradeScreenLayout.MANUAL_ROW_X
                        + VillagerTradeScreenLayout.MANUAL_ROW_WIDTH - 1,
                "Trader scrollbar must join the offer rows at their right border"
        );
        require(
                VillagerTradeMenuLayout.autotraderOutputFrameY(AutotraderPolicy.OUTPUT_SLOTS - 1)
                        + SlotRenderer.FRAME_SIZE
                        <= VillagerTradeMenuLayout.AUTOTRADER_OUTPUT_PANEL_Y
                        + VillagerTradeMenuLayout.AUTOTRADER_OUTPUT_PANEL_HEIGHT,
                "Autotrader output slots must remain inside their visual panel"
        );
        require(
                AutotraderPolicy.OUTPUT_SLOTS == 8
                        && VillagerTradeMenuLayout.AUTOTRADER_INPUT_B_PANEL_Y
                        == VillagerTradeMenuLayout.AUTOTRADER_INPUT_A_PANEL_Y
                        + VillagerTradeMenuLayout.AUTOTRADER_PANEL_HEIGHT
                        && VillagerTradeMenuLayout.AUTOTRADER_OUTPUT_PANEL_Y
                        == VillagerTradeMenuLayout.AUTOTRADER_INPUT_B_PANEL_Y
                        + VillagerTradeMenuLayout.AUTOTRADER_PANEL_HEIGHT,
                "Autotrader buffers must use eight outputs and contiguous panels"
        );
        require(
                VillagerTradeScreenLayout.DROPDOWN_VISIBLE_ROWS == 8
                        && VillagerTradeScreenLayout.dropdownContentHeight(1)
                        == VillagerTradeScreenLayout.DROPDOWN_ROW_HEIGHT
                        && VillagerTradeScreenLayout.dropdownContentHeight(2)
                        == VillagerTradeScreenLayout.DROPDOWN_ROW_HEIGHT * 2
                        && VillagerTradeScreenLayout.dropdownContentHeight(8)
                        == VillagerTradeScreenLayout.AUTOTRADER_DROPDOWN_HEIGHT
                        && VillagerTradeScreenLayout.dropdownContentHeight(9)
                        == VillagerTradeScreenLayout.AUTOTRADER_DROPDOWN_HEIGHT,
                "Autotrader dropdown height must follow its rows up to the eight-row maximum"
        );
    }

    private static boolean close(double left, double right) {
        return Math.abs(left - right) < 1.0E-12D;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
