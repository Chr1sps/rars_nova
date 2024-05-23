package io.github.chr1sps.rars.venus.run;

import io.github.chr1sps.rars.Globals;
import io.github.chr1sps.rars.Settings;
import io.github.chr1sps.rars.exceptions.SimulationException;
import io.github.chr1sps.rars.notices.SimulatorNotice;
import io.github.chr1sps.rars.riscv.hardware.RegisterFile;
import io.github.chr1sps.rars.simulator.ProgramArgumentList;
import io.github.chr1sps.rars.simulator.Simulator;
import io.github.chr1sps.rars.util.SimpleSubscriber;
import io.github.chr1sps.rars.venus.ExecutePane;
import io.github.chr1sps.rars.venus.FileStatus;
import io.github.chr1sps.rars.venus.GuiAction;
import io.github.chr1sps.rars.venus.VenusUI;

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

    private String name;
    private ExecutePane executePane;
    private final VenusUI mainUI;

    /**
     * <p>Constructor for RunStepAction.</p>
     *
     * @param name     a {@link java.lang.String} object
     * @param icon     a {@link javax.swing.Icon} object
     * @param descrip  a {@link java.lang.String} object
     * @param mnemonic a {@link java.lang.Integer} object
     * @param accel    a {@link javax.swing.KeyStroke} object
     * @param gui      a {@link io.github.chr1sps.rars.venus.VenusUI} object
     */
    public RunStepAction(String name, Icon icon, String descrip,
                         Integer mnemonic, KeyStroke accel, VenusUI gui) {
        super(name, icon, descrip, mnemonic, accel);
        mainUI = gui;
    }

    /**
     * {@inheritDoc}
     * <p>
     * perform next simulated instruction step.
     */
    public void actionPerformed(ActionEvent e) {
        name = this.getValue(Action.NAME).toString();
        executePane = mainUI.getMainPane().getExecutePane();
        if (FileStatus.isAssembled()) {
            if (!mainUI.getStarted()) { // DPS 17-July-2008
                processProgramArgumentsIfAny();
            }
            mainUI.setStarted(true);
            mainUI.getMessagesPane().selectRunMessageTab();
            executePane.getTextSegmentWindow().setCodeHighlighting(true);

            final var stopListener = new SimpleSubscriber<SimulatorNotice>() {
                private Flow.Subscription subscription;

                @Override
                public void onSubscribe(Flow.Subscription subscription) {
                    this.subscription = subscription;
                    this.subscription.request(1);
                }

                @Override
                public void onNext(SimulatorNotice item) {
                    if (item.getAction() != SimulatorNotice.SIMULATOR_STOP) {
                        this.subscription.request(1);
                        return;
                    }
                    EventQueue.invokeLater(() -> stepped(item.getDone(), item.getReason(), item.getException()));
                    this.subscription.cancel();
                }
            };
            // Setup callback for after step finishes
//                public void update(Observable o, Object simulator) {
//                    SimulatorNotice notice = ((SimulatorNotice) simulator);
//                    if (notice.getAction() != SimulatorNotice.SIMULATOR_STOP)
//                        return;
//                    EventQueue.invokeLater(() -> stepped(notice.getDone(), notice.getReason(), notice.getException()));
//                    o.deleteObserver(this);
//                }
            Simulator.getInstance().subscribe(stopListener);

            Globals.program.startSimulation(1, null);
        } else {
            // note: this should never occur since "Step" is only enabled after successful
            // assembly.
            JOptionPane.showMessageDialog(mainUI, "The program must be assembled before it can be run.");
        }
    }

    // When step is completed, control returns here (from execution thread,
    // indirectly)
    // to update the GUI.

    /**
     * <p>stepped.</p>
     *
     * @param done   a boolean
     * @param reason a {@link io.github.chr1sps.rars.simulator.Simulator.Reason} object
     * @param pe     a {@link SimulationException} object
     */
    public void stepped(boolean done, Simulator.Reason reason, SimulationException pe) {
        executePane.getRegistersWindow().updateRegisters();
        executePane.getFloatingPointWindow().updateRegisters();
        executePane.getControlAndStatusWindow().updateRegisters();
        executePane.getDataSegmentWindow().updateValues();
        if (!done) {
            executePane.getTextSegmentWindow().highlightStepAtPC();
            FileStatus.set(FileStatus.RUNNABLE);
        }
        if (done) {
            RunGoAction.resetMaxSteps();
            executePane.getTextSegmentWindow().unhighlightAllSteps();
            FileStatus.set(FileStatus.TERMINATED);
        }
        if (done && pe == null) {
            mainUI.getMessagesPane().postMessage(
                    "\n" + name + ": execution " +
                            ((reason == Simulator.Reason.CLIFF_TERMINATION) ? "terminated due to null instruction."
                                    : "completed successfully.")
                            + "\n\n");
            mainUI.getMessagesPane().postRunMessage(
                    "\n-- program is finished running" +
                            ((reason == Simulator.Reason.CLIFF_TERMINATION) ? "(dropped off bottom)"
                                    : " (" + Globals.exitCode + ")")
                            + " --\n\n");
            mainUI.getMessagesPane().selectRunMessageTab();
        }
        if (pe != null) {
            RunGoAction.resetMaxSteps();
            mainUI.getMessagesPane().postMessage(
                    pe.errorMessage.generateReport());
            mainUI.getMessagesPane().postMessage(
                    "\n" + name + ": execution terminated with errors.\n\n");
            mainUI.getRegistersPane().setSelectedComponent(executePane.getControlAndStatusWindow());
            FileStatus.set(FileStatus.TERMINATED); // should be redundant.
            executePane.getTextSegmentWindow().setCodeHighlighting(true);
            executePane.getTextSegmentWindow().unhighlightAllSteps();
            executePane.getTextSegmentWindow().highlightStepAtAddress(RegisterFile.getProgramCounter() - 4);
        }
        mainUI.setReset(false);
    }

    ////////////////////////////////////////////////////////////////////////////////////
    // Method to store any program arguments into MIPS memory and registers before
    // execution begins. Arguments go into the gap between $sp and kernel memory.
    // Argument pointers and count go into runtime stack and $sp is adjusted
    //////////////////////////////////////////////////////////////////////////////////// accordingly.
    // $a0 gets argument count (argc), $a1 gets stack address of first arg pointer
    //////////////////////////////////////////////////////////////////////////////////// (argv).
    private void processProgramArgumentsIfAny() {
        String programArguments = executePane.getTextSegmentWindow().getProgramArguments();
        if (programArguments == null || programArguments.isEmpty() ||
                !Globals.getSettings().getBooleanSetting(Settings.Bool.PROGRAM_ARGUMENTS)) {
            return;
        }
        new ProgramArgumentList(programArguments).storeProgramArguments();
    }
}
