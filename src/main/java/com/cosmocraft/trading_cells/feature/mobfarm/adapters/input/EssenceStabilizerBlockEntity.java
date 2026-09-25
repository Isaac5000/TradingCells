package com.cosmocraft.trading_cells.feature.mobfarm.adapters.input;

import com.cosmocraft.trading_cells.feature.mobfarm.adapters.output.MobFarmRegistrationAdapter;
import com.cosmocraft.trading_cells.platform.neoforge.machine.MachineInsertionLimit;
import java.util.List;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class EssenceStabilizerBlockEntity extends SimulationInventoryBlockEntity implements MenuProvider, MachineInsertionLimit {
    private final RecipeManager.CachedCheck<EssenceStabilizationRecipe.Input, EssenceStabilizationRecipe> recipes =
            RecipeManager.createCheck(MobFarmRegistrationAdapter.STABILIZATION_TYPE.get());
    private int progress;
    private int duration = 100;
    private List<ItemStack> lastInputs = List.of();
    private String activeRecipe = "";
    private static final int[] LEFT_SLOTS = {0, 3, 4, 5}, TOP_SLOTS = {1, 3, 4, 5}, RIGHT_SLOTS = {2, 3, 4, 5}, OUTPUT_SLOTS = {3, 4, 5};
    private final ContainerData data = new ContainerData() {
        public int get(int index) { return index == 0 ? progress : duration; }
        public void set(int index, int value) { }
        public int getCount() { return 2; }
    };
    public EssenceStabilizerBlockEntity(BlockPos pos, BlockState state) {
        super(MobFarmRegistrationAdapter.STABILIZER_BLOCK_ENTITY.get(), pos, state, 6, 3);
    }
    @Override public boolean supportsRedstoneControl() { return false; }
    @Override public int[] getSlotsForFace(Direction side) {
        Direction front = getBlockState().getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING);
        return side == Direction.UP ? TOP_SLOTS : side == front.getClockWise() ? LEFT_SLOTS
                : side == front.getCounterClockWise() ? RIGHT_SLOTS : OUTPUT_SLOTS;
    }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        int[] slots = getSlotsForFace(side);
        return slots != OUTPUT_SLOTS && slots[0] == slot && insertionLimit(slot, stack) > 0;
    }
    @Override public int insertionLimit(int slot, ItemStack stack) {
        if (slot < 0 || slot > 2 || !acceptsInput(slot, stack)) { return 0; }
        if (slot == 0) { return Math.max(0, 1 - items.get(0).getCount()); }
        if (items.get(0).isEmpty() || !(level instanceof ServerLevel server)) { return 0; }
        int tier = EntityEssenceData.tier(items.get(0)).id();
        return server.recipeAccess().getRecipes().stream()
                .filter(holder -> holder.value() instanceof EssenceStabilizationRecipe recipe && recipe.tier() == tier)
                .map(holder -> (EssenceStabilizationRecipe) holder.value())
                .map(recipe -> slot == 1 ? recipe.amethyst() : recipe.reagent())
                .filter(ingredient -> ingredient.ingredient().test(stack))
                .mapToInt(ingredient -> Math.max(0, ingredient.count() - items.get(slot).getCount())).max().orElse(0);
    }
    @Override protected boolean acceptsInput(int slot, ItemStack stack) {
        return switch (slot) {
            case 0 -> stack.is(MobFarmRegistrationAdapter.RAW_ESSENCE.get());
            case 1 -> stack.is(Items.AMETHYST_SHARD);
            case 2 -> stack.is(Items.REDSTONE) || stack.is(Items.GLOWSTONE_DUST)
                    || stack.is(Items.ENDER_PEARL) || stack.is(Items.DRAGON_BREATH);
            default -> false;
        };
    }
    private List<ItemStack> snapshotInputs() { return items.subList(0, 3).stream().map(ItemStack::copy).toList(); }
    @Override public void processTick() {
        if (!(level instanceof ServerLevel server)) { return; }
        boolean changed = lastInputs.size() != 3;
        for (int i = 0; i < lastInputs.size(); i++) { changed |= !ItemStack.matches(lastInputs.get(i), items.get(i)); }
        if (changed) { progress = 0; lastInputs = snapshotInputs(); }
        var input = new EssenceStabilizationRecipe.Input(items.get(0), items.get(1), items.get(2));
        var match = recipes.getRecipeFor(input, server);
        if (match.isEmpty()) { if (progress != 0) { progress = 0; markChangedAndSync(); } return; }
        var holder = match.get();
        var recipe = holder.value();
        String id = holder.id().identifier().toString();
        if (!activeRecipe.equals(id)) { progress = 0; activeRecipe = id; }
        duration = recipe.duration();
        ItemStack core = recipe.assemble(input);
        ItemStack vial = MobFarmRegistrationAdapter.EMPTY_VIAL.get().getDefaultInstance();
        var remainder = items.get(2).getCraftingRemainder();
        ItemStack bottles = remainder == null ? ItemStack.EMPTY : remainder.create();
        if (!bottles.isEmpty()) {
            long count = (long) bottles.getCount() * recipe.reagent().count();
            if (count > bottles.getMaxStackSize()) { return; }
            bottles.setCount((int) count);
        }
        if (core.isEmpty() || !fits(3, core) || !fits(4, vial) || !fits(5, bottles)) { return; }
        if (++progress < duration) { setChanged(); return; }
        items.get(0).shrink(1);
        items.get(1).shrink(recipe.amethyst().count());
        items.get(2).shrink(recipe.reagent().count());
        merge(3, core); merge(4, vial); merge(5, bottles);
        progress = 0;
        lastInputs = snapshotInputs();
        markChangedAndSync();
    }
    private boolean fits(int slot, ItemStack result) {
        ItemStack current = items.get(slot);
        return result.isEmpty() || current.isEmpty() || ItemStack.isSameItemSameComponents(current, result)
                && (long) current.getCount() + result.getCount() <= current.getMaxStackSize();
    }
    private void merge(int slot, ItemStack result) {
        if (result.isEmpty()) { return; }
        if (items.get(slot).isEmpty()) { items.set(slot, result); } else { items.get(slot).grow(result.getCount()); }
    }
    @Override public Component getDisplayName() { return Component.translatable("block.trading_cells.essence_stabilizer"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new EssenceStabilizerMenu(id, inventory, this, data);
    }
    @Override protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("StabilizationProgress", progress);
        output.putString("StabilizationRecipe", activeRecipe);
    }
    @Override protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        progress = Math.max(0, input.getIntOr("StabilizationProgress", 0));
        activeRecipe = input.getStringOr("StabilizationRecipe", "");
        lastInputs = snapshotInputs();
    }
}
