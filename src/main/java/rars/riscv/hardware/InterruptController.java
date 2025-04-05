package rars.riscv.hardware;

import org.jetbrains.annotations.NotNull;
import rars.exceptions.SimulationError;
import rars.riscv.BasicInstruction;
import rars.riscv.hardware.registerfiles.RegisterFile;
import rars.simulator.Simulator;

// TODO: add backstepper support

/**
 * Manages the flow of interrupts to the processor
 * <p>
 * Roughly corresponds to PLIC in the spec, but it additionally (kindof) handles
 */
public final class InterruptController {

    private final @NotNull Simulator simulator;
    private final @NotNull RegisterFile registerFile;
    /** Status for the interrupt state */
    private boolean externalPending = false;
    private int externalValue;
    private boolean timerPending = false;
    private int timerValue;

    /** Status for trap state */
    private boolean trapPending = false;
    private SimulationError trapSE;
    private int trapPC;

    public InterruptController(
        final @NotNull Simulator simulator,
        final @NotNull RegisterFile registerFile
    ) {
        this.simulator = simulator;
        this.registerFile = registerFile;
    }

    public synchronized SimulationError claimTrap() {
        assert trapPending : "Cannot claim, no trap pending";
        assert trapPC == this.registerFile.getProgramCounter() - BasicInstruction.BASIC_INSTRUCTION_LENGTH
            : "trapPC doesn't match current pc";
        trapPending = false;
        return trapSE;

    }

    public synchronized boolean externalPending() {
        return externalPending;
    }

    public synchronized boolean timerPending() {
        return timerPending;
    }

    public synchronized boolean trapPending() {
        return trapPending;
    }

    public synchronized int claimExternal() {
        assert externalPending : "Cannot claim, no external interrupt pending";
        externalPending = false;
        return externalValue;

    }

    public synchronized int claimTimer() {
        assert timerPending : "Cannot claim, no timer interrupt pending";
        timerPending = false;
        return timerValue;

    }

    public synchronized void reset() {
        externalPending = false;
        timerPending = false;
        trapPending = false;
    }

    public synchronized boolean registerExternalInterrupt(final int value) {
        if (externalPending) {
            return false;
        }
        externalValue = value;
        externalPending = true;
        this.simulator.interrupt();
        return true;
    }

    public synchronized boolean registerTimerInterrupt(final int value) {
        if (timerPending) {
            return false;
        }
        timerValue = value;
        timerPending = true;
        this.simulator.interrupt();
        return true;
    }

    public synchronized boolean registerSynchronousTrap(final SimulationError se, final int pc) {
        if (trapPending) {
            return false;
        }
        trapSE = se;
        trapPC = pc;
        trapPending = true;
        return true;

    }
}
