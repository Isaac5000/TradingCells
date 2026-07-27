package com.cosmocraft.trading_cells.platform.neoforge.menu;

import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.TradingCells;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ArmorSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Shared single-column equipment layout for both villager trading menus. */
public final class VillagerTradeEquipmentSlots {
    private static final Identifier EMPTY_OFFHAND = emptyIcon("shield");
    private static final Identifier EMPTY_HELMET = emptyIcon("helmet");
    private static final Identifier EMPTY_CHESTPLATE = emptyIcon("chestplate");
    private static final Identifier EMPTY_LEGGINGS = emptyIcon("leggings");
    private static final Identifier EMPTY_BOOTS = emptyIcon("boots");

    private VillagerTradeEquipmentSlots() {
    }

    private static Identifier emptyIcon(String name) {
        return Identifier.fromNamespaceAndPath(TradingCells.MOD_ID, "container/slot/" + name);
    }

    public static List<Slot> create(Inventory inventory) {
        return List.of(
                new ArmorSlot(
                        inventory,
                        inventory.player,
                        EquipmentSlot.HEAD,
                        39,
                        VillagerTradeMenuLayout.itemX(VillagerTradeMenuLayout.EQUIPMENT_X),
                        VillagerTradeMenuLayout.itemY(VillagerTradeMenuLayout.EQUIPMENT_HEAD_Y),
                        EMPTY_HELMET
                ),
                new ArmorSlot(
                        inventory,
                        inventory.player,
                        EquipmentSlot.CHEST,
                        38,
                        VillagerTradeMenuLayout.itemX(VillagerTradeMenuLayout.EQUIPMENT_X),
                        VillagerTradeMenuLayout.itemY(VillagerTradeMenuLayout.EQUIPMENT_CHEST_Y),
                        EMPTY_CHESTPLATE
                ),
                new ArmorSlot(
                        inventory,
                        inventory.player,
                        EquipmentSlot.LEGS,
                        37,
                        VillagerTradeMenuLayout.itemX(VillagerTradeMenuLayout.EQUIPMENT_X),
                        VillagerTradeMenuLayout.itemY(VillagerTradeMenuLayout.EQUIPMENT_LEGS_Y),
                        EMPTY_LEGGINGS
                ),
                new ArmorSlot(
                        inventory,
                        inventory.player,
                        EquipmentSlot.FEET,
                        36,
                        VillagerTradeMenuLayout.itemX(VillagerTradeMenuLayout.EQUIPMENT_X),
                        VillagerTradeMenuLayout.itemY(VillagerTradeMenuLayout.EQUIPMENT_FEET_Y),
                        EMPTY_BOOTS
                ),
                new OffhandSlot(inventory)
        );
    }


    private static final class OffhandSlot extends Slot {
        private final Inventory inventory;

        private OffhandSlot(Inventory inventory) {
            super(
                    inventory,
                    40,
                    VillagerTradeMenuLayout.itemX(VillagerTradeMenuLayout.EQUIPMENT_X),
                    VillagerTradeMenuLayout.itemY(VillagerTradeMenuLayout.EQUIPMENT_OFFHAND_Y)
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
