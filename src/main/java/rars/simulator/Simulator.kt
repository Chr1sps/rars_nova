package rars.simulator;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rars.exceptions.SimulationException;
import rars.io.ConsoleIO;
import rars.notices.SimulatorNotice;
import rars.util.ListenerDispatcher;
import rars.venus.VenusUI;

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
public final class Simulator {
    public final @NotNull ListenerDispatcher<@NotNull SimulatorNotice>.Hook simulatorNoticeHook;
    public final @NotNull ListenerDispatcher<Void>.Hook stopEventHook;
    private final @NotNull ListenerDispatcher<@NotNull SimulatorNotice> simulatorNoticeDispatcher;
    private final @NotNull ListenerDispatcher<Void> stopEventDispatcher;

    private @Nullable SimThread simulatorThread;

    public Simulator() {
        this.simulatorThread = null;
        this.simulatorNoticeDispatcher = new ListenerDispatcher<>();
        this.simulatorNoticeHook = this.simulatorNoticeDispatcher.getHook();
        this.stopEventDispatcher = new ListenerDispatcher<>();
        this.stopEventHook = this.stopEventDispatcher.getHook();
    }

    /**
     * Simulate execution of given source program (in this thread). It must have
     * already been assembled.
     *
     * @param pc
     *     address of first instruction to simulate; this goes into
     *     program counter
     * @param maxSteps
     *     maximum number of steps to perform before returning false
     *     (0 or less means no max)
     * @return a {@link Reason} object that indicates how the simulation ended/was stopped
     * @throws SimulationException
     *     Throws exception if run-time exception occurs.
     */
    public Reason simulateCli(
        final int pc,
        final int maxSteps,
        final @NotNull ConsoleIO consoleIO
    ) throws SimulationException {
        this.simulatorThread = new SimThread(
            pc,
            maxSteps,
            new int[0],
            consoleIO,
            this.simulatorNoticeDispatcher
        );
        this.simulatorThread.run(); // Just call run, this is a blocking method
        final SimulationException pe = this.simulatorThread.getPe();
        final Reason out = this.simulatorThread.getConstructReturnReason();
        this.simulatorThread = null;
        if (pe != null) {
            throw pe;
        }
        return out;
    }

    // region UI control methods

    /**
     * Start simulated execution of given source program (in a new thread). It must
     * have already been assembled.
     *
     * @param pc
     *     address of first instruction to simulate; this goes into
     *     program counter
     * @param maxSteps
     *     maximum number of steps to perform before returning false
     *     (0 or less means no max)
     * @param breakPoints
     *     array of breakpoint program counter values, use null if
     *     none
     */
    public void startSimulation(
        final int pc,
        final int maxSteps,
        final int[] breakPoints,
        final @NotNull VenusUI mainUI
    ) {
        this.simulatorThread = new GuiSimThread(pc, maxSteps, breakPoints, this.simulatorNoticeDispatcher, mainUI);
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
            this.stopEventDispatcher.dispatch(null);
            this.simulatorThread = null;
        }
    }

    public void stopExecution() {
        this.interruptExecution(Reason.STOP);
    }

    public void pauseExecution() {
        this.interruptExecution(Reason.PAUSE);
    }

    // endregion UI control methods

    /**
     * <p>interrupt.</p>
     */
    public void interrupt() {
        if (this.simulatorThread == null) {
            return;
        }
        synchronized (this.simulatorThread) {
            this.simulatorThread.notify();
        }
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
}
