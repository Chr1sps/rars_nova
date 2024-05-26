package io.github.chr1sps.rars.venus.run;

import io.github.chr1sps.rars.Globals;
import io.github.chr1sps.rars.Settings;
import io.github.chr1sps.rars.riscv.hardware.ControlAndStatusRegisterFile;
import io.github.chr1sps.rars.riscv.hardware.FloatingPointRegisterFile;
import io.github.chr1sps.rars.riscv.hardware.Memory;
import io.github.chr1sps.rars.riscv.hardware.RegisterFile;
import io.github.chr1sps.rars.venus.ExecutePane;
import io.github.chr1sps.rars.venus.FileStatus;
import io.github.chr1sps.rars.venus.GuiAction;
import io.github.chr1sps.rars.venus.VenusUI;

import javax.swing.*;
import java.awt.event.ActionEvent;

/*
Copyright (c) 2003-2009,  Pete Sanderson and Kenneth Vollmar

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
 * Action for the Run -> Backstep menu item
 */
public class RunBackstepAction extends GuiAction {

    private String name;
    private ExecutePane executePane;
    private final VenusUI mainUI;

    /**
     * <p>Constructor for RunBackstepAction.</p>
     *
     * @param name     a {@link java.lang.String} object
     * @param icon     a {@link javax.swing.Icon} object
     * @param descrip  a {@link java.lang.String} object
     * @param mnemonic a {@link java.lang.Integer} object
     * @param accel    a {@link javax.swing.KeyStroke} object
     * @param gui      a {@link io.github.chr1sps.rars.venus.VenusUI} object
     */
    public RunBackstepAction(final String name, final Icon icon, final String descrip,
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
        if (!FileStatus.isAssembled()) {
            // note: this should never occur since backstepping is only enabled after
            // successful assembly.
            JOptionPane.showMessageDialog(this.mainUI, "The program must be assembled before it can be run.");
            return;
        }
        this.mainUI.setStarted(true);
        this.mainUI.getMessagesPane().selectRunMessageTab();
        this.executePane.getTextSegmentWindow().setCodeHighlighting(true);

        if (Settings.getBackSteppingEnabled()) {
            Memory.getInstance().subscribe(this.executePane.getDataSegmentWindow());
            RegisterFile.addRegistersObserver(this.executePane.getRegistersWindow());
            ControlAndStatusRegisterFile.addRegistersObserver(this.executePane.getControlAndStatusWindow());
            FloatingPointRegisterFile.addRegistersSubscriber(this.executePane.getFloatingPointWindow());
            Globals.program.getBackStepper().backStep();
            Memory.getInstance().deleteSubscriber(this.executePane.getDataSegmentWindow());
            RegisterFile.deleteRegistersObserver(this.executePane.getRegistersWindow());
            this.executePane.getRegistersWindow().updateRegisters();
            this.executePane.getFloatingPointWindow().updateRegisters();
            this.executePane.getControlAndStatusWindow().updateRegisters();
            this.executePane.getDataSegmentWindow().updateValues();
            this.executePane.getTextSegmentWindow().highlightStepAtPC(); // Argument aded 25 June 2007
            FileStatus.set(FileStatus.RUNNABLE);
            // if we've backed all the way, disable the button
            // if (Globals.program.getBackStepper().empty()) {
            // ((AbstractAction)((AbstractButton)e.getSource()).getAction()).setEnabled(false);
            // }
            /*
             * if (pe !=null) {
             * RunGoAction.resetMaxSteps();
             * mainUI.getMessagesPane().postMessage(
             * pe.errors().generateErrorReport());
             * mainUI.getMessagesPane().postMessage(
             * "\n"+name+": execution terminated with errors.\n\n");
             * mainUI.getRegistersPane().setSelectedComponent(executePane.
             * getControlAndStatusWindow());
             * FileStatus.set(FileStatus.TERMINATED); // should be redundant.
             * executePane.getTextSegmentWindow().setCodeHighlighting(true);
             * executePane.getTextSegmentWindow().unhighlightAllSteps();
             * executePane.getTextSegmentWindow().highlightStepAtAddress(RegisterFile.
             * getProgramCounter()-4);
             * }
             */
            this.mainUI.setReset(false);
        }
    }
}
