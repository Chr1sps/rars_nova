package rars.notices;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rars.exceptions.SimulationException;
import rars.simulator.Simulator;
import rars.venus.run.RunSpeedPanel;

/**
 * Object provided to Observers of the Simulator.
 * They are notified at important phases of the runtime simulator,
 * such as start and stop of simulation.
 *
 * @author Pete Sanderson
 * @version January 2009
 */

public record SimulatorNotice(Action action, int maxSteps, double runSpeed, int programCounter,
                              @Nullable Simulator.Reason reason,
                              SimulationException exception, boolean done) {

    @Contract(pure = true)
    @Override
    public @NotNull String toString() {
        return ((this.action == Action.START) ? "START " : "STOP  ") +
            "Max Steps " + this.maxSteps + " " +
            "Speed "
            + ((this.runSpeed == RunSpeedPanel.UNLIMITED_SPEED) ? "unlimited " : this.runSpeed + " inst/sec") +
            "Prog Ctr " + this.programCounter;
    }

    public enum Action {
        START,
        STOP
    }
}