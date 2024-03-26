package io.github.chr1sps.rars.simulator;

import io.github.chr1sps.rars.exceptions.SimulationException;
import io.github.chr1sps.rars.venus.run.RunSpeedPanel;

/**
 * Object provided to Observers of the Simulator.
 * They are notified at important phases of the runtime simulator,
 * such as start and stop of simulation.
 *
 * @author Pete Sanderson
 * @version January 2009
 */
public class SimulatorNotice {
    private int action;
    private int maxSteps;
    private Simulator.Reason reason;
    private boolean done;
    private SimulationException exception;
    private double runSpeed;
    private int programCounter;
    /**
     * Constant <code>SIMULATOR_START=0</code>
     */
    public static final int SIMULATOR_START = 0;
    /**
     * Constant <code>SIMULATOR_STOP=1</code>
     */
    public static final int SIMULATOR_STOP = 1;

    /**
     * Constructor will be called only within this package, so assume
     * address and length are in valid ranges.
     *
     * @param action         a int
     * @param maxSteps       a int
     * @param runSpeed       a double
     * @param programCounter a int
     * @param reason         a {@link io.github.chr1sps.rars.simulator.Simulator.Reason} object
     * @param se             a {@link SimulationException} object
     * @param done           a boolean
     */
    public SimulatorNotice(int action, int maxSteps, double runSpeed, int programCounter, Simulator.Reason reason,
                           SimulationException se, boolean done) {
        this.action = action;
        this.maxSteps = maxSteps;
        this.runSpeed = runSpeed;
        this.programCounter = programCounter;
        this.reason = reason;
        this.exception = se;
        this.done = done;
    }

    /**
     * <p>Getter for the field <code>action</code>.</p>
     *
     * @return a int
     */
    public int getAction() {
        return this.action;
    }

    /**
     * <p>Getter for the field <code>maxSteps</code>.</p>
     *
     * @return a int
     */
    public int getMaxSteps() {
        return this.maxSteps;
    }

    /**
     * <p>Getter for the field <code>runSpeed</code>.</p>
     *
     * @return a double
     */
    public double getRunSpeed() {
        return this.runSpeed;
    }

    /**
     * <p>Getter for the field <code>programCounter</code>.</p>
     *
     * @return a int
     */
    public int getProgramCounter() {
        return this.programCounter;
    }

    /**
     * <p>Getter for the field <code>reason</code>.</p>
     *
     * @return a {@link io.github.chr1sps.rars.simulator.Simulator.Reason} object
     */
    public Simulator.Reason getReason() {
        return this.reason;
    }

    /**
     * <p>Getter for the field <code>exception</code>.</p>
     *
     * @return a {@link SimulationException} object
     */
    public SimulationException getException() {
        return this.exception;
    }

    /**
     * <p>Getter for the field <code>done</code>.</p>
     *
     * @return a boolean
     */
    public boolean getDone() {
        return this.done;
    }

    /**
     * String representation indicates access type, address and length in bytes
     *
     * @return a {@link java.lang.String} object
     */
    public String toString() {
        return ((this.getAction() == SIMULATOR_START) ? "START " : "STOP  ") +
                "Max Steps " + this.maxSteps + " " +
                "Speed "
                + ((this.runSpeed == RunSpeedPanel.UNLIMITED_SPEED) ? "unlimited " : "" + this.runSpeed + " inst/sec") +
                "Prog Ctr " + this.programCounter;
    }
}
