package rars.simulator;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rars.Globals;
import rars.ProgramStatement;
import rars.exceptions.*;
import rars.notices.SimulatorNotice;
import rars.riscv.BasicInstruction;
import rars.riscv.hardware.ControlAndStatusRegisterFile;
import rars.riscv.hardware.InterruptController;
import rars.riscv.hardware.Memory;
import rars.riscv.hardware.RegisterFile;
import rars.settings.OtherSettings;
import rars.util.Binary;
import rars.util.CustomPublisher;
import rars.util.SystemIO;
import rars.venus.run.RunSpeedPanel;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Consumer;

/*
Copyright (c) 2003-2010,  Pete Sanderson and Kenneth Vollmar

Developed by Pete Sanderson (psanderson@otterbein.edu)
and Kenneth Vollmar (kenvollmar@missouristate.edu)

Permission is hereby granted, free of charge, to any person obtaining 
a copy of this software and associated documentation files (the 
"Software"), to deal in the Software without restriction, including 
without limitation the rights to use, copy, modify, merge, publish, 
distribute, sublicense, and/or sell copies of the Software, and to 
permit persons to whom the Software is furnished to do so, subject 
to the following conditions:

The above copyright notice and this permission notice shall be 
included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF 
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR 
ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION 
WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

(MIT license, http://www.opensource.org/licenses/mit-license.html)
*/

/**
 * Used to simulate the execution of an assembled source program.
 *
 * @author Pete Sanderson
 * @version August 2005
 */
public final class Simulator extends CustomPublisher<SimulatorNotice> {
    private static @Nullable Simulator simulator = null; // Singleton object
    private static @Nullable Runnable interactiveGUIUpdater = null;
    private final @NotNull ArrayList<Consumer<Simulator>> stopListeners = new ArrayList<>(1);
    private @Nullable SimThread simulatorThread;

    private Simulator() {
        this.simulatorThread = null;
        if (Globals.gui != null) {
            Simulator.interactiveGUIUpdater = Simulator::updateUi;
        }
    }

    /**
     * Returns the Simulator object
     *
     * @return the Simulator object in use
     */
    public static @NotNull Simulator getInstance() {
        // Do NOT change this to create the Simulator at load time (in declaration
        // above)!
        // Its constructor looks for the GUI, which at load time is not created yet,
        // and incorrectly leaves interactiveGUIUpdater null! This causes runtime
        // exceptions while running in timed mode.
        if (Simulator.simulator == null) {
            Simulator.simulator = new Simulator();
        }
        return Simulator.simulator;
    }

    private static void updateUi() {
        if (Globals.gui.registersPane.getSelectedComponent() == Globals.gui.mainPane.executeTab.registerValues) {
            Globals.gui.mainPane.executeTab.registerValues.updateRegisters();
        } else {
            Globals.gui.mainPane.executeTab.fpRegValues.updateRegisters();
        }
        Globals.gui.mainPane.executeTab.dataSegment.updateValues();
        Globals.gui.mainPane.executeTab.textSegment.setCodeHighlighting(true);
        Globals.gui.mainPane.executeTab.textSegment.highlightStepAtPC();
    }

    /**
     * Simulate execution of given source program (in this thread). It must have
     * already been assembled.
     *
     * @param pc          address of first instruction to simulate; this goes into
     *                    program counter
     * @param maxSteps    maximum number of steps to perform before returning false
     *                    (0 or less means no max)
     * @param breakPoints array of breakpoint program counter values, use null if
     *                    none
     * @return a {@link Reason} object that indicates how the simulation ended/was stopped
     * @throws SimulationException Throws exception if run-time exception occurs.
     */
    public Reason simulate(final int pc, final int maxSteps, final int[] breakPoints) throws SimulationException {
        this.simulatorThread = new SimThread(pc, maxSteps, breakPoints);
        this.simulatorThread.run(); // Just call run, this is a blocking method
        final SimulationException pe = this.simulatorThread.pe;
        final boolean done = this.simulatorThread.done;
        final Reason out = this.simulatorThread.constructReturnReason;
        if (done)
            SystemIO.resetFiles(); // close any files opened in the process of simulating
        this.simulatorThread = null;
        if (pe != null) {
            throw pe;
        }
        return out;
    }

    /**
     * Start simulated execution of given source program (in a new thread). It must
     * have already been assembled.
     *
     * @param pc          address of first instruction to simulate; this goes into
     *                    program counter
     * @param maxSteps    maximum number of steps to perform before returning false
     *                    (0 or less means no max)
     * @param breakPoints array of breakpoint program counter values, use null if
     *                    none
     */
    public void startSimulation(final int pc, final int maxSteps, final int[] breakPoints) {
        this.simulatorThread = new SimThread(pc, maxSteps, breakPoints);
        new Thread(this.simulatorThread, "RISCV").start();
    }

    /**
     * Set the volatile stop boolean variable checked by the execution
     * thread at the end of each instruction execution. If variable
     * is found to be true, the execution thread will depart
     * gracefully so the main thread handling the GUI can take over.
     * This is used by both STOP and PAUSE features.
     */
    private void interruptExecution(final @NotNull Reason reason) {
        if (this.simulatorThread != null) {
            this.simulatorThread.setStop(reason);
            for (final var listener : this.stopListeners) {
                listener.accept(this);
            }
            this.simulatorThread = null;
        }
    }

    public void stopExecution() {
        this.interruptExecution(Reason.STOP);
    }

    public void pauseExecution() {
        this.interruptExecution(Reason.PAUSE);
    }

    public void addStopListener(final @NotNull Consumer<Simulator> l) {
        this.stopListeners.add(l);
    }

    public void removeStopListener(final @NotNull Consumer<Simulator> l) {
        this.stopListeners.remove(l);
    }

    /**
     * The Simthread object will call this method when it enters and returns from
     * its run() method. These signal start and stop, respectively, of
     * simulation execution. The observer can then adjust its own state depending
     * on the execution state. Note that "stop" and "done" are not the same thing.
     * "stop" just means it is leaving execution state; this could be triggered
     * by Stop button, by Pause button, by Step button, by runtime exception, by
     * instruction count limit, by breakpoint, or by end of simulation (truly done).
     */
    private void notifyObserversOfExecution(final @NotNull SimulatorNotice notice) {
        // TODO: this is not completely threadsafe, if anything using Swing is observing
        // This can be fixed by making a SwingObserver class that is thread-safe
        this.submit(notice);
    }

    /**
     * <p>interrupt.</p>
     */
    public void interrupt() {
        if (this.simulatorThread == null)
            return;
        this.simulatorThread.interrupt();
    }

    /**
     * various reasons for simulate to end...
     */
    public enum Reason {
        BREAKPOINT,
        EXCEPTION,
        MAX_STEPS, // includes step mode (where maxSteps is 1)
        NORMAL_TERMINATION,
        CLIFF_TERMINATION, // run off bottom of program
        PAUSE,
        STOP
    }

    /**
     * Perform the simulated execution. It is "interrupted" when main thread sets
     * the "stop" variable to true. The variable is tested before the next
     * instruction
     * is simulated. Thus interruption occurs in a tightly controlled fashion.
     */

    static class SimThread implements Runnable {
        private final int maxSteps;
        private int pc;
        private int[] breakPoints;
        private boolean done;
        private SimulationException pe;
        private volatile boolean stop = false;
        private Reason constructReturnReason;

        /**
         * SimThread constructor. Receives all the information it needs to simulate
         * execution.
         *
         * @param pc          address in text segment of first instruction to simulate
         * @param maxSteps    maximum number of instruction steps to simulate. Default
         *                    of -1 means no maximum
         * @param breakPoints array of breakpoints (instruction addresses) specified by
         *                    user
         */
        SimThread(final int pc, final int maxSteps, final int[] breakPoints) {
            this.pc = pc;
            this.maxSteps = maxSteps;
            this.breakPoints = breakPoints;
            this.done = false;
            this.pe = null;
        }

        /**
         * Sets to "true" the volatile boolean variable that is tested after each
         * instruction is executed. After calling this method, the next test
         * will yield "true" and "construct" will return.
         *
         * @param reason the Reason for stopping (PAUSE or STOP)
         */
        public synchronized void setStop(final Reason reason) {
            this.stop = true;
            this.constructReturnReason = reason;
            this.notify();
        }

        private void startExecution() {
            Simulator.getInstance().notifyObserversOfExecution(new SimulatorNotice(SimulatorNotice.Action.START,
                this.maxSteps,
                Globals.gui != null
                    ? Globals.gui.runSpeedPanel.getRunSpeed()
                    : RunSpeedPanel.UNLIMITED_SPEED,
                this.pc, null, this.pe, this.done));
        }

        private void stopExecution(final boolean done, final Reason reason) {
            this.done = done;
            this.constructReturnReason = reason;
            SystemIO.flush(true);
            if (done)
                SystemIO.resetFiles(); // close any files opened in the process of simulating
            Simulator.getInstance().notifyObserversOfExecution(new SimulatorNotice(SimulatorNotice.Action.STOP,
                this.maxSteps,
                Globals.gui != null
                    ? Globals.gui.runSpeedPanel.getRunSpeed()
                    : RunSpeedPanel.UNLIMITED_SPEED,
                this.pc, reason, this.pe, done));
        }

        private synchronized void interrupt() {
            this.notify();
        }

        private boolean handleTrap(final SimulationException se, final int pc) {
            assert se.reason != ExceptionReason.OTHER : "Unhandlable exception not thrown through ExitingEception";
            assert !se.reason.isInterrupt() : "Interrupts cannot be handled by the trap handler";

            // set the relevant CSRs
            ControlAndStatusRegisterFile.updateRegister("ucause", se.reason.value);
            ControlAndStatusRegisterFile.updateRegister("uepc", pc);
            ControlAndStatusRegisterFile.updateRegister("utval", se.value);

            // Get the interrupt handler if it exists
            final int utvec = ControlAndStatusRegisterFile.getValue("utvec");

            // Mode can be ignored because we are only handling traps
            final int base = utvec & 0xFFFFFFFC;

            ProgramStatement exceptionHandler = null;
            if ((ControlAndStatusRegisterFile.getValue("ustatus") & 0x1) != 0) { // test user-interrupt enable (UIE)
                try {
                    exceptionHandler = Memory.getInstance().getStatement(base);
                } catch (final AddressErrorException aee) {
                    // Handled below
                }
            }

            if (exceptionHandler != null) {
                ControlAndStatusRegisterFile.orRegister("ustatus", 0x10); // Set UPIE
                ControlAndStatusRegisterFile.clearRegister("ustatus", 0x1); // Clear UIE
                RegisterFile.setProgramCounter(base);
                return true;
            } else {
                // If we don't have an error handler or exceptions are disabled terminate the
                // process
                this.pe = se;
                this.stopExecution(true, Reason.EXCEPTION);
                return false;
            }
        }

        private boolean handleInterrupt(final int value, final int cause, final int pc) {
            assert (cause & 0x80000000) != 0 : "Traps cannot be handled by the interupt handler";
            final int code = cause & 0x7FFFFFFF;

            // Don't handle cases where that interrupt isn't enabled
            assert ((ControlAndStatusRegisterFile.getValue("ustatus") & 0x1) != 0
                && (ControlAndStatusRegisterFile.getValue("uie") & (1 << code)) != 0)
                : "The interrupt handler must be enabled";

            // set the relevant CSRs
            ControlAndStatusRegisterFile.updateRegister("ucause", cause);
            ControlAndStatusRegisterFile.updateRegister("uepc", pc);
            ControlAndStatusRegisterFile.updateRegister("utval", value);

            // Get the interrupt handler if it exists
            final int utvec = ControlAndStatusRegisterFile.getValue("utvec");

            // Handle vectored mode
            int base = utvec & 0xFFFFFFFC;
            final int mode = utvec & 0x3;
            if (mode == 2) {
                base += 4 * code;
            }

            ProgramStatement exceptionHandler = null;
            try {
                exceptionHandler = Memory.getInstance().getStatement(base);
            } catch (final AddressErrorException aee) {
                // handled below
            }
            if (exceptionHandler != null) {
                ControlAndStatusRegisterFile.orRegister("ustatus", 0x10); // Set UPIE
                ControlAndStatusRegisterFile.clearRegister("ustatus", ControlAndStatusRegisterFile.INTERRUPT_ENABLE);
                RegisterFile.setProgramCounter(base);
                return true;
            } else {
                // If we don't have an error handler or exceptions are disabled terminate the
                // process
                this.pe = new SimulationException("Interrupt handler was not supplied, but interrupt enable was high");
                this.stopExecution(true, Reason.EXCEPTION);
                return false;
            }
        }

        @Override
        public void run() {
            // The next two statements are necessary for GUI to be consistently updated
            // before the simulation gets underway. Without them, this happens only
            // intermittently,
            // with a consequence that some simulations are interruptable using PAUSE/STOP
            // and others
            // are not (because one or the other or both is not yet enabled).
            Thread.currentThread().setPriority(Thread.NORM_PRIORITY - 1);
            Thread.yield(); // let the main thread run a bit to finish updating the GUI

            if (this.breakPoints == null || this.breakPoints.length == 0) {
                this.breakPoints = null;
            } else {
                Arrays.sort(this.breakPoints); // must be pre-sorted for binary search
            }

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

            RegisterFile.initializeProgramCounter(this.pc);
            ProgramStatement statement;
            int steps = 0;
            boolean ebreak = false, waiting = false;

            // Volatile variable initialized false but can be set true by the main thread.
            // Used to stop or pause a running program. See stopSimulation() above.
            while (!this.stop) {
                SystemIO.flush(false);
                // Perform the RISCV instruction in synchronized block. If external threads
                // agree
                // to access memory and registers only through synchronized blocks on same
                // lock variable, then full (albeit heavy-handed) protection of memory and
                // registers is assured. Not as critical for reading from those resources.
                Globals.memoryAndRegistersLock.lock();
                try {
                    // Handle pending interupts and traps first
                    long uip = ControlAndStatusRegisterFile.getValueNoNotify("uip");
                    final long uie = ControlAndStatusRegisterFile.getValueNoNotify("uie");
                    final boolean IE = (ControlAndStatusRegisterFile.getValueNoNotify("ustatus")
                        & ControlAndStatusRegisterFile.INTERRUPT_ENABLE) != 0;
                    // make sure no interrupts sneak in while we are processing them
                    this.pc = RegisterFile.getProgramCounter();
                    synchronized (InterruptController.lock) {
                        boolean pendingExternal = InterruptController.externalPending();
                        boolean pendingTimer = InterruptController.timerPending();
                        final boolean pendingTrap = InterruptController.trapPending();
                        // This is the explicit (in the spec) order that interrupts should be serviced
                        if (IE && pendingExternal && (uie & ControlAndStatusRegisterFile.EXTERNAL_INTERRUPT) != 0) {
                            if (this.handleInterrupt(InterruptController.claimExternal(),
                                ExceptionReason.EXTERNAL_INTERRUPT.value, this.pc)) {
                                pendingExternal = false;
                                uip &= ~0x100;
                            } else {
                                return; // if the interrupt can't be handled, but the interrupt enable bit is high,
                                // thats an error
                            }
                        } else if (IE && (uip & 0x1) != 0
                            && (uie & ControlAndStatusRegisterFile.SOFTWARE_INTERRUPT) != 0) {
                            if (this.handleInterrupt(0, ExceptionReason.SOFTWARE_INTERRUPT.value, this.pc)) {
                                uip &= ~0x1;
                            } else {
                                return; // if the interrupt can't be handled, but the interrupt enable bit is high,
                                // thats an error
                            }
                        } else if (IE && pendingTimer && (uie & ControlAndStatusRegisterFile.TIMER_INTERRUPT) != 0) {
                            if (this.handleInterrupt(InterruptController.claimTimer(),
                                ExceptionReason.TIMER_INTERRUPT.value,
                                this.pc)) {
                                pendingTimer = false;
                                uip &= ~0x10;
                            } else {
                                return; // if the interrupt can't be handled, but the interrupt enable bit is high,
                                // thats an error
                            }
                        } else if (pendingTrap) { // if we have a pending trap and aren't handling an interrupt it must
                            // be handled
                            if (!this.handleTrap(InterruptController.claimTrap(),
                                this.pc - BasicInstruction.BASIC_INSTRUCTION_LENGTH)) {
                                return;
                            }
                        }
                        uip |= (pendingExternal ? ControlAndStatusRegisterFile.EXTERNAL_INTERRUPT : 0)
                            | (pendingTimer ? ControlAndStatusRegisterFile.TIMER_INTERRUPT : 0);
                    }
                    if (uip != ControlAndStatusRegisterFile.getValueNoNotify("uip")) {
                        ControlAndStatusRegisterFile.updateRegister("uip", uip);
                    }

                    // always handle interrupts and traps before quiting
                    // Check number of instructions executed. Return if at limit (-1 is no limit).
                    if (this.maxSteps > 0) {
                        steps++;
                        if (steps > this.maxSteps) {
                            this.stopExecution(false, Reason.MAX_STEPS);
                            return;
                        }
                    }

                    this.pc = RegisterFile.getProgramCounter();
                    RegisterFile.incrementPC();
                    // Get instuction
                    try {
                        statement = Memory.getInstance().getStatement(this.pc);
                    } catch (final AddressErrorException e) {
                        final SimulationException tmp;
                        if (e.reason == ExceptionReason.LOAD_ACCESS_FAULT) {
                            tmp = new SimulationException("Instruction load access error",
                                ExceptionReason.INSTRUCTION_ACCESS_FAULT);
                        } else {
                            tmp = new SimulationException("Instruction load alignment error",
                                ExceptionReason.INSTRUCTION_ADDR_MISALIGNED);
                        }
                        if (!InterruptController.registerSynchronousTrap(tmp, this.pc)) {
                            this.pe = tmp;
                            ControlAndStatusRegisterFile.updateRegister("uepc", this.pc);
                            this.stopExecution(true, Reason.EXCEPTION);
                            return;
                        } else {
                            continue;
                        }
                    }
                    if (statement == null) {
                        this.stopExecution(true, Reason.CLIFF_TERMINATION);
                        return;
                    }

                    try {
                        final BasicInstruction instruction = (BasicInstruction) statement.getInstruction();
                        if (instruction == null) {
                            // TODO: Proper error handling here
                            throw new SimulationException(statement,
                                "undefined instruction (" + Binary.intToHexString(statement.getBinaryStatement())
                                    + ")",
                                ExceptionReason.ILLEGAL_INSTRUCTION);
                        }
                        // THIS IS WHERE THE INSTRUCTION EXECUTION IS ACTUALLY SIMULATED!
                        instruction.simulate(statement);

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
                            this.constructReturnReason = Reason.NORMAL_TERMINATION;
                        } else {
                            this.constructReturnReason = Reason.EXCEPTION;
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
                            this.stopExecution(true, Reason.EXCEPTION);
                            return;
                        }
                    }
                } finally {
                    Globals.memoryAndRegistersLock.unlock();
                }

                // Update cycle(h) and instret(h)
                final long cycle = ControlAndStatusRegisterFile.getValueNoNotify("cycle");
                final long instret = ControlAndStatusRegisterFile.getValueNoNotify("instret");
                final long time = System.currentTimeMillis();
                ControlAndStatusRegisterFile.updateRegisterBackdoor("cycle", cycle + 1);
                ControlAndStatusRegisterFile.updateRegisterBackdoor("instret", instret + 1);
                ControlAndStatusRegisterFile.updateRegisterBackdoor("time", time);

                // Return if we've reached a breakpoint.
                if (ebreak || (this.breakPoints != null) &&
                    (Arrays.binarySearch(this.breakPoints, RegisterFile.getProgramCounter()) >= 0)) {
                    this.stopExecution(false, Reason.BREAKPOINT);
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

                // schedule GUI update only if: there is in fact a GUI! AND
                // using Run, not Step (maxSteps != 1) AND
                // running slowly enough for GUI to keep up
                if (Simulator.interactiveGUIUpdater != null && this.maxSteps != 1 &&
                    Globals.gui.runSpeedPanel.getRunSpeed() < RunSpeedPanel.UNLIMITED_SPEED) {
                    SwingUtilities.invokeLater(Simulator.interactiveGUIUpdater);
                }
                if (Globals.gui != null) { // OR added by DPS 24 July 2008 to enable
                    // speed control by stand-alone tool
                    final var runSpeedPanel = Globals.gui.runSpeedPanel;
                    if (this.maxSteps != 1 &&
                        runSpeedPanel.getRunSpeed() < RunSpeedPanel.UNLIMITED_SPEED) {
                        try {
                            this.wait((int) (1000 / runSpeedPanel.getRunSpeed()));
                        } catch (final InterruptedException ignored) {
                        }
                    }
                }
            }
            this.stopExecution(false, this.constructReturnReason);
        }
    }
}
