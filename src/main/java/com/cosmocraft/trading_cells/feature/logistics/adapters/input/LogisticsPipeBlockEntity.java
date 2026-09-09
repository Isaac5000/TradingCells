package com.cosmocraft.trading_cells.feature.logistics.adapters.input;

import com.cosmocraft.trading_cells.feature.logistics.adapters.api.LogisticsResourceAdapters;
import com.cosmocraft.trading_cells.feature.logistics.domain.model.LogisticsResourceType;
import com.cosmocraft.trading_cells.feature.logistics.domain.model.PipeKind;
import com.cosmocraft.trading_cells.feature.logistics.domain.model.PipeSideMode;
import com.cosmocraft.trading_cells.feature.logistics.domain.model.PipeUpgradeTier;
import com.cosmocraft.trading_cells.feature.logistics.adapters.output.LogisticsRegistrationAdapter;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.NonNull;

public final class LogisticsPipeBlockEntity extends BlockEntity implements LogisticsBridgeNode {
    public static final int SCHEMA_VERSION = 1;
    private static final String FACE_PREFIX = "LogisticsFace";

    private final Map<Direction, PipeFaceConfiguration> faces = new EnumMap<>(Direction.class);
    private final Map<Direction, net.minecraft.world.SimpleContainer> upgrades = new EnumMap<>(Direction.class);
    private final Map<Direction, java.util.List<ItemStack>> legacyUpgrades = new EnumMap<>(Direction.class);
    private boolean loadingUpgrades;
    private long revision;
    private final java.util.List<net.neoforged.neoforge.capabilities.ICapabilityInvalidationListener>
            capabilityListeners = new java.util.ArrayList<>(6);

    public LogisticsPipeBlockEntity(BlockPos pos, BlockState state) {
        super(LogisticsRegistrationAdapter.PIPE_BLOCK_ENTITY.get(), pos, state);
        for (Direction direction : Direction.values()) {
            faces.put(direction, new PipeFaceConfiguration());
            legacyUpgrades.put(direction, new java.util.ArrayList<>(3));
            upgrades.put(direction, new net.minecraft.world.SimpleContainer(1) {
                @Override
                public int getMaxStackSize() { return 1; }

                @Override
                public boolean canPlaceItem(int slot, ItemStack stack) {
                    return stack.getItem() instanceof PipeUpgradeItem;
                }

                @Override
                public ItemStack removeItem(int slot, int count) {
                    CompoundTag profiles = face(direction).saveActiveProfiles();
                    return PipeUpgradeItem.withProfiles(super.removeItem(slot, count), profiles);
                }

                @Override
                public void setItem(int slot, ItemStack stack) {
                    if (!stack.isEmpty() && !canPlaceItem(slot, stack)) { return; }
                    boolean replacement = getItem(slot) != stack;
                    super.setItem(slot, stack);
                    if (!loadingUpgrades && replacement && !stack.isEmpty()) {
                        CompoundTag profile = PipeUpgradeItem.storedProfiles(stack);
                        face(direction).loadActiveProfiles(profile == null ? new CompoundTag() : profile);
                        writeInstalledProfile(direction);
                        changed(true);
                    }
                }

                @Override
                public void setChanged() {
                    super.setChanged();
                    if (!loadingUpgrades) { upgradesChanged(direction); }
                }
            });
        }
    }

    public PipeKind kind() {
        return getBlockState().getBlock() instanceof LogisticsPipeBlock pipe ? pipe.kind() : PipeKind.ITEM;
    }

    @Override
    public boolean supportsLogisticsResource(LogisticsResourceType type) {
        return kind().supports(type);
    }

    public PipeFaceConfiguration face(Direction direction) {
        return faces.get(direction);
    }

    public long revision() {
        return revision;
    }

    public boolean isConnectedToPipe(Direction direction) {
        if (level == null || face(direction).pipeDisconnected()) {
            return false;
        }
        BlockEntity neighbor = loadedNeighbor(direction);
        return neighbor instanceof LogisticsPipeBlockEntity other
                && !other.face(direction.getOpposite()).pipeDisconnected()
                && kind().connectsTo(other.kind());
    }

    public boolean hasCompatibleEndpoint(Direction direction) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return getBlockState().getValue(LogisticsPipeBlock.connectionProperty(direction))
                    != PipeConnection.NONE;
        }
        BlockPos target = worldPosition.relative(direction);
        Direction targetSide = direction.getOpposite();
        for (LogisticsResourceType type : kind().resourceTypes()) {
            for (var adapter : LogisticsResourceAdapters.forType(type)) {
                if (LogisticsNetworkManager.findHandler(adapter, serverLevel, target, targetSide) != null) {
                    return true;
                }
            }
        }
        return false;
    }

    public void cycleSide(Direction direction) {
        PipeFaceConfiguration configuration = face(direction);
        if (isNeighborPipe(direction)) {
            boolean disconnect = isConnectedToPipe(direction);
            configuration.setPipeDisconnected(disconnect);
            if (loadedNeighbor(direction) instanceof LogisticsPipeBlockEntity other) {
                other.face(direction.getOpposite()).setPipeDisconnected(disconnect);
                other.changed(true);
            }
            changed(true);
            refreshConnectionsAround();
            return;
        }
        PipeSideMode next = configuration.mode().next();
        configuration.setMode(next, true);
        changed(true);
        refreshConnections();
    }

    public void setMode(Direction direction, PipeSideMode mode) {
        face(direction).setMode(mode, true);
        changed(true);
        refreshConnections();
    }

    public void setPriority(Direction direction, int priority) {
        face(direction).setPriority(priority);
        changed(true);
    }

    public void setUpgradeTier(Direction direction, PipeUpgradeTier tier) {
        upgrades.get(direction).setItem(0, tier == PipeUpgradeTier.BARE ? ItemStack.EMPTY
                : LogisticsRegistrationAdapter.upgradeItem(tier).get().getDefaultInstance());
    }

    public net.minecraft.world.SimpleContainer upgrades(Direction direction) { return upgrades.get(direction); }

    public long transferRate(Direction direction, LogisticsResourceType type) {
        return highestInstalledTier(direction).rate(type);
    }

    private void upgradesChanged(Direction direction) {
        face(direction).setUpgradeTier(highestInstalledTier(direction));
        changed(true);
    }

    private PipeUpgradeTier highestInstalledTier(Direction direction) {
        return upgrades.get(direction).getItem(0).getItem() instanceof PipeUpgradeItem upgrade
                ? upgrade.tier() : PipeUpgradeTier.BARE;
    }

    private void writeInstalledProfile(Direction direction) {
        PipeUpgradeItem.withProfiles(upgrades.get(direction).getItem(0), face(direction).saveActiveProfiles());
    }

    public void reclaimLegacyUpgrades(Direction direction, net.minecraft.world.entity.player.Player player) {
        if (!(level instanceof ServerLevel) || player.isSpectator()) { return; }
        var pending = legacyUpgrades.get(direction);
        if (pending.isEmpty()) { return; }
        var returned = java.util.List.copyOf(pending);
        pending.clear();
        changed(true);
        for (ItemStack upgrade : returned) {
            if (!player.getInventory().add(upgrade)) { player.drop(upgrade, false); }
        }
    }

    public void applyFaceConfiguration(Direction direction, PipeFaceConfiguration requested) {
        face(direction).applyEditableSettings(requested);
        writeInstalledProfile(direction);
        changed(true);
        refreshConnections();
    }

    public void applyFaceModeAndPriority(Direction direction, PipeFaceConfiguration requested) {
        PipeFaceConfiguration configuration = face(direction);
        configuration.setMode(requested.mode(), true);
        configuration.setPriority(requested.priority());
        changed(true);
        refreshConnections();
    }

    public ItemStack replaceUpgrade(
            Direction direction,
            PipeUpgradeTier replacement,
            ItemStack replacementStack
    ) {
        PipeFaceConfiguration configuration = face(direction);
        ItemStack returned = PipeUpgradeItem.withProfiles(upgrades.get(direction).getItem(0).copy(),
                configuration.saveActiveProfiles());
        upgrades.get(direction).setItem(0, replacementStack.copyWithCount(replacement == PipeUpgradeTier.BARE ? 0 : 1));
        if (replacement != PipeUpgradeTier.BARE) {
            CompoundTag storedProfiles = PipeUpgradeItem.storedProfiles(replacementStack);
            if (storedProfiles != null) {
                configuration.loadActiveProfiles(storedProfiles);
            }
        }
        changed(true);
        return returned;
    }

    public void refreshConnections() {
        if (!(level instanceof ServerLevel)) {
            return;
        }
        BlockState current = getBlockState();
        BlockState updated = current;
        boolean configurationChanged = false;
        for (Direction direction : Direction.values()) {
            PipeFaceConfiguration face = face(direction);
            PipeConnection connection = PipeConnection.NONE;
            if (isConnectedToPipe(direction) || isConnectedBridge(direction)) {
                connection = PipeConnection.PIPE;
            } else if (hasCompatibleEndpoint(direction)) {
                if (!face.explicitMode() && face.mode() == PipeSideMode.NONE) {
                    face.setMode(PipeSideMode.INSERT, false);
                    configurationChanged = true;
                }
                connection = switch (face.mode()) {
                    case NONE -> PipeConnection.NONE;
                    case INSERT -> PipeConnection.INSERT;
                    case EXTRACT -> PipeConnection.EXTRACT;
                };
            }
            updated = updated.setValue(LogisticsPipeBlock.connectionProperty(direction), connection);
        }
        if (updated != current) {
            level.setBlock(worldPosition, updated, Block.UPDATE_CLIENTS);
        }
        if (configurationChanged) {
            changed(true);
        }
    }

    public void refreshConnectionsAround() {
        refreshConnections();
        if (level == null) {
            return;
        }
        for (Direction direction : Direction.values()) {
            if (loadedNeighbor(direction) instanceof LogisticsPipeBlockEntity pipe) {
                pipe.refreshConnections();
            }
        }
        if (level instanceof ServerLevel serverLevel) {
            LogisticsNetworkManager.get(serverLevel).configurationChanged(this);
        }
    }

    public java.util.List<ItemStack> installedUpgradeDrops() {
        var drops = new java.util.ArrayList<ItemStack>();
        for (Direction direction : Direction.values()) {
            var inventory = upgrades.get(direction);
            for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
                if (!inventory.getItem(slot).isEmpty()) {
                    drops.add(PipeUpgradeItem.withProfiles(inventory.getItem(slot).copy(), face(direction).saveActiveProfiles()));
                }
            }
            for (ItemStack legacy : legacyUpgrades.get(direction)) { drops.add(legacy.copy()); }
        }
        return java.util.List.copyOf(drops);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel serverLevel) {
            LogisticsNetworkManager.get(serverLevel).register(this);
            capabilityListeners.clear();
            for (Direction direction : Direction.values()) {
                net.neoforged.neoforge.capabilities.ICapabilityInvalidationListener listener = () -> {
                    if (isRemoved() || level != serverLevel) {
                        return false;
                    }
                    LogisticsNetworkManager.get(serverLevel).requestEndpointRefresh(worldPosition);
                    return true;
                };
                capabilityListeners.add(listener);
                serverLevel.registerCapabilityListener(worldPosition.relative(direction), listener);
            }
            refreshConnectionsAround();
        }
    }

    @Override
    public void setRemoved() {
        capabilityListeners.clear();
        if (level instanceof ServerLevel serverLevel) {
            LogisticsNetworkManager.get(serverLevel).unregister(worldPosition);
        }
        super.setRemoved();
    }

    @Override
    protected void loadAdditional(@NonNull ValueInput input) {
        super.loadAdditional(input);
        revision = input.getLongOr("LogisticsRevision", 0);
        loadingUpgrades = true;
        for (Direction direction : Direction.values()) {
            input.read(FACE_PREFIX + direction.getSerializedName(), CompoundTag.CODEC)
                    .ifPresent(tag -> faces.put(direction, PipeFaceConfiguration.load(tag)));
            var inventory = upgrades.get(direction);
            int version = input.getIntOr("UpgradeSlotsVersion", 0);
            var candidates = new java.util.ArrayList<ItemStack>(4);
            var pending = legacyUpgrades.get(direction);
            pending.clear();
            for (int slot = 0; slot < (version < 2 ? 4 : 1); slot++) {
                ItemStack stack = input.read("Upgrade" + direction.getSerializedName() + slot, ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
                if (version == 0 && slot == 0 && face(direction).upgradeTier() != PipeUpgradeTier.BARE) {
                    stack = LogisticsRegistrationAdapter.upgradeItem(face(direction).upgradeTier()).get().getDefaultInstance();
                }
                if (stack.getItem() instanceof PipeUpgradeItem) {
                    candidates.add(PipeUpgradeItem.withProfiles(stack.copyWithCount(1), face(direction).saveActiveProfiles()));
                }
            }
            if (version < 2) {
                candidates.sort(java.util.Comparator.comparingInt(stack -> -((PipeUpgradeItem) stack.getItem()).tier().id()));
                if (candidates.size() > 1) { pending.addAll(candidates.subList(1, candidates.size())); }
            } else {
                for (int slot = 0; slot < 3; slot++) {
                    ItemStack stack = input.read("LegacyUpgrade" + direction.getSerializedName() + slot,
                            ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
                    if (stack.getItem() instanceof PipeUpgradeItem) { pending.add(stack.copyWithCount(1)); }
                }
            }
            inventory.setItem(0, candidates.isEmpty() ? ItemStack.EMPTY : candidates.getFirst());
            face(direction).setUpgradeTier(highestInstalledTier(direction));
        }
        loadingUpgrades = false;
        if (level instanceof ServerLevel serverLevel) {
            LogisticsNetworkManager.get(serverLevel).configurationChanged(this);
            refreshConnections();
        }
    }

    @Override
    protected void saveAdditional(@NonNull ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("LogisticsSchemaVersion", SCHEMA_VERSION);
        output.putLong("LogisticsRevision", revision);
        output.putInt("UpgradeSlotsVersion", 2);
        for (Direction direction : Direction.values()) {
            output.store(FACE_PREFIX + direction.getSerializedName(), CompoundTag.CODEC, face(direction).save());
            output.store("Upgrade" + direction.getSerializedName() + 0, ItemStack.OPTIONAL_CODEC,
                    PipeUpgradeItem.withProfiles(upgrades.get(direction).getItem(0).copy(), face(direction).saveActiveProfiles()));
            var pending = legacyUpgrades.get(direction);
            for (int slot = 0; slot < pending.size(); slot++) {
                output.store("LegacyUpgrade" + direction.getSerializedName() + slot, ItemStack.OPTIONAL_CODEC, pending.get(slot));
            }
        }
    }

    @Override
    public @NonNull Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public @NonNull CompoundTag getUpdateTag(HolderLookup.@NonNull Provider registries) {
        return saveCustomOnly(registries);
    }

    private boolean isNeighborPipe(Direction direction) {
        return loadedNeighbor(direction) instanceof LogisticsPipeBlockEntity;
    }

    private boolean isConnectedBridge(Direction direction) {
        if (level == null || face(direction).pipeDisconnected()) {
            return false;
        }
        BlockEntity neighbor = loadedNeighbor(direction);
        if (neighbor instanceof LogisticsPipeBlockEntity || !(neighbor instanceof LogisticsBridgeNode bridge)) {
            return false;
        }
        for (LogisticsResourceType type : kind().resourceTypes()) {
            if (bridge.supportsLogisticsResource(type)) {
                return true;
            }
        }
        return false;
    }

    private void changed(boolean sync) {
        revision++;
        setChanged();
        if (level instanceof ServerLevel serverLevel) {
            LogisticsNetworkManager.get(serverLevel).configurationChanged(this);
            if (sync) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            }
        }
    }

    private BlockEntity loadedNeighbor(Direction direction) {
        if (level == null) {
            return null;
        }
        BlockPos pos = worldPosition.relative(direction);
        if (level instanceof ServerLevel serverLevel) {
            var chunk = serverLevel.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
            return chunk == null ? null : chunk.getBlockEntity(pos);
        }
        return level.getBlockEntity(pos);
    }
}
