package com.cosmocraft.trading_cells.feature.experience.adapters.input;

import com.cosmocraft.trading_cells.feature.experience.adapters.output.ExperienceStorageRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.experience.application.port.input.ExperienceStorageUseCase;
import com.cosmocraft.trading_cells.feature.experience.domain.model.ExperienceTransferAction;
import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.FeatureComposition;
import com.cosmocraft.trading_cells.platform.neoforge.fluid.ExperienceFluidHandler;
import com.cosmocraft.trading_cells.platform.neoforge.machine.PortableMachineBlockEntity;
import com.cosmocraft.trading_cells.platform.neoforge.registration.ExperienceFluidRegistration;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import org.jspecify.annotations.NonNull;

public final class ExperienceStorageBlockEntity extends PortableMachineBlockEntity implements MenuProvider {
    public static final int CAPACITY = Integer.MAX_VALUE;
    static final String STORED_EXPERIENCE_TAG = "StoredExperience";

    private final ExperienceStorageUseCase service = FeatureComposition.experienceStorage();
    private final ExperienceFluidHandler fluidHandler = new ExperienceFluidHandler(
            () -> FluidResource.of(ExperienceFluidRegistration.SOURCE.get()),
            this::storedExperience,
            this::setStoredExperienceRaw,
            () -> CAPACITY,
            true,
            this::markChangedAndSync
    );
    private int storedExperience;

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> storedExperience & 0xFFFF;
                case 1 -> storedExperience >>> 16;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            int unsignedValue = value & 0xFFFF;
            if (index == 0) {
                setStoredExperienceRaw((storedExperience & 0x7FFF0000) | unsignedValue);
            } else if (index == 1) {
                setStoredExperienceRaw((storedExperience & 0xFFFF) | ((unsignedValue & 0x7FFF) << 16));
            }
        }

        @Override
        public int getCount() {
            return 2;
        }
    };

    public ExperienceStorageBlockEntity(BlockPos pos, BlockState state) {
        super(ExperienceStorageRegistrationAdapter.BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    public void processTick() {
        // This storage is event-driven and intentionally has no server tick.
    }

    public int storedExperience() {
        return storedExperience;
    }

    public ResourceHandler<FluidResource> fluidHandler() {
        return fluidHandler;
    }

    public void transferExperience(
            ServerPlayer player,
            ExperienceTransferAction action,
            int requestedLevels
    ) {
        if (level == null || level.isClientSide()) {
            return;
        }
        int points = switch (action) {
            case DEPOSIT -> service.depositLevels(
                    player.experienceLevel,
                    player.experienceProgress,
                    storedExperience,
                    CAPACITY,
                    requestedLevels
            );
            case DEPOSIT_ALL -> service.depositAll(
                    player.experienceLevel,
                    player.experienceProgress,
                    storedExperience,
                    CAPACITY
            );
            case WITHDRAW -> service.withdrawLevels(
                    player.experienceLevel,
                    player.experienceProgress,
                    storedExperience,
                    requestedLevels
            );
            case WITHDRAW_ALL -> service.withdrawAll(
                    player.experienceLevel,
                    player.experienceProgress,
                    storedExperience
            );
        };
        if (points <= 0) {
            return;
        }
        if (action == ExperienceTransferAction.WITHDRAW
                || action == ExperienceTransferAction.WITHDRAW_ALL) {
            storedExperience = Math.max(0, storedExperience - points);
            player.giveExperiencePoints(points);
        } else {
            storedExperience = (int) Math.min(CAPACITY, (long) storedExperience + points);
            player.giveExperiencePoints(-points);
        }
        markChangedAndSync();
    }

    @Override
    public @NonNull Component getDisplayName() {
        return Component.translatable("container.trading_cells.experience_storage");
    }

    @Override
    public @NonNull AbstractContainerMenu createMenu(
            int containerId,
            @NonNull Inventory inventory,
            @NonNull Player player
    ) {
        return new ExperienceStorageMenu(containerId, inventory, this, dataAccess);
    }

    @Override
    protected void loadAdditional(@NonNull ValueInput input) {
        super.loadAdditional(input);
        setStoredExperienceRaw(input.getIntOr(STORED_EXPERIENCE_TAG, 0));
    }

    @Override
    protected void saveAdditional(@NonNull ValueOutput output) {
        super.saveAdditional(output);
        if (storedExperience > 0) {
            output.putInt(STORED_EXPERIENCE_TAG, storedExperience);
        }
    }

    @Override
    protected void clearContentsForBlockDrop() {
        storedExperience = 0;
        setChanged();
    }

    private void setStoredExperienceRaw(int value) {
        storedExperience = Math.clamp(value, 0, CAPACITY);
    }
}
