package io.github.chr1sps.rars.notices;

import io.github.chr1sps.rars.exceptions.SimulationException;
import io.github.chr1sps.rars.simulator.Simulator;
import io.github.chr1sps.rars.venus.run.RunSpeedPanel;

/**
 * Object provided to Observers of the Simulator.
 * They are notified at important phases of the runtime simulator,
 * such as start and stop of simulation.
 *
 * @author Pete Sanderson
 * @version January 2009
 */

public record SimulatorNotice(int action, int maxSteps, double runSpeed, int programCounter, Simulator.Reason reason,
                              SimulationException exception, boolean done) implements Notice {
    /**
     * Constant <code>SIMULATOR_START=0</code>
     */
    public static final int SIMULATOR_START = 0;
    /**
     * Constant <code>SIMULATOR_STOP=1</code>
     */
    public static final int SIMULATOR_STOP = 1;

    @Override
    public String toString() {
        return ((this.action == SimulatorNotice.SIMULATOR_START) ? "START " : "STOP  ") +
                "Max Steps " + this.maxSteps + " " +
                "Speed "
                + ((this.runSpeed == RunSpeedPanel.UNLIMITED_SPEED) ? "unlimited " : this.runSpeed + " inst/sec") +
                "Prog Ctr " + this.programCounter;
    }
}