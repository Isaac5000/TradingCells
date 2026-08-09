package com.cosmocraft.trading_cells.feature.infusion.domain.model;

public record ArcaneInfusionAttempt(
        boolean ingredientsAvailable,
        boolean outputAvailable,
        int storedExperience,
        int requiredExperience
) {
    public ArcaneInfusionAttempt {
        storedExperience = Math.max(0, storedExperience);
        requiredExperience = Math.max(1, requiredExperience);
    }

    public ArcaneInfusionDecision decision() {
        if (!outputAvailable) {
            return ArcaneInfusionDecision.OUTPUT_BLOCKED;
        }
        if (!ingredientsAvailable) {
            return ArcaneInfusionDecision.INGREDIENTS_REQUIRED;
        }
        if (storedExperience < requiredExperience) {
            return ArcaneInfusionDecision.EXPERIENCE_REQUIRED;
        }
        return ArcaneInfusionDecision.READY;
    }
}
