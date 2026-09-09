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
