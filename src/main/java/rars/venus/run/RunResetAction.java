package rars.venus.run;

import rars.Globals;
import rars.exceptions.AssemblyException;
import rars.riscv.hardware.ControlAndStatusRegisterFile;
import rars.riscv.hardware.InterruptController;
import rars.util.SystemIO;
import rars.venus.ExecutePane;
import rars.venus.FileStatus;
import rars.venus.GuiAction;
import rars.venus.VenusUI;

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
 * Action for the Run -> Reset menu item
 */
public final class RunResetAction extends GuiAction {
    private final VenusUI mainUI;

    public RunResetAction(
        final String name, final Icon icon, final String descrip,
        final Integer mnemonic, final KeyStroke accel, final VenusUI gui
    ) {
        super(name, icon, descrip, mnemonic, accel);
        mainUI = gui;
    }

    /**
     * {@inheritDoc}
     * <p>
     * reset GUI components and MIPS resources
     */
    @Override
    public void actionPerformed(final ActionEvent e) {
        RunGoAction.resetMaxSteps();
        final String name = this.getValue(Action.NAME).toString();
        final ExecutePane executePane = mainUI.mainPane.executeTab;
        // The difficult part here is resetting the data segment. Two approaches are:
        // 1. After each assembly, get a deep copy of the Globals.memory array
        // containing data segment. Then replace it upon reset.
        // 2. Simply re-assemble the program upon reset, and the assembler will
        // build a new data segment. Reset can only be done after a successful
        // assembly, so there is "no" chance of assembler error.
        // I am choosing the second approach although it will slow down the reset
        // operation. The first approach requires additional Memory class methods.
        try {
            Globals.program.assemble(
                RunAssembleAction.getProgramsToAssemble(),
                RunAssembleAction.getExtendedAssemblerEnabled(),
                RunAssembleAction.getWarningsAreErrors()
            );
        } catch (final AssemblyException pe) {
            // Should not be possible
            mainUI.messagesPane.postMessage(
                // pe.errors().generateErrorReport());
                "Unable to reset.  Please close file then re-open and re-assemble.\n");
            return;
        }

        Globals.REGISTER_FILE.resetRegisters();
        Globals.FP_REGISTER_FILE.resetRegisters();
        ControlAndStatusRegisterFile.resetRegisters();
        InterruptController.reset();

        executePane.registerValues.clearHighlighting();
        executePane.registerValues.updateRegisters();
        executePane.fpRegValues.clearHighlighting();
        executePane.fpRegValues.updateRegisters();
        executePane.csrValues.clearHighlighting();
        executePane.csrValues.updateRegisters();
        executePane.dataSegment.highlightCellForAddress(Globals.MEMORY_INSTANCE.getMemoryConfiguration().dataBaseAddress);
        executePane.dataSegment.clearHighlighting();
        executePane.textSegment.resetModifiedSourceCode();
        executePane.textSegment.setCodeHighlighting(true);
        executePane.textSegment.highlightStepAtPC();
        mainUI.registersPane.setSelectedComponent(executePane.registerValues);
        FileStatus.set(FileStatus.State.RUNNABLE);
        mainUI.isMemoryReset = true;
        mainUI.isExecutionStarted = false;

        // Aug. 24, 2005 Ken Vollmar
        SystemIO.resetFiles(); // Ensure that I/O "file descriptors" are initialized for a new program run

        mainUI.messagesPane.postRunMessage(
            "\n" + name + ": reset completed.\n\n");
    }
}
