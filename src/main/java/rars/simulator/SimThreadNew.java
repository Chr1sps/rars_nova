package rars.simulator;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rars.Globals;
import rars.ProgramStatement;
import rars.exceptions.*;
import rars.io.AbstractIO;
import rars.notices.SimulatorNotice;
import rars.riscv.BasicInstruction;
import rars.riscv.SimulationContext;
import rars.riscv.hardware.InterruptController;
import rars.riscv.hardware.registerFiles.CSRegisterFile;
import rars.settings.OtherSettings;
import rars.util.BinaryUtils;
import rars.util.ListenerDispatcher;
import rars.venus.run.RunSpeedPanel;

import java.util.Arrays;

import static rars.Globals.CS_REGISTER_FILE;

public class SimThreadNew implements Runnable {
    protected final int maxSteps;
    private final @NotNull AbstractIO io;
    private final @NotNull ListenerDispatcher<@NotNull SimulatorNotice> simulatorNoticeDispatcher;
    private final int @NotNull [] breakPoints;
    private int pc;
    private boolean done;
    private @Nullable SimulationException pe;
    private volatile boolean stop = false;
    private Simulator.Reason constructReturnReason;

    protected SimThreadNew(
        final int pc,
        final int maxSteps,
        final int[] breakPoints,
        final @NotNull AbstractIO io,
        final @NotNull ListenerDispatcher<@NotNull SimulatorNotice> simulatorNoticeDispatcher
    ) {
        this.pc = pc;
        this.maxSteps = maxSteps;
        this.breakPoints = Arrays.stream(breakPoints).sorted().toArray();
        this.io = io;
        this.simulatorNoticeDispatcher = simulatorNoticeDispatcher;
        this.done = false;
        this.pe = null;
    }

    public @Nullable SimulationException getPe() {
        return pe;
    }

    public Simulator.Reason getConstructReturnReason() {
        return constructReturnReason;
    }

    /**
     * Sets to "true" the volatile boolean variable that is tested after each
     * instruction is executed. After calling this method, the next test
     * will yield "true" and "construct" will return.
     *
     * @param reason
     *     the Reason for stopping (PAUSE or STOP)
     */
    public final synchronized void setStop(final Simulator.Reason reason) {
        this.stop = true;
        this.constructReturnReason = reason;
        this.notify();
    }

    protected double getRunSpeed() {
        return RunSpeedPanel.UNLIMITED_SPEED;
    }

    ;

    private void startExecution() {
        final @NotNull SimulatorNotice notice = new SimulatorNotice(
            SimulatorNotice.Action.START,
            this.maxSteps,
            this.getRunSpeed(),
            this.pc, null, this.pe, this.done
        );
        // TODO: this is not completely threadsafe, if anything using Swing is observing
        // This can be fixed by making a SwingObserver class that is thread-safe
        this.simulatorNoticeDispatcher.dispatch(notice);
    }

    private void stopExecution(final boolean done, final Simulator.Reason reason) {
        this.done = done;
        this.constructReturnReason = reason;
        this.io.flush();
        final @NotNull SimulatorNotice notice = new SimulatorNotice(
            SimulatorNotice.Action.STOP,
            this.maxSteps,
            this.getRunSpeed(),
            this.pc, reason, this.pe, done
        );
        // TODO: this is not completely threadsafe, if anything using Swing is observing
        // This can be fixed by making a SwingObserver class that is thread-safe
        this.simulatorNoticeDispatcher.dispatch(notice);
    }

    private boolean handleTrap(final @NotNull SimulationException se, final int pc) {
        assert se.reason != ExceptionReason.OTHER : "Unhandlable exception not thrown through ExitingEception";
        assert !se.reason.isInterrupt() : "Interrupts cannot be handled by the trap handler";

        // set the relevant CSRs
        try {
            CS_REGISTER_FILE.updateRegisterByName("ucause", se.reason.value);
            CS_REGISTER_FILE.updateRegisterByName("uepc", pc);
            CS_REGISTER_FILE.updateRegisterByName("utval", se.value);
        } catch (final SimulationException e) {
            // should never happen
            throw new RuntimeException(e);
        }

        // Get the interrupt handler if it exists
        final var utvec = CS_REGISTER_FILE.getIntValue("utvec");

        // Mode can be ignored because we are only handling traps
        final int base = utvec & 0xFFFFFFFC;

        ProgramStatement exceptionHandler = null;
        if ((CS_REGISTER_FILE.getIntValue("ustatus") & 0x1) != 0) { // test user-interrupt enable (UIE)
            try {
                exceptionHandler = Globals.MEMORY_INSTANCE.getStatement(base);
            } catch (final AddressErrorException aee) {
                // Handled below
            }
        }

        if (exceptionHandler != null) {
            try {
                // Set UPIE
                CS_REGISTER_FILE.updateRegisterByName(
                    "ustatus",
                    CS_REGISTER_FILE.getIntValue("ustatus") | (long) 0x10
                );
                // Clear UIE
                CS_REGISTER_FILE.updateRegisterByName(
                    "ustatus",
                    CS_REGISTER_FILE.getLongValue("ustatus") & ~0x1
                );
            } catch (final SimulationException e) {
                throw new RuntimeException(e);
            }
            Globals.REGISTER_FILE.setProgramCounter(base);
            return true;
        } else {
            // If we don't have an error handler or exceptions are disabled terminate the
            // process
            this.pe = se;
            this.stopExecution(true, Simulator.Reason.EXCEPTION);
            return false;
        }
    }

    private boolean handleInterrupt(final int value, final int cause, final int pc) {
        assert (cause & 0x80000000) != 0 : "Traps cannot be handled by the interupt handler";
        final int code = cause & 0x7FFFFFFF;

        // Don't handle cases where that interrupt isn't enabled
        assert (
            (CS_REGISTER_FILE.getLongValue("ustatus") & 0x1) != 0
                && (CS_REGISTER_FILE.getLongValue("uie") & (1 << code)) != 0
        )
            : "The interrupt handler must be enabled";

        // set the relevant CSRs
        try {
            CS_REGISTER_FILE.updateRegisterByName("ucause", cause);
            CS_REGISTER_FILE.updateRegisterByName("uepc", pc);
            CS_REGISTER_FILE.updateRegisterByName("utval", value);
        } catch (final SimulationException e) {
            // should never happen
            throw new RuntimeException(e);
        }

        // Get the interrupt handler if it exists
        final int utvec = CS_REGISTER_FILE.getIntValue("utvec");

        // Handle vectored mode
        int base = utvec & 0xFFFFFFFC;
        final int mode = utvec & 0x3;
        if (mode == 2) {
            base += 4 * code;
        }

        ProgramStatement exceptionHandler = null;
        try {
            exceptionHandler = Globals.MEMORY_INSTANCE.getStatement(base);
        } catch (final AddressErrorException aee) {
            // handled below
        }
        if (exceptionHandler != null) {
            try {
                // Set UPIE
                CS_REGISTER_FILE.updateRegisterByName(
                    "ustatus", CS_REGISTER_FILE.getLongValue("ustatus") | (long) 0x10);
                CS_REGISTER_FILE.updateRegisterByName(
                    "ustatus", CS_REGISTER_FILE.getLongValue("ustatus") & ~CSRegisterFile.INTERRUPT_ENABLE);
            } catch (final SimulationException e) {
                throw new RuntimeException(e);
            }

            // ControlAndStatusRegisterFile.clearRegister("ustatus", ControlAndStatusRegisterFile.INTERRUPT_ENABLE);
            Globals.REGISTER_FILE.setProgramCounter(base);
            return true;
        } else {
            // If we don't have an error handler or exceptions are disabled terminate the
            // process
            this.pe = new SimulationException(
                "Interrupt handler was not supplied, but interrupt enable was high",
                ExceptionReason.OTHER
            );
            this.stopExecution(true, Simulator.Reason.EXCEPTION);
            return false;
        }
    }

    @Override
    public final void run() {
        // The next two statements are necessary for GUI to be consistently updated
        // before the simulation gets underway. Without them, this happens only
        // intermittently,
        // with a consequence that some simulations are interruptable using PAUSE/STOP
        // and others
        // are not (because one or the other or both is not yet enabled).
        Thread.currentThread().setPriority(Thread.NORM_PRIORITY - 1);
        Thread.yield(); // let the main thread run a bit to finish updating the GUI

        this.startExecution();

        // ******************* PS addition 26 July 2006 **********************
        // A couple statements below were added for the purpose of assuring that when
        // "back stepping" is enabled, every instruction will have at least one entry
        // on the back-stepping stack. Most instructions will because they write either
        // to a register or memory. But "nop" and branches not taken do not. When the
        // user is stepping backward through the program, the stack is popped and if
        // an instruction has no entry it will be skipped over in the process. This has
        // no effect on the correctness of the mechanism but the visual jerkiness when
        // instruction highlighting skips such instrutions is disruptive. Current
        // solution
        // is to add a "do nothing" stack entry for instructions that do no write
        // anything.
        // To keep this invisible to the "simulate()" method writer, we
        // will push such an entry onto the stack here if there is none for this
        // instruction
        // by the time it has completed simulating. This is done by the IF statement
        // just after the call to the simulate method itself. The BackStepper method
        // does
        // the aforementioned check and decides whether to push or not. The result
        // is a a smoother interaction experience. But it comes at the cost of slowing
        // simulation speed for flat-out runs, for every instruction executed even
        // though very few will require the "do nothing" stack entry. For stepped or
        // timed execution the slower execution speed is not noticeable.
        // To avoid this cost I tried a different technique: back-fill with "do
        // nothings"
        // during the backstepping itself when this situation is recognized. Problem
        // was in recognizing all possible situations in which the stack contained such
        // a "gap". It became a morass of special cases and it seemed every weird test
        // case revealed another one. In addition, when a program
        // begins with one or more such instructions ("nop" and branches not taken),
        // the backstep button is not enabled until a "real" instruction is executed.
        // This is noticeable in stepped mode.
        // *********************************************************************

        Globals.REGISTER_FILE.initializeProgramCounter(this.pc);
        int steps = 0;

        // Volatile variable initialized false but can be set true by the main thread.
        // Used to stop or pause a running program. See stopSimulation() above.
        boolean ebreak = false;
        boolean waiting = false;
        final var context = new SimulationContext(
            Globals.REGISTER_FILE,
            Globals.FP_REGISTER_FILE,
            Globals.CS_REGISTER_FILE,
            Globals.MEMORY_INSTANCE,
            this.io
        );
        while (!this.stop) {
            // Perform the RISCV instruction in synchronized block. If external threads
            // agree
            // to access memory and registers only through synchronized blocks on same
            // lock variable, then full (albeit heavy-handed) protection of memory and
            // registers is assured. Not as critical for reading from those resources.
            Globals.MEMORY_REGISTERS_LOCK.lock();
            try {
                // Handle pending interupts and traps first
                long uip = CS_REGISTER_FILE.uip.getValueNoNotify();
                final long uie = CS_REGISTER_FILE.uie.getValueNoNotify();
                final boolean IE = (CS_REGISTER_FILE.ustatus.getValueNoNotify() & CSRegisterFile.INTERRUPT_ENABLE) != 0;
                // make sure no interrupts sneak in while we are processing them
                this.pc = Globals.REGISTER_FILE.getProgramCounter();
                synchronized (InterruptController.LOCK) {
                    boolean pendingExternal = InterruptController.externalPending();
                    boolean pendingTimer = InterruptController.timerPending();
                    final boolean pendingTrap = InterruptController.trapPending();
                    // This is the explicit (in the spec) order that interrupts should be serviced
                    if (IE && pendingExternal && (uie & CSRegisterFile.EXTERNAL_INTERRUPT) != 0) {
                        if (this.handleInterrupt(
                            InterruptController.claimExternal(),
                            ExceptionReason.EXTERNAL_INTERRUPT.value, this.pc
                        )) {
                            pendingExternal = false;
                            uip &= ~0x100;
                        } else {
                            return; // if the interrupt can't be handled, but the interrupt enable bit is high,
                            // thats an error
                        }
                    } else if (IE && (uip & 0x1) != 0
                        && (uie & CSRegisterFile.SOFTWARE_INTERRUPT) != 0) {
                        if (this.handleInterrupt(0, ExceptionReason.SOFTWARE_INTERRUPT.value, this.pc)) {
                            uip &= ~0x1;
                        } else {
                            return; // if the interrupt can't be handled, but the interrupt enable bit is high,
                            // thats an error
                        }
                    } else if (IE && pendingTimer && (uie & CSRegisterFile.TIMER_INTERRUPT) != 0) {
                        if (this.handleInterrupt(
                            InterruptController.claimTimer(),
                            ExceptionReason.TIMER_INTERRUPT.value,
                            this.pc
                        )) {
                            pendingTimer = false;
                            uip &= ~0x10;
                        } else {
                            return; // if the interrupt can't be handled, but the interrupt enable bit is high,
                            // thats an error
                        }
                    } else if (pendingTrap) { // if we have a pending trap and aren't handling an interrupt it must
                        // be handled
                        if (!this.handleTrap(
                            InterruptController.claimTrap(),
                            this.pc - BasicInstruction.BASIC_INSTRUCTION_LENGTH
                        )) {
                            return;
                        }
                    }
                    uip |= (pendingExternal ? CSRegisterFile.EXTERNAL_INTERRUPT : 0)
                        | (pendingTimer ? CSRegisterFile.TIMER_INTERRUPT : 0);
                }
                if (uip != CS_REGISTER_FILE.uip.getValueNoNotify()) {

                    try {
                        CS_REGISTER_FILE.updateRegisterByName("uip", uip);
                    } catch (final SimulationException e) {
                        // should never happen
                        throw new RuntimeException(e);
                    }
                }

                // always handle interrupts and traps before quiting
                // Check number of instructions executed. Return if at limit (-1 is no limit).
                if (this.maxSteps > 0) {
                    steps++;
                    if (steps > this.maxSteps) {
                        this.stopExecution(false, Simulator.Reason.MAX_STEPS);
                        return;
                    }
                }

                this.pc = Globals.REGISTER_FILE.getProgramCounter();
                // Get instuction
                final ProgramStatement statement;
                try {
                    statement = Globals.MEMORY_INSTANCE.getStatement(this.pc);
                } catch (final AddressErrorException e) {
                    final SimulationException tmp;
                    if (e.reason == ExceptionReason.LOAD_ACCESS_FAULT) {
                        tmp = new SimulationException(
                            "Instruction load access error",
                            ExceptionReason.INSTRUCTION_ACCESS_FAULT
                        );
                    } else {
                        tmp = new SimulationException(
                            "Instruction load alignment error",
                            ExceptionReason.INSTRUCTION_ADDR_MISALIGNED
                        );
                    }
                    if (!InterruptController.registerSynchronousTrap(tmp, this.pc)) {
                        this.pe = tmp;
                        try {
                            CS_REGISTER_FILE.updateRegisterByName("uepc", this.pc);
                        } catch (final SimulationException ex) {
                            throw new RuntimeException(ex);
                        }
                        this.stopExecution(true, Simulator.Reason.EXCEPTION);
                        return;
                    } else {
                        continue;
                    }
                }
                if (statement == null) {
                    this.stopExecution(true, Simulator.Reason.CLIFF_TERMINATION);
                    return;
                }

                try {
                    final BasicInstruction instruction = (BasicInstruction) statement.getInstruction();
                    if (instruction == null) {
                        // TODO: Proper error handling here
                        throw new SimulationException(
                            statement,
                            "undefined instruction (" + BinaryUtils.intToHexString(statement.getBinaryStatement())
                                + ")",
                            ExceptionReason.ILLEGAL_INSTRUCTION
                        );
                    }
                    Globals.REGISTER_FILE.incrementPC(instruction.getInstructionLength());
                    // THIS IS WHERE THE INSTRUCTION EXECUTION IS ACTUALLY SIMULATED!
                    instruction.simulate(statement, context);

                    // IF statement added 7/26/06 (explanation above)
                    if (OtherSettings.getBackSteppingEnabled()) {
                        Globals.program.getBackStepper().addDoNothing(this.pc);
                    }
                } catch (final BreakpointException b) {
                    // EBREAK needs backstepping support too.
                    if (OtherSettings.getBackSteppingEnabled()) {
                        Globals.program.getBackStepper().addDoNothing(this.pc);
                    }
                    ebreak = true;
                } catch (final WaitException w) {
                    if (OtherSettings.getBackSteppingEnabled()) {
                        Globals.program.getBackStepper().addDoNothing(this.pc);
                    }
                    waiting = true;
                } catch (final ExitingException e) {
                    if (e.reason == ExceptionReason.OTHER) {
                        this.constructReturnReason = Simulator.Reason.NORMAL_TERMINATION;
                    } else {
                        this.constructReturnReason = Simulator.Reason.EXCEPTION;
                        this.pe = e;
                    }
                    // TODO: remove access to constructReturnReason
                    this.stopExecution(true, this.constructReturnReason);
                    return;
                } catch (final SimulationException se) {
                    if (InterruptController.registerSynchronousTrap(se, this.pc)) {
                        continue;
                    } else {
                        this.pe = se;
                        this.stopExecution(true, Simulator.Reason.EXCEPTION);
                        return;
                    }
                }
            } finally {
                Globals.MEMORY_REGISTERS_LOCK.unlock();
            }

            // Update cycle(h) and instret(h)
            final long cycle = CS_REGISTER_FILE.cycle.getValueNoNotify();
            final long instret = CS_REGISTER_FILE.instret.getValueNoNotify();
            final long time = System.currentTimeMillis();
            CS_REGISTER_FILE.updateRegisterBackdoor(CS_REGISTER_FILE.cycle, cycle + 1);
            CS_REGISTER_FILE.updateRegisterBackdoor(CS_REGISTER_FILE.instret, instret + 1);
            CS_REGISTER_FILE.updateRegisterBackdoor(CS_REGISTER_FILE.time, time);

            // Return if we've reached a breakpoint.
            if (ebreak || Arrays.binarySearch(this.breakPoints, Globals.REGISTER_FILE.getProgramCounter()) >= 0) {
                this.stopExecution(false, Simulator.Reason.BREAKPOINT);
                return;
            }

            // Wait if WFI ran
            if (waiting) {
                if (!(InterruptController.externalPending() || InterruptController.timerPending())) {
                    synchronized (this) {
                        try {
                            this.wait();
                        } catch (final InterruptedException ie) {
                            // Don't bother catching an interruption
                        }
                    }
                }
                waiting = false;
            }

            this.onEndLoop();
        }
        this.stopExecution(false, this.constructReturnReason);
    }

    protected void onEndLoop() {
    }
}

