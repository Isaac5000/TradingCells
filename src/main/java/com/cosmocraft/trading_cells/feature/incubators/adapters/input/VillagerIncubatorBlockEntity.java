package com.cosmocraft.trading_cells.feature.incubators.adapters.input;

import com.cosmocraft.trading_cells.feature.incubators.adapters.output.IncubatorRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.captures.domain.model.CapturedMobKind;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public final class VillagerIncubatorBlockEntity extends IncubatorBlockEntity {
    public VillagerIncubatorBlockEntity(BlockPos pos, BlockState state) {
        super(IncubatorRegistrationAdapter.VILLAGER_INCUBATOR_BLOCK_ENTITY.get(), pos, state, CapturedMobKind.VILLAGER);
    }
}
