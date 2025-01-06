package rars.venus.run;

import rars.Globals;
import rars.RISCVProgram;
import rars.exceptions.SimulationException;
import rars.notices.SimulatorNotice;
import rars.riscv.hardware.RegisterFile;
import rars.settings.BoolSetting;
import rars.simulator.ProgramArgumentList;
import rars.simulator.Simulator;
import rars.util.SimpleSubscriber;
import rars.util.SystemIO;
import rars.venus.ExecutePane;
import rars.venus.FileStatus;
import rars.venus.GuiAction;
import rars.venus.VenusUI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.concurrent.Flow;

import static rars.settings.BoolSettings.BOOL_SETTINGS;

/*
Copyright (c) 2003-2007,  Pete Sanderson and Kenneth Vollmar

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
 * Action class for the Run -> Go menu item (and toolbar icon)
 */
public class RunGoAction extends GuiAction {

    public static final int defaultMaxSteps = -1; // "forever", formerly 10000000; // 10 million
    public static int maxSteps = RunGoAction.defaultMaxSteps;
    private final VenusUI mainUI;
    private String name;
    private ExecutePane executePane;

    /**
     * <p>Constructor for RunGoAction.</p>
     *
     * @param name
     *     a {@link java.lang.String} object
     * @param icon
     *     a {@link javax.swing.Icon} object
     * @param descrip
     *     a {@link java.lang.String} object
     * @param mnemonic
     *     a {@link java.lang.Integer} object
     * @param accel
     *     a {@link javax.swing.KeyStroke} object
     * @param gui
     *     a {@link VenusUI} object
     */
    public RunGoAction(
        final String name, final Icon icon, final String descrip,
        final Integer mnemonic, final KeyStroke accel, final VenusUI gui
    ) {
        super(name, icon, descrip, mnemonic, accel);
        this.mainUI = gui;
    }

    /**
     * Reset max steps limit to default value at termination of a simulated
     * execution.
     */
    public static void resetMaxSteps() {
        RunGoAction.maxSteps = RunGoAction.defaultMaxSteps;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Action to take when GO is selected -- run the MIPS program!
     */
    @Override
    public void actionPerformed(final ActionEvent e) {
        this.name = this.getValue(Action.NAME).toString();
        this.executePane = this.mainUI.mainPane.executeTab;
        if (FileStatus.isAssembled()) {
            if (!this.mainUI.isExecutionStarted) {
                this.processProgramArgumentsIfAny(); // DPS 17-July-2008
            }
            if (this.mainUI.isMemoryReset || this.mainUI.isExecutionStarted) {

                // added 8/27/05
                this.mainUI.isExecutionStarted = true;

                this.mainUI.messagesPane.postMessage(
                    this.name + ": running " + FileStatus.getSystemFile().getName() + "\n\n");
                this.mainUI.messagesPane.selectRunMessageTab();
                this.executePane.textSegment.setCodeHighlighting(false);
                this.executePane.textSegment.unhighlightAllSteps();
                // FileStatus.set(FileStatus.RUNNING);
                this.mainUI.setMenuState(FileStatus.State.RUNNING);

                // Setup cleanup procedures for the simulation
                final var stopListener = new SimpleSubscriber<SimulatorNotice>() {
                    private Flow.Subscription subscription;

                    @Override
                    public void onSubscribe(final Flow.Subscription subscription) {
                        this.subscription = subscription;
                        this.subscription.request(1);
                    }

                    @Override
                    public void onNext(final SimulatorNotice notice) {
                        if (notice.action() != SimulatorNotice.Action.STOP) {
                            this.subscription.request(1);
                            return;
                        }
                        final Simulator.Reason reason = notice.reason();
                        if (reason == Simulator.Reason.PAUSE || reason == Simulator.Reason.BREAKPOINT) {
                            EventQueue.invokeLater(() -> RunGoAction.this.paused(
                                notice.done(), reason,
                                notice.exception()
                            ));
                        } else {
                            EventQueue.invokeLater(() -> RunGoAction.this.stopped(notice.exception(), reason));
                        }
                        this.subscription.cancel();
                    }
                };
                Simulator.getInstance().subscribe(stopListener);

                final int[] breakPoints = this.executePane.textSegment.getSortedBreakPointsArray();
                RISCVProgram.startSimulation(RunGoAction.maxSteps, breakPoints);
            } else {
                // This should never occur because at termination the Go and Step buttons are
                // disabled.
                JOptionPane.showMessageDialog(
                    this.mainUI,
                    "reset " + this.mainUI.isMemoryReset + " started " + this.mainUI.isExecutionStarted
                );// "You
                // must
                // reset
                // before
                // you
                // can
                // execute
                // the
                // program
                // again.");
            }
        } else {
            // note: this should never occur since "Go" is only enabled after successful
            // assembly.
            JOptionPane.showMessageDialog(this.mainUI, "The program must be assembled before it can be run.");
        }
    }

    /**
     * Method to be called when Pause is selected through menu/toolbar/shortcut.
     * This should only
     * happen when MIPS program is running (FileStatus.RUNNING). See VenusUI.java
     * for enabled
     * status of menu items based on FileStatus. Set GUI as if at breakpoint or
     * executing
     * step by step.
     *
     * @param done
     *     a boolean
     * @param pauseReason
     *     a {@link Simulator.Reason} object
     * @param pe
     *     a {@link SimulationException} object
     */
    public void paused(final boolean done, final Simulator.Reason pauseReason, final SimulationException pe) {
        // I doubt this can happen (pause when execution finished), but if so treat it
        // as stopped.
        if (done) {
            this.stopped(pe, Simulator.Reason.NORMAL_TERMINATION);
            return;
        }
        if (pauseReason == Simulator.Reason.BREAKPOINT) {
            this.mainUI.messagesPane.postMessage(
                this.name + ": execution paused at breakpoint: " + FileStatus.getSystemFile().getName() + "\n\n");
        } else {
            this.mainUI.messagesPane.postMessage(
                this.name + ": execution paused by user: " + FileStatus.getSystemFile().getName() + "\n\n");
        }
        this.mainUI.messagesPane.selectMessageTab();
        this.executePane.textSegment.setCodeHighlighting(true);
        this.executePane.textSegment.highlightStepAtPC();
        this.executePane.registerValues.updateRegisters();
        this.executePane.fpRegValues.updateRegisters();
        this.executePane.csrValues.updateRegisters();
        this.executePane.dataSegment.updateValues();
        FileStatus.set(FileStatus.State.RUNNABLE);
        this.mainUI.isMemoryReset = false;
    }

    /**
     * Method to be called when Stop is selected through menu/toolbar/shortcut. This
     * should only
     * happen when MIPS program is running (FileStatus.RUNNING). See VenusUI.java
     * for enabled
     * status of menu items based on FileStatus. Display finalized values as if
     * execution
     * terminated due to completion or exception.
     *
     * @param pe
     *     a {@link SimulationException} object
     * @param reason
     *     a {@link Simulator.Reason} object
     */
    public void stopped(final SimulationException pe, final Simulator.Reason reason) {
        // show final register and data segment values.
        this.executePane.registerValues.updateRegisters();
        this.executePane.fpRegValues.updateRegisters();
        this.executePane.csrValues.updateRegisters();
        this.executePane.dataSegment.updateValues();
        FileStatus.set(FileStatus.State.TERMINATED);
        SystemIO.resetFiles(); // close any files opened in MIPS program
        // Bring CSRs to the front if terminated due to exception.
        if (pe != null) {
            this.mainUI.registersPane.setSelectedComponent(this.executePane.csrValues);
            this.executePane.textSegment.setCodeHighlighting(true);
            this.executePane.textSegment.unhighlightAllSteps();
            this.executePane.textSegment.highlightStepAtAddress(RegisterFile.INSTANCE.getProgramCounter() - 4);
        }
        switch (reason) {
            case NORMAL_TERMINATION:
                this.mainUI.messagesPane.postMessage(
                    "\n" + this.name + ": execution completed successfully.\n\n");
                this.mainUI.messagesPane.postRunMessage(
                    "\n-- program is finished running (" + Globals.exitCode + ") --\n\n");
                this.mainUI.messagesPane.selectRunMessageTab();
                break;
            case CLIFF_TERMINATION:
                this.mainUI.messagesPane.postMessage(
                    "\n" + this.name + ": execution terminated by null instruction.\n\n");
                this.mainUI.messagesPane.postRunMessage(
                    "\n-- program is finished running (dropped off bottom) --\n\n");
                this.mainUI.messagesPane.selectRunMessageTab();
                break;
            case EXCEPTION:
                this.mainUI.messagesPane.postMessage(
                    pe.errorMessage.generateReport());
                this.mainUI.messagesPane.postMessage(
                    "\n" + this.name + ": execution terminated with errors.\n\n");
                break;
            case STOP:
                this.mainUI.messagesPane.postMessage(
                    "\n" + this.name + ": execution terminated by user.\n\n");
                this.mainUI.messagesPane.selectMessageTab();
                break;
            case MAX_STEPS:
                this.mainUI.messagesPane.postMessage(
                    "\n" + this.name + ": execution step limit of " + RunGoAction.maxSteps + " exceeded.\n\n");
                this.mainUI.messagesPane.selectMessageTab();
                break;
            default:
                // should never get here, because the other two cases are covered by paused()
        }
        RunGoAction.resetMaxSteps();
        this.mainUI.isMemoryReset = false;
    }

    /**
     * Method to store any program arguments into MIPS memory and registers before
     * execution begins. Arguments go into the gap between $sp and kernel memory.
     * Argument pointers and count go into runtime stack and $sp is adjusted accordingly.
     * $a0 gets argument count (argc), $a1 gets stack address of first arg pointer (argv).
     */
    private void processProgramArgumentsIfAny() {
        final String programArguments = this.executePane.textSegment.getProgramArguments();
        if (programArguments == null || programArguments.isEmpty() ||
            !BOOL_SETTINGS.getSetting(BoolSetting.PROGRAM_ARGUMENTS)) {
            return;
        }
        new ProgramArgumentList(programArguments).storeProgramArguments();
    }

}
