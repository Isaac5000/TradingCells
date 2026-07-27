package com.cosmocraft.trading_cells.feature.trader.adapters.input;

import com.cosmocraft.trading_cells.feature.trader.adapters.output.TraderRegistrationAdapter;
import com.cosmocraft.trading_cells.platform.neoforge.menu.VillagerTradeEquipmentSlots;
import com.cosmocraft.trading_cells.platform.neoforge.menu.VillagerTradeMenuLayout;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.npc.ClientSideMerchant;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.npc.villager.VillagerType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MerchantContainer;
import net.minecraft.world.inventory.MerchantResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/** Custom manual-trading menu used only by the Villager Trading Cell. */
public final class VillagerTradingCellMenu extends AbstractContainerMenu {
    public static final int SELECT_OFFER_BUTTON_BASE = 100;

    private static final int PAYMENT_A_SLOT = 0;
    private static final int PAYMENT_B_SLOT = 1;
    private static final int RESULT_SLOT = 2;
    private static final int PLAYER_INVENTORY_START = 3;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int PLAYER_HOTBAR_END = PLAYER_INVENTORY_END + 9;
    private static final int EQUIPMENT_START = PLAYER_HOTBAR_END;
    private static final int EQUIPMENT_END = EQUIPMENT_START + 5;

    private final Merchant trader;
    private final MerchantContainer tradeContainer;
    private int merchantLevel;
    private int merchantXp;
    private int storedExperience;
    private int offersRevision;
    private boolean showProgressBar;
    private boolean canRestock;
    private boolean canResetTrades;
    private boolean hasServerSnapshot;
    private VillagerData villagerData;
    private int selectedOfferIndex;
    private boolean batchingResultQuickMove;
    private boolean completedBatchedTrade;

    public VillagerTradingCellMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new ClientSideMerchant(inventory.player));
    }

    public VillagerTradingCellMenu(int containerId, Inventory inventory, Merchant merchant) {
        super(TraderRegistrationAdapter.VILLAGER_TRADING_CELL_MENU.get(), containerId);
        this.trader = merchant;
        this.tradeContainer = new MerchantContainer(merchant);
        this.villagerData = new VillagerData(
                BuiltInRegistries.VILLAGER_TYPE.getOrThrow(VillagerType.PLAINS),
                BuiltInRegistries.VILLAGER_PROFESSION.getOrThrow(VillagerProfession.NONE),
                1
        );

        addSlot(new Slot(
                tradeContainer,
                PAYMENT_A_SLOT,
                VillagerTradeMenuLayout.itemX(VillagerTradeMenuLayout.MANUAL_PAYMENT_A_X),
                VillagerTradeMenuLayout.itemY(VillagerTradeMenuLayout.MANUAL_TRADER_SLOT_Y)
        ));
        addSlot(new Slot(
                tradeContainer,
                PAYMENT_B_SLOT,
                VillagerTradeMenuLayout.itemX(VillagerTradeMenuLayout.MANUAL_PAYMENT_B_X),
                VillagerTradeMenuLayout.itemY(VillagerTradeMenuLayout.MANUAL_TRADER_SLOT_Y)
        ));
        addSlot(new MerchantResultSlot(
                inventory.player,
                merchant,
                tradeContainer,
                RESULT_SLOT,
                VillagerTradeMenuLayout.itemX(VillagerTradeMenuLayout.MANUAL_RESULT_X),
                VillagerTradeMenuLayout.itemY(VillagerTradeMenuLayout.MANUAL_TRADER_SLOT_Y)
        ));
        addStandardInventorySlots(
                inventory,
                VillagerTradeMenuLayout.itemX(VillagerTradeMenuLayout.PLAYER_INVENTORY_X),
                VillagerTradeMenuLayout.itemY(VillagerTradeMenuLayout.PLAYER_INVENTORY_Y)
        );
        for (Slot equipmentSlot : VillagerTradeEquipmentSlots.create(inventory)) {
            addSlot(equipmentSlot);
        }
    }

    public MerchantOffers getOffers() {
        return trader.getOffers();
    }

    public void setOffers(MerchantOffers offers) {
        trader.overrideOffers(offers);
        selectedOfferIndex = offers.isEmpty() ? 0 : Math.min(selectedOfferIndex, offers.size() - 1);
        setSelectionHint(selectedOfferIndex);
    }

    public int selectedOfferIndex() {
        return selectedOfferIndex;
    }

    public @Nullable MerchantOffer selectedOffer() {
        MerchantOffers offers = getOffers();
        return offers.isEmpty() ? null : offers.get(Math.floorMod(selectedOfferIndex, offers.size()));
    }

    public void setSelectionHint(int index) {
        selectedOfferIndex = Math.max(0, index);
        tradeContainer.setSelectionHint(selectedOfferIndex);
    }

    public int merchantLevel() {
        return merchantLevel;
    }

    public int merchantXp() {
        return merchantXp;
    }

    public boolean showProgressBar() {
        return showProgressBar;
    }

    public boolean canRestock() {
        return canRestock;
    }

    public int storedExperience() {
        return storedExperience;
    }

    public void setStoredExperience(int experience) {
        storedExperience = Math.max(0, experience);
    }

    public boolean canResetTrades() {
        return canResetTrades;
    }

    public VillagerData villagerData() {
        return villagerData;
    }

    public int offersRevision() {
        return offersRevision;
    }

    public void applyServerState(ServerState state) {
        if (hasServerSnapshot && state.menu().revision() < offersRevision) {
            return;
        }
        setOffers(state.offers());
        villagerData = state.merchant().data();
        merchantLevel = state.merchant().level();
        merchantXp = state.merchant().experience();
        storedExperience = Math.max(0, state.menu().storedExperience());
        int selectedIndex = state.offers().isEmpty()
                ? 0
                : Math.clamp(state.menu().selectedIndex(), 0, state.offers().size() - 1);
        setSelectionHint(selectedIndex);
        showProgressBar = state.menu().showProgress();
        canRestock = state.menu().canRestock();
        canResetTrades = state.menu().canResetTrades();
        offersRevision = Math.max(0, state.menu().revision());
        hasServerSnapshot = true;
    }

    @Override
    public void slotsChanged(Container container) {
        tradeContainer.updateSellItem();
        super.slotsChanged(container);
    }

    public boolean selectOfferFromPacket(Player player, int offerIndex, int knownRevision) {
        if (knownRevision != offersRevision
                || offerIndex < 0
                || offerIndex >= getOffers().size()
                || !stillValid(player)) {
            return false;
        }
        return tryChangeOffer(player, offerIndex);
    }

    @Override
    public boolean clickMenuButton(@NonNull Player player, int buttonId) {
        int offerIndex = buttonId - SELECT_OFFER_BUTTON_BASE;
        if (offerIndex < 0 || offerIndex >= getOffers().size()) {
            return false;
        }
        return selectOfferFromPacket(player, offerIndex, offersRevision);
    }

    @Override
    public boolean stillValid(@NonNull Player player) {
        return trader.stillValid(player);
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack carried, Slot target) {
        return false;
    }

    @Override
    public void clicked(int slotIndex, int buttonNum, @NonNull ContainerInput containerInput, @NonNull Player player) {
        if (slotIndex != RESULT_SLOT || containerInput != ContainerInput.QUICK_MOVE) {
            super.clicked(slotIndex, buttonNum, containerInput, player);
            return;
        }

        TradeBatchMerchant batchMerchant = trader instanceof TradeBatchMerchant merchant ? merchant : null;
        batchingResultQuickMove = true;
        completedBatchedTrade = false;
        if (batchMerchant != null) {
            batchMerchant.beginTradeBatch();
        }
        try {
            super.clicked(slotIndex, buttonNum, containerInput, player);
        } finally {
            finishResultQuickMoveBatch(batchMerchant);
        }
    }

    @Override
    public @NonNull ItemStack quickMoveStack(@NonNull Player player, int slotIndex) {
        Slot slot = slots.get(slotIndex);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getItem();
        ItemStack clicked = stack.copy();
        if (!moveQuickMovedStack(player, slotIndex, stack)) {
            return ItemStack.EMPTY;
        }
        if (slotIndex == RESULT_SLOT) {
            slot.onQuickCraft(stack, clicked);
            if (!batchingResultQuickMove) {
                playTradeSound();
            }
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == clicked.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        if (slotIndex == RESULT_SLOT) {
            completedBatchedTrade |= batchingResultQuickMove;
            refillSelectedOfferPayments();
        }
        return clicked;
    }

    private void finishResultQuickMoveBatch(@Nullable TradeBatchMerchant batchMerchant) {
        try {
            if (completedBatchedTrade) {
                topUpSelectedOfferPayments();
            }
        } finally {
            closeTradeBatch(batchMerchant);
        }
    }

    private void closeTradeBatch(@Nullable TradeBatchMerchant batchMerchant) {
        try {
            if (batchMerchant != null) {
                batchMerchant.endTradeBatch();
            }
        } finally {
            boolean playBatchedSound = completedBatchedTrade;
            batchingResultQuickMove = false;
            completedBatchedTrade = false;
            if (playBatchedSound) {
                playTradeSound();
            }
        }
    }

    private boolean moveQuickMovedStack(Player player, int slotIndex, ItemStack stack) {
        if (slotIndex == RESULT_SLOT) {
            return moveItemStackTo(stack, PLAYER_INVENTORY_START, PLAYER_HOTBAR_END, true);
        }
        if (slotIndex == PAYMENT_A_SLOT
                || slotIndex == PAYMENT_B_SLOT
                || slotIndex >= EQUIPMENT_START && slotIndex < EQUIPMENT_END) {
            return moveItemStackTo(stack, PLAYER_INVENTORY_START, PLAYER_HOTBAR_END, false);
        }
        if (slotIndex < PLAYER_INVENTORY_START || slotIndex >= PLAYER_HOTBAR_END) {
            return false;
        }

        int preferredPayment = preferredPaymentSlot(selectedOffer(), stack);
        if (preferredPayment >= PAYMENT_A_SLOT
                && preferredPayment <= PAYMENT_B_SLOT
                && moveItemStackTo(stack, preferredPayment, preferredPayment + 1, false)) {
            return true;
        }
        if (tryMoveToEquipment(player, stack)) {
            return true;
        }
        return slotIndex < PLAYER_INVENTORY_END
                ? moveItemStackTo(stack, PLAYER_INVENTORY_END, PLAYER_HOTBAR_END, false)
                : moveItemStackTo(stack, PLAYER_INVENTORY_START, PLAYER_INVENTORY_END, false);
    }

    @Override
    public void removed(@NonNull Player player) {
        super.removed(player);
        trader.setTradingPlayer(null);
        if (!trader.isClientSide()) {
            returnPaymentItems(player);
        }
    }


    private int preferredPaymentSlot(@Nullable MerchantOffer offer, ItemStack stack) {
        if (offer == null) {
            return -1;
        }
        boolean matchesA = offer.getItemCostA().test(stack);
        boolean matchesB = offer.getItemCostB().map(cost -> cost.test(stack)).orElse(false);
        if (!matchesA && !matchesB) {
            return -1;
        }
        if (matchesA && !matchesB) {
            return PAYMENT_A_SLOT;
        }
        if (!matchesA) {
            return PAYMENT_B_SLOT;
        }
        int currentA = tradeContainer.getItem(PAYMENT_A_SLOT).getCount();
        if (currentA < offer.getCostA().getCount()) {
            return PAYMENT_A_SLOT;
        }
        int currentB = tradeContainer.getItem(PAYMENT_B_SLOT).getCount();
        return currentB < offer.getCostB().getCount() ? PAYMENT_B_SLOT : PAYMENT_A_SLOT;
    }

    private boolean tryMoveToEquipment(Player player, ItemStack stack) {
        EquipmentSlot equipmentSlot = player.getEquipmentSlotForItem(stack);
        int target = switch (equipmentSlot) {
            case OFFHAND -> EQUIPMENT_START;
            case HEAD -> EQUIPMENT_START + 1;
            case CHEST -> EQUIPMENT_START + 2;
            case LEGS -> EQUIPMENT_START + 3;
            case FEET -> EQUIPMENT_START + 4;
            default -> -1;
        };
        return target >= EQUIPMENT_START
                && target < EQUIPMENT_END
                && !slots.get(target).hasItem()
                && slots.get(target).mayPlace(stack)
                && moveItemStackTo(stack, target, target + 1, false);
    }

    /**
     * Changes the selected offer without ever destroying or trapping payment items.
     * Both current payment stacks are first simulated back into the 36 player
     * inventory slots. The selection changes only if that complete return fits.
     */
    private boolean tryChangeOffer(Player player, int newTradeIndex) {
        if (newTradeIndex < 0 || newTradeIndex >= getOffers().size()) {
            return false;
        }

        Optional<PaymentReturnPlan> returnPlan = simulatePaymentReturn(player);
        if (returnPlan.isEmpty()) {
            return false;
        }

        commitPaymentReturn(player, returnPlan.orElseThrow());
        setSelectionHint(newTradeIndex);

        MerchantOffer offer = getOffers().get(newTradeIndex);
        moveFromInventoryToPaymentSlot(PAYMENT_A_SLOT, offer.getItemCostA());
        offer.getItemCostB().ifPresent(cost -> moveFromInventoryToPaymentSlot(PAYMENT_B_SLOT, cost));
        tradeContainer.updateSellItem();
        broadcastChanges();
        return true;
    }

    Optional<PaymentReturnPlan> simulatePaymentReturn(Player player) {
        ItemStack oldA = tradeContainer.getItem(PAYMENT_A_SLOT).copy();
        ItemStack oldB = tradeContainer.getItem(PAYMENT_B_SLOT).copy();
        return simulatePlayerInventory(player, oldA, oldB).map(PaymentReturnPlan::new);
    }

    void commitPaymentReturn(Player player, PaymentReturnPlan plan) {
        commitPlayerInventory(player, plan.inventory());
        tradeContainer.setItem(PAYMENT_A_SLOT, ItemStack.EMPTY);
        tradeContainer.setItem(PAYMENT_B_SLOT, ItemStack.EMPTY);
        tradeContainer.updateSellItem();
        broadcastChanges();
    }

    private static Optional<List<ItemStack>> simulatePlayerInventory(
            Player player,
            ItemStack... returnedStacks
    ) {
        List<ItemStack> simulated = new ArrayList<>(36);
        for (int index = 0; index < 36; index++) {
            simulated.add(player.getInventory().getItem(index).copy());
        }
        for (ItemStack source : returnedStacks) {
            ItemStack remaining = source.copy();
            if (remaining.isEmpty()) {
                continue;
            }
            mergeIntoExistingStacks(simulated, remaining);
            fillEmptySlots(simulated, remaining);
            if (!remaining.isEmpty()) {
                return Optional.empty();
            }
        }
        return Optional.of(simulated);
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

    private void moveFromInventoryToPaymentSlot(int paymentSlot, ItemCost cost) {
        for (int i = PLAYER_INVENTORY_START; i < PLAYER_HOTBAR_END; i++) {
            ItemStack inventoryItem = slots.get(i).getItem();
            ItemStack payment = tradeContainer.getItem(paymentSlot);
            if (!inventoryItem.isEmpty()
                    && cost.test(inventoryItem)
                    && (payment.isEmpty()
                    || ItemStack.isSameItemSameComponents(inventoryItem, payment))) {
                int moveCount = Math.min(
                        inventoryItem.getCount(),
                        inventoryItem.getMaxStackSize() - payment.getCount()
                );
                ItemStack newPayment = inventoryItem.copyWithCount(payment.getCount() + moveCount);
                inventoryItem.shrink(moveCount);
                tradeContainer.setItem(paymentSlot, newPayment);
                if (newPayment.getCount() >= newPayment.getMaxStackSize()) {
                    return;
                }
            }
        }
    }

    private void refillSelectedOfferPayments() {
        refillSelectedOfferPayments(false);
    }

    private void topUpSelectedOfferPayments() {
        refillSelectedOfferPayments(true);
    }

    private void refillSelectedOfferPayments(boolean topUp) {
        MerchantOffer offer = selectedOffer();
        if (offer == null || offer.isOutOfStock()) {
            return;
        }
        refillPaymentSlot(PAYMENT_A_SLOT, offer.getItemCostA(), offer.getCostA().getCount(), topUp);
        offer.getItemCostB().ifPresent(cost ->
                refillPaymentSlot(PAYMENT_B_SLOT, cost, offer.getCostB().getCount(), topUp)
        );
    }

    private void refillPaymentSlot(int paymentSlot, ItemCost cost, int requiredCount, boolean topUp) {
        if (topUp || tradeContainer.getItem(paymentSlot).getCount() < requiredCount) {
            moveFromInventoryToPaymentSlot(paymentSlot, cost);
        }
    }

    private void returnPaymentItems(Player player) {
        ItemStack first = tradeContainer.removeItemNoUpdate(PAYMENT_A_SLOT);
        ItemStack second = tradeContainer.removeItemNoUpdate(PAYMENT_B_SLOT);
        if (!player.isAlive() || player instanceof ServerPlayer serverPlayer && serverPlayer.hasDisconnected()) {
            if (!first.isEmpty()) {
                player.drop(first, false);
            }
            if (!second.isEmpty()) {
                player.drop(second, false);
            }
            return;
        }
        if (!first.isEmpty()) {
            player.getInventory().placeItemBackInInventory(first);
        }
        if (!second.isEmpty()) {
            player.getInventory().placeItemBackInInventory(second);
        }
    }

    record PaymentReturnPlan(List<ItemStack> inventory) {
        PaymentReturnPlan {
            inventory = List.copyOf(inventory);
        }
    }

    public record ServerState(
            MerchantOffers offers,
            MerchantState merchant,
            MenuState menu
    ) {
    }

    public record MerchantState(VillagerData data, int level, int experience) {
    }

    public record MenuState(
            int storedExperience,
            int selectedIndex,
            boolean showProgress,
            boolean canRestock,
            boolean canResetTrades,
            int revision
    ) {
    }

    private void playTradeSound() {
        if (!trader.isClientSide() && trader instanceof Entity entity) {
            entity.level().playLocalSound(
                    entity.getX(),
                    entity.getY(),
                    entity.getZ(),
                    trader.getNotifyTradeSound(),
                    SoundSource.NEUTRAL,
                    1.0F,
                    1.0F,
                    false
            );
        }
    }
}
