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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

public final class FluidLogisticsAdapter extends AbstractResourceHandlerAdapter<FluidResource> {
    private final LogisticsResourceType type;
    private final Identifier id;

    public FluidLogisticsAdapter(LogisticsResourceType type) {
        if (type != LogisticsResourceType.FLUID && type != LogisticsResourceType.GAS) {
            throw new IllegalArgumentException("Fluid adapter requires FLUID or GAS");
        }
        this.type = type;
        this.id = Identifier.fromNamespaceAndPath(TradingCells.MOD_ID, type.serializedName());
    }

    @Override
    public Identifier id() {
        return id;
    }

    @Override
    public LogisticsResourceType type() {
        return type;
    }

    @Override
    public ResourceHandler<FluidResource> find(ServerLevel level, BlockPos pos, Direction side) {
        return level.getCapability(Capabilities.Fluid.BLOCK, pos, side);
    }

    @Override
    public ResourceHandler<FluidResource> findCarried(ServerPlayer player, InteractionHand hand) {
        return ItemAccess.forPlayerInteraction(player, hand).getCapability(Capabilities.Fluid.ITEM);
    }

    @Override
    public ResourceHandler<FluidResource> findContainer(ItemAccess access) {
        return access.getCapability(Capabilities.Fluid.ITEM);
    }

    @Override
    protected boolean accepts(FluidResource resource) {
        return !resource.isEmpty() && FluidGasClassifier.isGas(resource) == (type == LogisticsResourceType.GAS);
    }

    @Override
    protected boolean matchesIdentity(FluidResource resource, PipeFilterRule rule) {
        Identifier candidate = parsedIdentifier(rule.key());
        if (candidate == null) {
            return false;
        }
        return switch (rule.matchKind()) {
            case ID -> BuiltInRegistries.FLUID.getKey(resource.getFluid()).equals(candidate);
            case TAG -> resource.typeHolder().is(TagKey.create(Registries.FLUID, candidate));
        };
    }

    @Override
    protected LogisticsResourceSnapshot<FluidResource> snapshot(FluidResource resource, long amount) {
        ItemStack icon = resource.getFluid().getBucket() == Items.AIR
                ? new ItemStack(type == LogisticsResourceType.GAS ? Items.GLASS_BOTTLE : Items.BUCKET)
                : new ItemStack(resource.getFluid().getBucket());
        return new LogisticsResourceSnapshot<>(
                resource,
                BuiltInRegistries.FLUID.getKey(resource.getFluid()),
                LogisticsComponentData.fingerprint(resource.getComponentsPatch()),
                resource.getHoverName(),
                icon,
                amount
        );
    }
}
