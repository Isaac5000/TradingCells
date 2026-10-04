package com.cosmocraft.trading_cells.feature.mobfarm.adapters.input;

import com.cosmocraft.trading_cells.feature.captures.adapters.api.CapturedMobStackAdapter;
import com.cosmocraft.trading_cells.feature.captures.domain.model.CapturedMobKind;
import com.cosmocraft.trading_cells.feature.mobfarm.adapters.interaction.InteractionHarvestDispatcher;
import com.cosmocraft.trading_cells.feature.mobfarm.adapters.interaction.InteractionHarvestResult;
import com.cosmocraft.trading_cells.feature.mobfarm.adapters.interaction.InteractionHarvestResult.Status;
import com.cosmocraft.trading_cells.feature.mobfarm.adapters.interaction.InteractionOutputPlan;
import com.cosmocraft.trading_cells.feature.mobfarm.adapters.output.MobFarmRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.mobfarm.domain.model.InteractionFarmRules;
import com.cosmocraft.trading_cells.feature.mobfarm.domain.model.InteractionSource;
import com.cosmocraft.trading_cells.platform.neoforge.machine.AbstractPortableMachineBlock;
import com.cosmocraft.trading_cells.platform.neoforge.machine.MachineInsertionLimit;
import com.cosmocraft.trading_cells.shared.machines.domain.model.MachineDiagnosticSnapshot;
import com.cosmocraft.trading_cells.shared.machines.domain.model.MachineDiagnosticStatus;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public final class EntityInteractionFarmBlockEntity extends SimulationInventoryBlockEntity implements MenuProvider, MachineInsertionLimit {
    public static final int WORKER = 0, MODULE = 1, ITEM = 2, TOOL = 3, SPEED = 4, FIRST_OUTPUT = 5;
    public static final int OUTPUT_COUNT = 27, CAPACITY = FIRST_OUTPUT + OUTPUT_COUNT, SIZE = CAPACITY + 1;
    private static final int[] OUTPUTS = IntStream.range(FIRST_OUTPUT, CAPACITY).toArray();
    private static final int[] TOP = slotsWithOutputs(WORKER, MODULE, SPEED, CAPACITY);
    private static final int[] LEFT = slotsWithOutputs(ITEM), RIGHT = slotsWithOutputs(TOOL);
    private final Pending[] pending = new Pending[2];
    private final Status[] statuses = {Status.EMPTY, Status.EMPTY};
    private int progress;
    private int duration = 1200;
    private boolean enabled = true;
    private InteractionSource priority = InteractionSource.ITEM;
    private final ContainerData data = new ContainerData() {
        public int get(int index) {
            return switch (index) {
                case 0 -> progress; case 1 -> duration; case 2 -> enabled ? 1 : 0;
                case 3, 4 -> statuses[index - 3].ordinal();
                case 5 -> 0; // Reserved former redstone field; preserve the menu data layout.
                case 6 -> !enabled ? 1 : 0; default -> 0;
            };
        }
        public void set(int index, int value) { }
        public int getCount() { return 7; }
    };
    private record Pending(ItemStack module, ItemStack input, InteractionHarvestResult result) {
        boolean matches(ItemStack actualModule, ItemStack actualInput) {
            return !actualInput.isEmpty() && ItemStack.matches(module, actualModule)
                    && ItemStack.isSameItemSameComponents(input, actualInput);
        }
    }
    public EntityInteractionFarmBlockEntity(BlockPos pos, BlockState state) {
        super(MobFarmRegistrationAdapter.INTERACTION_BLOCK_ENTITY.get(), pos, state, SIZE, FIRST_OUTPUT);
    }
    public ItemStack worker() { return items.get(WORKER); }
    public ItemStack creature() { return items.get(MODULE); }
    public int progress() { return progress; }
    public int duration() { return duration; }
    public boolean enabled() { return enabled; }
    @Override public boolean supportsRedstoneControl() { return false; }
    public Status status(InteractionSource source) { return statuses[source.ordinal()]; }
    public void toggleEnabled() { enabled = !enabled; markChangedAndSync(); }
    public static boolean accepts(int slot, ItemStack stack) {
        if (stack.isEmpty()) { return false; }
        return switch (slot) {
            case WORKER -> CapturedMobStackAdapter.isFilledCapturer(CapturedMobKind.VILLAGER, stack)
                    && !CapturedMobStackAdapter.isBaby(CapturedMobKind.VILLAGER, stack);
            case MODULE -> stack.is(MobFarmRegistrationAdapter.ENTITY_MODULE.get()) && EntityEssenceData.entityTypeId(stack) != null;
            case ITEM, TOOL -> true;
            case SPEED -> MobFarmUpgradeItem.accepts(stack, MobFarmUpgradeItem.Kind.SPEED);
            case CAPACITY -> MobFarmUpgradeItem.accepts(stack, MobFarmUpgradeItem.Kind.CAPACITY);
            default -> false;
        };
    }
    @Override protected boolean acceptsInput(int slot, ItemStack stack) { return accepts(slot, stack); }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return accepts(slot, stack); }
    @Override public void setItem(int slot, ItemStack stack, boolean transaction) {
        if (!transaction && (slot < FIRST_OUTPUT || slot == CAPACITY) && slot != ITEM && stack.getCount() > 1) { return; }
        super.setItem(slot, stack, transaction);
    }
    @Override public int insertionLimit(int slot, ItemStack stack) {
        return accepts(slot, stack) ? Math.max(0, (slot == ITEM ? Math.min(64, stack.getMaxStackSize()) : 1) - items.get(slot).getCount()) : 0;
    }
    @Override public int[] getSlotsForFace(Direction face) {
        if (face == Direction.UP) { return TOP; }
        Direction front = getBlockState().getValue(AbstractPortableMachineBlock.FACING);
        if (face == front.getClockWise()) { return LEFT; }
        if (face == front.getCounterClockWise()) { return RIGHT; }
        return OUTPUTS;
    }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction face) {
        return Arrays.stream(getSlotsForFace(face)).anyMatch(candidate -> candidate == slot) && insertionLimit(slot, stack) > 0;
    }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction face) { return slot >= FIRST_OUTPUT && slot < CAPACITY; }
    private static int[] slotsWithOutputs(int... inputs) {
        return IntStream.concat(Arrays.stream(inputs), Arrays.stream(OUTPUTS)).toArray();
    }
    @Override public void processTick() {
        if (!(level instanceof ServerLevel server)) { return; }
        for (var source : InteractionSource.values()) {
            int index = source.ordinal();
            if (items.get(source.slot()).isEmpty() && statuses[index] != Status.EMPTY) {
                statuses[index] = Status.EMPTY; markChangedAndSync();
            }
            if (pending[index] != null && !pending[index].matches(creature(), items.get(source.slot()))) {
                pending[index] = null; statuses[index] = Status.EMPTY; markChangedAndSync();
            }
        }
        int nextDuration = InteractionFarmRules.cycleDuration(MobFarmUpgradeItem.tier(items.get(SPEED), MobFarmUpgradeItem.Kind.SPEED));
        if (duration != nextDuration) {
            progress = (int) ((long) progress * nextDuration / duration);
            duration = nextDuration; markChangedAndSync();
        }
        if (!enabled || !accepts(WORKER, worker()) || !accepts(MODULE, creature())) { return; }
        if (items.get(ITEM).isEmpty() && items.get(TOOL).isEmpty()) { return; }
        progress++;
        setChanged();
        if (progress < duration) { return; }
        progress = 0;
        boolean firstReady = true, secondReady = true;
        int operations = InteractionFarmRules.operations(MobFarmUpgradeItem.tier(items.get(CAPACITY), MobFarmUpgradeItem.Kind.CAPACITY));
        for (int operation = 0; operation < operations && (firstReady || secondReady); operation++) {
            if (firstReady) { firstReady = attempt(server, priority); }
            if (secondReady) { secondReady = attempt(server, priority.other()); }
        }
        priority = priority.other();
        markChangedAndSync();
    }
    private boolean attempt(ServerLevel server, InteractionSource source) {
        int index = source.ordinal();
        ItemStack input = items.get(source.slot());
        if (input.isEmpty()) { statuses[index] = Status.EMPTY; return false; }
        Pending prepared = pending[index];
        if (prepared == null) {
            var result = InteractionHarvestDispatcher.harvest(server, creature(), input, source);
            statuses[index] = result.status();
            if (result.status() != Status.READY) { return false; }
            List<ItemStack> empty = java.util.Collections.nCopies(OUTPUT_COUNT, ItemStack.EMPTY);
            if (InteractionOutputPlan.insert(empty, result.products()) == null || result.remainder().getCount() > 1) {
                statuses[index] = Status.TOO_LARGE; return false;
            }
            prepared = new Pending(creature().copy(), input.copyWithCount(1), result);
            pending[index] = prepared;
        }
        var result = prepared.result();
        var outputs = InteractionOutputPlan.insert(items.subList(FIRST_OUTPUT, CAPACITY), result.products());
        if (outputs == null) { statuses[index] = Status.OUTPUT_FULL; return false; }
        ItemStack remainder = result.remainder();
        if (source == InteractionSource.ITEM && !remainder.isEmpty()) {
            if (!ItemStack.isSameItemSameComponents(remainder, input)) { statuses[index] = Status.UNSUPPORTED; pending[index] = null; return false; }
            // A reusable input is charged only if the real interaction consumed it.
        } else if (source == InteractionSource.ITEM) { input.shrink(1); }
        else { items.set(source.slot(), remainder.copy()); }
        for (int slot = 0; slot < outputs.size(); slot++) { items.set(FIRST_OUTPUT + slot, outputs.get(slot)); }
        pending[index] = null;
        statuses[index] = Status.READY;
        return true;
    }
    @Override public MachineDiagnosticSnapshot machineDiagnosticSnapshot() {
        boolean full = Arrays.stream(statuses).anyMatch(status -> status == Status.OUTPUT_FULL);
        boolean ready = accepts(WORKER, worker()) && accepts(MODULE, creature()) && (!items.get(ITEM).isEmpty() || !items.get(TOOL).isEmpty());
        return new MachineDiagnosticSnapshot(!enabled ? MachineDiagnosticStatus.PAUSED : full
                ? MachineDiagnosticStatus.BLOCKED : ready ? MachineDiagnosticStatus.RUNNING : MachineDiagnosticStatus.INACTIVE,
                !enabled ? "disabled" : full ? "output_full" : !accepts(WORKER, worker()) ? "missing_worker" : ready ? "" : "missing_input",
                progress, duration, 0, (int) items.subList(FIRST_OUTPUT, CAPACITY).stream().filter(stack -> !stack.isEmpty()).count(), OUTPUT_COUNT);
    }
    @Override public Component getDisplayName() { return Component.translatable("block.trading_cells.entity_interaction_farm"); }
    @Override public @Nullable AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new EntityInteractionFarmMenu(id, inventory, this, data);
    }
    @Override protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("InteractionProgress", progress); output.putInt("InteractionDuration", duration);
        output.putBoolean("Enabled", enabled); output.putInt("InteractionPriority", priority.ordinal());
        for (var source : InteractionSource.values()) {
            Pending value = pending[source.ordinal()];
            if (value == null) { continue; }
            var child = output.child("Pending" + source.name());
            child.store("Module", ItemStack.CODEC, value.module()); child.store("Input", ItemStack.CODEC, value.input());
            if (!value.result().remainder().isEmpty()) { child.store("Remainder", ItemStack.CODEC, value.result().remainder()); }
            child.store("Products", ItemStack.CODEC.listOf(), value.result().products());
        }
    }
    @Override protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        duration = Math.clamp(input.getIntOr("InteractionDuration", 1200), 20, 1200);
        progress = Math.clamp(input.getIntOr("InteractionProgress", 0), 0, duration - 1);
        enabled = input.getBooleanOr("Enabled", true);
        priority = input.getIntOr("InteractionPriority", 0) == 1 ? InteractionSource.TOOL : InteractionSource.ITEM;
        Arrays.fill(pending, null); Arrays.fill(statuses, Status.EMPTY);
        for (var source : InteractionSource.values()) {
            input.child("Pending" + source.name()).ifPresent(child -> {
                var module = child.read("Module", ItemStack.CODEC).orElse(ItemStack.EMPTY);
                var stack = child.read("Input", ItemStack.CODEC).orElse(ItemStack.EMPTY);
                var remainder = child.read("Remainder", ItemStack.CODEC).orElse(ItemStack.EMPTY);
                var products = child.read("Products", ItemStack.CODEC.listOf()).orElse(List.of());
                if (!module.isEmpty() && !stack.isEmpty() && !products.isEmpty() && products.size() <= 256 && remainder.getCount() <= 1
                        && InteractionOutputPlan.insert(java.util.Collections.nCopies(OUTPUT_COUNT, ItemStack.EMPTY), products) != null) {
                    pending[source.ordinal()] = new Pending(module, stack, new InteractionHarvestResult(remainder, products, Status.READY));
                    statuses[source.ordinal()] = Status.OUTPUT_FULL;
                }
            });
        }
    }
    @Override protected void clearContentsForBlockDrop() {
        super.clearContentsForBlockDrop(); Arrays.fill(pending, null); Arrays.fill(statuses, Status.EMPTY); progress = 0;
    }
}
