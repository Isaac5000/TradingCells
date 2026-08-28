package com.cosmocraft.trading_cells.platform.neoforge.menu;

import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ArmorSlot;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Shared armor and offhand slots for every block-entity menu. */
public final class PlayerEquipmentSlots {
    public static final int X = MachineMenuLayout.EQUIPMENT_X;
    public static final int HEAD_Y = MachineMenuLayout.EQUIPMENT_HEAD_Y;
    public static final int CHEST_Y = MachineMenuLayout.EQUIPMENT_CHEST_Y;
    public static final int LEGS_Y = MachineMenuLayout.EQUIPMENT_LEGS_Y;
    public static final int FEET_Y = MachineMenuLayout.EQUIPMENT_FEET_Y;
    public static final int OFFHAND_Y = MachineMenuLayout.EQUIPMENT_OFFHAND_Y;

    private PlayerEquipmentSlots() {
    }

    public static List<Slot> create(Inventory inventory) {
        return create(
                inventory,
                X,
                HEAD_Y,
                CHEST_Y,
                LEGS_Y,
                FEET_Y,
                OFFHAND_Y
        );
    }

    public static List<Slot> create(
            Inventory inventory,
            int x,
            int headY,
            int chestY,
            int legsY,
            int feetY,
            int offhandY
    ) {
        return List.of(
                new ArmorSlot(inventory, inventory.player, EquipmentSlot.HEAD, 39, x, headY, InventoryMenu.EMPTY_ARMOR_SLOT_HELMET),
                new ArmorSlot(inventory, inventory.player, EquipmentSlot.CHEST, 38, x, chestY, InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE),
                new ArmorSlot(inventory, inventory.player, EquipmentSlot.LEGS, 37, x, legsY, InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS),
                new ArmorSlot(inventory, inventory.player, EquipmentSlot.FEET, 36, x, feetY, InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS),
                new OffhandSlot(inventory, x, offhandY)
        );
    }

    private static final class OffhandSlot extends Slot {
        private final Inventory inventory;

        private OffhandSlot(Inventory inventory, int x, int y) {
            super(inventory, 40, x, y);
            this.inventory = inventory;
        }

        @Override
        public void setByPlayer(ItemStack stack, ItemStack previous) {
            inventory.player.onEquipItem(EquipmentSlot.OFFHAND, previous, stack);
            super.setByPlayer(stack, previous);
        }

        @Override
        public Identifier getNoItemIcon() {
            return InventoryMenu.EMPTY_ARMOR_SLOT_SHIELD;
        }
    }
}
