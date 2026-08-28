package com.cosmocraft.trading_cells.feature.silktouch.adapters.input;

import java.util.Optional;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.TrialSpawnerBlockEntity;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.jspecify.annotations.Nullable;

/** Installs and reads the comparator-powered pause option for vanilla spawners. */
public final class SpawnerRedstoneControlAdapter {
    private SpawnerRedstoneControlAdapter() {
    }

    public static InteractionResult install(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getItemStack().is(Items.COMPARATOR)) {
            return InteractionResult.PASS;
        }

        BlockEntity blockEntity = event.getLevel().getBlockEntity(event.getPos());
        Optional<SpawnerRedstoneControl> control = control(blockEntity);
        if (control.isEmpty()) {
            return InteractionResult.PASS;
        }

        if (event.getLevel().isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        SpawnerRedstoneControl redstoneControl = control.orElseThrow();
        if (!redstoneControl.tradingCells$isRedstoneControlInstalled()) {
            redstoneControl.tradingCells$setRedstoneControlInstalled(true);
            event.getItemStack().consume(1, event.getEntity());
            blockEntity.setChanged();
            event.getLevel().sendBlockUpdated(
                    event.getPos(),
                    blockEntity.getBlockState(),
                    blockEntity.getBlockState(),
                    3
            );
        }
        return InteractionResult.SUCCESS;
    }

    public static boolean isInstalled(ItemStack stack) {
        TypedEntityData<BlockEntityType<?>> blockEntityData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        return blockEntityData != null
                && blockEntityData.copyTagWithoutId()
                        .getBoolean(SpawnerRedstoneControl.PERSISTENCE_TAG)
                        .orElse(false);
    }

    public static Optional<SpawnerRedstoneControl> control(@Nullable BlockEntity blockEntity) {
        if (blockEntity instanceof TrialSpawnerBlockEntity trialSpawner) {
            return Optional.of((SpawnerRedstoneControl) (Object) trialSpawner.getTrialSpawner());
        }
        return blockEntity instanceof SpawnerRedstoneControl control
                ? Optional.of(control)
                : Optional.empty();
    }
}
