package com.cosmocraft.trading_cells.feature.skeletonfarm.adapters.input;

import com.cosmocraft.trading_cells.feature.captures.adapters.api.CapturedMobStackAdapter;
import com.cosmocraft.trading_cells.feature.captures.domain.model.CapturedMobKind;
import com.cosmocraft.trading_cells.feature.combat.adapters.api.CombatEnchantments;
import com.cosmocraft.trading_cells.feature.skeletonfarm.adapters.output.SkeletonFarmRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.skeletonfarm.domain.model.SkeletonFarmKind;
import com.cosmocraft.trading_cells.feature.skeletonfarm.domain.model.SkeletonFarmLoot;
import com.cosmocraft.trading_cells.platform.neoforge.mobfarm.MobFarmCatalog;
import com.cosmocraft.trading_cells.platform.neoforge.mobfarm.MobFarmSwordTierCatalog;
import com.cosmocraft.trading_cells.platform.neoforge.mobfarm.MobFarmEquipmentSlots;
import com.cosmocraft.trading_cells.platform.neoforge.network.MobFarmCatalogSyncPayload;
import com.cosmocraft.trading_cells.shared.machines.domain.model.MinecraftExperience;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NonNull;

public final class SkeletonFarmMenu extends AbstractContainerMenu {
    public static final int WIDTH = SkeletonFarmMenuLayout.WIDTH;
    public static final int HEIGHT = SkeletonFarmMenuLayout.HEIGHT;
    public static final int WORKER_SLOT_X = 145;
    public static final int SWORD_SLOT_X = 173;
    public static final int INPUT_SLOT_Y = 31;
    public static final int OUTPUT_FIRST_X = 152;
    public static final int OUTPUT_FIRST_Y = 66;
    public static final int OUTPUT_COLUMNS = 9;
    public static final int PLAYER_INVENTORY_X = SkeletonFarmMenuLayout.itemX(
            SkeletonFarmMenuLayout.PLAYER_INVENTORY_X
    );
    public static final int PLAYER_INVENTORY_Y = SkeletonFarmMenuLayout.itemY(
            SkeletonFarmMenuLayout.PLAYER_INVENTORY_Y
    );
    public static final int PLAYER_HOTBAR_Y = SkeletonFarmMenuLayout.itemY(
            SkeletonFarmMenuLayout.PLAYER_HOTBAR_Y
    );
    public static final int SELECT_KIND_BUTTON_BASE = 100;
    public static final int TOGGLE_LOOT_BUTTON_BASE = 1_000;
    public static final int TOGGLE_DYNAMIC_LOOT_BUTTON_BASE = 2_000;
    public static final int EXTRACT_EXPERIENCE_BUTTON = 3_000;
    public static final int TOGGLE_ENABLED_BUTTON = 3_001;
    private static final int MACHINE_SLOT_COUNT = SkeletonFarmBlockEntity.CONTAINER_SIZE;
    private static final int PLAYER_INVENTORY_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int PLAYER_HOTBAR_END = PLAYER_INVENTORY_END + 9;

    private final Container container;
    private final ContainerData data;
    private final HolderLookup.Provider registries;
    private List<MobFarmCatalog.Target> catalogTargets;
    private Identifier selectedTargetId;
    private final Set<Identifier> disabledDynamicLoot = new HashSet<>();
    private int catalogRevision = -1;

    public SkeletonFarmMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(MACHINE_SLOT_COUNT), new SimpleContainerData(9));
    }

    public SkeletonFarmMenu(
            int containerId,
            Inventory inventory,
            Container container,
            ContainerData data
    ) {
        super(SkeletonFarmRegistrationAdapter.MENU.get(), containerId);
        checkContainerSize(container, MACHINE_SLOT_COUNT);
        checkContainerDataCount(data, 9);
        this.container = container;
        this.data = data;
        this.registries = inventory.player.registryAccess();
        this.catalogTargets = SkeletonFarmTargetCatalog.targets();
        if (container instanceof SkeletonFarmBlockEntity farm) {
            selectedTargetId = farm.selectedTargetId();
            disabledDynamicLoot.addAll(farm.disabledDynamicLoot());
        } else {
            selectedTargetId = SkeletonFarmTargetCatalog.id(SkeletonFarmKind.SKELETON);
        }

        addSlot(new WorkerSlot(container, SkeletonFarmBlockEntity.WORKER_SLOT, WORKER_SLOT_X, INPUT_SLOT_Y));
        addSlot(new SwordSlot(container, SkeletonFarmBlockEntity.SWORD_SLOT, SWORD_SLOT_X, INPUT_SLOT_Y));
        for (int index = 0; index < SkeletonFarmBlockEntity.OUTPUT_SLOT_COUNT; index++) {
            addSlot(new OutputSlot(
                    container,
                    SkeletonFarmBlockEntity.FIRST_OUTPUT_SLOT + index,
                    outputSlotX(index),
                    outputSlotY(index)
            ));
        }
        addStandardInventorySlots(inventory, PLAYER_INVENTORY_X, PLAYER_INVENTORY_Y);
        for (Slot equipmentSlot : MobFarmEquipmentSlots.create(inventory)) {
            addSlot(equipmentSlot);
        }
        addDataSlots(data);
    }

    public SkeletonFarmKind selectedKind() {
        return SkeletonFarmTargetCatalog.staticKind(selectedTargetId);
    }

    public Identifier selectedTargetId() {
        return selectedTargetId;
    }

    public List<MobFarmCatalog.Target> targetEntries() {
        return catalogTargets;
    }

    public int catalogRevision() {
        return catalogRevision;
    }

    public Set<Identifier> disabledDynamicLootIds() {
        return Set.copyOf(disabledDynamicLoot);
    }

    public void applyCatalogSnapshot(MobFarmCatalogSyncPayload payload) {
        if (!payload.familyId().equals(MobFarmCatalog.Family.SKELETON.id())) {
            return;
        }
        catalogTargets = payload.targets().stream()
                .map(entry -> new MobFarmCatalog.Target(
                        entry.entityTypeId(),
                        entry.generatorItemId(),
                        entry.lootItemIds()
                ))
                .toList();
        selectedTargetId = payload.selectedTargetId();
        disabledDynamicLoot.clear();
        disabledDynamicLoot.addAll(payload.disabledDynamicLootIds());
        catalogRevision = payload.revision();
    }

    public int cycleTicks() {
        return data.get(0);
    }

    public int maxCycleTicks() {
        return Math.max(1, data.get(1));
    }

    public int storedExperience() {
        return Math.max(0, data.get(4));
    }

    public int storedLevels() {
        return MinecraftExperience.levelForTotalPoints(storedExperience());
    }

    public int simulatedKills() {
        return Math.max(1, data.get(6));
    }

    public boolean isHunting() {
        return data.get(5) != 0;
    }

    public boolean isLootEnabled(SkeletonFarmLoot loot) {
        return isLootAvailable(loot) && (data.get(3) & loot.bit()) != 0;
    }

    public boolean isLootAvailable(SkeletonFarmLoot loot) {
        return availableLootOptions().contains(loot);
    }

    public List<SkeletonFarmLoot> availableLootOptions() {
        if (SkeletonFarmTargetCatalog.isStaticTarget(selectedTargetId)) {
            return selectedKind().availableLoot(hasDecapitation());
        }
        LinkedHashSet<SkeletonFarmLoot> categories = new LinkedHashSet<>();
        selectedTarget().lootItemIds().stream()
                .map(id -> BuiltInRegistries.ITEM.getOptional(id).orElse(null))
                .filter(java.util.Objects::nonNull)
                .map(SkeletonFarmTargetCatalog::category)
                .filter(java.util.Objects::nonNull)
                .forEach(categories::add);
        return List.copyOf(categories);
    }

    public List<ItemStack> dynamicLootOptions() {
        return selectedTarget().lootItemIds().stream()
                .map(id -> BuiltInRegistries.ITEM.getOptional(id).orElse(null))
                .filter(item -> item != null && SkeletonFarmTargetCatalog.category(item) == null)
                .map(ItemStack::new)
                .toList();
    }

    public boolean isDynamicLootEnabled(ItemStack stack) {
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id != null && !disabledDynamicLoot.contains(id);
    }

    public int enabledLootMask() {
        return data.get(3);
    }

    public boolean isDeliveringOversizedBatch() {
        return data.get(7) != 0;
    }

    public boolean isEnabled() {
        return data.get(8) != 0;
    }

    public boolean hasWorker() {
        return getSlot(SkeletonFarmBlockEntity.WORKER_SLOT).hasItem();
    }

    public boolean hasSword() {
        return getSlot(SkeletonFarmBlockEntity.SWORD_SLOT).hasItem();
    }

    public int lootingLevel() {
        return CombatEnchantments.lootingLevel(
                getSlot(SkeletonFarmBlockEntity.SWORD_SLOT).getItem(),
                registries
        );
    }

    public boolean hasDecapitation() {
        return decapitationLevel() > 0;
    }

    public int decapitationLevel() {
        return CombatEnchantments.decapitationLevel(
                getSlot(SkeletonFarmBlockEntity.SWORD_SLOT).getItem(),
                registries
        );
    }

    public static int outputSlotX(int index) {
        return OUTPUT_FIRST_X + index % OUTPUT_COLUMNS * 18;
    }

    public static int outputSlotY(int index) {
        return OUTPUT_FIRST_Y + index / OUTPUT_COLUMNS * 18;
    }

    @Override
    public boolean clickMenuButton(@NonNull Player player, int buttonId) {
        if (isDeliveringOversizedBatch()
                && (isTargetButton(buttonId) || isLootButton(buttonId))) {
            return false;
        }
        if (buttonId >= SELECT_KIND_BUTTON_BASE
                && buttonId < SELECT_KIND_BUTTON_BASE + catalogTargets.size()) {
            Identifier target = catalogTargets.get(buttonId - SELECT_KIND_BUTTON_BASE).entityTypeId();
            selectedTargetId = target;
            if (container instanceof SkeletonFarmBlockEntity farm) {
                farm.selectTarget(target);
            } else {
                data.set(2, SkeletonFarmTargetCatalog.staticKind(target).ordinal());
            }
            return true;
        }
        if (buttonId >= TOGGLE_LOOT_BUTTON_BASE
                && buttonId < TOGGLE_LOOT_BUTTON_BASE + SkeletonFarmLoot.values().length) {
            SkeletonFarmLoot loot = SkeletonFarmLoot.values()[buttonId - TOGGLE_LOOT_BUTTON_BASE];
            if (!isLootAvailable(loot)) {
                return false;
            }
            data.set(3, data.get(3) ^ loot.bit());
            return true;
        }
        if (buttonId >= TOGGLE_DYNAMIC_LOOT_BUTTON_BASE
                && buttonId < TOGGLE_DYNAMIC_LOOT_BUTTON_BASE + dynamicLootOptions().size()) {
            ItemStack stack = dynamicLootOptions().get(buttonId - TOGGLE_DYNAMIC_LOOT_BUTTON_BASE);
            Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (itemId == null) {
                return false;
            }
            if (!disabledDynamicLoot.remove(itemId)) {
                disabledDynamicLoot.add(itemId);
            }
            if (container instanceof SkeletonFarmBlockEntity farm) {
                farm.toggleDynamicLoot(itemId);
            }
            return true;
        }
        if (buttonId == EXTRACT_EXPERIENCE_BUTTON) {
            if (container instanceof SkeletonFarmBlockEntity farm) {
                farm.extractExperience(player);
            }
            return true;
        }
        if (buttonId == TOGGLE_ENABLED_BUTTON) {
            if (container instanceof SkeletonFarmBlockEntity farm) {
                farm.toggleEnabled();
            }
            return true;
        }
        return false;
    }

    private MobFarmCatalog.Target selectedTarget() {
        return catalogTargets.stream()
                .filter(target -> target.entityTypeId().equals(selectedTargetId))
                .findFirst()
                .orElseGet(() -> new MobFarmCatalog.Target(
                        selectedTargetId,
                        Identifier.withDefaultNamespace("spawner"),
                        List.of()
                ));
    }

    private boolean isTargetButton(int buttonId) {
        return buttonId >= SELECT_KIND_BUTTON_BASE
                && buttonId < SELECT_KIND_BUTTON_BASE + catalogTargets.size();
    }

    private boolean isLootButton(int buttonId) {
        return buttonId >= TOGGLE_LOOT_BUTTON_BASE
                        && buttonId < TOGGLE_LOOT_BUTTON_BASE + SkeletonFarmLoot.values().length
                || buttonId >= TOGGLE_DYNAMIC_LOOT_BUTTON_BASE
                        && buttonId < TOGGLE_DYNAMIC_LOOT_BUTTON_BASE + dynamicLootOptions().size();
    }

    @Override
    public boolean stillValid(@NonNull Player player) {
        return container.stillValid(player);
    }

    @Override
    public @NonNull ItemStack quickMoveStack(@NonNull Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack result = stack.copy();
        if (index < MACHINE_SLOT_COUNT) {
            if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, PLAYER_HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (isAdultVillager(stack)) {
            if (!moveItemStackTo(stack, SkeletonFarmBlockEntity.WORKER_SLOT, SkeletonFarmBlockEntity.WORKER_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (MobFarmSwordTierCatalog.isSupported(stack)) {
            if (!moveItemStackTo(stack, SkeletonFarmBlockEntity.SWORD_SLOT, SkeletonFarmBlockEntity.SWORD_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index < PLAYER_INVENTORY_END) {
            if (!moveItemStackTo(stack, PLAYER_INVENTORY_END, PLAYER_HOTBAR_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, PLAYER_INVENTORY_END, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return result;
    }

    private static boolean isAdultVillager(ItemStack stack) {
        return CapturedMobStackAdapter.isFilledCapturer(CapturedMobKind.VILLAGER, stack)
                && !CapturedMobStackAdapter.isBaby(CapturedMobKind.VILLAGER, stack);
    }

    private static final class WorkerSlot extends Slot {
        private WorkerSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(@NonNull ItemStack stack) {
            return isAdultVillager(stack);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }

    private static final class SwordSlot extends Slot {
        private SwordSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(@NonNull ItemStack stack) {
            return MobFarmSwordTierCatalog.isSupported(stack);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }

    private static final class OutputSlot extends Slot {
        private OutputSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(@NonNull ItemStack stack) {
            return false;
        }
    }
}
