package com.cosmocraft.trading_cells.platform.neoforge.fluid;

import com.cosmocraft.trading_cells.platform.neoforge.registration.ExperienceFluidRegistration;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

public final class ExperienceFluidHandlers {
    private ExperienceFluidHandlers() {
    }

    public static ExperienceFluidHandler source(
            IntSupplier amountGetter,
            IntConsumer amountSetter,
            Runnable committedChange
    ) {
        return new ExperienceFluidHandler(
                () -> FluidResource.of(ExperienceFluidRegistration.SOURCE.get()),
                amountGetter,
                amountSetter,
                () -> Integer.MAX_VALUE,
                false,
                true,
                committedChange
        );
    }

    public static ExperienceFluidHandler destination(
            IntSupplier amountGetter,
            IntConsumer amountSetter,
            IntSupplier capacityGetter,
            Runnable committedChange
    ) {
        return new ExperienceFluidHandler(
                () -> FluidResource.of(ExperienceFluidRegistration.SOURCE.get()),
                amountGetter,
                amountSetter,
                capacityGetter,
                true,
                false,
                committedChange
        );
    }

    public static ExperienceFluidHandler bidirectional(
            IntSupplier amountGetter,
            IntConsumer amountSetter,
            IntSupplier capacityGetter,
            Runnable committedChange
    ) {
        return new ExperienceFluidHandler(
                () -> FluidResource.of(ExperienceFluidRegistration.SOURCE.get()),
                amountGetter,
                amountSetter,
                capacityGetter,
                true,
                true,
                committedChange
        );
    }
}
