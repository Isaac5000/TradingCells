package com.cosmocraft.trading_cells.feature.infusion.application.port.input;

import com.cosmocraft.trading_cells.feature.infusion.domain.model.ArcaneInfusionAttempt;
import com.cosmocraft.trading_cells.feature.infusion.domain.model.ArcaneInfusionDecision;

public interface ArcaneInfusionUseCase {
    ArcaneInfusionDecision evaluate(ArcaneInfusionAttempt attempt);

    int depositLevels(int playerLevel, float playerProgress, int storedPoints, int capacity, int requestedLevels);

    int depositAll(int playerLevel, float playerProgress, int storedPoints, int capacity);

    int withdrawLevels(int playerLevel, float playerProgress, int storedPoints, int requestedLevels);

    int withdrawAll(int playerLevel, float playerProgress, int storedPoints);
}
