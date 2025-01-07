package rars.riscv.hardware;

import rars.Globals;
import rars.exceptions.SimulationException;
import rars.riscv.BasicInstruction;
import rars.simulator.Simulator;

/**
 * Manages the flow of interrupts to the processor
 * <p>
 * Roughly corresponds to PLIC in the spec, but it additionally (kindof) handles
 */
// TODO: add backstepper support
public final class InterruptController {
    /// Lock for synchronizing as this is a static class
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

    private InterruptController() {
    }

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
     * @param value
     *     a int
     * @return a boolean
     */
    public static boolean registerExternalInterrupt(final int value) {
        synchronized (lock) {
            if (externalPending) {
                return false;
            }
            externalValue = value;
            externalPending = true;
            Simulator.INSTANCE.interrupt();
            return true;
        }
    }

    /**
     * <p>registerTimerInterrupt.</p>
     *
     * @param value
     *     a int
     * @return a boolean
     */
    public static boolean registerTimerInterrupt(final int value) {
        synchronized (lock) {
            if (timerPending) {
                return false;
            }
            timerValue = value;
            timerPending = true;
            Simulator.INSTANCE.interrupt();
            return true;
        }
    }

    /**
     * <p>registerSynchronousTrap.</p>
     *
     * @param se
     *     a {@link SimulationException} object
     * @param pc
     *     a int
     * @return a boolean
     */
    public static boolean registerSynchronousTrap(final SimulationException se, final int pc) {
        synchronized (lock) {
            if (trapPending) {
                return false;
            }
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
            assert trapPC == Globals.REGISTER_FILE.getProgramCounter() - BasicInstruction.BASIC_INSTRUCTION_LENGTH
                : "trapPC doesn't match current pc";
            trapPending = false;
            return trapSE;
        }
    }
}
