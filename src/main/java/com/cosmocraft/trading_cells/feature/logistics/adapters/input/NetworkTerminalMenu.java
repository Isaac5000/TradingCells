package com.cosmocraft.trading_cells.feature.logistics.adapters.input;

import com.cosmocraft.trading_cells.feature.logistics.adapters.api.LogisticsResourceAdapter;
import com.cosmocraft.trading_cells.feature.logistics.adapters.api.LogisticsResourceAdapters;
import com.cosmocraft.trading_cells.feature.logistics.adapters.output.LogisticsRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.logistics.domain.model.LogisticsResourceType;
import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.Config;
import com.cosmocraft.trading_cells.platform.neoforge.menu.MachineMenuLayout;
import com.cosmocraft.trading_cells.platform.neoforge.network.NetworkTerminalActionPayload;
import com.cosmocraft.trading_cells.platform.neoforge.network.NetworkTerminalSyncPayload;
import com.cosmocraft.trading_cells.platform.neoforge.network.NetworkCraftingSyncPayload;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.transfer.item.PlayerInventoryWrapper;
import net.neoforged.neoforge.transfer.item.CarriedSlotWrapper;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class NetworkTerminalMenu extends AbstractContainerMenu {
    public static final int NETWORK_COLUMNS = 8;
    // Nine rows can intersect a smooth-scroll viewport, plus the preceding buffer row.
    public static final int PAGE_SIZE = NETWORK_COLUMNS * 10;
    private static final int PLAYER_INVENTORY_END = 27;
    private static final int PLAYER_HOTBAR_END = 36;

    private final @Nullable NetworkTerminalBlockEntity terminal;
    private final @Nullable ServerPlayer serverPlayer;
    private final ContainerLevelAccess access;
    private final boolean crafting;
    private final Map<String, NetworkTerminalSyncPayload.Entry> entries = new LinkedHashMap<>();
    private LogisticsResourceType selectedType = LogisticsResourceType.ITEM;
    private int page;
    private int totalPages = 1;
    private String query = "";
    private int updateTicks;
    private boolean forceReplace = true;
    private long revision;
    private boolean recipeView;
    private List<NetworkCraftingCatalog.Entry> recipeCatalog;
    private net.minecraft.world.item.crafting.RecipeMap catalogRecipes;
    private List<ItemStack> craftingGrid = new ArrayList<>(java.util.Collections.nCopies(9, ItemStack.EMPTY));
    private ItemStack craftingResult = ItemStack.EMPTY;
    private long lastActionTick = Long.MIN_VALUE;
    private int actionsThisTick;
    private LogisticsNetworkManager.ResourceListing pendingListing;
    private BlockPos listingOrigin;
    private LogisticsResourceType listingType;
    private List<NetworkTerminalSyncPayload.Entry> sortedEntries = List.of();
    private int sentTotalPages;
    private int sentPage = -1;
    private final SimpleContainer resourceContainer = new SimpleContainer(1);
    private PendingAction pendingAction;

    public NetworkTerminalMenu(int containerId, Inventory inventory, boolean crafting) {
        this(containerId, inventory, null, inventory.player, crafting);
    }

    public NetworkTerminalMenu(
            int containerId,
            Inventory inventory,
            @Nullable NetworkTerminalBlockEntity terminal,
            Player player,
            boolean crafting
    ) {
        super(crafting
                ? LogisticsRegistrationAdapter.CRAFTING_TERMINAL_MENU.get()
                : LogisticsRegistrationAdapter.TERMINAL_MENU.get(), containerId);
        this.terminal = terminal;
        this.serverPlayer = player instanceof ServerPlayer candidate ? candidate : null;
        this.crafting = crafting;
        access = terminal == null || terminal.getLevel() == null
                ? ContainerLevelAccess.NULL
                : ContainerLevelAccess.create(terminal.getLevel(), terminal.getBlockPos());
        addStandardInventorySlots(
                inventory,
                12,
                154
        );
        addSlot(new Slot(resourceContainer, 0, 85, 91) {
            @Override
            public boolean isActive() {
                return selectedType != LogisticsResourceType.ITEM;
            }
        });
    }

    public List<NetworkTerminalSyncPayload.Entry> entries() {
        return sortedEntries;
    }

    public LogisticsResourceType selectedType() {
        return selectedType;
    }

    public int page() {
        return page;
    }

    public int totalPages() {
        return totalPages;
    }

    public long revision() {
        return revision;
    }

    public boolean crafting() {
        return crafting;
    }

    public List<ItemStack> craftingGrid() {
        return craftingGrid;
    }

    public ItemStack craftingResult() {
        return craftingResult;
    }

    public void applyCraftingState(NetworkCraftingSyncPayload payload) {
        craftingGrid = new ArrayList<>(payload.grid());
        craftingResult = payload.result();
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (serverPlayer == null || terminal == null) {
            return;
        }
        resumePendingAction();
        int interval = Config.LOGISTICS_TERMINAL_REFRESH_TICKS.get();
        if (forceReplace || pendingListing != null || ++updateTicks >= interval) {
            refreshAndSend(forceReplace);
            updateTicks = 0;
        }
    }

    public void handleAction(Player player, NetworkTerminalActionPayload payload) {
        if (player != serverPlayer || serverPlayer == null || terminal == null
                || player.containerMenu != this || payload.containerId() != containerId
                || player.isSpectator() || !stillValid(player)) {
            return;
        }
        long now = player.level().getGameTime();
        if (lastActionTick != now) { actionsThisTick = 0; }
        if (++actionsThisTick > 8) { return; }
        lastActionTick = now;
        if (usesNetwork(payload.action()) && terminal.getLevel() instanceof ServerLevel level) {
            if (pendingAction != null) { return; }
            BlockPos origin = terminal.networkOrigin(payload.resourceType());
            if (origin != null && !LogisticsNetworkManager.get(level).topologyReady(origin, payload.resourceType())) {
                pendingAction = new PendingAction(payload, getCarried().copy(), resourceContainer.getItem(0).copy(),
                        craftingGrid.stream().map(ItemStack::copy).toList(), now + 200);
                return;
            }
        }
        switch (payload.action()) {
            case VIEW -> {
                recipeView = false;
                selectedType = payload.resourceType();
                page = payload.page();
                query = payload.query().trim().toLowerCase(Locale.ROOT);
                refreshAndSend(true);
            }
            case RECIPES -> {
                if (crafting) {
                    recipeView = true;
                    selectedType = LogisticsResourceType.ITEM;
                    page = payload.page();
                    query = payload.query().trim().toLowerCase(Locale.ROOT);
                    refreshAndSend(true);
                }
            }
            case SELECT_RECIPE -> {
                if (crafting && payload.resourceId() != null
                        && terminal.getLevel() instanceof ServerLevel level) {
                    refreshRecipeCatalog(level);
                    var selected = recipeCatalog.stream().filter(entry -> entry.id().equals(payload.resourceId()))
                            .findFirst().orElse(null);
                    if (selected != null) {
                        BlockPos origin = terminal.networkOrigin(LogisticsResourceType.ITEM);
                        List<ItemStack> available = origin == null ? List.of() : LogisticsNetworkManager.get(level)
                                .resourcesFrom(origin, LogisticsResourceType.ITEM).stream()
                                .map(LogisticsNetworkManager.AggregatedResource::icon).toList();
                        craftingGrid = new ArrayList<>(selected.grid(level, available));
                        syncCrafting();
                    }
                }
            }
            case GHOST -> {
                if (crafting && payload.page() >= 0 && payload.page() < 9) {
                    ItemStack stack = payload.amount() == 0 ? ItemStack.EMPTY
                            : getCarried();
                    craftingGrid.set(payload.page(), stack.copyWithCount(stack.isEmpty() ? 0 : 1));
                    syncCrafting();
                }
            }
            case CRAFT -> {
                if (crafting && payload.amount() >= 1 && payload.amount() <= 64
                        && terminal.getLevel() instanceof ServerLevel level) {
                    BlockPos origin = terminal.networkOrigin(LogisticsResourceType.ITEM);
                    if (origin != null) {
                        NetworkCraftingService.craft(level, origin, serverPlayer, craftingGrid, (int) payload.amount());
                        refreshAndSend(false);
                        syncCrafting();
                    }
                }
            }
            case INSERT_HAND -> {
                if (payload.resourceType() == selectedType) {
                    insertFromHand(payload.hand(), payload.amount());
                }
            }
            case INSERT_INVENTORY -> {
                if (payload.resourceType() == LogisticsResourceType.ITEM
                        && selectedType == LogisticsResourceType.ITEM) {
                    if (payload.resourceId() == null) { insertInventory(); }
                    else { insertMatchingInventory(payload); }
                }
            }
            case WITHDRAW -> {
                if (payload.resourceType() == selectedType
                        && payload.adapterId() != null
                        && payload.resourceId() != null
                        && payload.amount() > 0) {
                    withdraw(payload);
                }
            }
            case CURSOR_DEPOSIT -> depositCursor(payload.amount());
            case CURSOR_WITHDRAW -> withdrawTo(payload, payload.page() == 1
                    ? PlayerInventoryWrapper.of(serverPlayer).getMainSlots() : CarriedSlotWrapper.of(this));
            case CONTAINER_DEPOSIT -> transferContainer(payload, false);
            case CONTAINER_WITHDRAW -> transferContainer(payload, true);
            case CRAFT_CURSOR, CRAFT_STACK -> {
                if (crafting && terminal.getLevel() instanceof ServerLevel level) {
                    BlockPos origin = terminal.networkOrigin(LogisticsResourceType.ITEM);
                    var target = payload.action() == NetworkTerminalActionPayload.Action.CRAFT_CURSOR
                            ? CarriedSlotWrapper.of(this) : PlayerInventoryWrapper.of(serverPlayer).getMainSlots();
                    if (origin != null) {
                        int crafts = payload.action() == NetworkTerminalActionPayload.Action.CRAFT_CURSOR ? 1 : 64;
                        for (int index = 0; index < crafts && NetworkCraftingService.craft(
                                level, origin, serverPlayer, craftingGrid, 1, target); index++) {
                            // Each completed recipe remains atomic when the requested stack cannot all fit.
                        }
                        refreshAndSend(false);
                        syncCrafting();
                    }
                }
            }
        }
        broadcastChanges();
    }

    private void resumePendingAction() {
        if (pendingAction == null || !(terminal.getLevel() instanceof ServerLevel level)) { return; }
        var pending = pendingAction;
        if (level.getGameTime() > pending.expiresAt() || serverPlayer.containerMenu != this
                || !stillValid(serverPlayer) || pending.payload().resourceType() != selectedType
                || !ItemStack.matches(getCarried(), pending.cursor())
                || !ItemStack.matches(resourceContainer.getItem(0), pending.container())
                || isCraft(pending.payload().action()) && java.util.stream.IntStream.range(0, 9)
                        .anyMatch(index -> !ItemStack.matches(craftingGrid.get(index), pending.grid().get(index)))) {
            pendingAction = null;
            return;
        }
        BlockPos origin = terminal.networkOrigin(selectedType);
        if (origin != null && LogisticsNetworkManager.get(level).topologyReady(origin, selectedType)
                && (lastActionTick != level.getGameTime() || actionsThisTick < 8)) {
            pendingAction = null;
            handleAction(serverPlayer, pending.payload());
        }
    }

    private static boolean usesNetwork(NetworkTerminalActionPayload.Action action) {
        return switch (action) {
            case INSERT_INVENTORY, CURSOR_DEPOSIT, CURSOR_WITHDRAW, CONTAINER_DEPOSIT, CONTAINER_WITHDRAW,
                    CRAFT_CURSOR, CRAFT_STACK -> true;
            default -> false;
        };
    }

    private static boolean isCraft(NetworkTerminalActionPayload.Action action) {
        return action == NetworkTerminalActionPayload.Action.CRAFT || action == NetworkTerminalActionPayload.Action.CRAFT_CURSOR
                || action == NetworkTerminalActionPayload.Action.CRAFT_STACK;
    }

    private record PendingAction(NetworkTerminalActionPayload payload, ItemStack cursor, ItemStack container,
                                 List<ItemStack> grid, long expiresAt) { }

    public void applyServerState(NetworkTerminalSyncPayload payload) {
        if (payload.revision() < revision) {
            return;
        }
        if (payload.replace()) {
            entries.clear();
        }
        payload.removedKeys().forEach(entries::remove);
        payload.entries().forEach(entry -> entries.put(entry.key(), entry));
        selectedType = payload.resourceType();
        page = payload.page();
        totalPages = payload.totalPages();
        revision = payload.revision();
        rebuildEntries();
    }

    @Override
    public boolean stillValid(@NonNull Player player) {
        return stillValid(
                access,
                player,
                crafting
                        ? LogisticsRegistrationAdapter.CRAFTING_TERMINAL_BLOCK.get()
                        : LogisticsRegistrationAdapter.TERMINAL_BLOCK.get()
        );
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
        ItemStack stack = slot.getItem();
        ItemStack result = stack.copy();
        if (index < PLAYER_HOTBAR_END && selectedType != LogisticsResourceType.ITEM) {
            if (!moveItemStackTo(stack, PLAYER_HOTBAR_END, PLAYER_HOTBAR_END + 1, false)) { return ItemStack.EMPTY; }
            if (stack.isEmpty()) { slot.setByPlayer(ItemStack.EMPTY); } else { slot.setChanged(); }
            return result;
        }
        // Virtual network slots cannot be predicted by moving items into the hotbar.
        if (index < PLAYER_HOTBAR_END && serverPlayer == null) { return ItemStack.EMPTY; }
        if (index < PLAYER_HOTBAR_END && serverPlayer != null && terminal != null
                && terminal.getLevel() instanceof ServerLevel level) {
            BlockPos origin = terminal.networkOrigin(LogisticsResourceType.ITEM);
            if (origin != null) {
                var source = net.neoforged.neoforge.transfer.RangedResourceHandler.ofSingleIndex(
                        VanillaContainerWrapper.of(slot.container), slot.getContainerSlot());
                long moved = LogisticsNetworkManager.get(level).transferIntoNetwork(origin,
                        new com.cosmocraft.trading_cells.feature.logistics.adapters.neoforge.ItemLogisticsAdapter().id(), source, stack.getCount());
                refreshAndSend(false);
                return moved > 0 ? result : ItemStack.EMPTY;
            }
            return ItemStack.EMPTY;
        }
        boolean moved;
        if (index >= PLAYER_HOTBAR_END) {
            moved = moveItemStackTo(stack, 0, PLAYER_HOTBAR_END, false);
        } else if (index < PLAYER_INVENTORY_END) {
            moved = moveItemStackTo(stack, PLAYER_INVENTORY_END, PLAYER_HOTBAR_END, false);
        } else {
            moved = moveItemStackTo(stack, 0, PLAYER_INVENTORY_END, false);
        }
        if (!moved) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return result;
    }

    private void refreshAndSend(boolean replace) {
        if (!(terminal.getLevel() instanceof ServerLevel level) || serverPlayer.connection == null) {
            return;
        }
        BlockPos origin = terminal.networkOrigin(selectedType);
        if (!recipeView && origin != null
                && !LogisticsNetworkManager.get(level).topologyReady(origin, selectedType)) {
            forceReplace = true;
            return;
        }
        List<LogisticsNetworkManager.AggregatedResource> resources = List.of();
        if (!recipeView && origin != null) {
            var manager = LogisticsNetworkManager.get(level);
            if (pendingListing == null || pendingListing.stale() || !origin.equals(listingOrigin)
                    || listingType != selectedType) {
                pendingListing = manager.beginListing(origin, selectedType);
                listingOrigin = origin;
                listingType = selectedType;
            }
            if (!pendingListing.advance()) {
                forceReplace |= replace;
                return;
            }
            resources = pendingListing.result();
        }
        pendingListing = null;
        replace |= forceReplace;
        forceReplace = false;
        if (recipeView) {
            refreshRecipeCatalog(level);
            resources = recipeCatalog.stream().map(entry -> new LogisticsNetworkManager.AggregatedResource(
                    net.minecraft.resources.Identifier.fromNamespaceAndPath("trading_cells", "crafting"),
                    LogisticsResourceType.ITEM, entry.id(), "", entry.result().getHoverName(), entry.result(), 1
            )).sorted(Comparator.comparing(value -> value.displayName().getString())).toList();
        }
        if (!query.isEmpty()) {
            resources = resources.stream().filter(resource ->
                    resource.displayName().getString().toLowerCase(Locale.ROOT).contains(query)
                            || resource.resourceId().toString().contains(query)
            ).toList();
        }
        totalPages = Math.max(1, (resources.size() + NETWORK_COLUMNS - 1) / NETWORK_COLUMNS);
        page = Math.clamp(page, 0, totalPages - 1);
        int start = Math.min(resources.size(), page * NETWORK_COLUMNS);
        int end = Math.min(resources.size(), start + PAGE_SIZE);
        Map<String, NetworkTerminalSyncPayload.Entry> current = new LinkedHashMap<>();
        for (int index = start; index < end; index++) {
            var resource = resources.get(index);
            NetworkTerminalSyncPayload.Entry entry = new NetworkTerminalSyncPayload.Entry(
                    resource.adapterId(),
                    resource.resourceId(),
                    resource.componentFingerprint(),
                    resource.displayName().getString(),
                    resource.icon(),
                    resource.amount()
            );
            current.put(entry.key(), entry);
        }

        List<NetworkTerminalSyncPayload.Entry> changed = new ArrayList<>();
        List<String> removed = new ArrayList<>();
        if (replace) {
            changed.addAll(current.values());
        } else {
            current.forEach((key, value) -> {
                NetworkTerminalSyncPayload.Entry previous = entries.get(key);
                if (previous == null || previous.amount() != value.amount()
                        || !previous.displayName().equals(value.displayName())
                        || !ItemStack.matches(previous.icon(), value.icon())) {
                    changed.add(value);
                }
            });
            entries.keySet().stream().filter(key -> !current.containsKey(key)).forEach(removed::add);
            if (changed.isEmpty() && removed.isEmpty() && page == sentPage && totalPages == sentTotalPages) {
                return;
            }
        }
        entries.clear();
        entries.putAll(current);
        rebuildEntries();
        sentPage = page;
        sentTotalPages = totalPages;
        revision++;
        PacketDistributor.sendToPlayer(serverPlayer, new NetworkTerminalSyncPayload(
                NetworkTerminalSyncPayload.CURRENT_PROTOCOL_VERSION,
                containerId,
                selectedType,
                page,
                totalPages,
                revision,
                replace,
                changed,
                removed
        ));
    }

    private void syncCrafting() {
        if (serverPlayer != null && terminal.getLevel() instanceof ServerLevel level) {
            craftingResult = NetworkCraftingService.preview(level, craftingGrid);
            if (serverPlayer.connection != null) {
                PacketDistributor.sendToPlayer(serverPlayer,
                        new NetworkCraftingSyncPayload(containerId, craftingGrid, craftingResult));
            }
        }
    }

    @Override
    public void removed(Player player) {
        pendingAction = null;
        super.removed(player);
        if (player instanceof ServerPlayer) {
            clearContainer(player, resourceContainer);
        }
    }

    private void depositCursor(long amount) {
        if (selectedType != LogisticsResourceType.ITEM || !(terminal.getLevel() instanceof ServerLevel level)) {
            return;
        }
        BlockPos origin = terminal.networkOrigin(selectedType);
        if (origin != null) {
            LogisticsNetworkManager.get(level).transferIntoNetwork(origin,
                    new com.cosmocraft.trading_cells.feature.logistics.adapters.neoforge.ItemLogisticsAdapter().id(),
                    CarriedSlotWrapper.of(this), Math.min(64, amount));
            refreshAndSend(false);
        }
    }

    private void withdrawTo(NetworkTerminalActionPayload payload, Object target) {
        if (payload.resourceType() != selectedType || payload.adapterId() == null || payload.resourceId() == null
                || !(terminal.getLevel() instanceof ServerLevel level)) {
            return;
        }
        var adapter = LogisticsResourceAdapters.byId(payload.adapterId());
        BlockPos origin = terminal.networkOrigin(selectedType);
        if (adapter != null && adapter.type() == selectedType && origin != null) {
            LogisticsNetworkManager.get(level).transferFromNetwork(origin, adapter.id(), payload.resourceId(),
                    payload.componentFingerprint(), target, Math.min(64, payload.amount()));
            refreshAndSend(false);
        }
    }

    @SuppressWarnings("unchecked")
    private void transferContainer(NetworkTerminalActionPayload payload, boolean withdraw) {
        if (selectedType == LogisticsResourceType.ITEM || !(terminal.getLevel() instanceof ServerLevel level)) {
            return;
        }
        BlockPos origin = terminal.networkOrigin(selectedType);
        if (origin == null) {
            return;
        }
        ItemAccess access = ItemAccess.forHandlerIndexStrict(VanillaContainerWrapper.of(resourceContainer), 0);
        var manager = LogisticsNetworkManager.get(level);
        for (var adapter : LogisticsResourceAdapters.forType(selectedType)) {
            Object handler = adapter.findContainer(access);
            if (handler == null) {
                continue;
            }
            if (withdraw && adapter.id().equals(payload.adapterId()) && payload.resourceId() != null) {
                manager.transferFromNetwork(origin, adapter.id(), payload.resourceId(), payload.componentFingerprint(), handler, Long.MAX_VALUE);
            } else if (!withdraw) {
                manager.transferIntoNetwork(origin, adapter.id(), handler, Long.MAX_VALUE);
            }
        }
        refreshAndSend(false);
    }

    private void rebuildEntries() {
        sortedEntries = entries.values().stream()
                .sorted(Comparator.comparing(NetworkTerminalSyncPayload.Entry::displayName)).toList();
    }

    private void refreshRecipeCatalog(ServerLevel level) {
        var recipes = level.recipeAccess().recipeMap();
        if (recipeCatalog == null || catalogRecipes != recipes) {
            recipeCatalog = NetworkCraftingCatalog.create(level);
            catalogRecipes = recipes;
        }
    }

    private void insertFromHand(InteractionHand hand, long amount) {
        BlockPos origin = terminal.networkOrigin(selectedType);
        if (origin == null || !(terminal.getLevel() instanceof ServerLevel level)) {
            return;
        }
        LogisticsNetworkManager manager = LogisticsNetworkManager.get(level);
        for (LogisticsResourceAdapter<?, ?> adapter : LogisticsResourceAdapters.forType(selectedType)) {
            Object source = findCarried(adapter, serverPlayer, hand);
            if (source != null && manager.transferIntoNetwork(origin, adapter.id(), source, amount) > 0) {
                refreshAndSend(false);
                return;
            }
        }
    }

    private void insertMatchingInventory(NetworkTerminalActionPayload payload) {
        BlockPos origin = terminal.networkOrigin(LogisticsResourceType.ITEM);
        if (origin == null || !(terminal.getLevel() instanceof ServerLevel level)) { return; }
        var manager = LogisticsNetworkManager.get(level);
        var adapter = new com.cosmocraft.trading_cells.feature.logistics.adapters.neoforge.ItemLogisticsAdapter();
        for (int index = 0; index < PLAYER_HOTBAR_END; index++) {
            Slot slot = slots.get(index);
            ItemStack stack = slot.getItem();
            if (stack.isEmpty() || !net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(payload.resourceId())
                    || !com.cosmocraft.trading_cells.feature.logistics.adapters.neoforge.LogisticsComponentData
                    .fingerprint(stack.getComponentsPatch()).equals(payload.componentFingerprint())) { continue; }
            var source = net.neoforged.neoforge.transfer.RangedResourceHandler.ofSingleIndex(
                    VanillaContainerWrapper.of(slot.container), slot.getContainerSlot());
            manager.transferIntoNetwork(origin, adapter.id(), source, stack.getCount());
        }
        refreshAndSend(false);
    }

    private void insertInventory() {
        BlockPos origin = terminal.networkOrigin(LogisticsResourceType.ITEM);
        if (origin == null || !(terminal.getLevel() instanceof ServerLevel level)) {
            return;
        }
        LogisticsResourceAdapters.forType(LogisticsResourceType.ITEM).stream()
                .filter(adapter -> adapter.id().getNamespace().equals("trading_cells")
                        && adapter.id().getPath().equals("item"))
                .findFirst()
                .ifPresent(adapter -> LogisticsNetworkManager.get(level).transferIntoNetwork(
                        origin,
                        adapter.id(),
                        PlayerInventoryWrapper.of(serverPlayer).getMainSlots(),
                        Long.MAX_VALUE
                ));
        refreshAndSend(false);
    }

    private void withdraw(NetworkTerminalActionPayload payload) {
        BlockPos origin = terminal.networkOrigin(selectedType);
        if (origin == null || !(terminal.getLevel() instanceof ServerLevel level)) {
            return;
        }
        LogisticsResourceAdapter<?, ?> adapter = LogisticsResourceAdapters.byId(payload.adapterId());
        if (adapter == null || adapter.type() != selectedType) {
            return;
        }
        Object target = selectedType == LogisticsResourceType.ITEM
                ? PlayerInventoryWrapper.of(serverPlayer)
                : findCarried(adapter, serverPlayer, payload.hand());
        if (target == null) {
            return;
        }
        LogisticsNetworkManager.get(level).transferFromNetwork(
                origin,
                payload.adapterId(),
                payload.resourceId(),
                payload.componentFingerprint(),
                target,
                payload.amount()
        );
        refreshAndSend(false);
    }

    @SuppressWarnings("unchecked")
    private static Object findCarried(
            LogisticsResourceAdapter<?, ?> adapter,
            ServerPlayer player,
            InteractionHand hand
    ) {
        return ((LogisticsResourceAdapter<Object, Object>) adapter).findCarried(player, hand);
    }
}
