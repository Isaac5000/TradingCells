package com.cosmocraft.trading_cells.feature.trader.adapters.input;

import com.cosmocraft.trading_cells.feature.trader.adapters.output.AutotraderRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.trader.domain.model.AutotraderPolicy;
import com.cosmocraft.trading_cells.platform.neoforge.menu.VillagerTradeEquipmentSlots;
import com.cosmocraft.trading_cells.platform.neoforge.menu.VillagerTradeMenuLayout;
import com.cosmocraft.trading_cells.platform.neoforge.network.AutotraderMenuSyncPayload;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.npc.villager.VillagerType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class AutotraderMenu extends AbstractContainerMenu {
    public static final int EXTRACT_EXPERIENCE_BUTTON = 1;
    public static final int RESET_TRADES_BUTTON = 2;
    public static final int EXTRACT_NEXT_LEVEL_BUTTON = 3;
    public static final int SELECT_OFFER_BUTTON_BASE = 100;
    public static final int INPUT_ROW_X = VillagerTradeMenuLayout.itemX(VillagerTradeMenuLayout.AUTOTRADER_ROW_X);
    public static final int INPUT_A_ROW_Y = VillagerTradeMenuLayout.itemY(VillagerTradeMenuLayout.AUTOTRADER_INPUT_A_Y);
    public static final int INPUT_B_ROW_Y = VillagerTradeMenuLayout.itemY(VillagerTradeMenuLayout.AUTOTRADER_INPUT_B_Y);
    public static final int OUTPUT_ROW_Y = VillagerTradeMenuLayout.itemY(VillagerTradeMenuLayout.AUTOTRADER_OUTPUT_Y);
    private static final int MACHINE_SLOT_COUNT = AutotraderBlockEntity.CONTAINER_SIZE;
    private static final int PLAYER_INVENTORY_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int PLAYER_HOTBAR_END = PLAYER_INVENTORY_END + 9;
    private static final int EQUIPMENT_START = PLAYER_HOTBAR_END;
    private static final int EQUIPMENT_END = EQUIPMENT_START + 5;

    private final Container container;
    private final ContainerData data;
    private final Player menuPlayer;
    private VillagerData syncedVillagerData;
    private int syncedVillagerXp;
    private boolean syncedCanResetTrades;
    private boolean syncedHasVillager;
    private boolean hasOffersSnapshot;
    private int syncedOffersRevision = -1;
    private int lastSentRevision = Integer.MIN_VALUE;
    private MerchantOffers cachedOffers = new MerchantOffers();

    public AutotraderMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(MACHINE_SLOT_COUNT), new SimpleContainerData(2));
    }

    public AutotraderMenu(int containerId, Inventory inventory, Container container, ContainerData data) {
        super(AutotraderRegistrationAdapter.AUTOTRADER_MENU.get(), containerId);
        checkContainerSize(container, MACHINE_SLOT_COUNT);
        checkContainerDataCount(data, 2);
        this.container = container;
        this.data = data;
        this.menuPlayer = inventory.player;
        this.syncedVillagerData = new VillagerData(
                BuiltInRegistries.VILLAGER_TYPE.getOrThrow(VillagerType.PLAINS),
                BuiltInRegistries.VILLAGER_PROFESSION.getOrThrow(VillagerProfession.NONE),
                1
        );
        addSlot(new HiddenVillagerSlot(container, AutotraderBlockEntity.VILLAGER_SLOT));
        for (int index = 0; index < AutotraderPolicy.INPUT_SLOTS_PER_COST; index++) {
            addSlot(new InputASlot(
                    container,
                    AutotraderBlockEntity.FIRST_INPUT_A_SLOT + index,
                    INPUT_ROW_X + index * 18,
                    INPUT_A_ROW_Y
            ));
        }
        for (int index = 0; index < AutotraderPolicy.INPUT_SLOTS_PER_COST; index++) {
            addSlot(new InputBSlot(
                    container,
                    AutotraderBlockEntity.FIRST_INPUT_B_SLOT + index,
                    INPUT_ROW_X + index * 18,
                    INPUT_B_ROW_Y
            ));
        }
        for (int index = 0; index < AutotraderPolicy.OUTPUT_SLOTS; index++) {
            addSlot(new OutputSlot(
                    container,
                    AutotraderBlockEntity.FIRST_OUTPUT_SLOT + index,
                    INPUT_ROW_X + index * 18,
                    OUTPUT_ROW_Y
            ));
        }
        addStandardInventorySlots(
                inventory,
                VillagerTradeMenuLayout.itemX(VillagerTradeMenuLayout.PLAYER_INVENTORY_X),
                VillagerTradeMenuLayout.itemY(VillagerTradeMenuLayout.PLAYER_INVENTORY_Y)
        );
        for (Slot equipmentSlot : VillagerTradeEquipmentSlots.create(inventory)) {
            addSlot(equipmentSlot);
        }
        addDataSlots(data);
    }

    public int selectedOfferIndex() {
        return data.get(0);
    }

    public int offerCount() {
        return offers().size();
    }

    public int storedExperience() {
        return data.get(1);
    }

    public boolean canResetTrades() {
        return container instanceof AutotraderBlockEntity autotrader
                ? autotrader.canResetTradesForMenu()
                : syncedCanResetTrades;
    }

    public int offersRevision() {
        return container instanceof AutotraderBlockEntity autotrader
                ? autotrader.offersRevisionSnapshot()
                : syncedOffersRevision;
    }

    public @Nullable MerchantOffer selectedOffer() {
        MerchantOffers offers = offers();
        if (offers.isEmpty()) {
            return null;
        }
        return offers.get(Math.floorMod(selectedOfferIndex(), offers.size()));
    }

    public MerchantOffers offers() {
        if (container instanceof AutotraderBlockEntity autotrader) {
            return autotrader.offersView();
        }
        return cachedOffers;
    }

    public boolean hasVillager() {
        return container instanceof AutotraderBlockEntity autotrader
                ? autotrader.hasStoredVillager()
                : syncedHasVillager;
    }

    public VillagerData villagerData() {
        if (container instanceof AutotraderBlockEntity autotrader) {
            return autotrader.villagerDataSnapshot();
        }
        return syncedVillagerData;
    }

    public int villagerXp() {
        if (container instanceof AutotraderBlockEntity autotrader) {
            return autotrader.villagerXpSnapshot();
        }
        return syncedVillagerXp;
    }

    public void applyServerState(
            boolean hasVillager,
            MerchantOffers offers,
            VillagerData villagerData,
            int villagerXp,
            boolean canResetTrades,
            int offersRevision
    ) {
        if (hasOffersSnapshot && offersRevision < syncedOffersRevision) {
            return;
        }
        syncedHasVillager = hasVillager;
        cachedOffers = offers.copy();
        syncedVillagerData = villagerData;
        syncedVillagerXp = Math.max(0, villagerXp);
        syncedCanResetTrades = canResetTrades;
        syncedOffersRevision = Math.max(0, offersRevision);
        hasOffersSnapshot = true;
    }

    public boolean selectOfferFromPacket(Player player, int offerIndex, int knownRevision) {
        if (knownRevision != offersRevision()
                || offerIndex < 0
                || offerIndex >= offerCount()
                || !stillValid(player)) {
            return false;
        }
        return container instanceof AutotraderBlockEntity autotrader
                && autotrader.trySelectOffer(player, offerIndex);
    }

    public boolean resetTradesFromPacket(Player player, int knownRevision) {
        if (knownRevision != offersRevision() || !stillValid(player)) {
            return false;
        }
        return container instanceof AutotraderBlockEntity autotrader
                && autotrader.resetTransientTrades(player);
    }

    public void extractExperienceFromPacket(Player player, byte mode) {
        if (!stillValid(player) || !(container instanceof AutotraderBlockEntity autotrader)) {
            return;
        }
        if (mode == com.cosmocraft.trading_cells.platform.neoforge.network.ExtractTradingCellExperiencePayload.NEXT_LEVEL) {
            autotrader.extractExperienceToNextLevel(player);
        } else {
            autotrader.extractExperience(player);
        }
    }

    @Override
    public boolean clickMenuButton(@NonNull Player player, int buttonId) {
        if (buttonId == EXTRACT_EXPERIENCE_BUTTON) {
            if (container instanceof AutotraderBlockEntity autotrader) {
                autotrader.extractExperience(player);
                return true;
            }
            return false;
        }
        if (buttonId == RESET_TRADES_BUTTON) {
            return resetTradesFromPacket(player, offersRevision());
        }
        if (buttonId == EXTRACT_NEXT_LEVEL_BUTTON) {
            if (container instanceof AutotraderBlockEntity autotrader) {
                autotrader.extractExperienceToNextLevel(player);
                return true;
            }
            return false;
        }
        int offerIndex = buttonId - SELECT_OFFER_BUTTON_BASE;
        if (offerIndex < 0 || offerIndex >= offerCount()) {
            return false;
        }
        if (container instanceof AutotraderBlockEntity) {
            return selectOfferFromPacket(player, offerIndex, offersRevision());
        }
        data.set(0, offerIndex);
        container.setChanged();
        return true;
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (!(container instanceof AutotraderBlockEntity autotrader)
                || !(menuPlayer instanceof ServerPlayer serverPlayer)) {
            return;
        }
        int revision = autotrader.offersRevisionSnapshot();
        if (revision == lastSentRevision) {
            return;
        }
        sendMenuSnapshot(serverPlayer);
    }

    public void sendMenuSnapshot(ServerPlayer serverPlayer) {
        if (!(container instanceof AutotraderBlockEntity autotrader)
                || serverPlayer.containerMenu != this) {
            return;
        }
        int revision = autotrader.offersRevisionSnapshot();
        PacketDistributor.sendToPlayer(serverPlayer, new AutotraderMenuSyncPayload(
                containerId,
                autotrader.hasStoredVillager(),
                autotrader.offersView(),
                autotrader.villagerDataSnapshot(),
                autotrader.villagerXpSnapshot(),
                autotrader.canResetTradesForMenu(),
                revision
        ));
        lastSentRevision = revision;
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
        if (index == AutotraderBlockEntity.VILLAGER_SLOT
                || !moveQuickMovedStack(player, index, stack)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return result;
    }

    private boolean moveQuickMovedStack(Player player, int index, ItemStack stack) {
        if (index < MACHINE_SLOT_COUNT) {
            return moveItemStackTo(stack, PLAYER_INVENTORY_START, PLAYER_HOTBAR_END, true);
        }
        if (index >= EQUIPMENT_START && index < EQUIPMENT_END) {
            return moveItemStackTo(stack, PLAYER_INVENTORY_START, PLAYER_HOTBAR_END, false);
        }
        int preferredInput = preferredInputRow(selectedOffer(), stack);
        if (preferredInput == 1) {
            return moveItemStackTo(
                    stack,
                    AutotraderBlockEntity.FIRST_INPUT_A_SLOT,
                    AutotraderBlockEntity.FIRST_INPUT_B_SLOT,
                    false
            );
        }
        if (preferredInput == 2) {
            return moveItemStackTo(
                    stack,
                    AutotraderBlockEntity.FIRST_INPUT_B_SLOT,
                    AutotraderBlockEntity.FIRST_OUTPUT_SLOT,
                    false
            );
        }
        if (tryMoveToEquipment(player, stack)) {
            return true;
        }
        return index < PLAYER_INVENTORY_END
                ? moveItemStackTo(stack, PLAYER_INVENTORY_END, PLAYER_HOTBAR_END, false)
                : moveItemStackTo(stack, PLAYER_INVENTORY_START, PLAYER_INVENTORY_END, false);
    }


    /**
     * Returns 1 for the first cost row, 2 for the second row and 0 when the
     * stack is not a valid selected-offer ingredient. If it matches both
     * costs, the still-incomplete required row is preferred.
     */
    private int preferredInputRow(@Nullable MerchantOffer offer, ItemStack stack) {
        if (offer == null) {
            return 0;
        }
        boolean matchesA = offer.getItemCostA().test(stack);
        boolean matchesB = offer.getItemCostB().map(cost -> cost.test(stack)).orElse(false);
        if (!matchesA && !matchesB) {
            return 0;
        }
        if (matchesA && !matchesB) {
            return 1;
        }
        if (!matchesA) {
            return 2;
        }
        int storedA = matchingCount(
                AutotraderBlockEntity.FIRST_INPUT_A_SLOT,
                AutotraderBlockEntity.FIRST_INPUT_B_SLOT,
                offer,
                true
        );
        if (storedA < offer.getCostA().getCount()) {
            return 1;
        }
        int storedB = matchingCount(
                AutotraderBlockEntity.FIRST_INPUT_B_SLOT,
                AutotraderBlockEntity.FIRST_OUTPUT_SLOT,
                offer,
                false
        );
        return storedB < offer.getCostB().getCount() ? 2 : 1;
    }

    private int matchingCount(int start, int end, MerchantOffer offer, boolean firstCost) {
        int total = 0;
        for (int slotIndex = start; slotIndex < end; slotIndex++) {
            ItemStack candidate = slots.get(slotIndex).getItem();
            boolean matches = firstCost
                    ? offer.getItemCostA().test(candidate)
                    : offer.getItemCostB().map(cost -> cost.test(candidate)).orElse(false);
            if (matches) {
                total += candidate.getCount();
            }
        }
        return total;
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

    private static final class HiddenVillagerSlot extends Slot {
        private HiddenVillagerSlot(Container container, int slot) {
            super(container, slot, -1_000, -1_000);
        }

        @Override
        public boolean mayPlace(@NonNull ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(@NonNull Player player) {
            return false;
        }
    }

    private final class InputASlot extends Slot {
        private InputASlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(@NonNull ItemStack stack) {
            MerchantOffer offer = selectedOffer();
            return offer != null && offer.getItemCostA().test(stack);
        }
    }

    private final class InputBSlot extends Slot {
        private InputBSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(@NonNull ItemStack stack) {
            MerchantOffer offer = selectedOffer();
            return offer != null && offer.getItemCostB().map(cost -> cost.test(stack)).orElse(false);
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
