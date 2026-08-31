package com.cosmocraft.trading_cells.feature.infusion.adapters.input;

import com.cosmocraft.trading_cells.feature.infusion.adapters.output.ArcaneInfuserRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.infusion.adapters.minecraft.ArcaneInfusionRecipe;
import com.cosmocraft.trading_cells.feature.infusion.domain.model.ArcaneInfusionTransferAction;
import com.cosmocraft.trading_cells.platform.neoforge.menu.MachineMenuLayout;
import com.cosmocraft.trading_cells.platform.neoforge.menu.PlayerEquipmentSlots;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.inventory.RecipeBookType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jspecify.annotations.NonNull;

public final class ArcaneInfuserMenu extends RecipeBookMenu {
    private static final int INPUT_GRID_X = MachineMenuLayout.machineX(-2);
    private static final int INPUT_GRID_Y = 39;
    private static final int INPUT_GRID_SPACING = 18;
    public static final int OUTPUT_SLOT_X = MachineMenuLayout.machineX(75);
    public static final int OUTPUT_SLOT_Y = 57;

    private static final int MACHINE_SLOT_COUNT = ArcaneInfuserBlockEntity.CONTAINER_SIZE;
    private static final int PLAYER_INVENTORY_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int PLAYER_HOTBAR_END = PLAYER_INVENTORY_END + 9;

    private final Container container;
    private final ContainerData data;

    public ArcaneInfuserMenu(int containerId, Inventory inventory) {
        this(
                containerId,
                inventory,
                new SimpleContainer(MACHINE_SLOT_COUNT),
                new SimpleContainerData(5)
        );
    }

    public ArcaneInfuserMenu(
            int containerId,
            Inventory inventory,
            Container container,
            ContainerData data
    ) {
        super(ArcaneInfuserRegistrationAdapter.MENU.get(), containerId);
        checkContainerSize(container, MACHINE_SLOT_COUNT);
        checkContainerDataCount(data, 5);
        this.container = container;
        this.data = data;

        for (int slot = 0; slot < ArcaneInfuserBlockEntity.INPUT_SLOT_COUNT; slot++) {
            addSlot(new Slot(container, slot, inputSlotX(slot), inputSlotY(slot)));
        }
        addSlot(new OutputSlot(
                container,
                ArcaneInfuserBlockEntity.OUTPUT_SLOT,
                OUTPUT_SLOT_X,
                OUTPUT_SLOT_Y,
                this::resultCanBeTaken
        ));
        addStandardInventorySlots(
                inventory,
                MachineMenuLayout.PLAYER_INVENTORY_X,
                MachineMenuLayout.PLAYER_INVENTORY_SLOT_Y
        );
        for (Slot equipmentSlot : PlayerEquipmentSlots.create(inventory)) {
            addSlot(equipmentSlot);
        }
        addDataSlots(data);
    }

    public int storedExperience() {
        return (data.get(0) & 0xFFFF) | ((data.get(1) & 0x7FFF) << 16);
    }

    public int capacity() {
        return ArcaneInfuserBlockEntity.EXPERIENCE_CAPACITY;
    }

    public int requiredExperience() {
        return (data.get(2) & 0xFFFF) | ((data.get(3) & 0x7FFF) << 16);
    }

    public boolean insufficientRecipeExperience() {
        return data.get(4) == ArcaneInfuserBlockEntity.OUTPUT_STATE_INSUFFICIENT_EXPERIENCE;
    }

    private boolean resultCanBeTaken() {
        int outputState = data.get(4);
        return outputState == ArcaneInfuserBlockEntity.OUTPUT_STATE_MANUAL_READY
                || outputState == ArcaneInfuserBlockEntity.OUTPUT_STATE_PHYSICAL;
    }

    public void handleTransfer(
            ServerPlayer player,
            ArcaneInfusionTransferAction action,
            int requestedLevels
    ) {
        if (container instanceof ArcaneInfuserBlockEntity infuser && stillValid(player)) {
            infuser.transferExperience(player, action, requestedLevels);
        }
    }

    @Override
    public boolean stillValid(@NonNull Player player) {
        return container.stillValid(player);
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack carried, Slot target) {
        return !(target instanceof OutputSlot);
    }

    @Override
    public PostPlaceAction handlePlacement(
            boolean useMaxItems,
            boolean allowDroppingItemsToClear,
            RecipeHolder<?> holder,
            ServerLevel level,
            Inventory inventory
    ) {
        if (!(holder.value() instanceof ArcaneInfusionRecipe recipe)) {
            return PostPlaceAction.NOTHING;
        }

        List<ItemStack> returnedInventory = copyInventory(inventory.getNonEquipmentItems());
        for (int slot = 0; slot < ArcaneInfuserBlockEntity.INPUT_SLOT_COUNT; slot++) {
            if (!insertIntoInventory(
                    returnedInventory,
                    container.getItem(slot),
                    allowDroppingItemsToClear
            )) {
                return PostPlaceAction.NOTHING;
            }
        }

        PlacementPlan plan = useMaxItems
                ? largestPlacement(recipe, returnedInventory)
                : placement(recipe, returnedInventory, 1);
        if (plan == null) {
            applyPlacement(inventory, returnedInventory, emptyInputs());
            return PostPlaceAction.PLACE_GHOST_RECIPE;
        }

        applyPlacement(inventory, plan.inventory(), plan.inputs());
        return PostPlaceAction.NOTHING;
    }

    @Override
    public void fillCraftSlotsStackedContents(StackedItemContents stackedContents) {
        for (int slot = 0; slot < ArcaneInfuserBlockEntity.INPUT_SLOT_COUNT; slot++) {
            stackedContents.accountStack(container.getItem(slot));
        }
    }

    @Override
    public RecipeBookType getRecipeBookType() {
        return RecipeBookType.CRAFTING;
    }

    public static int inputSlotX(int slot) {
        return INPUT_GRID_X + slot % 3 * INPUT_GRID_SPACING;
    }

    public static int inputSlotY(int slot) {
        return INPUT_GRID_Y + slot / 3 * INPUT_GRID_SPACING;
    }

    @Override
    public @NonNull ItemStack quickMoveStack(@NonNull Player player, int index) {
        if (index < 0 || index >= slots.size()) {
            return ItemStack.EMPTY;
        }
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        if (index == ArcaneInfuserBlockEntity.OUTPUT_SLOT) {
            return quickMoveResult(player, slot);
        }
        ItemStack stack = slot.getItem();
        ItemStack result = stack.copy();
        if (!moveQuickMovedStack(index, stack)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return result;
    }

    private ItemStack quickMoveResult(Player player, Slot slot) {
        if (!slot.mayPickup(player)) {
            return ItemStack.EMPTY;
        }
        ItemStack result = slot.getItem();
        ItemStack original = result.copy();
        if (!moveItemStackTo(result, PLAYER_INVENTORY_START, PLAYER_HOTBAR_END, true)) {
            return ItemStack.EMPTY;
        }
        slot.onQuickCraft(result, original);
        if (result.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.setByPlayer(ItemStack.EMPTY, original);
        slot.onTake(player, result);
        return original;
    }

    private boolean moveQuickMovedStack(int index, ItemStack stack) {
        if (index < MACHINE_SLOT_COUNT) {
            return moveItemStackTo(stack, PLAYER_INVENTORY_START, PLAYER_HOTBAR_END, true);
        }
        for (int slot = 0; slot < ArcaneInfuserBlockEntity.INPUT_SLOT_COUNT; slot++) {
            if (slots.get(slot).mayPlace(stack) && moveItemStackTo(stack, slot, slot + 1, false)) {
                return true;
            }
        }
        return index < PLAYER_INVENTORY_END
                ? moveItemStackTo(stack, PLAYER_INVENTORY_END, PLAYER_HOTBAR_END, false)
                : moveItemStackTo(stack, PLAYER_INVENTORY_START, PLAYER_INVENTORY_END, false);
    }

    private void applyPlacement(
            Inventory inventory,
            List<ItemStack> inventoryContents,
            List<ItemStack> inputs
    ) {
        for (int slot = 0; slot < inventoryContents.size(); slot++) {
            inventory.setItem(slot, inventoryContents.get(slot).copy());
        }
        for (int slot = 0; slot < ArcaneInfuserBlockEntity.INPUT_SLOT_COUNT; slot++) {
            container.setItem(slot, inputs.get(slot).copy());
        }
        inventory.setChanged();
        container.setChanged();
    }

    private static PlacementPlan largestPlacement(
            ArcaneInfusionRecipe recipe,
            List<ItemStack> inventory
    ) {
        int maximum = Item.ABSOLUTE_MAX_STACK_SIZE;
        for (int slot = 0; slot < ArcaneInfuserBlockEntity.INPUT_SLOT_COUNT; slot++) {
            int count = recipe.ingredient(slot).count();
            if (count > 0) {
                maximum = Math.min(maximum, Item.ABSOLUTE_MAX_STACK_SIZE / count);
            }
        }
        for (int batches = maximum; batches >= 1; batches--) {
            PlacementPlan plan = placement(recipe, inventory, batches);
            if (plan != null) {
                return plan;
            }
        }
        return null;
    }

    private static PlacementPlan placement(
            ArcaneInfusionRecipe recipe,
            List<ItemStack> inventory,
            int batches
    ) {
        List<ItemStack> workingInventory = copyInventory(inventory);
        List<ItemStack> inputs = emptyInputs();
        return assignInput(recipe, 0, batches, workingInventory, inputs)
                ? new PlacementPlan(workingInventory, inputs)
                : null;
    }

    private static boolean assignInput(
            ArcaneInfusionRecipe recipe,
            int slot,
            int batches,
            List<ItemStack> inventory,
            List<ItemStack> inputs
    ) {
        if (slot == ArcaneInfuserBlockEntity.INPUT_SLOT_COUNT) {
            return true;
        }

        int perBatch = recipe.ingredient(slot).count();
        if (perBatch == 0) {
            inputs.set(slot, ItemStack.EMPTY);
            return assignInput(recipe, slot + 1, batches, inventory, inputs);
        }

        int required = perBatch * batches;
        List<ItemStack> considered = new ArrayList<>();
        for (ItemStack candidate : inventory) {
            if (candidate.isEmpty() || containsSameComponents(considered, candidate)) {
                continue;
            }
            considered.add(candidate.copyWithCount(1));
            if (required > candidate.getMaxStackSize()) {
                continue;
            }

            ItemStack target = candidate.copyWithCount(required);
            if (!recipe.matchesPlacementStack(slot, target)
                    || countMatching(inventory, candidate) < required) {
                continue;
            }

            List<ItemStack> nextInventory = copyInventory(inventory);
            consumeMatching(nextInventory, candidate, required);
            ItemStack previous = inputs.set(slot, target);
            if (assignInput(recipe, slot + 1, batches, nextInventory, inputs)) {
                replaceContents(inventory, nextInventory);
                return true;
            }
            inputs.set(slot, previous);
        }
        return false;
    }

    private static boolean insertIntoInventory(
            List<ItemStack> inventory,
            ItemStack source,
            boolean discardOverflow
    ) {
        ItemStack remainder = source.copy();
        if (remainder.isEmpty()) {
            return true;
        }
        for (ItemStack existing : inventory) {
            if (ItemStack.isSameItemSameComponents(existing, remainder)) {
                int moved = Math.min(remainder.getCount(), existing.getMaxStackSize() - existing.getCount());
                if (moved > 0) {
                    existing.grow(moved);
                    remainder.shrink(moved);
                }
                if (remainder.isEmpty()) {
                    return true;
                }
            }
        }
        for (int slot = 0; slot < inventory.size(); slot++) {
            if (inventory.get(slot).isEmpty()) {
                int moved = Math.min(remainder.getCount(), remainder.getMaxStackSize());
                inventory.set(slot, remainder.copyWithCount(moved));
                remainder.shrink(moved);
                if (remainder.isEmpty()) {
                    return true;
                }
            }
        }
        return discardOverflow;
    }

    private static int countMatching(List<ItemStack> inventory, ItemStack template) {
        int count = 0;
        for (ItemStack stack : inventory) {
            if (ItemStack.isSameItemSameComponents(stack, template)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static void consumeMatching(
            List<ItemStack> inventory,
            ItemStack template,
            int amount
    ) {
        int remaining = amount;
        for (int slot = 0; slot < inventory.size() && remaining > 0; slot++) {
            ItemStack stack = inventory.get(slot);
            if (!ItemStack.isSameItemSameComponents(stack, template)) {
                continue;
            }
            int consumed = Math.min(stack.getCount(), remaining);
            stack.shrink(consumed);
            remaining -= consumed;
            if (stack.isEmpty()) {
                inventory.set(slot, ItemStack.EMPTY);
            }
        }
    }

    private static boolean containsSameComponents(List<ItemStack> stacks, ItemStack candidate) {
        return stacks.stream().anyMatch(stack -> ItemStack.isSameItemSameComponents(stack, candidate));
    }

    private static List<ItemStack> copyInventory(List<ItemStack> source) {
        return source.stream().map(ItemStack::copy).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }

    private static List<ItemStack> emptyInputs() {
        List<ItemStack> inputs = new ArrayList<>(ArcaneInfuserBlockEntity.INPUT_SLOT_COUNT);
        for (int slot = 0; slot < ArcaneInfuserBlockEntity.INPUT_SLOT_COUNT; slot++) {
            inputs.add(ItemStack.EMPTY);
        }
        return inputs;
    }

    private static void replaceContents(List<ItemStack> target, List<ItemStack> source) {
        target.clear();
        target.addAll(source);
    }

    private record PlacementPlan(List<ItemStack> inventory, List<ItemStack> inputs) {
    }

    private static final class OutputSlot extends Slot {
        private final BooleanSupplier canTakeResult;
        private ItemStack pendingResult = ItemStack.EMPTY;

        private OutputSlot(
                Container container,
                int slot,
                int x,
                int y,
                BooleanSupplier canTakeResult
        ) {
            super(container, slot, x, y);
            this.canTakeResult = canTakeResult;
        }

        @Override
        public boolean mayPlace(@NonNull ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(@NonNull Player player) {
            return canTakeResult.getAsBoolean();
        }

        @Override
        public @NonNull ItemStack getItem() {
            return container instanceof ArcaneInfuserBlockEntity infuser
                    ? infuser.visibleResult()
                    : super.getItem();
        }

        @Override
        public @NonNull ItemStack remove(int amount) {
            if (!(container instanceof ArcaneInfuserBlockEntity)) {
                return super.remove(amount);
            }
            if (!canTakeResult.getAsBoolean()) {
                return ItemStack.EMPTY;
            }
            ItemStack visible = getItem();
            if (visible.isEmpty() || amount <= 0) {
                return ItemStack.EMPTY;
            }
            pendingResult = visible.copyWithCount(Math.min(amount, visible.getCount()));
            return pendingResult.copy();
        }

        @Override
        public void setByPlayer(@NonNull ItemStack stack, @NonNull ItemStack previous) {
            if (!(container instanceof ArcaneInfuserBlockEntity)) {
                super.setByPlayer(stack, previous);
            } else if (stack.isEmpty() && !previous.isEmpty()) {
                pendingResult = previous.copy();
            }
        }

        @Override
        public void set(@NonNull ItemStack stack) {
            if (!(container instanceof ArcaneInfuserBlockEntity)) {
                super.set(stack);
            }
        }

        @Override
        public void onTake(@NonNull Player player, @NonNull ItemStack stack) {
            if (container instanceof ArcaneInfuserBlockEntity infuser && !pendingResult.isEmpty()) {
                infuser.takeVisibleResult(pendingResult);
                pendingResult = ItemStack.EMPTY;
            }
            super.onTake(player, stack);
        }

        @Override
        public boolean isFake() {
            return true;
        }
    }
}
