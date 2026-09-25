package com.cosmocraft.trading_cells.feature.mobfarm.adapters.input;

import com.cosmocraft.trading_cells.feature.mobfarm.adapters.output.MobFarmRegistrationAdapter;
import com.cosmocraft.trading_cells.platform.neoforge.experience.MachineExperienceAccount;
import com.cosmocraft.trading_cells.platform.neoforge.machine.MachineInsertionLimit;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

public final class EssenceWorkbenchBlockEntity extends SimulationInventoryBlockEntity implements MenuProvider, MachineInsertionLimit {
    private final MachineExperienceAccount experience = new MachineExperienceAccount(this::markChangedAndSync);
    private final ResourceHandler<FluidResource> fluid = experience.handler(this::recipeCost);
    private boolean migrationPending = true;
    private boolean automatic;
    private ItemStack checkedCore = ItemStack.EMPTY;
    private boolean coreValid;
    private static final int[] LEFT_SLOTS = {0, 2, 3}, RIGHT_SLOTS = {1, 2, 3}, OUTPUT_SLOTS = {2, 3};
    private final ContainerData data = new ContainerData() {
        public int get(int index) {
            return switch (index) {
                case 0 -> experience.amount() & 0xFFFF;
                case 1 -> experience.amount() >>> 16;
                case 2 -> experience.fillStorage() ? 1 : 0;
                case 3 -> automatic ? 1 : 0;
                case 4 -> !visibleResult().isEmpty() ? 1 : 0;
                default -> 0;
            };
        }
        public void set(int index, int value) { }
        public int getCount() { return 5; }
    };

    public EssenceWorkbenchBlockEntity(BlockPos pos, BlockState state) {
        // Keep old indices: slot 2 is withdrawal-only recovery for old metals.
        super(MobFarmRegistrationAdapter.WORKBENCH_BLOCK_ENTITY.get(), pos, state, 4, 2);
    }
    @Override protected void slotChanged(int slot) { if (slot == 0) { migrationPending = true; } }
    @Override public void processTick() {
        if (migrationPending && level instanceof ServerLevel server) {
            migrationPending = false;
            if (!items.get(0).isEmpty() && EntityEssenceData.classificationVersion(items.get(0)) == 0
                    && EntityEssenceData.ensureClassified(server, items.get(0))) { markChangedAndSync(); }
        }
        if (automatic) { craft(); }
    }
    @Override public boolean supportsRedstoneControl() { return false; }
    @Override public Component getDisplayName() { return Component.translatable("block.trading_cells.essence_workbench"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new EssenceWorkbenchMenu(id, inventory, this, data);
    }
    public MachineExperienceAccount experience() { return experience; }
    public ResourceHandler<FluidResource> fluidHandler() { return fluid; }
    public boolean automatic() { return automatic; }
    public void toggleAutomatic() { automatic = !automatic; markChangedAndSync(); }
    @Override public int[] getSlotsForFace(Direction side) {
        Direction front = getBlockState().getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING);
        return side == front.getClockWise() ? LEFT_SLOTS : side == front.getCounterClockWise() ? RIGHT_SLOTS : OUTPUT_SLOTS;
    }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        int[] slots = getSlotsForFace(side);
        return slots != OUTPUT_SLOTS && slots[0] == slot && insertionLimit(slot, stack) > 0;
    }
    @Override public int insertionLimit(int slot, ItemStack stack) {
        return slot >= 0 && slot < 2 && acceptsInput(slot, stack) ? Math.max(0, 1 - items.get(slot).getCount()) : 0;
    }
    @Override protected boolean acceptsInput(int slot, ItemStack stack) {
        return slot == 0 ? stack.is(MobFarmRegistrationAdapter.ENTITY_ESSENCE.get())
                && EntityEssenceData.entityTypeId(stack) != null
                : slot == 1 && stack.getItem() instanceof CreatureModelBaseItem;
    }
    public int recipeCost() {
        if (!acceptsInput(0, items.get(0)) || !(items.get(1).getItem() instanceof CreatureModelBaseItem base)
                || base.tier() != EntityEssenceData.tier(items.get(0))) { return 0; }
        return base.tier().modelExperience();
    }
    public boolean synthesize(ServerPlayer player) {
        return stillValid(player) && craft();
    }
    public ItemStack visibleResult() {
        if (!items.get(3).isEmpty()) { return items.get(3).copyWithCount(1); }
        if (!validateCore()) { return ItemStack.EMPTY; }
        int cost = recipeCost();
        var type = EntityEssenceData.entityTypeId(items.get(0));
        if (cost <= 0 || experience.amount() < cost || type == null
                || !net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.containsKey(type)) { return ItemStack.EMPTY; }
        return EntityEssenceData.moduleOf(items.get(0));
    }
    private boolean validateCore() {
        ItemStack core = items.get(0);
        if (core.isEmpty() || !(level instanceof ServerLevel server)) { return false; }
        if (!ItemStack.matches(core, checkedCore)) {
            coreValid = EntityEssenceData.ensureClassified(server, core);
            checkedCore = core.copy();
        }
        return coreValid;
    }
    public boolean takeVisibleResult(ItemStack expected) {
        if (!ItemStack.matches(expected, visibleResult())) { return false; }
        if (items.get(3).isEmpty() && !craft()) { return false; }
        removeItem(3, 1);
        return true;
    }
    private boolean craft() {
        if (!items.get(3).isEmpty() || !validateCore() || recipeCost() <= 0) { return false; }
        int cost = recipeCost();
        if (cost <= 0 || experience.amount() < cost) { return false; }
        ItemStack module = EntityEssenceData.moduleOf(items.get(0));
        if (module.isEmpty()) { return false; }
        items.get(0).shrink(1);
        items.get(1).shrink(1);
        items.set(3, module);
        experience.spend(cost);
        markChangedAndSync();
        return true;
    }
    @Override protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        experience.load(input);
        automatic = input.getBooleanOr("AutomaticSynthesis", false);
        migrationPending = true;
    }
    @Override protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output); experience.save(output); output.putBoolean("AutomaticSynthesis", automatic);
    }
    @Override protected void clearContentsForBlockDrop() { super.clearContentsForBlockDrop(); experience.clear(); automatic = false; }
}
