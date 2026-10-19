package com.cosmocraft.trading_cells.feature.logistics.adapters.neoforge;

import com.cosmocraft.trading_cells.feature.logistics.adapters.api.LogisticsResourceSnapshot;
import com.cosmocraft.trading_cells.feature.logistics.domain.model.LogisticsResourceType;
import com.cosmocraft.trading_cells.feature.logistics.domain.model.PipeFilterRule;
import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.TradingCells;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.transfer.RangedResourceHandler;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.PlayerInventoryWrapper;

public final class ItemLogisticsAdapter extends AbstractResourceHandlerAdapter<ItemResource> {
    private static final Identifier ID = Identifier.fromNamespaceAndPath(TradingCells.MOD_ID, "item");

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public LogisticsResourceType type() {
        return LogisticsResourceType.ITEM;
    }

    @Override
    public ResourceHandler<ItemResource> find(ServerLevel level, BlockPos pos, Direction side) {
        return level.getCapability(Capabilities.Item.BLOCK, pos, side);
    }

    @Override
    public java.util.List<com.cosmocraft.trading_cells.feature.logistics.adapters.api.LogisticsListingEntry<ItemResource>>
            listingContents(ResourceHandler<ItemResource> handler, ServerLevel level, BlockPos pos, Direction side) {
        var entries = new java.util.ArrayList<com.cosmocraft.trading_cells.feature.logistics.adapters.api.LogisticsListingEntry<ItemResource>>();
        var entity = level.getBlockEntity(pos);
        var state = level.getBlockState(pos);
        int size = handler.size();
        int[] faceSlots = null;
        ItemResource previousResource = null;
        LogisticsResourceSnapshot<ItemResource> previousSnapshot = null;
        int metadataMisses = 0;
        for (int index = 0; index < size; index++) {
            ItemResource resource = handler.getResource(index);
            long amount = handler.getAmountAsLong(index);
            if (resource.isEmpty() || amount <= 0) { continue; }
            Object inventory = handler;
            int slot = index;
            if (handler instanceof com.cosmocraft.trading_cells.platform.neoforge.machine.PortableMachineItemHandler machine) {
                inventory = entity;
                slot = machine.containerSlot(index);
            } else if (handler instanceof net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper
                    && entity instanceof net.minecraft.world.WorldlyContainer container) {
                inventory = entity;
                if (faceSlots == null) { faceSlots = container.getSlotsForFace(side); }
                slot = faceSlots[index];
            } else if ((handler instanceof net.neoforged.neoforge.transfer.item.VanillaContainerWrapper
                    || handler instanceof net.neoforged.neoforge.transfer.CombinedResourceHandler<?>
                    && state.getBlock() instanceof net.minecraft.world.level.block.ChestBlock)
                    && entity instanceof net.minecraft.world.Container container) {
                inventory = entity;
                if (state.getBlock() instanceof net.minecraft.world.level.block.ChestBlock
                        && size == 2 * container.getContainerSize()) {
                    var type = state.getValue(net.minecraft.world.level.block.ChestBlock.TYPE);
                    boolean first = type == net.minecraft.world.level.block.state.properties.ChestType.RIGHT;
                    boolean firstSlot = index < container.getContainerSize();
                    if (first != firstSlot) {
                        var other = pos.relative(net.minecraft.world.level.block.ChestBlock.getConnectedDirection(state));
                        if (level.getChunkSource().getChunkNow(other.getX() >> 4, other.getZ() >> 4) != null) { inventory = level.getBlockEntity(other); }
                    }
                    slot = index % container.getContainerSize();
                }
            }
            // Only reuse immutable metadata within this read, never quantities or live handlers across ticks.
            boolean sameMetadata = metadataMisses < 8 && previousSnapshot != null && resource.equals(previousResource);
            var currentSnapshot = sameMetadata
                    ? new LogisticsResourceSnapshot<>(resource, previousSnapshot.resourceId(),
                            previousSnapshot.componentFingerprint(), previousSnapshot.displayName(),
                            previousSnapshot.icon(), amount)
                    : snapshot(resource, amount);
            // Stop comparing large component trees when this inventory has no useful repetition.
            if (!sameMetadata && metadataMisses < 8) { metadataMisses++; }
            previousResource = resource;
            previousSnapshot = currentSnapshot;
            entries.add(new com.cosmocraft.trading_cells.feature.logistics.adapters.api.LogisticsListingEntry<>(
                    new com.cosmocraft.trading_cells.feature.logistics.adapters.api.LogisticsListingEntry.StorageKey(
                            inventory == null ? handler : inventory, slot), currentSnapshot));
        }
        return entries;
    }

    @Override
    public ResourceHandler<ItemResource> findCarried(ServerPlayer player, InteractionHand hand) {
        ResourceHandler<ItemResource> inventory = PlayerInventoryWrapper.of(player);
        int slot = hand == InteractionHand.MAIN_HAND
                ? player.getInventory().getSelectedSlot()
                : Inventory.SLOT_OFFHAND;
        return RangedResourceHandler.ofSingleIndex(inventory, slot);
    }

    @Override
    protected boolean accepts(ItemResource resource) {
        return !resource.isEmpty();
    }

    @Override
    protected boolean matchesIdentity(ItemResource resource, PipeFilterRule rule) {
        Identifier id = parsedIdentifier(rule.key());
        if (id == null) {
            return false;
        }
        return switch (rule.matchKind()) {
            case ID -> BuiltInRegistries.ITEM.getKey(resource.getItem()).equals(id);
            case TAG -> resource.typeHolder().is(TagKey.create(Registries.ITEM, id));
        };
    }

    @Override
    protected LogisticsResourceSnapshot<ItemResource> snapshot(ItemResource resource, long amount) {
        return new LogisticsResourceSnapshot<>(
                resource,
                BuiltInRegistries.ITEM.getKey(resource.getItem()),
                LogisticsComponentData.fingerprint(resource.getComponentsPatch()),
                resource.getHoverName(),
                resource.toStack(),
                amount
        );
    }
}
