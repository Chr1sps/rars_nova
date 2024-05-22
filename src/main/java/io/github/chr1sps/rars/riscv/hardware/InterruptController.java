package io.github.chr1sps.rars.riscv.hardware;

import io.github.chr1sps.rars.exceptions.SimulationException;
import io.github.chr1sps.rars.riscv.Instruction;
import io.github.chr1sps.rars.simulator.Simulator;

/**
 * Manages the flow of interrupts to the processor
 * <p>
 * Roughly corresponds to PLIC in the spec, but it additionally (kindof) handles
 *
 */
// TODO: add backstepper support
public class InterruptController {
    // Lock for synchronizing as this is a static class
    /**
     * Constant <code>lock</code>
     */
    public static final Object lock = new Object();

    // Status for the interrupt state
    private static boolean externalPending = false;
    private static int externalValue;
    private static boolean timerPending = false;
    private static int timerValue;

    // Status for trap state
    private static boolean trapPending = false;
    private static SimulationException trapSE;
    private static int trapPC;

    /**
     * <p>reset.</p>
     */
    public static void reset() {
        synchronized (lock) {
            externalPending = false;
            timerPending = false;
            trapPending = false;
        }
    }

    /**
     * <p>registerExternalInterrupt.</p>
     *
     * @param value a int
     * @return a boolean
     */
    public static boolean registerExternalInterrupt(int value) {
        synchronized (lock) {
            if (externalPending)
                return false;
            externalValue = value;
            externalPending = true;
            Simulator.getInstance().interrupt();
            return true;
        }
    }

    /**
     * <p>registerTimerInterrupt.</p>
     *
     * @param value a int
     * @return a boolean
     */
    public static boolean registerTimerInterrupt(int value) {
        synchronized (lock) {
            if (timerPending)
                return false;
            timerValue = value;
            timerPending = true;
            Simulator.getInstance().interrupt();
            return true;
        }
    }

    /**
     * <p>registerSynchronousTrap.</p>
     *
     * @param se a {@link SimulationException} object
     * @param pc a int
     * @return a boolean
     */
    public static boolean registerSynchronousTrap(SimulationException se, int pc) {
        synchronized (lock) {
            if (trapPending)
                return false;
            trapSE = se;
            trapPC = pc;
            trapPending = true;
            return true;
        }
    }

    /**
     * <p>externalPending.</p>
     *
     * @return a boolean
     */
    public static boolean externalPending() {
        synchronized (lock) {
            return externalPending;
        }
    }

    /**
     * <p>timerPending.</p>
     *
     * @return a boolean
     */
    public static boolean timerPending() {
        synchronized (lock) {
            return timerPending;
        }
    }

    /**
     * <p>trapPending.</p>
     *
     * @return a boolean
     */
    public static boolean trapPending() {
        synchronized (lock) {
            return trapPending;
        }
    }

    /**
     * <p>claimExternal.</p>
     *
     * @return a int
     */
    public static int claimExternal() {
        synchronized (lock) {
            assert externalPending : "Cannot claim, no external interrupt pending";
            externalPending = false;
            return externalValue;
        }
    }

    /**
     * <p>claimTimer.</p>
     *
     * @return a int
     */
    public static int claimTimer() {
        synchronized (lock) {
            assert timerPending : "Cannot claim, no timer interrupt pending";
            timerPending = false;
            return timerValue;
        }
    }

    /**
     * <p>claimTrap.</p>
     *
     * @return a {@link SimulationException} object
     */
    public static SimulationException claimTrap() {
        synchronized (lock) {
            assert trapPending : "Cannot claim, no trap pending";
            assert trapPC == RegisterFile.getProgramCounter() - Instruction.INSTRUCTION_LENGTH
                    : "trapPC doesn't match current pc";
            trapPending = false;
            return trapSE;
        }
    }
}
