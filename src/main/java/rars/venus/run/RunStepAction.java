package rars.venus.run;

import org.jetbrains.annotations.NotNull;
import rars.Globals;
import rars.exceptions.SimulationException;
import rars.notices.SimulatorNotice;
import rars.settings.BoolSetting;
import rars.simulator.ProgramArgumentList;
import rars.simulator.Simulator;
import rars.venus.ExecutePane;
import rars.venus.FileStatus;
import rars.venus.GuiAction;
import rars.venus.VenusUI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.function.Consumer;

import static rars.Globals.BOOL_SETTINGS;

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
public final class RunStepAction extends GuiAction {

    private String name;
    private ExecutePane executePane;

    public RunStepAction(
        final String name, final Icon icon, final String descrip,
        final Integer mnemonic, final KeyStroke accel, final VenusUI gui
    ) {
        super(name, icon, descrip, mnemonic, accel, gui);
    }

    /**
     * {@inheritDoc}
     * <p>
     * perform next simulated instruction step.
     */
    @Override
    public void actionPerformed(final ActionEvent e) {
        this.name = this.getValue(Action.NAME).toString();
        this.executePane = this.mainUI.mainPane.executePane;
        if (FileStatus.isAssembled()) {
            if (!this.mainUI.isExecutionStarted) { // DPS 17-July-2008
                this.processProgramArgumentsIfAny();
            }
            this.mainUI.isExecutionStarted = true;
            this.mainUI.messagesPane.selectRunMessageTab();
            this.executePane.textSegment.setCodeHighlighting(true);

            // Setup callback for after step finishes
            final var stopListener = new Consumer<@NotNull SimulatorNotice>() {
                @Override
                public void accept(final @NotNull SimulatorNotice item) {
                    if (item.action() != SimulatorNotice.Action.STOP) {
                        return;
                    }
                    EventQueue.invokeLater(() -> RunStepAction.this.stepped(
                        item.done(), item.reason(),
                        item.exception()
                    ));

                    Globals.SIMULATOR.simulatorNoticeHook.unsubscribe(this);
                }
            };
            Globals.SIMULATOR.simulatorNoticeHook.subscribe(stopListener);

            Globals.SIMULATOR.startSimulation(Globals.REGISTER_FILE.getProgramCounter(), 1, new int[0], this.mainUI);
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
     * @param done
     *     a boolean
     * @param reason
     *     a {@link Simulator.Reason} object
     * @param pe
     *     a {@link SimulationException} object
     */
    public void stepped(final boolean done, final Simulator.Reason reason, final SimulationException pe) {
        this.executePane.registerValues.updateRegisters();
        this.executePane.fpRegValues.updateRegisters();
        this.executePane.csrValues.updateRegisters();
        this.executePane.dataSegment.updateValues();
        if (!done) {
            this.executePane.textSegment.highlightStepAtPC();
            FileStatus.set(FileStatus.State.RUNNABLE);
        }
        if (done) {
            RunGoAction.resetMaxSteps();
            this.executePane.textSegment.unhighlightAllSteps();
            FileStatus.set(FileStatus.State.TERMINATED);
        }
        if (done && pe == null) {
            this.mainUI.messagesPane.postMessage(
                "\n" + this.name + ": execution " +
                    (
                        (reason == Simulator.Reason.CLIFF_TERMINATION) ? "terminated due to null instruction."
                            : "completed successfully."
                    )
                    + "\n\n");
            this.mainUI.messagesPane.postRunMessage(
                "\n-- program is finished running" +
                    (
                        (reason == Simulator.Reason.CLIFF_TERMINATION) ? "(dropped off bottom)"
                            : " (" + Globals.exitCode + ")"
                    )
                    + " --\n\n");
            this.mainUI.messagesPane.selectRunMessageTab();
        }
        if (pe != null) {
            RunGoAction.resetMaxSteps();
            this.mainUI.messagesPane.postMessage(
                pe.errorMessage.generateReport());
            this.mainUI.messagesPane.postMessage(
                "\n" + this.name + ": execution terminated with errors.\n\n");
            this.mainUI.registersPane.setSelectedComponent(this.executePane.csrValues);
            FileStatus.set(FileStatus.State.TERMINATED); // should be redundant.
            this.executePane.textSegment.setCodeHighlighting(true);
            this.executePane.textSegment.unhighlightAllSteps();
            this.executePane.textSegment.highlightStepAtAddress(Globals.REGISTER_FILE.getProgramCounter() - 4);
        }
        this.mainUI.isMemoryReset = false;
    }

    // Method to store any program arguments into MIPS memory and registers before
    // execution begins. Arguments go into the gap between $sp and kernel memory.
    // Argument pointers and count go into runtime stack and $sp is adjusted
    //////////////////////////////////////////////////////////////////////////////////// accordingly.
    // $a0 gets argument count (argc), $a1 gets stack address of first arg pointer

    /// ///////////////////////////////////////////////////////////////////////////////// (argv).
    private void processProgramArgumentsIfAny() {
        final String programArguments = this.executePane.textSegment.getProgramArguments();
        if (programArguments == null || programArguments.isEmpty() ||
            !BOOL_SETTINGS.getSetting(BoolSetting.PROGRAM_ARGUMENTS)) {
            return;
        }
        new ProgramArgumentList(programArguments).storeProgramArguments();
    }
}
