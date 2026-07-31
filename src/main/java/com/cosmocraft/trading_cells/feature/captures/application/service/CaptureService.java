package com.cosmocraft.trading_cells.feature.captures.application.service;

import com.cosmocraft.trading_cells.feature.captures.application.port.input.CaptureUseCase;
import com.cosmocraft.trading_cells.feature.captures.application.port.output.CaptureSettingsPort;
import com.cosmocraft.trading_cells.feature.captures.domain.model.CapturerDurability;
import java.util.Objects;

/** Application boundary for capturer durability rules. */
public final class CaptureService implements CaptureUseCase {
    private final CaptureSettingsPort settings;

    public CaptureService(CaptureSettingsPort settings) {
        this.settings = Objects.requireNonNull(settings);
    }

    @Override
    public int maximumDurability() {
        return CapturerDurability.maximum(settings.capturerDurability());
    }
}
