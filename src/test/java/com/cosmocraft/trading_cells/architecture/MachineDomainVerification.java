package com.cosmocraft.trading_cells.architecture;

import com.cosmocraft.trading_cells.feature.incubators.domain.model.IncubationCycle;
import com.cosmocraft.trading_cells.shared.machines.domain.model.TimedProcess;
import com.cosmocraft.trading_cells.shared.machines.domain.model.MachineActivityController;
import com.cosmocraft.trading_cells.shared.machines.domain.model.MachineDiagnosticSnapshot;
import com.cosmocraft.trading_cells.shared.machines.domain.model.MachineDiagnosticStatus;
import com.cosmocraft.trading_cells.shared.machines.domain.model.MachineRedstoneMode;

final class MachineDomainVerification {
    private MachineDomainVerification() {
    }

    static void verify() {
        verifyTimedProcesses();
        verifyMachineActivity();
        verifyMachineActivityEquivalence();
        verifyDiagnosticsAndRedstoneModes();
    }

    private static void verifyTimedProcesses() {
        TimedProcess.Step paused = TimedProcess.advance(
                7,
                20,
                TimedProcess.Availability.BLOCKED
        );
        require(paused.ticks() == 7 && paused.transition() == TimedProcess.Transition.PAUSED,
                "A blocked output must pause progress");

        TimedProcess.Step completed = IncubationCycle.advance(9, 10, true, true);
        require(completed.transition() == TimedProcess.Transition.COMPLETED,
                "Incubation must complete on the configured server tick");

        TimedProcess.Step reset = IncubationCycle.advance(5, 10, false, true);
        require(reset.ticks() == 0 && reset.transition() == TimedProcess.Transition.RESET,
                "Invalid incubation input must reset progress");
    }

    private static void verifyMachineActivity() {
        MachineActivityController activity = new MachineActivityController();
        require(activity.activity() == MachineActivityController.Activity.INACTIVE,
                "A newly loaded machine must recalculate from an inactive state");
        activity.transition(MachineActivityController.Activity.BLOCKED);
        require(activity.remainsBlocked(), "A settled blocked machine must skip repeated capacity work");
        activity.wake();
        require(!activity.remainsBlocked(), "Inventory changes must wake a blocked machine immediately");
        activity.transition(MachineActivityController.Activity.ACTIVE);
        require(activity.activity() == MachineActivityController.Activity.ACTIVE,
                "A woken machine must return to active processing");
        activity.transition(MachineActivityController.Activity.INACTIVE);
        require(activity.remainsInactive(), "A settled inactive machine must skip repeated input work");
        MachineActivityController.wakeAll();
        require(!activity.remainsInactive(), "Configuration and datapack reloads must wake inactive machines");
    }

    private static void verifyMachineActivityEquivalence() {
        MachineActivityController activity = new MachineActivityController();
        int baselineTicks = 0;
        int optimizedTicks = 0;
        boolean hasInputs = false;
        boolean outputAvailable = true;
        for (int tick = 0; tick < 500; tick++) {
            if (tick == 3 || tick == 400) {
                hasInputs = true;
                activity.wake();
            } else if (tick == 80) {
                outputAvailable = false;
                activity.wake();
            } else if (tick == 130) {
                outputAvailable = true;
                activity.wake();
            } else if (tick == 260) {
                hasInputs = false;
                activity.wake();
            }

            // Neither implementation progresses while its chunk is unloaded.
            if (tick >= 200 && tick < 220) {
                require(baselineTicks == optimizedTicks,
                        "Chunk unloading must preserve identical machine progress");
                continue;
            }

            TimedProcess.Step baseline = TimedProcess.advance(
                    baselineTicks,
                    37,
                    TimedProcess.availability(hasInputs, outputAvailable)
            );
            baselineTicks = baseline.ticks();

            if (!activity.remainsInactive() && !activity.remainsBlocked()) {
                TimedProcess.Step optimized = TimedProcess.advance(
                        optimizedTicks,
                        37,
                        TimedProcess.availability(hasInputs, outputAvailable)
                );
                optimizedTicks = optimized.ticks();
                activity.transition(!hasInputs
                        ? MachineActivityController.Activity.INACTIVE
                        : outputAvailable
                                ? MachineActivityController.Activity.ACTIVE
                                : MachineActivityController.Activity.BLOCKED);
            }
            require(baselineTicks == optimizedTicks,
                    "Sleeping inactive and blocked machines must remain tick-equivalent");
        }
    }

    private static void verifyDiagnosticsAndRedstoneModes() {
        require(!MachineRedstoneMode.IGNORE.pauses(true), "Ignore mode must never pause");
        require(MachineRedstoneMode.HIGH_SIGNAL_PAUSES.pauses(true),
                "A high signal must pause high-pause mode");
        require(MachineRedstoneMode.LOW_SIGNAL_PAUSES.pauses(false),
                "No signal must pause low-pause mode");
        require(MachineRedstoneMode.fromSerializedName("unknown") == MachineRedstoneMode.IGNORE,
                "Unknown persisted redstone modes must retain the compatible default");

        MachineDiagnosticSnapshot snapshot = new MachineDiagnosticSnapshot(
                MachineDiagnosticStatus.BLOCKED,
                "output_full",
                99,
                20,
                -1,
                99,
                10
        );
        require(snapshot.progress() == 20 && snapshot.storedExperience() == 0,
                "Diagnostic values must be clamped before transport");
        require(snapshot.outputFull(), "A saturated diagnostic output must report full");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
