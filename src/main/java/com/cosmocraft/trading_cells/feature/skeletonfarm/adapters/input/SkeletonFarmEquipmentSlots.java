package com.cosmocraft.trading_cells.feature.skeletonfarm.adapters.input;

import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.TradingCells;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ArmorSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Equipment column owned by the mob-farm menu. */
public final class SkeletonFarmEquipmentSlots {
    private static final Identifier EMPTY_OFFHAND = emptyIcon("shield");
    private static final Identifier EMPTY_HELMET = emptyIcon("helmet");
    private static final Identifier EMPTY_CHESTPLATE = emptyIcon("chestplate");
    private static final Identifier EMPTY_LEGGINGS = emptyIcon("leggings");
    private static final Identifier EMPTY_BOOTS = emptyIcon("boots");

    private SkeletonFarmEquipmentSlots() {
    }

    public static List<Slot> create(Inventory inventory) {
        return List.of(
                armorSlot(inventory, EquipmentSlot.HEAD, 39, SkeletonFarmMenuLayout.EQUIPMENT_HEAD_Y, EMPTY_HELMET),
                armorSlot(inventory, EquipmentSlot.CHEST, 38, SkeletonFarmMenuLayout.EQUIPMENT_CHEST_Y, EMPTY_CHESTPLATE),
                armorSlot(inventory, EquipmentSlot.LEGS, 37, SkeletonFarmMenuLayout.EQUIPMENT_LEGS_Y, EMPTY_LEGGINGS),
                armorSlot(inventory, EquipmentSlot.FEET, 36, SkeletonFarmMenuLayout.EQUIPMENT_FEET_Y, EMPTY_BOOTS),
                new OffhandSlot(inventory)
        );
    }

    private static ArmorSlot armorSlot(
            Inventory inventory,
            EquipmentSlot equipmentSlot,
            int inventorySlot,
            int frameY,
            Identifier emptyIcon
    ) {
        return new ArmorSlot(
                inventory,
                inventory.player,
                equipmentSlot,
                inventorySlot,
                SkeletonFarmMenuLayout.itemX(SkeletonFarmMenuLayout.EQUIPMENT_X),
                SkeletonFarmMenuLayout.itemY(frameY),
                emptyIcon
        );
    }

    private static Identifier emptyIcon(String name) {
        return Identifier.fromNamespaceAndPath(TradingCells.MOD_ID, "container/slot/" + name);
    }

    private static final class OffhandSlot extends Slot {
        private final Inventory inventory;

        private OffhandSlot(Inventory inventory) {
            super(
                    inventory,
                    40,
                    SkeletonFarmMenuLayout.itemX(SkeletonFarmMenuLayout.EQUIPMENT_X),
                    SkeletonFarmMenuLayout.itemY(SkeletonFarmMenuLayout.EQUIPMENT_OFFHAND_Y)
            );
            this.inventory = inventory;
        }

        @Override
        public void setByPlayer(ItemStack stack, ItemStack previous) {
            inventory.player.onEquipItem(EquipmentSlot.OFFHAND, previous, stack);
            super.setByPlayer(stack, previous);
        }

        @Override
        public Identifier getNoItemIcon() {
            return EMPTY_OFFHAND;
        }
    }
}
