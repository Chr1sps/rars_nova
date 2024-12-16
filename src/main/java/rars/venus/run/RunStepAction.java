package rars.venus.run;

import rars.Globals;
import rars.RISCVprogram;
import rars.exceptions.SimulationException;
import rars.notices.SimulatorNotice;
import rars.riscv.hardware.RegisterFile;
import rars.settings.BoolSetting;
import rars.simulator.ProgramArgumentList;
import rars.simulator.Simulator;
import rars.util.SimpleSubscriber;
import rars.venus.ExecutePane;
import rars.venus.FileStatus;
import rars.venus.GuiAction;
import rars.venus.VenusUI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.concurrent.Flow;

/*
Copyright (c) 2003-2006,  Pete Sanderson and Kenneth Vollmar

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
 * Action for the Run -> Step menu item
 */
public class RunStepAction extends GuiAction {

    private final VenusUI mainUI;
    private String name;
    private ExecutePane executePane;

    /**
     * <p>Constructor for RunStepAction.</p>
     *
     * @param name     a {@link java.lang.String} object
     * @param icon     a {@link javax.swing.Icon} object
     * @param descrip  a {@link java.lang.String} object
     * @param mnemonic a {@link java.lang.Integer} object
     * @param accel    a {@link javax.swing.KeyStroke} object
     * @param gui      a {@link VenusUI} object
     */
    public RunStepAction(final String name, final Icon icon, final String descrip,
                         final Integer mnemonic, final KeyStroke accel, final VenusUI gui) {
        super(name, icon, descrip, mnemonic, accel);
        this.mainUI = gui;
    }

    /**
     * {@inheritDoc}
     * <p>
     * perform next simulated instruction step.
     */
    @Override
    public void actionPerformed(final ActionEvent e) {
        this.name = this.getValue(Action.NAME).toString();
        this.executePane = this.mainUI.getMainPane().getExecutePane();
        if (FileStatus.isAssembled()) {
            if (!this.mainUI.getStarted()) { // DPS 17-July-2008
                this.processProgramArgumentsIfAny();
            }
            this.mainUI.setStarted(true);
            this.mainUI.getMessagesPane().selectRunMessageTab();
            this.executePane.getTextSegmentWindow().setCodeHighlighting(true);

            final var stopListener = new SimpleSubscriber<SimulatorNotice>() {
                private Flow.Subscription subscription;

                @Override
                public void onSubscribe(final Flow.Subscription subscription) {
                    this.subscription = subscription;
                    this.subscription.request(1);
                }

                @Override
                public void onNext(final SimulatorNotice item) {
                    if (item.action() != SimulatorNotice.Action.STOP) {
                        this.subscription.request(1);
                        return;
                    }
                    EventQueue.invokeLater(() -> RunStepAction.this.stepped(item.done(), item.reason(),
                            item.exception()));
                    this.subscription.cancel();
                }
            };
            // Setup callback for after step finishes
//                public void update(Observable o, Object simulator) {
//                    SimulatorNotice notice = ((SimulatorNotice) simulator);
//                    if (notice.getAction() != SimulatorNotice.SIMULATOR_STOP)
//                        return;
//                    EventQueue.invokeLater(() -> stepped(notice.getDone(), notice.getReason(), notice.getException
//                    ()));
//                    o.deleteObserver(this);
//                }
            Simulator.getInstance().subscribe(stopListener);

            RISCVprogram.startSimulation(1, null);
        } else {
            // note: this should never occur since "Step" is only enabled after successful
            // assembly.
            JOptionPane.showMessageDialog(this.mainUI, "The program must be assembled before it can be run.");
        }
    }

    // When step is completed, control returns here (from execution thread,
    // indirectly)
    // to update the GUI.

    /**
     * <p>stepped.</p>
     *
     * @param done   a boolean
     * @param reason a {@link Simulator.Reason} object
     * @param pe     a {@link SimulationException} object
     */
    public void stepped(final boolean done, final Simulator.Reason reason, final SimulationException pe) {
        this.executePane.getRegistersWindow().updateRegisters();
        this.executePane.getFloatingPointWindow().updateRegisters();
        this.executePane.getControlAndStatusWindow().updateRegisters();
        this.executePane.getDataSegmentWindow().updateValues();
        if (!done) {
            this.executePane.getTextSegmentWindow().highlightStepAtPC();
            FileStatus.set(FileStatus.State.RUNNABLE);
        }
        if (done) {
            RunGoAction.resetMaxSteps();
            this.executePane.getTextSegmentWindow().unhighlightAllSteps();
            FileStatus.set(FileStatus.State.TERMINATED);
        }
        if (done && pe == null) {
            this.mainUI.getMessagesPane().postMessage(
                    "\n" + this.name + ": execution " +
                            ((reason == Simulator.Reason.CLIFF_TERMINATION) ? "terminated due to null instruction."
                                    : "completed successfully.")
                            + "\n\n");
            this.mainUI.getMessagesPane().postRunMessage(
                    "\n-- program is finished running" +
                            ((reason == Simulator.Reason.CLIFF_TERMINATION) ? "(dropped off bottom)"
                                    : " (" + Globals.exitCode + ")")
                            + " --\n\n");
            this.mainUI.getMessagesPane().selectRunMessageTab();
        }
        if (pe != null) {
            RunGoAction.resetMaxSteps();
            this.mainUI.getMessagesPane().postMessage(
                    pe.errorMessage.generateReport());
            this.mainUI.getMessagesPane().postMessage(
                    "\n" + this.name + ": execution terminated with errors.\n\n");
            this.mainUI.getRegistersPane().setSelectedComponent(this.executePane.getControlAndStatusWindow());
            FileStatus.set(FileStatus.State.TERMINATED); // should be redundant.
            this.executePane.getTextSegmentWindow().setCodeHighlighting(true);
            this.executePane.getTextSegmentWindow().unhighlightAllSteps();
            this.executePane.getTextSegmentWindow().highlightStepAtAddress(RegisterFile.getProgramCounter() - 4);
        }
        this.mainUI.setReset(false);
    }

    ////////////////////////////////////////////////////////////////////////////////////
    // Method to store any program arguments into MIPS memory and registers before
    // execution begins. Arguments go into the gap between $sp and kernel memory.
    // Argument pointers and count go into runtime stack and $sp is adjusted
    //////////////////////////////////////////////////////////////////////////////////// accordingly.
    // $a0 gets argument count (argc), $a1 gets stack address of first arg pointer

    /// ///////////////////////////////////////////////////////////////////////////////// (argv).
    private void processProgramArgumentsIfAny() {
        final String programArguments = this.executePane.getTextSegmentWindow().getProgramArguments();
        if (programArguments == null || programArguments.isEmpty() ||
                !Globals.getSettings().getBoolSettings().getSetting(BoolSetting.PROGRAM_ARGUMENTS)) {
            return;
        }
        new ProgramArgumentList(programArguments).storeProgramArguments();
    }
}
