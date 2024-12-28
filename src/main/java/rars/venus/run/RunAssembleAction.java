package rars.venus.run;

import rars.ErrorList;
import rars.ErrorMessage;
import rars.Globals;
import rars.RISCVprogram;
import org.jetbrains.annotations.NotNull;
import rars.exceptions.AssemblyException;
import rars.riscv.hardware.*;
import rars.settings.BoolSetting;
import rars.util.FilenameFinder;
import rars.util.SystemIO;
import rars.venus.*;
import rars.venus.registers.RegistersPane;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static rars.settings.Settings.BOOL_SETTINGS;
import static rars.settings.Settings.OTHER_SETTINGS;

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
 * Action class for the Run -> Assemble menu item (and toolbar icon)
 */
public class RunAssembleAction extends GuiAction {

    // Threshold for adding filename to printed message of files being assembled.
    private static final int LINE_LENGTH_LIMIT = 60;
    private static List<RISCVprogram> programsToAssemble;
    private static boolean extendedAssemblerEnabled;
    private static boolean warningsAreErrors;
    private final VenusUI mainUI;

    /**
     * <p>Constructor for RunAssembleAction.</p>
     *
     * @param name     a {@link java.lang.String} object
     * @param icon     a {@link javax.swing.Icon} object
     * @param descrip  a {@link java.lang.String} object
     * @param mnemonic a {@link java.lang.Integer} object
     * @param accel    a {@link javax.swing.KeyStroke} object
     * @param gui      a {@link VenusUI} object
     */
    public RunAssembleAction(final String name, final Icon icon, final String descrip,
                             final Integer mnemonic, final KeyStroke accel, final VenusUI gui) {
        super(name, icon, descrip, mnemonic, accel);
        this.mainUI = gui;
    }

    // These are both used by RunResetAction to re-assemble under identical
    // conditions.

    /**
     * <p>Getter for the field <code>programsToAssemble</code>.</p>
     *
     * @return a {@link java.util.ArrayList} object
     */
    public static List<RISCVprogram> getProgramsToAssemble() {
        return RunAssembleAction.programsToAssemble;
    }

    static boolean getExtendedAssemblerEnabled() {
        return RunAssembleAction.extendedAssemblerEnabled;
    }

    static boolean getWarningsAreErrors() {
        return RunAssembleAction.warningsAreErrors;
    }

    // Handy little utility for building comma-separated list of filenames
    // while not letting line length get out of hand.
    private static @NotNull String buildFileNameList(final @NotNull String preamble,
                                                     final @NotNull List<RISCVprogram> programList) {
        final StringBuilder result = new StringBuilder(preamble);
        int lineLength = result.length();
        for (int i = 0; i < programList.size(); i++) {
            final String filename = programList.get(i).getFilename();
            result.append(filename).append((i < programList.size() - 1) ? ", " : "");
            lineLength += filename.length();
            if (lineLength > RunAssembleAction.LINE_LENGTH_LIMIT) {
                result.append("\n");
                lineLength = 0;
            }
        }
        return result + ((lineLength == 0) ? "" : "\n") + "\n";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(final ActionEvent e) {
        final String name = this.getValue(Action.NAME).toString();
        final MessagesPane messagesPane = this.mainUI.messagesPane;
        final ExecutePane executePane = this.mainUI.mainPane.executeTab;
        final RegistersPane registersPane = this.mainUI.registersPane;
        RunAssembleAction.extendedAssemblerEnabled =
            BOOL_SETTINGS.getSetting(BoolSetting.EXTENDED_ASSEMBLER_ENABLED);
        RunAssembleAction.warningsAreErrors =
            BOOL_SETTINGS.getSetting(BoolSetting.WARNINGS_ARE_ERRORS);
        if (FileStatus.getFile() != null) {
            if (FileStatus.get() == FileStatus.State.EDITED) {
                this.mainUI.editor.save();
            }
            try {
                Globals.program = new RISCVprogram();
                final List<String> filesToAssemble;
                if (BOOL_SETTINGS.getSetting(BoolSetting.ASSEMBLE_ALL)) {// setting calls 
                    // for multiple
                    // file assembly
                    filesToAssemble = FilenameFinder.getFilenameList(
                        new File(FileStatus.getName()).getParent(), Globals.fileExtensions);
                } else {
                    filesToAssemble = List.of(FileStatus.getName());
                }
                if (BOOL_SETTINGS.getSetting(BoolSetting.ASSEMBLE_OPEN)) {
                    this.mainUI.editor.saveAll();
                    final String[] paths = this.mainUI.editor.getOpenFilePaths();
                    for (final String path : paths) {
                        if (!filesToAssemble.contains(path)) {
                            filesToAssemble.add(path);
                        }
                    }
                }
                String exceptionHandler = null;
                if (BOOL_SETTINGS.getSetting(BoolSetting.EXCEPTION_HANDLER_ENABLED)) {
                    if (!OTHER_SETTINGS.getExceptionHandler().isEmpty()) {
                        exceptionHandler = OTHER_SETTINGS.getExceptionHandler();
                    }
                }
                RunAssembleAction.programsToAssemble = Globals.program.prepareFilesForAssembly(filesToAssemble,
                    FileStatus.getFile().getPath(), exceptionHandler);
                messagesPane.postMessage(RunAssembleAction.buildFileNameList(name + ": assembling ",
                    RunAssembleAction.programsToAssemble));
                // added logic to receive any warnings and output them.... DPS 11/28/06
                final ErrorList warnings = Globals.program.assemble(RunAssembleAction.programsToAssemble,
                    RunAssembleAction.extendedAssemblerEnabled,
                    RunAssembleAction.warningsAreErrors);
                if (warnings.warningsOccurred()) {
                    messagesPane.postMessage(warnings.generateWarningReport());
                }
                messagesPane.postMessage(
                    name + ": operation completed successfully.\n\n");
                FileStatus.setAssembled(true);
                FileStatus.set(FileStatus.State.RUNNABLE);

                RegisterFile.resetRegisters();
                FloatingPointRegisterFile.resetRegisters();
                ControlAndStatusRegisterFile.resetRegisters();
                InterruptController.reset();

                executePane.textSegment.setupTable();
                executePane.dataSegment.setupTable();
                executePane.dataSegment.highlightCellForAddress(Memory.dataBaseAddress);
                executePane.dataSegment.clearHighlighting();
                executePane.labelValues.setupTable();
                executePane.textSegment.setCodeHighlighting(true);
                executePane.textSegment.highlightStepAtPC();
                registersPane.getRegistersWindow().clearWindow();
                registersPane.getFloatingPointWindow().clearWindow();
                registersPane.getControlAndStatusWindow().clearWindow();
                this.mainUI.isMemoryReset = true;
                this.mainUI.isExecutionStarted = false;
                this.mainUI.mainPane.setSelectedComponent(executePane);

                // Aug. 24, 2005 Ken Vollmar
                SystemIO.resetFiles(); // Ensure that I/O "file descriptors" are initialized for a new program run

            } catch (final AssemblyException pe) {
                final String errorReport = pe.errors().generateErrorAndWarningReport();
                messagesPane.postMessage(errorReport);
                messagesPane.postMessage(
                    name + ": operation completed with errors.\n\n");
                // Select editor line containing first error, and corresponding error message.
                final var errorMessages = pe.errors().getErrorMessages();
                for (final ErrorMessage em : errorMessages) {
                    // No line or position may mean File Not Found (e.g. exception file). Don't try
                    // to open. DPS 3-Oct-2010
                    if (em.getLine() == 0 && em.getPosition() == 0) {
                        continue;
                    }
                    if (!em.isWarning() || RunAssembleAction.warningsAreErrors) {
                        Globals.gui.messagesPane.selectErrorMessage(em.getFilename(), em.getLine(),
                            em.getPosition());
                        // Bug workaround: Line selection does not work correctly for the JEditTextArea
                        // editor
                        // when the file is opened then automatically assembled (assemble-on-open
                        // setting).
                        // Automatic assemble happens in EditTabbedPane's openFile() method, by invoking
                        // this method (actionPerformed) explicitly with null argument. Thus e!=null
                        // test.
                        // DPS 9-Aug-2010
                        if (e != null) {
                            MessagesPane.selectEditorTextLine(em.getFilename(), em.getLine(),
                                em.getPosition());
                        }
                        break;
                    }
                }
                FileStatus.setAssembled(false);
                FileStatus.set(FileStatus.State.NOT_EDITED);
            }
        }
    }
}
