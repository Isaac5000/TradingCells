package com.cosmocraft.trading_cells.platform.neoforge.fluid;

import java.util.Objects;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/** A one-tank, rollback-safe mapping where one fluid unit equals one XP point. */
public final class ExperienceFluidHandler extends SnapshotJournal<Integer>
        implements ResourceHandler<FluidResource> {
    private final Supplier<FluidResource> resourceSupplier;
    private final IntSupplier amountGetter;
    private final IntConsumer amountSetter;
    private final IntSupplier capacityGetter;
    private final boolean acceptsInput;
    private final Runnable committedChange;

    public ExperienceFluidHandler(
            Supplier<FluidResource> resourceSupplier,
            IntSupplier amountGetter,
            IntConsumer amountSetter,
            IntSupplier capacityGetter,
            boolean acceptsInput,
            Runnable committedChange
    ) {
        this.resourceSupplier = Objects.requireNonNull(resourceSupplier);
        this.amountGetter = Objects.requireNonNull(amountGetter);
        this.amountSetter = Objects.requireNonNull(amountSetter);
        this.capacityGetter = Objects.requireNonNull(capacityGetter);
        this.acceptsInput = acceptsInput;
        this.committedChange = Objects.requireNonNull(committedChange);
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public FluidResource getResource(int index) {
        checkIndex(index);
        return amount() == 0 ? FluidResource.EMPTY : resource();
    }

    @Override
    public long getAmountAsLong(int index) {
        checkIndex(index);
        return amount();
    }

    @Override
    public long getCapacityAsLong(int index, FluidResource resource) {
        checkIndex(index);
        return resource.isEmpty() || matches(resource) ? capacity() : 0L;
    }

    @Override
    public boolean isValid(int index, FluidResource resource) {
        checkIndex(index);
        return !resource.isEmpty() && matches(resource);
    }

    @Override
    public int insert(
            int index,
            FluidResource resource,
            int requestedAmount,
            TransactionContext transaction
    ) {
        checkIndex(index);
        TransferPreconditions.checkNonEmptyNonNegative(resource, requestedAmount);
        if (!acceptsInput || !matches(resource) || requestedAmount == 0) {
            return 0;
        }
        int inserted = Math.min(requestedAmount, capacity() - amount());
        if (inserted <= 0) {
            return 0;
        }
        updateSnapshots(transaction);
        setAmount(amount() + inserted);
        return inserted;
    }

    @Override
    public int extract(
            int index,
            FluidResource resource,
            int requestedAmount,
            TransactionContext transaction
    ) {
        checkIndex(index);
        TransferPreconditions.checkNonEmptyNonNegative(resource, requestedAmount);
        if (!matches(resource) || requestedAmount == 0) {
            return 0;
        }
        int extracted = Math.min(requestedAmount, amount());
        if (extracted <= 0) {
            return 0;
        }
        updateSnapshots(transaction);
        setAmount(amount() - extracted);
        return extracted;
    }

    @Override
    protected Integer createSnapshot() {
        return amount();
    }

    @Override
    protected void revertToSnapshot(Integer snapshot) {
        setAmount(snapshot);
    }

    @Override
    protected void onRootCommit(Integer originalState) {
        if (originalState != amount()) {
            committedChange.run();
        }
    }

    private int amount() {
        return Math.clamp(amountGetter.getAsInt(), 0, capacity());
    }

    private int capacity() {
        return Math.max(0, capacityGetter.getAsInt());
    }

    private void setAmount(int amount) {
        amountSetter.accept(Math.clamp(amount, 0, capacity()));
    }

    private FluidResource resource() {
        return resourceSupplier.get();
    }

    private boolean matches(FluidResource candidate) {
        return resource().equals(candidate);
    }

    private static void checkIndex(int index) {
        Objects.checkIndex(index, 1);
    }
}
