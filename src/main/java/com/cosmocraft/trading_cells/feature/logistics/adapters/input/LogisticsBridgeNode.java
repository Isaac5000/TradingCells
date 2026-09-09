package com.cosmocraft.trading_cells.feature.logistics.adapters.input;

import com.cosmocraft.trading_cells.feature.logistics.domain.model.LogisticsResourceType;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public interface LogisticsBridgeNode {
    boolean supportsLogisticsResource(LogisticsResourceType type);

    default List<BlockPos> bridgePipePositions(ServerLevel level, LogisticsResourceType type) {
        return List.of();
    }
}
