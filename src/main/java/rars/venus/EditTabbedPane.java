package rars.venus;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rars.Globals;
import rars.RISCVProgram;
import rars.exceptions.AssemblyException;
import rars.settings.BoolSetting;
import rars.util.FilenameFinder;
import rars.util.Pair;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import static com.formdev.flatlaf.FlatClientProperties.*;
import static rars.Globals.BOOL_SETTINGS;

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
 * Tabbed pane for the editor. Each of its tabs represents an open file.
 *
 * @author Sanderson
 */
public final class EditTabbedPane extends JPanel {
    private final @NotNull MainPane mainPane;
    private final @NotNull VenusUI mainUI;
    private final @NotNull Editor editor;
    private final @NotNull FileOpener fileOpener;
    private final @NotNull JTabbedPane tabbedPane;

    /**
     * Constructor for the EditTabbedPane class.
     *
     * @param mainUI
     *     a {@link VenusUI} object
     * @param editor
     *     a {@link Editor} object
     * @param mainPane
     *     a {@link MainPane} object
     */
    public EditTabbedPane(
        final @NotNull VenusUI mainUI,
        final @NotNull Editor editor,
        final @NotNull MainPane mainPane
    ) {
        super();
        this.tabbedPane = new JTabbedPane();
        this.mainUI = mainUI;
        this.editor = editor;
        this.fileOpener = new FileOpener(editor);
        this.mainPane = mainPane;
        this.editor.setEditTabbedPane(this);
        this.tabbedPane.addChangeListener(
            e -> {
                final EditPane editPane = (EditPane) tabbedPane.getSelectedComponent();
                if (editPane != null) {
                    // New IF statement to permit free traversal of edit panes w/o invalidating
                    // assembly if assemble-all is selected. DPS 9-Aug-2011
                    if (BOOL_SETTINGS.getSetting(BoolSetting.ASSEMBLE_ALL)) {
                        EditTabbedPane.this.updateTitles(editPane);
                    } else {
                        EditTabbedPane.this.updateTitlesAndMenuState(editPane);
                        this.mainPane.executePane.clearPane();
                    }
                    editPane.tellEditingComponentToRequestFocusInWindow();
                }
            });
        this.tabbedPane.putClientProperty(TABBED_PANE_TAB_CLOSABLE, true);
        this.tabbedPane.putClientProperty(TABBED_PANE_TAB_CLOSE_TOOLTIPTEXT, "Close current file");
        this.tabbedPane.putClientProperty(
            TABBED_PANE_TAB_CLOSE_CALLBACK, (BiConsumer<JTabbedPane, Integer>) (
                pane,
                index
            ) -> this.closeFile(index)
        );
        this.setLayout(new BorderLayout());
        this.add(this.tabbedPane, BorderLayout.CENTER);
    }

    /**
     * The current EditPane representing a file. Returns null if
     * no files open.
     *
     * @return the current editor pane
     */
    public EditPane getCurrentEditTab() {
        return (EditPane) tabbedPane.getSelectedComponent();
    }

    /**
     * Select the specified EditPane to be the current tab.
     *
     * @param editPane
     *     The EditPane tab to become current.
     */
    public void setCurrentEditTab(final EditPane editPane) {
        tabbedPane.setSelectedComponent(editPane);
    }

    /**
     * Carries out all necessary operations to implement
     * the New operation from the File menu.
     */
    public void newFile() {
        final EditPane editPane = new EditPane(this.mainUI);
        editPane.setSourceCode("", true);
        editPane.setFileStatus(FileStatus.State.NEW_NOT_EDITED);
        final var name = this.editor.getNextDefaultFilename();
        editPane.setFile(new File(name));
        tabbedPane.addTab(name, editPane);

        FileStatus.reset();
        FileStatus.setSystemState(FileStatus.State.NEW_NOT_EDITED);

        Globals.REGISTER_FILE.resetRegisters();
        this.mainUI.isMemoryReset = true;
        this.mainPane.executePane.clearPane();
        this.mainPane.setSelectedComponent(this);
        editPane.displayCaretPosition(Pair.of(1, 1));
        tabbedPane.setSelectedComponent(editPane);
        this.updateTitlesAndMenuState(editPane);
        editPane.tellEditingComponentToRequestFocusInWindow();
    }

    /**
     * Carries out all necessary operations to implement
     * the Open operation from the File menu. This
     * begins with an Open File dialog.
     *
     * @return true if file was opened, false otherwise.
     */
    public boolean openFile() {
        return this.fileOpener.openFile();
    }

    /**
     * Carries out all necessary operations to open the
     * specified file in the editor.
     *
     * @param file
     *     a {@link java.io.File} object
     * @return true if file was opened, false otherwise.
     */
    public boolean openFile(final File file) {
        return this.fileOpener.openFile(file);
    }

    /**
     * Carries out all necessary operations to implement
     * the Close operation from the File menu. May return
     * false, for instance when file has unsaved changes
     * and user selects Cancel from the warning dialog.
     *
     * @return true if file was closed, false otherwise.
     */
    public boolean closeCurrentFile() {
        final EditPane editPane = this.getCurrentEditTab();
        if (editPane != null) {
            if (this.editsSavedOrAbandoned()) {
                this.remove(editPane);
                this.mainPane.executePane.clearPane();
                this.mainPane.setSelectedComponent(this);
            } else {
                return false;
            }
        }
        return true;
    }

    private void closeFile(final int index) {
        final EditPane editPane = (EditPane) tabbedPane.getComponentAt(index);
        if (this.editsSavedOrAbandoned()) {
            this.remove(editPane);
            this.mainPane.executePane.clearPane();
            this.mainPane.setSelectedComponent(this);
        }
    }

    /**
     * Carries out all necessary operations to implement
     * the Close All operation from the File menu.
     *
     * @return true if files closed, false otherwise.
     */
    public boolean closeAllFiles() {
        final int tabCount = tabbedPane.getTabCount();
        if (tabCount > 0) {
            this.mainPane.executePane.clearPane();
            this.mainPane.setSelectedComponent(this);
            final EditPane[] tabs = new EditPane[tabCount];
            boolean unsavedChanges = false;
            for (int i = 0; i < tabCount; i++) {
                tabs[i] = (EditPane) tabbedPane.getComponentAt(i);
                if (tabs[i].hasUnsavedEdits()) {
                    unsavedChanges = true;
                }
            }
            if (unsavedChanges) {
                switch (this.confirm("one or more files")) {
                    case JOptionPane.YES_OPTION:
                        boolean removedAll = true;
                        for (int i = 0; i < tabCount; i++) {
                            if (tabs[i].hasUnsavedEdits()) {
                                tabbedPane.setSelectedComponent(tabs[i]);
                                final boolean saved = this.saveCurrentFile();
                                if (saved) {
                                    this.remove(tabs[i]);
                                } else {
                                    removedAll = false;
                                }
                            } else {
                                this.remove(tabs[i]);
                            }
                        }
                        return removedAll;
                    case JOptionPane.NO_OPTION:
                        for (int i = 0; i < tabCount; i++) {
                            this.remove(tabs[i]);
                        }
                        return true;
                    case JOptionPane.CANCEL_OPTION:
                        return false;
                    default: // should never occur
                        return false;
                }
            } else {
                for (int i = 0; i < tabCount; i++) {
                    this.remove(tabs[i]);
                }
            }
        }
        final boolean result = true;
        return result;
    }

    /**
     * Saves file under existing name. If no name, will invoke Save As.
     *
     * @return true if the file was actually saved.
     */
    public boolean saveCurrentFile() {
        final EditPane editPane = this.getCurrentEditTab();
        if (this.saveFile(editPane)) {
            FileStatus.setSystemState(FileStatus.State.NOT_EDITED);
            editPane.setFileStatus(FileStatus.State.NOT_EDITED);
            this.updateTitlesAndMenuState(editPane);
            return true;
        }
        return false;
    }

    // Save file associatd with specified edit pane.
    // Returns true if save operation worked, else false.
    private boolean saveFile(final EditPane editPane) {
        if (editPane != null) {
            if (editPane.isNew()) {
                final File theFile = this.saveAsFile(editPane);
                if (theFile != null) {
                    editPane.setFile(theFile);
                }
                return (theFile != null);
            }
            final var theFile = editPane.getFile();
            try {
                final BufferedWriter outFileStream = new BufferedWriter(new FileWriter(theFile));
                outFileStream.write(editPane.getSource(), 0, editPane.getSource().length());
                outFileStream.close();
            } catch (final java.io.IOException c) {
                JOptionPane.showMessageDialog(
                    null, "Save operation could not be completed due to an error:\n" + c,
                    "Save Operation Failed", JOptionPane.ERROR_MESSAGE
                );
                return false;
            }
            return true;
        }
        return false;
    }

    /**
     * Pops up a dialog box to do "Save As" operation. If necessary
     * an additional overwrite dialog is performed.
     *
     * @return true if the file was actually saved.
     */
    public boolean saveAsCurrentFile() {
        final EditPane editPane = this.getCurrentEditTab();
        final File theFile = this.saveAsFile(editPane);
        if (theFile != null) {
            FileStatus.systemFile = theFile;
            FileStatus.setSystemState(FileStatus.State.NOT_EDITED);
            this.editor.setCurrentSaveDirectory(theFile.getParent());
            editPane.setFile(theFile);
            editPane.setFileStatus(FileStatus.State.NOT_EDITED);
            this.updateTitlesAndMenuState(editPane);
            return true;
        }
        return false;
    }

    // perform Save As for selected edit pane. If the save is performed,
    // return its File object. Otherwise return null.
    private @Nullable File saveAsFile(final EditPane editPane) {
        File theFile = null;
        if (editPane != null) {
            boolean operationOK = false;
            while (!operationOK) {
                // Set Save As dialog directory in a logical way. If file in
                // edit pane had been previously saved, default to its directory.
                // If a new file (mipsN.asm), default to current save directory.
                // DPS 13-July-2011
                JFileChooser saveDialog;
                if (editPane.isNew()) {
                    saveDialog = new JFileChooser(this.editor.getCurrentSaveDirectory());
                } else {
                    final File f = editPane.getFile();
                    saveDialog = new JFileChooser(f.getParent());
                }
                final var paneFile = editPane.getFile();
                if (paneFile != null) {
                    saveDialog.setSelectedFile(paneFile);
                }
                // end of 13-July-2011 code.
                saveDialog.setDialogTitle("Save As");

                final int decision = saveDialog.showSaveDialog(this.mainUI);
                if (decision != JFileChooser.APPROVE_OPTION) {
                    return null;
                }
                theFile = saveDialog.getSelectedFile();
                operationOK = true;
                if (theFile.exists()) {
                    final int overwrite = JOptionPane.showConfirmDialog(
                        this.mainUI,
                        "File " + theFile.getName() + " already exists.  Do you wish to overwrite it?",
                        "Overwrite existing file?",
                        JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE
                    );
                    switch (overwrite) {
                        case JOptionPane.YES_OPTION:
                            break;
                        case JOptionPane.NO_OPTION:
                            operationOK = false;
                            break;
                        case JOptionPane.CANCEL_OPTION:
                            return null;
                        default: // should never occur
                            return null;
                    }
                }
            }
            // Either file with selected name does not exist or user wants to
            // overwrite it, so go for it!
            try {
                final BufferedWriter outFileStream = new BufferedWriter(new FileWriter(theFile));
                outFileStream.write(editPane.getSource(), 0, editPane.getSource().length());
                outFileStream.close();
            } catch (final java.io.IOException c) {
                JOptionPane.showMessageDialog(
                    null, "Save As operation could not be completed due to an error:\n" + c,
                    "Save As Operation Failed", JOptionPane.ERROR_MESSAGE
                );
                return null;
            }
        }
        return theFile;
    }

    /**
     * Saves all files currently open in the editor.
     *
     * @return true if operation succeeded otherwise false.
     */
    public boolean saveAllFiles() {
        boolean result = false;
        final int tabCount = tabbedPane.getTabCount();
        if (tabCount > 0) {

            result = true;
            final EditPane[] tabs = new EditPane[tabCount];
            final EditPane savedPane = this.getCurrentEditTab();
            for (int i = 0; i < tabCount; i++) {
                final var tab = (EditPane) tabbedPane.getComponentAt(i);
                if (tab.hasUnsavedEdits()) {
                    this.setCurrentEditTab(tab);
                    if (this.saveFile(tab)) {
                        tabs[i].setFileStatus(FileStatus.State.NOT_EDITED);
                        this.editor.setTitleFromFile(
                            tab.getFile(),
                            tab.getFileStatus()
                        );
                    } else {
                        result = false;
                    }
                }
                tabs[i] = tab;
            }
            this.setCurrentEditTab(savedPane);
            if (result) {
                final EditPane editPane = this.getCurrentEditTab();
                FileStatus.setSystemState(FileStatus.State.NOT_EDITED);
                editPane.setFileStatus(FileStatus.State.NOT_EDITED);
                this.updateTitlesAndMenuState(editPane);
            }
        }
        return result;
    }

    public @NotNull List<@NotNull File> getOpenFilePaths() {
        final var result = new ArrayList<File>();
        for (final var component : tabbedPane.getComponents()) {
            result.add(((EditPane) component).getFile());
        }
        return result;
    }

    /**
     * Remove the pane and update menu status
     *
     * @param editPane
     *     a {@link EditPane} object
     */
    public void remove(EditPane editPane) {
        tabbedPane.remove(editPane);
        editPane = this.getCurrentEditTab(); // is now next tab or null
        if (editPane == null) {
            FileStatus.setSystemState(FileStatus.State.NO_FILE);
            this.editor.setTitle("", "", FileStatus.State.NO_FILE);
            this.mainUI.setMenuState(FileStatus.State.NO_FILE);
        } else {
            FileStatus.setSystemState(editPane.getFileStatus());
            this.updateTitlesAndMenuState(editPane);
        }
        // When last file is closed, menu is unable to respond to mnemonics
        // and accelerators. Let's have it request focus so it may do so.
        if (tabbedPane.getTabCount() == 0) {
            this.mainUI.haveMenuRequestFocus();
        }
    }

    // Handy little utility to update the title on the current tab and the frame
    // title bar
    // and also to update the MARS menu state (controls which actions are enabled).
    private void updateTitlesAndMenuState(final @NotNull EditPane editPane) {
        this.editor.setTitleFromFile(editPane.getFile(), editPane.getFileStatus());
        editPane.updateStaticFileStatus(); // for legacy code that depends on the static FileStatus (pre 4.0)
        this.mainUI.setMenuState(editPane.getFileStatus());
    }

    // Handy little utility to update the title on the current tab and the frame
    // title bar
    // and also to update the MARS menu state (controls which actions are enabled).
    // DPS 9-Aug-2011
    private void updateTitles(final @NotNull EditPane editPane) {
        this.editor.setTitleFromFile(editPane.getFile(), editPane.getFileStatus());
        final boolean assembled = FileStatus.isAssembled();
        editPane.updateStaticFileStatus(); // for legacy code that depends on the static FileStatus (pre 4.0)
        FileStatus.setAssembled(assembled);
    }

    /**
     * If there is an EditPane for the given file, return it else return
     * null.
     *
     * @param file
     *     The given file
     * @return the EditPane for this file if it is open in the editor, or null if
     * not.
     */
    public @Nullable EditPane getEditPaneForFile(final @NotNull File file) {
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            final EditPane pane = (EditPane) tabbedPane.getComponentAt(i);
            if (pane.getFile().equals(file)) {
                return pane;
            }
        }
        return null;
    }

    /**
     * Check whether file has unsaved edits and, if so, check with user about saving
     * them.
     *
     * @return true if no unsaved edits or if user chooses to save them or not;
     * false
     * if there are unsaved edits and user cancels the operation.
     */
    public boolean editsSavedOrAbandoned() {
        final EditPane currentPane = this.getCurrentEditTab();
        if (currentPane != null && currentPane.hasUnsavedEdits()) {
            return switch (this.confirm(currentPane.getFile().getName())) {
                case JOptionPane.YES_OPTION -> this.saveCurrentFile();
                case JOptionPane.NO_OPTION -> true;
                case JOptionPane.CANCEL_OPTION -> false;
                default -> // should never occur
                    false;
            };
        } else {
            return true;
        }
    }

    private int confirm(final @NotNull String name) {
        return JOptionPane.showConfirmDialog(
            this.mainUI,
            "Changes to " + name + " will be lost unless you save.  Do you wish to save all changes now?",
            "Save program changes?",
            JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE
        );
    }

    public @NotNull JTabbedPane getTabbedPane() {
        return tabbedPane;
    }

    /**
     * Will select the specified line in an editor tab. If the file is open
     * but not current, its tab will be made current. If the file is not open,
     * it will be opened in a new tab and made current, however the line will
     * not be selected (apparent apparent problem with JEditTextArea).
     *
     * @param file
     *     A String containing the file path name.
     * @param line
     *     Line number for error message
     */
    public void selectEditorTextLine(
        final @NotNull File file,
        final int line
    ) {
        final EditPane editPane = this.getEditPaneForFile(file);
        EditPane currentPane = null;
        if (editPane != null) {
            if (editPane != this.getCurrentEditTab()) {
                this.setCurrentEditTab(editPane);
            }
            currentPane = editPane;
        } else { // file is not open. Try to open it.
            if (this.openFile(file)) {
                currentPane = this.getCurrentEditTab();
            }
        }
        // If editPane == null, it means the desired file was not open. Line selection
        // does not properly with the JEditTextArea editor in this situation (it works
        // fine for the original generic editor). So we just won't do it. DPS 9-Aug-2010
        if (editPane != null) {
            currentPane.selectLine(line);
        }
    }

    private final class FileOpener {
        private static final @NotNull Logger LOGGER = LogManager.getLogger(FileOpener.class);
        private final @NotNull JFileChooser fileChooser;
        private final @NotNull ArrayList<@NotNull FileFilter> fileFilterList;
        private final @NotNull PropertyChangeListener listenForUserAddedFileFilter;
        private final @NotNull Editor theEditor;
        private @Nullable File mostRecentlyOpenedFile;
        private int fileFilterCount;

        public FileOpener(final @NotNull Editor theEditor) {
            this.mostRecentlyOpenedFile = null;
            this.theEditor = theEditor;
            this.fileChooser = new JFileChooser();
            this.listenForUserAddedFileFilter = new ChoosableFileFilterChangeListener();
            this.fileChooser.addPropertyChangeListener(this.listenForUserAddedFileFilter);

            // Note: add sequence is significant - last one added becomes default.
            this.fileFilterList = new ArrayList<>();
            this.fileFilterList.add(this.fileChooser.getAcceptAllFileFilter());
            this.fileFilterList.add(FilenameFinder.RARS_FILE_FILTER);
            this.fileFilterCount = 0; // this will trigger fileChooser file filter load in next line
            this.setChoosableFileFilters();
        }

        /**
         * Launch a file chooser for name of file to open. Return true if file opened,
         * false otherwise
         */
        public boolean openFile() {
            // The fileChooser's list may be rebuilt from the master ArrayList if a new
            // filter
            // has been added by the user.
            this.setChoosableFileFilters();
            // get name of file to be opened and load contents into text editing area.
            this.fileChooser.setCurrentDirectory(new File(this.theEditor.getCurrentOpenDirectory()));
            // dark mode might have changed so we need to update the ui just in case
            this.fileChooser.updateUI();
            // Set default to previous file opened, if any. This is useful in conjunction
            // with option to assemble file automatically upon opening. File likely to have
            // been edited externally (e.g. by Mipster).
            if (BOOL_SETTINGS.getSetting(BoolSetting.ASSEMBLE_ON_OPEN)
                && this.mostRecentlyOpenedFile != null) {
                this.fileChooser.setSelectedFile(this.mostRecentlyOpenedFile);
            }

            if (this.fileChooser.showOpenDialog(EditTabbedPane.this.mainUI) == JFileChooser.APPROVE_OPTION) {
                final var startTime = Instant.now();
                final File theFile = this.fileChooser.getSelectedFile();
                this.theEditor.setCurrentOpenDirectory(theFile.getParent());
                // theEditor.setCurrentSaveDirectory(theFile.getParent());// 13-July-2011 DPS.
                if (!this.openFile(theFile)) {
                    return false;
                }

                // possibly send this file right through to the assembler by firing
                // Run->Assemble's
                // actionPerformed() method.
                if (theFile.canRead()) {
                    if (BOOL_SETTINGS.getSetting(BoolSetting.ASSEMBLE_ON_OPEN)) {
                        EditTabbedPane.this.mainUI.getRunAssembleAction().actionPerformed(null);
                    }
                }
                final var endTime = Instant.now();
                final var duration = Duration.between(startTime, endTime);
                LOGGER.info("Opened file in {}ms.", duration.toMillis());
            }
            return true;
        }

        /**
         * Open the specified file. Return true if file opened, false otherwise
         */
        public boolean openFile(File theFile) {
            try {
                theFile = theFile.getCanonicalFile();
            } catch (final IOException ioe) {
                // nothing to do, theFile will keep current value
            }
            // final String currentFilePath = theFile.getPath();
            // If this file is currently already open, then simply select its tab
            EditPane editPane = EditTabbedPane.this.getEditPaneForFile(theFile);
            if (editPane != null) {
                tabbedPane.setSelectedComponent(editPane);
                // updateTitlesAndMenuState(editPane);
                EditTabbedPane.this.updateTitles(editPane);
                return false;
            } else {
                editPane = new EditPane(EditTabbedPane.this.mainUI);
            }
            editPane.setFile(theFile);
            // FileStatus.reset();
            FileStatus.systemFile = theFile;
            FileStatus.setSystemState(FileStatus.State.OPENING);// DPS 9-Aug-2011
            if (theFile.canRead()) {
                Globals.program = new RISCVProgram();
                try {
                    Globals.program.readSource(theFile);
                } catch (final AssemblyException ignored) {
                }
                // DPS 1 Nov 2006. Defined a StringBuffer to receive all file contents,
                // one line at a time, before adding to the Edit pane with one setText.
                // StringBuffer is preallocated to full filelength to eliminate dynamic
                // expansion as lines are added to it. Previously, each line was appended
                // to the Edit pane as it was read, way slower due to dynamic string alloc.
                final StringBuilder fileContents = new StringBuilder((int) theFile.length());
                int lineNumber = 1;
                String line = Globals.program.getSourceLine(lineNumber++);
                while (line != null) {
                    fileContents.append(line).append("\n");
                    line = Globals.program.getSourceLine(lineNumber++);
                }
                editPane.setSourceCode(fileContents.toString(), true);
                // The above operation generates an undoable edit, setting the initial
                // text area contents, that should not be seen as undoable by the Undo
                // action. Let's get rid of it.
                editPane.discardAllUndoableEdits();
                editPane.setFileStatus(FileStatus.State.NOT_EDITED);

                tabbedPane.addTab(editPane.getFile().getName(), editPane);
                tabbedPane.setToolTipTextAt(tabbedPane.indexOfComponent(editPane), editPane.getFile().getPath());
                tabbedPane.setSelectedComponent(editPane);
                FileStatus.setSystemState(FileStatus.State.NOT_EDITED);

                // If assemble-all, then allow opening of any file w/o invalidating assembly.
                // DPS 9-Aug-2011
                if (BOOL_SETTINGS.getSetting(BoolSetting.ASSEMBLE_ALL)) {
                    EditTabbedPane.this.updateTitles(editPane);
                } else {// this was the original code...
                    EditTabbedPane.this.updateTitlesAndMenuState(editPane);
                    EditTabbedPane.this.mainPane.executePane.clearPane();
                }

                EditTabbedPane.this.mainPane.setSelectedComponent(EditTabbedPane.this);
                editPane.tellEditingComponentToRequestFocusInWindow();
                this.mostRecentlyOpenedFile = theFile;
            }
            return true;
        }

        /**
         * Private method to generate the file chooser's list of choosable file filters.
         * It is called when the file chooser is created, and called again each time the
         * Open
         * dialog is activated. We do this because the user may have added a new filter
         * during the previous dialog. This can be done by entering e.g. *.txt in the
         * file
         * name text field. Java is funny, however, in that if the user does this then
         * cancels the dialog, the new filter will remain in the list BUT if the user
         * does
         * this then ACCEPTS the dialog, the new filter will NOT remain in the list.
         * However
         * the act of entering it causes a property change event to occur, and we have a
         * handler that will add the new filter to our internal filter list and
         * "restore" it
         * the next time this method is called. Strangely, if the user then similarly
         * adds yet another new filter, the new one becomes simply a description change
         * to the previous one, the previous object is modified AND NO PROPERTY CHANGE
         * EVENT
         * IS FIRED! I could obviously deal with this situation if I wanted to, but
         * enough
         * is enough. The limit will be one alternative filter at a time.
         * DPS... 9 July 2008
         */
        private void setChoosableFileFilters() {
            // See if a new filter has been added to the master list. If so,
            // regenerate the fileChooser list from the master list.
            if (this.fileFilterCount < this.fileFilterList.size() ||
                this.fileFilterList.size() != this.fileChooser.getChoosableFileFilters().length) {
                this.fileFilterCount = this.fileFilterList.size();
                // First, "deactivate" the listener, because our addChoosableFileFilter
                // calls would otherwise activate it! We want it to be triggered only
                // by MARS user action.
                boolean activeListener = false;
                if (this.fileChooser.getPropertyChangeListeners().length > 0) {
                    this.fileChooser.removePropertyChangeListener(this.listenForUserAddedFileFilter);
                    activeListener = true; // we'll note this, for re-activation later
                }
                // clear out the list and populate from our own ArrayList.
                // Last one added becomes the default.
                this.fileChooser.resetChoosableFileFilters();
                for (final FileFilter ff : this.fileFilterList) {
                    this.fileChooser.addChoosableFileFilter(ff);
                }
                // Restore listener.
                if (activeListener) {
                    this.fileChooser.addPropertyChangeListener(this.listenForUserAddedFileFilter);
                }
            }
        }

        // Private inner class for special property change listener. DPS 9 July 2008.
        // If user adds a file filter, e.g. by typing *.txt into the file text field
        // Enter, then it is automatically added to the array of choosable file filters.
        // Cancel out of the Open dialog, it is then REMOVED from the list automatically
        // we will achieve a sort of persistence at least through the current activation
        private final class ChoosableFileFilterChangeListener implements PropertyChangeListener {
            @Override
            public void propertyChange(final @NotNull PropertyChangeEvent e) {
                if (e.getPropertyName().equals(JFileChooser.CHOOSABLE_FILE_FILTER_CHANGED_PROPERTY)) {
                    final FileFilter[] newFilters = (FileFilter[]) e.getNewValue();
                    if (newFilters.length > FileOpener.this.fileFilterList.size()) {
                        // new filter added, so add to end of master list.
                        FileOpener.this.fileFilterList.add(newFilters[newFilters.length - 1]);
                    }
                }
            }
        }

    }
}
