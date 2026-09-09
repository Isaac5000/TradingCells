package com.cosmocraft.trading_cells.feature.logistics.adapters.input;

import com.cosmocraft.trading_cells.feature.logistics.adapters.output.LogisticsRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.logistics.domain.model.LogisticsResourceType;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.NonNull;

public final class NetworkTerminalBlockEntity extends BlockEntity implements MenuProvider, LogisticsBridgeNode {
    public NetworkTerminalBlockEntity(BlockPos pos, BlockState state) {
        super(LogisticsRegistrationAdapter.TERMINAL_BLOCK_ENTITY.get(), pos, state);
    }

    public boolean craftingTerminal() {
        return getBlockState().getBlock() instanceof NetworkTerminalBlock terminal && terminal.crafting();
    }

    @Override
    public @NonNull Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, @NonNull Inventory inventory, @NonNull Player player) {
        return new NetworkTerminalMenu(containerId, inventory, this, player, craftingTerminal());
    }

    @Override
    public boolean supportsLogisticsResource(LogisticsResourceType type) {
        return true;
    }

    @Override
    public List<BlockPos> bridgePipePositions(
            net.minecraft.server.level.ServerLevel level,
            LogisticsResourceType type
    ) {
        List<BlockPos> connected = new ArrayList<>();
        for (Direction direction : Direction.values()) {
            BlockPos candidate = worldPosition.relative(direction);
            var chunk = level.getChunkSource().getChunkNow(candidate.getX() >> 4, candidate.getZ() >> 4);
            if (chunk != null && chunk.getBlockEntity(candidate) instanceof LogisticsPipeBlockEntity pipe
                    && pipe.kind().supports(type)
                    && !pipe.face(direction.getOpposite()).pipeDisconnected()) {
                connected.add(candidate.immutable());
            }
        }
        connected.sort(Comparator.comparingLong(BlockPos::asLong));
        return List.copyOf(connected);
    }

    public BlockPos networkOrigin(LogisticsResourceType type) {
        if (!(level instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return null;
        }
        List<BlockPos> connected = bridgePipePositions(serverLevel, type);
        return connected.isEmpty() ? null : connected.getFirst();
    }
}
