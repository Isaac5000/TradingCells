package com.cosmocraft.trading_cells.feature.breeders.application.service;

import com.cosmocraft.trading_cells.feature.breeders.application.port.input.BreederUseCase;
import com.cosmocraft.trading_cells.feature.breeders.application.port.output.BreederSettingsPort;
import com.cosmocraft.trading_cells.feature.breeders.domain.model.BreederFood;
import com.cosmocraft.trading_cells.feature.breeders.domain.model.BreederKind;
import com.cosmocraft.trading_cells.feature.breeders.domain.model.BreederRecipe;
import com.cosmocraft.trading_cells.feature.breeders.domain.model.BreederRules;
import com.cosmocraft.trading_cells.shared.machines.domain.model.TimedProcess;
import java.util.Objects;

/** Application boundary for breeder rules and progress. */
public final class BreederService implements BreederUseCase {
    private final BreederSettingsPort settings;

    public BreederService(BreederSettingsPort settings) {
        this.settings = Objects.requireNonNull(settings);
    }

    public int durationTicks(BreederKind kind) {
        return BreederRecipe.breedTicks(kind, rules());
    }

    public int foodCost(BreederKind kind, BreederFood food) {
        return BreederRecipe.cost(kind, food, rules());
    }

    public int maximumPendingBabies() {
        return rules().maximumPendingBabies();
    }

    public TimedProcess.Step advance(int ticks, BreederKind kind, boolean canGenerateBaby) {
        return TimedProcess.advance(
                ticks,
                durationTicks(kind),
                canGenerateBaby
                        ? TimedProcess.Availability.ACTIVE
                        : TimedProcess.Availability.INACTIVE
        );
    }

    private BreederRules rules() {
        return new BreederRules(
                settings.villagerBreederTicks(),
                settings.piglinBreederTicks(),
                settings.villagerBreadCost(),
                settings.villagerVegetableCost(),
                settings.maximumPendingBabies()
        );
    }
}
