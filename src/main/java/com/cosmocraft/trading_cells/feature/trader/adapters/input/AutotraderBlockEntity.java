package com.cosmocraft.trading_cells.feature.trader.adapters.input;

import com.cosmocraft.trading_cells.feature.trader.adapters.output.AutotraderRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.trader.application.port.input.AutotraderUseCase;
import com.cosmocraft.trading_cells.feature.trader.domain.model.AutotraderOfferLifecycle;
import com.cosmocraft.trading_cells.feature.trader.domain.model.AutotraderPolicy;
import com.cosmocraft.trading_cells.feature.captures.adapters.api.CapturedMobStackAdapter;
import com.cosmocraft.trading_cells.feature.captures.domain.model.CapturedMobKind;
import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.FeatureComposition;
import com.cosmocraft.trading_cells.feature.trader.adapters.minecraft.VillagerPoiAdapter;
import com.cosmocraft.trading_cells.feature.trader.adapters.minecraft.TemporaryTradeDiscountStore;
import com.cosmocraft.trading_cells.feature.trader.domain.service.TradeDiscountPolicy;
import com.cosmocraft.trading_cells.platform.neoforge.machine.AbstractPortableMachineBlock;
import com.cosmocraft.trading_cells.platform.neoforge.machine.OrderedOutputInserter;
import com.cosmocraft.trading_cells.platform.neoforge.machine.PortableMachineBlockEntity;
import com.cosmocraft.trading_cells.platform.neoforge.fluid.ExperienceFluidHandler;
import com.cosmocraft.trading_cells.platform.neoforge.registration.ExperienceFluidRegistration;
import com.cosmocraft.trading_cells.platform.neoforge.trading.MerchantOfferComparator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.npc.villager.VillagerType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.TagValueInput;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class AutotraderBlockEntity extends PortableMachineBlockEntity implements WorldlyContainer, MenuProvider { // NOSONAR - Minecraft fixes this hierarchy and BlockEntity uses identity, not value-based equals.
    public static final int VILLAGER_SLOT = 0;
    public static final int FIRST_INPUT_A_SLOT = 1;
    public static final int FIRST_INPUT_B_SLOT = FIRST_INPUT_A_SLOT + AutotraderPolicy.INPUT_SLOTS_PER_COST;
    public static final int FIRST_OUTPUT_SLOT = FIRST_INPUT_B_SLOT + AutotraderPolicy.INPUT_SLOTS_PER_COST;
    public static final int CONTAINER_SIZE = FIRST_OUTPUT_SLOT + AutotraderPolicy.OUTPUT_SLOTS;

    private static final String SLOT_TAG_PREFIX = "Slot";
    private static final String SELECTED_OFFER_TAG = "SelectedOffer";
    private static final String POI_STACK_TAG = "StoredPoi";
    private static final String STORED_EXPERIENCE_TAG = "StoredExperience";
    private static final String OFFER_AGE_TICKS_TAG = "OfferAgeTicks";
    private static final String RESTOCK_CHECK_TICKS_TAG = "RestockCheckTicks";
    private static final String CURE_DISCOUNT_TAG = "TradingCellsCureDiscount";
    private static final int[] INPUT_A_SLOTS = new int[]{1, 2, 3, 4};
    private static final int[] INPUT_B_SLOTS = new int[]{5, 6, 7, 8};
    private static final int[] OUTPUT_SLOTS = new int[]{9, 10, 11, 12};
    private static final int[] NO_SLOTS = new int[0];
    private static final int RESTOCK_CHECK_INTERVAL_TICKS = 300;

    private final AutotraderUseCase autotraderService = FeatureComposition.autotrader();
    private final NonNullList<ItemStack> items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
    private final ExperienceFluidHandler experienceFluidHandler = new ExperienceFluidHandler(
            () -> FluidResource.of(ExperienceFluidRegistration.SOURCE.get()),
            this::getStoredExperienceForFluid,
            this::setStoredExperienceFromFluid,
            () -> Integer.MAX_VALUE,
            false,
            this::markChangedAndSync
    );
    private int selectedOfferIndex;
    private int storedExperience;
    private int offerAgeTicks;
    private int restockCheckTicks;
    private int offersRevision;
    private long nextTemporaryDiscountExpiry = Long.MAX_VALUE;
    private boolean emptyOffersInitializationAttempted;
    private ItemStack storedPoiStack = ItemStack.EMPTY;
    private @Nullable AutotraderVillager cachedVillager;
    private ItemStack cachedVillagerStack = ItemStack.EMPTY;

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> selectedOfferIndex;
                case 1 -> storedExperience;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) {
                int count = offerCount();
                selectedOfferIndex = autotraderService.normalizeSelection(value, count);
                markChangedAndSync();
            }
        }

        @Override
        public int getCount() {
            return 2;
        }

        private int offerCount() {
            Villager villager = resolveVillager();
            return villager == null ? 0 : villager.getOffers().size();
        }
    };

    public AutotraderBlockEntity(BlockPos pos, BlockState state) {
        super(AutotraderRegistrationAdapter.AUTOTRADER_BLOCK_ENTITY.get(), pos, state);
    }

    public ContainerData dataAccess() {
        return dataAccess;
    }

    public boolean hasStoredVillager() {
        return isAdultVillager(items.get(VILLAGER_SLOT));
    }

    public ItemStack copyDisplayVillagerStack() {
        return items.get(VILLAGER_SLOT).copy();
    }

    public ItemStack copyPoiStack() {
        return storedPoiStack.copy();
    }

    public ResourceHandler<FluidResource> experienceFluidHandler() {
        return experienceFluidHandler;
    }

    public void extractExperience(Player player) {
        extractExperience(player, false);
    }

    public void extractExperienceToNextLevel(Player player) {
        extractExperience(player, true);
    }

    private void extractExperience(Player player, boolean nextLevelOnly) {
        if (level == null || level.isClientSide() || storedExperience <= 0) {
            return;
        }
        int extracted = storedExperience;
        if (nextLevelOnly) {
            int needed = Math.max(
                    1,
                    net.minecraft.util.Mth.ceil((1.0F - player.experienceProgress) * player.getXpNeededForNextLevel())
            );
            extracted = Math.min(extracted, needed);
        }
        storedExperience -= extracted;
        player.giveExperiencePoints(extracted);
        markChangedAndSync();
    }

    private void setStoredExperienceFromFluid(int amount) {
        storedExperience = Math.max(0, amount);
    }

    private int getStoredExperienceForFluid() {
        return storedExperience;
    }

    public boolean resetTransientTrades(Player player) {
        if (!(level instanceof ServerLevel) || !canResetTrades()) {
            return false;
        }
        AutotraderVillager villager = resolveVillager();
        if (villager == null || villager.getVillagerXp() != 0) {
            return false;
        }

        List<ItemStack> buffers = collectInputBuffers();
        List<ItemStack> resultingInventory = simulatePlayerInventoryAfterAdding(player, buffers);
        if (resultingInventory == null) {
            return false;
        }

        MerchantOffers previousOffers = villager.getOffers().copy();
        MerchantOffer previouslySelected = previousOffers.isEmpty()
                ? null
                : previousOffers.get(Math.floorMod(selectedOfferIndex, previousOffers.size()));
        CompoundTag previousData = CapturedMobStackAdapter.copyData(
                CapturedMobKind.VILLAGER,
                items.get(VILLAGER_SLOT)
        );
        if (previousData == null) {
            return false;
        }
        previousData = previousData.copy();

        String professionId = VillagerPoiAdapter.professionFor(level, storedPoiStack);
        CompoundTag refreshedData = previousData.copy();
        if (!VillagerPoiAdapter.clearTransientProfession(refreshedData)
                || !VillagerPoiAdapter.refreshProfession(refreshedData, professionId)) {
            return false;
        }

        CapturedMobStackAdapter.setData(
                CapturedMobKind.VILLAGER,
                items.get(VILLAGER_SLOT),
                refreshedData
        );
        invalidateVillagerCache();
        AutotraderVillager refreshedVillager = resolveVillager();
        if (refreshedVillager == null || refreshedVillager.getOffers().isEmpty()) {
            CapturedMobStackAdapter.setData(
                    CapturedMobKind.VILLAGER,
                    items.get(VILLAGER_SLOT),
                    previousData
            );
            invalidateVillagerCache();
            resolveVillager();
            return false;
        }

        MerchantOffers refreshedOffers = refreshedVillager.getOffers();
        commitPlayerInventory(player, resultingInventory);
        clearInputBuffers();
        selectedOfferIndex = MerchantOfferComparator.findEquivalentIndex(refreshedOffers, previouslySelected);
        offerAgeTicks = 0;
        restockCheckTicks = 0;
        prepareAutomaticPrices(refreshedVillager);
        persistCachedVillager();
        markOffersChanged();
        return true;
    }

    public boolean trySelectOffer(Player player, int offerIndex) {
        if (level == null || level.isClientSide()) {
            return false;
        }
        AutotraderVillager villager = resolveVillager();
        if (villager == null) {
            return false;
        }
        MerchantOffers offers = villager.getOffers();
        if (offerIndex < 0 || offerIndex >= offers.size()) {
            return false;
        }
        MerchantOffer candidate = offers.get(offerIndex);
        List<Integer> incompatibleSlots = new ArrayList<>();
        List<ItemStack> toReturn = new ArrayList<>();
        for (int slot : INPUT_A_SLOTS) {
            ItemStack stack = items.get(slot);
            if (!stack.isEmpty() && !candidate.getItemCostA().test(stack)) {
                incompatibleSlots.add(slot);
                toReturn.add(stack.copy());
            }
        }
        var secondCost = candidate.getItemCostB();
        for (int slot : INPUT_B_SLOTS) {
            ItemStack stack = items.get(slot);
            if (!stack.isEmpty() && secondCost.map(cost -> !cost.test(stack)).orElse(true)) {
                incompatibleSlots.add(slot);
                toReturn.add(stack.copy());
            }
        }
        List<ItemStack> resultingInventory = simulatePlayerInventoryAfterAdding(player, toReturn);
        if (resultingInventory == null) {
            return false;
        }
        commitPlayerInventory(player, resultingInventory);
        for (int slot : incompatibleSlots) {
            items.set(slot, ItemStack.EMPTY);
        }
        selectedOfferIndex = offerIndex;
        markChangedAndSync();
        return true;
    }

    private List<ItemStack> collectInputBuffers() {
        List<ItemStack> result = new ArrayList<>();
        for (int slot : INPUT_A_SLOTS) {
            if (!items.get(slot).isEmpty()) {
                result.add(items.get(slot).copy());
            }
        }
        for (int slot : INPUT_B_SLOTS) {
            if (!items.get(slot).isEmpty()) {
                result.add(items.get(slot).copy());
            }
        }
        return result;
    }

    private void clearInputBuffers() {
        for (int slot : INPUT_A_SLOTS) {
            items.set(slot, ItemStack.EMPTY);
        }
        for (int slot : INPUT_B_SLOTS) {
            items.set(slot, ItemStack.EMPTY);
        }
    }

    private static @Nullable List<ItemStack> simulatePlayerInventoryAfterAdding(Player player, List<ItemStack> stacks) {
        List<ItemStack> simulated = new ArrayList<>(36);
        for (int index = 0; index < 36; index++) {
            simulated.add(player.getInventory().getItem(index).copy());
        }
        for (ItemStack source : stacks) {
            ItemStack remaining = source.copy();
            mergeIntoExistingStacks(simulated, remaining);
            fillEmptySlots(simulated, remaining);
            if (!remaining.isEmpty()) {
                return null;
            }
        }
        return simulated;
    }

    private static void mergeIntoExistingStacks(List<ItemStack> simulated, ItemStack remaining) {
        for (ItemStack target : simulated) {
            if (remaining.isEmpty()) {
                return;
            }
            if (target.isEmpty() || !ItemStack.isSameItemSameComponents(target, remaining)) {
                continue;
            }
            int move = Math.min(remaining.getCount(), target.getMaxStackSize() - target.getCount());
            if (move > 0) {
                target.grow(move);
                remaining.shrink(move);
            }
        }
    }

    private static void fillEmptySlots(List<ItemStack> simulated, ItemStack remaining) {
        for (int index = 0; index < simulated.size() && !remaining.isEmpty(); index++) {
            if (!simulated.get(index).isEmpty()) {
                continue;
            }
            int move = Math.min(remaining.getCount(), remaining.getMaxStackSize());
            simulated.set(index, remaining.copyWithCount(move));
            remaining.shrink(move);
        }
    }

    private static void commitPlayerInventory(Player player, List<ItemStack> simulated) {
        for (int index = 0; index < 36; index++) {
            player.getInventory().setItem(index, simulated.get(index));
        }
        player.getInventory().setChanged();
    }

    public InteractionResult insertVillagerFromCapturer(ItemStack stack) {
        if (hasStoredVillager()) {
            return InteractionResult.SUCCESS_SERVER;
        }
        if (!isAdultVillager(stack)) {
            return InteractionResult.PASS;
        }

        items.set(VILLAGER_SLOT, stack.copyWithCount(1));
        CapturedMobStackAdapter.clearData(CapturedMobKind.VILLAGER, stack);
        selectedOfferIndex = 0;
        offerAgeTicks = 0;
        restockCheckTicks = 0;
        emptyOffersInitializationAttempted = false;
        invalidateVillagerCache();
        refreshVillagerProfessionFromPoi();
        ensureOffersInitialized();
        persistCachedVillager();
        markOffersChanged();
        return InteractionResult.SUCCESS_SERVER;
    }

    public InteractionResult extractVillagerToCapturer(ItemStack stack, Player player) {
        if (!hasStoredVillager()) {
            return InteractionResult.SUCCESS_SERVER;
        }
        if (CapturedMobStackAdapter.isFilledCapturer(CapturedMobKind.VILLAGER, stack)) {
            return InteractionResult.SUCCESS_SERVER;
        }

        persistCachedVillager();
        CompoundTag data = CapturedMobStackAdapter.copyData(
                CapturedMobKind.VILLAGER,
                items.get(VILLAGER_SLOT)
        );
        if (data == null) {
            return InteractionResult.FAIL;
        }
        ItemStack target = stack.getCount() <= 1 ? stack : new ItemStack(stack.getItem());
        CapturedMobStackAdapter.setData(CapturedMobKind.VILLAGER, target, data);
        if (target != stack) {
            stack.shrink(1);
            if (!player.getInventory().add(target)) {
                player.drop(target, false);
            }
        }

        items.set(VILLAGER_SLOT, ItemStack.EMPTY);
        selectedOfferIndex = 0;
        offerAgeTicks = 0;
        restockCheckTicks = 0;
        emptyOffersInitializationAttempted = false;
        invalidateVillagerCache();
        markOffersChanged();
        return InteractionResult.SUCCESS_SERVER;
    }

    public InteractionResult insertPoiFromStack(ItemStack stack, Player player) {
        if (level == null) {
            return InteractionResult.PASS;
        }
        String professionId = VillagerPoiAdapter.professionFor(level, stack);
        if (professionId == null) {
            return InteractionResult.PASS;
        }

        persistCachedVillager();
        CompoundTag villagerData = CapturedMobStackAdapter.copyData(
                CapturedMobKind.VILLAGER,
                items.get(VILLAGER_SLOT)
        );
        if (!storedPoiStack.isEmpty()) {
            if (villagerData != null && VillagerPoiAdapter.hasPersistentProfession(villagerData)) {
                return InteractionResult.SUCCESS_SERVER;
            }
            ItemStack previous = storedPoiStack.copy();
            if (!player.getInventory().add(previous)) {
                return InteractionResult.SUCCESS_SERVER;
            }
        }

        storedPoiStack = stack.copyWithCount(1);
        stack.shrink(1);
        refreshVillagerProfessionFromPoi();
        ensureOffersInitialized();
        persistCachedVillager();
        markOffersChanged();
        return InteractionResult.SUCCESS_SERVER;
    }

    public InteractionResult extractPoiToPlayer(Player player) {
        if (storedPoiStack.isEmpty()) {
            return InteractionResult.SUCCESS_SERVER;
        }
        ItemStack returned = storedPoiStack.copy();
        if (!player.getInventory().add(returned)) {
            return InteractionResult.SUCCESS_SERVER;
        }
        storedPoiStack = ItemStack.EMPTY;
        clearTransientProfession();
        markOffersChanged();
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    public void processTick() {
        if (level == null || level.isClientSide()) {
            return;
        }
        if (items.get(VILLAGER_SLOT).isEmpty()) {
            return;
        }
        AutotraderVillager villager = resolveVillager();
        refreshExpiredTemporaryDiscounts(villager);
        tickOfferAge(villager);
        tickVillagerRestock(villager);
        MerchantOffer offer = selectedOffer(villager);
        if (villager == null || offer == null || offer.isOutOfStock()) {
            return;
        }

        ItemCost costA = offer.getItemCostA();
        @Nullable ItemCost costB = offer.getItemCostB().orElse(null);
        if (countMatching(INPUT_A_SLOTS, costA) < offer.getCostA().getCount()
                || costB != null && countMatching(INPUT_B_SLOTS, costB) < offer.getCostB().getCount()
                || !canStoreOutput(offer.getResult())) {
            return;
        }

        ItemStack result = offer.assemble();
        int previousVillagerLevel = villager.getVillagerData().level();
        consume(INPUT_A_SLOTS, costA, offer.getCostA().getCount());
        if (costB != null) {
            consume(INPUT_B_SLOTS, costB, offer.getCostB().getCount());
        }
        storeOutput(result);
        storedExperience = (int) Math.min(
                Integer.MAX_VALUE,
                (long) storedExperience + villager.completeAutomaticTrade(offer)
        );
        prepareAutomaticPrices(villager);
        persistCachedVillager();
        markAutomaticTradeChanged(previousVillagerLevel != villager.getVillagerData().level());
    }

    public @Nullable MerchantOffer selectedOffer() {
        return selectedOffer(resolveVillager());
    }

    private @Nullable MerchantOffer selectedOffer(@Nullable Villager villager) {
        if (villager == null) {
            return null;
        }
        MerchantOffers offers = villager.getOffers();
        if (offers.isEmpty()) {
            selectedOfferIndex = 0;
            return null;
        }
        selectedOfferIndex = autotraderService.normalizeSelection(selectedOfferIndex, offers.size());
        return offers.get(selectedOfferIndex);
    }

    public MerchantOffers offersSnapshot() {
        return offersView().copy();
    }

    MerchantOffers offersView() {
        AutotraderVillager villager = resolveVillager();
        return villager == null ? new MerchantOffers() : villager.getOffers();
    }

    public VillagerData villagerDataSnapshot() {
        AutotraderVillager villager = resolveVillager();
        if (villager != null) {
            return villager.getVillagerData();
        }
        CompoundTag data = CapturedMobStackAdapter.copyData(
                CapturedMobKind.VILLAGER,
                items.get(VILLAGER_SLOT)
        );
        if (data != null) {
            VillagerData decoded = data.read("VillagerData", VillagerData.CODEC).orElse(null);
            if (decoded != null) {
                return decoded;
            }
        }
        return new VillagerData(
                BuiltInRegistries.VILLAGER_TYPE.getOrThrow(VillagerType.PLAINS),
                BuiltInRegistries.VILLAGER_PROFESSION.getOrThrow(VillagerProfession.NONE),
                1
        );
    }

    public int villagerXpSnapshot() {
        AutotraderVillager villager = resolveVillager();
        return villager == null ? 0 : Math.max(0, villager.getVillagerXp());
    }

    public boolean canResetTradesForMenu() {
        return canResetTrades();
    }

    public int offersRevisionSnapshot() {
        return offersRevision;
    }

    @Override
    public @NonNull Component getDisplayName() {
        return Component.translatable("container.trading_cells.autotrader");
    }

    @Override
    public @NonNull AbstractContainerMenu createMenu(
            int containerId,
            @NonNull Inventory inventory,
            @NonNull Player player // NOSONAR - MenuProvider fixes this parameter even when this menu needs only the inventory.
    ) {
        if (level != null && !level.isClientSide()) {
            boolean offersChanged = ensureOffersInitialized();
            persistCachedVillager();
            if (offersChanged) {
                markOffersChanged();
            } else {
                markChangedAndSync();
            }
        }
        return new AutotraderMenu(containerId, inventory, this, dataAccess);
    }

    @Override
    public int getContainerSize() {
        return CONTAINER_SIZE;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public @NonNull ItemStack getItem(int slot) {
        return isValidSlot(slot) ? items.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public @NonNull ItemStack removeItem(int slot, int count) {
        if (!isValidSlot(slot) || count <= 0 || items.get(slot).isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = items.get(slot).split(count);
        if (items.get(slot).isEmpty()) {
            items.set(slot, ItemStack.EMPTY);
        }
        if (slot == VILLAGER_SLOT) {
            invalidateVillagerCache();
            selectedOfferIndex = 0;
            offerAgeTicks = 0;
            restockCheckTicks = 0;
            emptyOffersInitializationAttempted = false;
        }
        markChangedAndSync();
        return removed;
    }

    @Override
    public @NonNull ItemStack removeItemNoUpdate(int slot) {
        if (!isValidSlot(slot)) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = items.get(slot);
        items.set(slot, ItemStack.EMPTY);
        if (slot == VILLAGER_SLOT) {
            invalidateVillagerCache();
            selectedOfferIndex = 0;
            offerAgeTicks = 0;
            restockCheckTicks = 0;
            emptyOffersInitializationAttempted = false;
        }
        return removed;
    }

    @Override
    public void setItem(int slot, @NonNull ItemStack stack) {
        if (!isValidSlot(slot) || !stack.isEmpty() && !canPlaceItem(slot, stack)) {
            return;
        }
        ItemStack inserted = stack.copy();
        if (slot == VILLAGER_SLOT) {
            inserted.setCount(Math.min(1, inserted.getCount()));
        } else {
            inserted.setCount(Math.min(inserted.getMaxStackSize(), inserted.getCount()));
        }
        items.set(slot, inserted);
        if (slot == VILLAGER_SLOT) {
            invalidateVillagerCache();
            selectedOfferIndex = 0;
            offerAgeTicks = 0;
            restockCheckTicks = 0;
            emptyOffersInitializationAttempted = false;
        }
        markChangedAndSync();
    }

    @Override
    public boolean stillValid(@NonNull Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public boolean canPlaceItem(int slot, @NonNull ItemStack stack) {
        if (slot == VILLAGER_SLOT) {
            return isAdultVillager(stack);
        }
        MerchantOffer offer = selectedOffer();
        if (offer == null) {
            return false;
        }
        if (isInputASlot(slot)) {
            return offer.getItemCostA().test(stack);
        }
        if (isInputBSlot(slot)) {
            return offer.getItemCostB().map(cost -> cost.test(stack)).orElse(false);
        }
        return false;
    }

    @Override
    public void clearContent() {
        clearContentsForBlockDrop();
        markChangedAndSync();
    }

    @Override
    public int @NonNull [] getSlotsForFace(@NonNull Direction direction) {
        if (direction == Direction.DOWN) {
            return OUTPUT_SLOTS;
        }
        Direction facing = getBlockState().getValue(AbstractPortableMachineBlock.FACING);
        if (direction == facing.getClockWise()) {
            return INPUT_A_SLOTS;
        }
        if (direction == facing.getCounterClockWise()) {
            return INPUT_B_SLOTS;
        }
        return NO_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace( // NOSONAR - Minecraft explicitly permits a null automation side.
            int slot,
            @NonNull ItemStack stack,
            @Nullable Direction direction
    ) {
        if (direction == null) {
            return false;
        }
        for (int allowedSlot : getSlotsForFace(direction)) {
            if (allowedSlot == slot) {
                return canPlaceItem(slot, stack);
            }
        }
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, @NonNull ItemStack stack, @NonNull Direction direction) {
        return direction == Direction.DOWN && isOutputSlot(slot);
    }

    @Override
    protected void loadAdditional(@NonNull ValueInput input) {
        super.loadAdditional(input);
        for (int slot = 0; slot < CONTAINER_SIZE; slot++) {
            items.set(slot, input.read(SLOT_TAG_PREFIX + slot, ItemStack.CODEC).orElse(ItemStack.EMPTY));
        }
        storedPoiStack = input.read(POI_STACK_TAG, ItemStack.CODEC).orElse(ItemStack.EMPTY);
        selectedOfferIndex = Math.max(0, input.getIntOr(SELECTED_OFFER_TAG, 0));
        storedExperience = Math.max(0, input.getIntOr(STORED_EXPERIENCE_TAG, 0));
        offerAgeTicks = Math.clamp(
                input.getIntOr(OFFER_AGE_TICKS_TAG, 0),
                0,
                autotraderService.offerRefreshTicks()
        );
        restockCheckTicks = Math.clamp(
                input.getIntOr(RESTOCK_CHECK_TICKS_TAG, 0),
                0,
                RESTOCK_CHECK_INTERVAL_TICKS - 1
        );
        // Never persist a failed initialization attempt. Older worlds may contain
        // Offers:[] plus the legacy flag, which otherwise blocks vanilla generation forever.
        emptyOffersInitializationAttempted = false;
        invalidateVillagerCache();
    }

    @Override
    protected void saveAdditional(@NonNull ValueOutput output) {
        super.saveAdditional(output);
        for (int slot = 0; slot < CONTAINER_SIZE; slot++) {
            if (!items.get(slot).isEmpty()) {
                output.store(SLOT_TAG_PREFIX + slot, ItemStack.CODEC, items.get(slot));
            }
        }
        if (selectedOfferIndex > 0) {
            output.putInt(SELECTED_OFFER_TAG, selectedOfferIndex);
        }
        if (!storedPoiStack.isEmpty()) {
            output.store(POI_STACK_TAG, ItemStack.CODEC, storedPoiStack);
        }
        if (storedExperience > 0) {
            output.putInt(STORED_EXPERIENCE_TAG, storedExperience);
        }
        if (offerAgeTicks > 0) {
            output.putInt(OFFER_AGE_TICKS_TAG, offerAgeTicks);
        }
        if (restockCheckTicks > 0) {
            output.putInt(RESTOCK_CHECK_TICKS_TAG, restockCheckTicks);
        }
    }

    @Override
    protected void beforeBlockDropSnapshot() {
        persistCachedVillager();
    }

    @Override
    protected void clearContentsForBlockDrop() {
        for (int slot = 0; slot < CONTAINER_SIZE; slot++) {
            items.set(slot, ItemStack.EMPTY);
        }
        storedPoiStack = ItemStack.EMPTY;
        selectedOfferIndex = 0;
        storedExperience = 0;
        offerAgeTicks = 0;
        restockCheckTicks = 0;
        emptyOffersInitializationAttempted = false;
        invalidateVillagerCache();
        setChanged();
    }

    private void markOffersChanged() {
        int offerCount = cachedVillager == null ? 0 : cachedVillager.getOffers().size();
        selectedOfferIndex = autotraderService.normalizeSelection(selectedOfferIndex, offerCount);
        offersRevision++;
        markChangedAndSync();
    }

    private void markAutomaticTradeChanged(boolean visualStateChanged) {
        int offerCount = cachedVillager == null ? 0 : cachedVillager.getOffers().size();
        selectedOfferIndex = autotraderService.normalizeSelection(selectedOfferIndex, offerCount);
        offersRevision++;
        if (visualStateChanged) {
            markChangedAndSync();
        } else {
            setChanged();
        }
    }

    private boolean canResetTrades() {
        if (level == null || level.isClientSide() || !hasStoredVillager() || storedPoiStack.isEmpty()) {
            return false;
        }
        CompoundTag villagerData = CapturedMobStackAdapter.copyData(
                CapturedMobKind.VILLAGER,
                items.get(VILLAGER_SLOT)
        );
        if (villagerData == null
                || VillagerPoiAdapter.hasPersistentProfession(villagerData)
                || VillagerPoiAdapter.professionFor(level, storedPoiStack) == null) {
            return false;
        }
        AutotraderVillager villager = resolveVillager();
        return villager != null
                && villager.getVillagerXp() <= 0
                && !villager.getOffers().isEmpty();
    }

    private boolean ensureOffersInitialized() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }
        CompoundTag currentData = CapturedMobStackAdapter.copyData(
                CapturedMobKind.VILLAGER,
                items.get(VILLAGER_SLOT)
        );
        if (currentData == null) {
            return false;
        }

        Optional<MerchantOffers> serializedOffers = currentData.read("Offers", MerchantOffers.CODEC);
        boolean offersWereAbsent = serializedOffers.isEmpty();
        AutotraderVillager villager = resolveVillager();
        if (villager == null) {
            return false;
        }

        var profession = villager.getVillagerData().profession();
        boolean employed = !profession.is(VillagerProfession.NONE)
                && !profession.is(VillagerProfession.NITWIT);
        AutotraderOfferLifecycle.Decision decision = autotraderService.offers(
                !villager.isBaby(),
                employed,
                !villager.getOffers().isEmpty()
        );
        if (decision == AutotraderOfferLifecycle.Decision.KEEP) {
            if (emptyOffersInitializationAttempted) {
                emptyOffersInitializationAttempted = false;
                setChanged();
            }
            return offersWereAbsent;
        }
        if (decision == AutotraderOfferLifecycle.Decision.UNAVAILABLE
                || emptyOffersInitializationAttempted) {
            return false;
        }

        emptyOffersInitializationAttempted = true;
        setChanged();
        if (!villager.forceInitializeOffers(serverLevel)) {
            return false;
        }

        prepareAutomaticPrices(villager);
        persistCachedVillager();
        return true;
    }

    private @Nullable AutotraderVillager resolveVillager() {
        if (level == null || level.isClientSide()) {
            return null;
        }
        ItemStack villagerStack = items.get(VILLAGER_SLOT);
        if (cachedVillager != null
                && ItemStack.isSameItemSameComponents(cachedVillagerStack, villagerStack)) {
            return cachedVillager;
        }
        CompoundTag currentData = CapturedMobStackAdapter.copyData(
                CapturedMobKind.VILLAGER,
                villagerStack
        );
        if (currentData == null) {
            invalidateVillagerCache();
            return null;
        }
        AutotraderVillager villager = new AutotraderVillager(level, autotraderService);
        villager.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), currentData.copy()));
        villager.setPos(worldPosition.getX() + 0.5D, worldPosition.getY(), worldPosition.getZ() + 0.5D);
        villager.setPersistenceRequired();
        villager.getOffers();
        if (!level.isClientSide()) {
            prepareAutomaticPrices(villager);
        }
        cachedVillager = villager;
        cachedVillagerStack = villagerStack.copy();
        persistCachedVillager();
        return cachedVillager;
    }

    private boolean refreshVillagerProfessionFromPoi() {
        persistCachedVillager();
        CompoundTag data = CapturedMobStackAdapter.copyData(
                CapturedMobKind.VILLAGER,
                items.get(VILLAGER_SLOT)
        );
        if (data == null || level == null) {
            return false;
        }
        boolean changed = VillagerPoiAdapter.refreshProfession(
                data,
                VillagerPoiAdapter.professionFor(level, storedPoiStack)
        );
        CapturedMobStackAdapter.setData(CapturedMobKind.VILLAGER, items.get(VILLAGER_SLOT), data);
        if (changed) {
            selectedOfferIndex = 0;
            offerAgeTicks = 0;
            restockCheckTicks = 0;
            emptyOffersInitializationAttempted = false;
            invalidateVillagerCache();
        }
        return changed;
    }

    private boolean clearTransientProfession() {
        persistCachedVillager();
        CompoundTag data = CapturedMobStackAdapter.copyData(
                CapturedMobKind.VILLAGER,
                items.get(VILLAGER_SLOT)
        );
        if (data == null) {
            return false;
        }
        boolean changed = VillagerPoiAdapter.clearTransientProfession(data);
        CapturedMobStackAdapter.setData(CapturedMobKind.VILLAGER, items.get(VILLAGER_SLOT), data);
        if (changed) {
            selectedOfferIndex = 0;
            offerAgeTicks = 0;
            restockCheckTicks = 0;
            emptyOffersInitializationAttempted = false;
            invalidateVillagerCache();
        }
        return changed;
    }

    private void prepareAutomaticPrices(Villager villager) {
        int careerDiscount = Math.max(0, villager.getVillagerData().level() - 1);
        int cureDiscount = Math.max(0, villager.getPersistentData().getInt(CURE_DISCOUNT_TAG).orElse(0));
        MerchantOffers offers = villager.getOffers();
        long gameTime = level == null ? 0L : level.getGameTime();
        TemporaryTradeDiscountStore.ActiveDiscounts temporaryDiscounts =
                TemporaryTradeDiscountStore.activeDiscounts(
                        villager.getPersistentData(),
                        offers,
                        gameTime,
                        villager.registryAccess()
                );
        nextTemporaryDiscountExpiry = temporaryDiscounts.nextExpiry();
        for (MerchantOffer offer : offers) {
            offer.resetSpecialPriceDiff();
            long totalDiscount = (long) careerDiscount + cureDiscount;
            if (temporaryDiscounts.appliesTo(offer)) {
                totalDiscount += TradeDiscountPolicy.TEMPORARY_DISCOUNT_PER_OFFER;
            }
            int discount = (int) Math.clamp(totalDiscount, 0L, Integer.MAX_VALUE);
            int applied = Math.clamp(discount, 0, offer.getBaseCostA().getCount() - 1);
            offer.addToSpecialPriceDiff(-applied);
        }
    }


    private void persistCachedVillager() {
        if (cachedVillager == null || level == null || level.isClientSide()) {
            return;
        }
        CompoundTag saved = CapturedMobStackAdapter.createVillagerData(cachedVillager);
        CapturedMobStackAdapter.setData(CapturedMobKind.VILLAGER, items.get(VILLAGER_SLOT), saved);
        cachedVillagerStack = items.get(VILLAGER_SLOT).copy();
        setChanged();
    }

    private void invalidateVillagerCache() {
        cachedVillager = null;
        cachedVillagerStack = ItemStack.EMPTY;
        nextTemporaryDiscountExpiry = Long.MAX_VALUE;
    }

    private void refreshExpiredTemporaryDiscounts(@Nullable AutotraderVillager villager) {
        if (villager == null
                || level == null
                || level.getGameTime() < nextTemporaryDiscountExpiry) {
            return;
        }
        prepareAutomaticPrices(villager);
        persistCachedVillager();
        markOffersChanged();
    }

    private void tickVillagerRestock(@Nullable AutotraderVillager villager) {
        if (!(level instanceof ServerLevel serverLevel) || villager == null || villager.getOffers().isEmpty()) {
            if (restockCheckTicks != 0) {
                restockCheckTicks = 0;
                setChanged();
            }
            return;
        }

        if (autotraderService.infiniteTrades()) {
            restockCheckTicks = 0;
            if (resetUsedOffers(villager.getOffers())) {
                prepareAutomaticPrices(villager);
                persistCachedVillager();
                markOffersChanged();
            }
            return;
        }

        restockCheckTicks++;
        if (restockCheckTicks < RESTOCK_CHECK_INTERVAL_TICKS) {
            if (restockCheckTicks % 20 == 0) {
                setChanged();
            }
            return;
        }

        restockCheckTicks = 0;
        boolean offersChanged = villager.shouldRestock(serverLevel);
        if (offersChanged) {
            villager.restock();
            prepareAutomaticPrices(villager);
        }
        // Persist both a completed restock and the internal day check performed by
        // shouldRestock, matching the state a normal villager would keep in its NBT.
        persistCachedVillager();
        if (offersChanged) {
            markOffersChanged();
        } else {
            markChangedAndSync();
        }
    }

    private static boolean resetUsedOffers(MerchantOffers offers) {
        boolean changed = false;
        for (MerchantOffer offer : offers) {
            if (offer.needsRestock()) {
                offer.resetUses();
                changed = true;
            }
        }
        return changed;
    }

    private void tickOfferAge(@Nullable Villager villager) {
        if (villager == null || villager.getOffers().isEmpty()) {
            if (offerAgeTicks != 0) {
                offerAgeTicks = 0;
                restockCheckTicks = 0;
                setChanged();
            }
            return;
        }
        int maximumAge = autotraderService.offerRefreshTicks();
        if (offerAgeTicks < maximumAge) {
            offerAgeTicks++;
            if (offerAgeTicks % 20 == 0 || offerAgeTicks == maximumAge) {
                setChanged();
            }
        }
    }

    private int countMatching(int[] slots, ItemCost cost) {
        int count = 0;
        for (int slot : slots) {
            ItemStack stack = items.get(slot);
            if (cost.test(stack)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private void consume(int[] slots, ItemCost cost, int count) {
        int remaining = count;
        for (int slot : slots) {
            ItemStack stack = items.get(slot);
            if (remaining > 0 && cost.test(stack)) {
                int consumed = Math.min(remaining, stack.getCount());
                stack.shrink(consumed);
                remaining -= consumed;
                if (stack.isEmpty()) {
                    items.set(slot, ItemStack.EMPTY);
                }
            }
        }
    }

    private boolean canStoreOutput(ItemStack source) {
        return OrderedOutputInserter.canInsert(
                items,
                FIRST_OUTPUT_SLOT,
                AutotraderPolicy.OUTPUT_SLOTS,
                source
        );
    }

    private void storeOutput(ItemStack source) {
        OrderedOutputInserter.insert(
                items,
                FIRST_OUTPUT_SLOT,
                AutotraderPolicy.OUTPUT_SLOTS,
                source
        );
    }

    private static boolean isAdultVillager(ItemStack stack) {
        return CapturedMobStackAdapter.isFilledCapturer(CapturedMobKind.VILLAGER, stack)
                && !CapturedMobStackAdapter.isBaby(CapturedMobKind.VILLAGER, stack);
    }

    private static boolean isInputASlot(int slot) {
        return slot >= FIRST_INPUT_A_SLOT && slot < FIRST_INPUT_B_SLOT;
    }

    private static boolean isInputBSlot(int slot) {
        return slot >= FIRST_INPUT_B_SLOT && slot < FIRST_OUTPUT_SLOT;
    }

    private static boolean isOutputSlot(int slot) {
        return slot >= FIRST_OUTPUT_SLOT && slot < CONTAINER_SIZE;
    }

    private static boolean isValidSlot(int slot) {
        return slot >= 0 && slot < CONTAINER_SIZE;
    }

    private static final class AutotraderVillager extends Villager { // NOSONAR - Minecraft requires Entity identity and framework inheritance for this server-side proxy.
        private final AutotraderUseCase autotraderService;

        private AutotraderVillager(
                net.minecraft.world.level.Level level,
                AutotraderUseCase autotraderService
        ) {
            super(CapturedMobStackAdapter.villagerType(), level);
            this.autotraderService = autotraderService;
        }

        private int completeAutomaticTrade(MerchantOffer offer) {
            offer.increaseUses();
            markTemporaryDiscount(offer);
            if (autotraderService.infiniteTrades()) {
                offer.resetUses();
            }
            setVillagerXp(getVillagerXp() + offer.getXp());
            boolean leveledUp = forceCareerLevelUpdate();
            return autotraderService.automaticExperience(
                    getRandom()::nextInt,
                    offer.shouldRewardExp(),
                    leveledUp
            );
        }

        private boolean forceInitializeOffers(ServerLevel serverLevel) {
            if (!getOffers().isEmpty()) {
                return false;
            }
            updateTrades(serverLevel);
            return !getOffers().isEmpty();
        }

        private void markTemporaryDiscount(MerchantOffer offer) {
            TemporaryTradeDiscountStore.markOffer(
                    getPersistentData(),
                    getOffers(),
                    offer,
                    level().getGameTime(),
                    registryAccess()
            );
        }

        private boolean forceCareerLevelUpdate() {
            if (!(level() instanceof ServerLevel serverLevel)) {
                return false;
            }
            boolean leveledUp = false;
            while (VillagerData.canLevelUp(getVillagerData().level())
                    && getVillagerXp() >= VillagerData.getMaxXpPerLevel(getVillagerData().level())) {
                setVillagerData(getVillagerData().withLevel(getVillagerData().level() + 1));
                updateTrades(serverLevel);
                leveledUp = true;
            }
            return leveledUp;
        }
    }
}
