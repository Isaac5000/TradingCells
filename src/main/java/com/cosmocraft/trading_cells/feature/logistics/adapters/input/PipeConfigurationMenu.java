package com.cosmocraft.trading_cells.feature.logistics.adapters.input;

import com.cosmocraft.trading_cells.feature.logistics.adapters.output.LogisticsRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.logistics.domain.model.PipeKind;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class PipeConfigurationMenu extends AbstractContainerMenu {
    private final LogisticsPipeBlockEntity pipe;
    private final BlockPos pos;
    private final Direction face;
    private final InteractionHand hand;
    private final PipeKind kind;
    private final PipeFaceConfiguration initial;
    private final long revision;
    private final Player owner;
    private PipeFaceConfiguration latest;
    private long syncedRevision = -1, upgradeGeneration;
    private ItemStack trackedUpgrade = ItemStack.EMPTY;
    private boolean trackedEmpty = true;
    private boolean ruleEditing;
    private LogisticsNetworkManager.ChannelSearch channelSearch;
    private long lastQueryTick = -100;
    private com.cosmocraft.trading_cells.platform.neoforge.network.PipeChannelSuggestionsPayload channelSuggestions;

    public PipeConfigurationMenu(int id, Inventory inventory, RegistryFriendlyByteBuf data) {
        this(id, inventory, null, data.readBlockPos(), data.readEnum(Direction.class),
                data.readEnum(InteractionHand.class), data.readEnum(PipeKind.class),
                PipeFaceConfiguration.load(data.readNbt()), data.readVarLong());
    }

    public PipeConfigurationMenu(int id, Inventory inventory, LogisticsPipeBlockEntity pipe,
            Direction face, InteractionHand hand) {
        this(id, inventory, pipe, pipe.getBlockPos(), face, hand, pipe.kind(), pipe.face(face).copy(), pipe.revision());
    }

    private PipeConfigurationMenu(int id, Inventory inventory, LogisticsPipeBlockEntity pipe,
            BlockPos pos, Direction face, InteractionHand hand, PipeKind kind,
            PipeFaceConfiguration initial, long revision) {
        super(LogisticsRegistrationAdapter.PIPE_MENU.get(), id);
        this.pipe = pipe;
        this.pos = pos;
        this.face = face;
        this.hand = hand;
        this.kind = kind;
        this.initial = initial;
        this.revision = revision;
        this.owner = inventory.player;
        this.latest = initial.copy();
        if (pipe != null && stillValid(inventory.player)) { pipe.reclaimLegacyUpgrades(face, inventory.player); }
        var upgrades = pipe == null ? new SimpleContainer(1) : pipe.upgrades(face);
        for (int index = 0; index < 1; index++) {
            addSlot(new Slot(upgrades, index, 17, 56) {
                @Override
                public boolean mayPlace(ItemStack stack) { return stack.getItem() instanceof PipeUpgradeItem; }
                @Override
                public int getMaxStackSize() { return 1; }
                @Override
                public boolean isActive() { return !ruleEditing; }
            });
        }
        addStandardInventorySlots(inventory, 36, 160);
        com.cosmocraft.trading_cells.platform.neoforge.menu.PlayerEquipmentSlots
                .create(inventory, 12, 146, 164, 182, 200, 218).forEach(this::addSlot);
        trackedUpgrade = upgrades.getItem(0);
        trackedEmpty = trackedUpgrade.isEmpty();
    }

    public BlockPos pos() { return pos; }
    public Direction face() { return face; }
    public InteractionHand hand() { return hand; }
    public PipeKind kind() { return kind; }
    public PipeFaceConfiguration initial() { return initial.copy(); }
    public long revision() { return revision; }
    public void setRuleEditing(boolean ruleEditing) { this.ruleEditing = ruleEditing; }
    public void applyChannelSuggestions(com.cosmocraft.trading_cells.platform.neoforge.network.PipeChannelSuggestionsPayload payload) { channelSuggestions = payload; }
    public com.cosmocraft.trading_cells.platform.neoforge.network.PipeChannelSuggestionsPayload channelSuggestions() { return channelSuggestions; }

    public void queryChannels(com.cosmocraft.trading_cells.feature.logistics.domain.model.LogisticsResourceType type, String prefix, boolean active) {
        if (pipe == null || !(owner.level() instanceof net.minecraft.server.level.ServerLevel level)) { return; }
        if (!active) { closeChannelSearch(); return; }
        if (!stillValid(owner) || !kind.supports(type) || !pipe.face(face).upgradeTier().allowsChannels()
                || level.getGameTime() - lastQueryTick < 4 || prefix.length() > 256) { return; }
        lastQueryTick = level.getGameTime();
        closeChannelSearch();
        channelSuggestions = null;
        channelSearch = LogisticsNetworkManager.get(level).searchChannels(pos, type, prefix);
    }

    private void closeChannelSearch() {
        if (channelSearch != null && owner.level() instanceof net.minecraft.server.level.ServerLevel level) {
            LogisticsNetworkManager.get(level).closeChannelSearch(channelSearch);
        }
        channelSearch = null;
    }

    @Override
    public void removed(Player player) { closeChannelSearch(); super.removed(player); }
    public long upgradeGeneration() { return upgradeGeneration; }
    public PipeFaceConfiguration latest() { return latest.copy(); }
    public long latestRevision() { return Math.max(revision, syncedRevision); }

    public void applyServerState(com.cosmocraft.trading_cells.platform.neoforge.network.PipeMenuSyncPayload payload) {
        if (payload.revision() < syncedRevision) { return; }
        syncedRevision = payload.revision();
        upgradeGeneration = payload.upgradeGeneration();
        latest = PipeFaceConfiguration.load(payload.configuration());
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (pipe == null || !(owner instanceof net.minecraft.server.level.ServerPlayer player) || !stillValid(player)) { return; }
        if (channelSearch != null && player.level().getGameTime() % 5 == 0) {
            LogisticsNetworkManager.get(player.level()).advanceChannelSearch(channelSearch);
            var result = new com.cosmocraft.trading_cells.platform.neoforge.network.PipeChannelSuggestionsPayload(
                    containerId, channelSearch.type(), channelSearch.prefix(), channelSearch.results(), channelSearch.complete());
            if (!result.equals(channelSuggestions)) {
                channelSuggestions = result;
                net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, result);
            }
        }
        var stack = getSlot(0).getItem();
        if (trackedUpgrade != stack || trackedEmpty != stack.isEmpty()) {
            upgradeGeneration++;
            trackedUpgrade = stack;
            trackedEmpty = stack.isEmpty();
        }
        if (syncedRevision != pipe.revision()) {
            syncedRevision = pipe.revision();
            latest = pipe.face(face).copy();
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,
                    new com.cosmocraft.trading_cells.platform.neoforge.network.PipeMenuSyncPayload(containerId, syncedRevision, upgradeGeneration, latest.save()));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return !player.isSpectator() && player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(pos)) <= 64
                && (pipe == null || !pipe.isRemoved() && player.level().getBlockEntity(pos) == pipe
                && player.level().mayInteract(player, pos)
                && player.mayBuild());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (!stillValid(player) || index < 0 || index >= slots.size()) { return ItemStack.EMPTY; }
        Slot slot = slots.get(index);
        ItemStack stack = slot.getItem();
        if (stack.isEmpty()) { return ItemStack.EMPTY; }
        if (index == 0 && pipe != null) { PipeUpgradeItem.withProfiles(stack, pipe.face(face).saveActiveProfiles()); }
        ItemStack copy = stack.copy();
        boolean moved = index == 0 || index >= 37 ? moveItemStackTo(stack, 1, 37, true)
                : stack.getItem() instanceof PipeUpgradeItem ? moveItemStackTo(stack, 0, 1, false)
                : moveItemStackTo(stack, 37, 41, false);
        if (!moved) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) { slot.setByPlayer(ItemStack.EMPTY); } else { slot.setChanged(); }
        slot.onTake(player, copy);
        return copy;
    }
}
