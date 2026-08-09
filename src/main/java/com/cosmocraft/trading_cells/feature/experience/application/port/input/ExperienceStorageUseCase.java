package com.cosmocraft.trading_cells.feature.experience.application.port.input;

public interface ExperienceStorageUseCase {
    int depositLevels(
            int playerLevel,
            float playerProgress,
            int storedPoints,
            int capacity,
            int requestedLevels
    );

    int depositAll(int playerLevel, float playerProgress, int storedPoints, int capacity);

    int withdrawLevels(
            int playerLevel,
            float playerProgress,
            int storedPoints,
            int requestedLevels
    );

    int withdrawAll(int playerLevel, float playerProgress, int storedPoints);
}
