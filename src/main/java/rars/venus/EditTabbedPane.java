package rars.venus;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rars.Globals;
import rars.RISCVprogram;
import rars.Settings;
import rars.exceptions.AssemblyException;
import rars.riscv.hardware.RegisterFile;
import rars.settings.BoolSetting;
import rars.util.FilenameFinder;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.beans.PropertyChangeListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.function.BiConsumer;

import static com.formdev.flatlaf.FlatClientProperties.*;

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
public class EditTabbedPane extends JPanel {
    final MainPane mainPane;
    private final VenusUI mainUI;
    private final Editor editor;
    private final FileOpener fileOpener;
    private final JTabbedPane tabbedPane;

    /**
     * Constructor for the EditTabbedPane class.
     *
     * @param appFrame a {@link VenusUI} object
     * @param editor   a {@link Editor} object
     * @param mainPane a {@link MainPane} object
     */
    public EditTabbedPane(final VenusUI appFrame, final Editor editor, final MainPane mainPane) {
        super();
        this.tabbedPane = new JTabbedPane();
        this.mainUI = appFrame;
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
                        if (Globals.getSettings().getBoolSettings().getSetting(BoolSetting.ASSEMBLE_ALL)) {
                            EditTabbedPane.this.updateTitles(editPane);
                        } else {
                            EditTabbedPane.this.updateTitlesAndMenuState(editPane);
                            EditTabbedPane.this.mainPane.getExecutePane().clearPane();
                        }
                        editPane.tellEditingComponentToRequestFocusInWindow();
                    }
                });
        this.tabbedPane.putClientProperty(TABBED_PANE_TAB_CLOSABLE, true);
        this.tabbedPane.putClientProperty(TABBED_PANE_TAB_CLOSE_TOOLTIPTEXT, "Close current file");
        this.tabbedPane.putClientProperty(TABBED_PANE_TAB_CLOSE_CALLBACK, (BiConsumer<JTabbedPane, Integer>) (pane, index) -> this.closeFile(index));
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
     * @param editPane The EditPane tab to become current.
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
        editPane.setShowLineNumbersEnabled(true);
        editPane.setFileStatus(FileStatus.NEW_NOT_EDITED);
        final String name = this.editor.getNextDefaultFilename();
        editPane.setPathname(name);
        tabbedPane.addTab(name, editPane);

        FileStatus.reset();
        FileStatus.setName(name);
        FileStatus.set(FileStatus.NEW_NOT_EDITED);

        RegisterFile.resetRegisters();
        this.mainUI.setReset(true);
        this.mainPane.getExecutePane().clearPane();
        this.mainPane.setSelectedComponent(this);
        editPane.displayCaretPosition(new Point(1, 1));
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
     * @param file a {@link java.io.File} object
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
                this.mainPane.getExecutePane().clearPane();
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
            this.mainPane.getExecutePane().clearPane();
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
        final boolean result = true;
        boolean unsavedChanges = false;
        final int tabCount = tabbedPane.getTabCount();
        if (tabCount > 0) {
            this.mainPane.getExecutePane().clearPane();
            this.mainPane.setSelectedComponent(this);
            final EditPane[] tabs = new EditPane[tabCount];
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
            FileStatus.setSaved(true);
            FileStatus.setEdited(false);
            FileStatus.set(FileStatus.NOT_EDITED);
            editPane.setFileStatus(FileStatus.NOT_EDITED);
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
                    editPane.setPathname(theFile.getPath());
                }
                return (theFile != null);
            }
            final File theFile = new File(editPane.getPathname());
            try {
                final BufferedWriter outFileStream = new BufferedWriter(new FileWriter(theFile));
                outFileStream.write(editPane.getSource(), 0, editPane.getSource().length());
                outFileStream.close();
            } catch (final java.io.IOException c) {
                JOptionPane.showMessageDialog(null, "Save operation could not be completed due to an error:\n" + c,
                        "Save Operation Failed", JOptionPane.ERROR_MESSAGE);
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
            FileStatus.setFile(theFile);
            FileStatus.setName(theFile.getPath());
            FileStatus.setSaved(true);
            FileStatus.setEdited(false);
            FileStatus.set(FileStatus.NOT_EDITED);
            this.editor.setCurrentSaveDirectory(theFile.getParent());
            editPane.setPathname(theFile.getPath());
            editPane.setFileStatus(FileStatus.NOT_EDITED);
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
            JFileChooser saveDialog;
            boolean operationOK = false;
            while (!operationOK) {
                // Set Save As dialog directory in a logical way. If file in
                // edit pane had been previously saved, default to its directory.
                // If a new file (mipsN.asm), default to current save directory.
                // DPS 13-July-2011
                if (editPane.isNew()) {
                    saveDialog = new JFileChooser(this.editor.getCurrentSaveDirectory());
                } else {
                    final File f = new File(editPane.getPathname());
                    saveDialog = new JFileChooser(f.getParent());
                }
                final String paneFile = editPane.getFilename();
                if (paneFile != null)
                    saveDialog.setSelectedFile(new File(paneFile));
                // end of 13-July-2011 code.
                saveDialog.setDialogTitle("Save As");

                final int decision = saveDialog.showSaveDialog(this.mainUI);
                if (decision != JFileChooser.APPROVE_OPTION) {
                    return null;
                }
                theFile = saveDialog.getSelectedFile();
                operationOK = true;
                if (theFile.exists()) {
                    final int overwrite = JOptionPane.showConfirmDialog(this.mainUI,
                            "File " + theFile.getName() + " already exists.  Do you wish to overwrite it?",
                            "Overwrite existing file?",
                            JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
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
                JOptionPane.showMessageDialog(null, "Save As operation could not be completed due to an error:\n" + c,
                        "Save As Operation Failed", JOptionPane.ERROR_MESSAGE);
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
                tabs[i] = (EditPane) tabbedPane.getComponentAt(i);
                if (tabs[i].hasUnsavedEdits()) {
                    this.setCurrentEditTab(tabs[i]);
                    if (this.saveFile(tabs[i])) {
                        tabs[i].setFileStatus(FileStatus.NOT_EDITED);
                        this.editor.setTitle(tabs[i].getPathname(), tabs[i].getFilename(), tabs[i].getFileStatus());
                    } else {
                        result = false;
                    }
                }
            }
            this.setCurrentEditTab(savedPane);
            if (result) {
                final EditPane editPane = this.getCurrentEditTab();
                FileStatus.setSaved(true);
                FileStatus.setEdited(false);
                FileStatus.set(FileStatus.NOT_EDITED);
                editPane.setFileStatus(FileStatus.NOT_EDITED);
                this.updateTitlesAndMenuState(editPane);
            }
        }
        return result;
    }

    /**
     * <p>getOpenFilePaths.</p>
     *
     * @return an array of {@link java.lang.String} objects
     */
    public String[] getOpenFilePaths() {
        final int tabCount = tabbedPane.getTabCount();
        final String[] tabs = new String[tabCount];
        for (int i = 0; i < tabCount; i++) {
            tabs[i] = ((EditPane) tabbedPane.getComponentAt(i)).getPathname();
        }
        return tabs;
    }

    /**
     * Remove the pane and update menu status
     *
     * @param editPane a {@link EditPane} object
     */
    public void remove(EditPane editPane) {
        tabbedPane.remove(editPane);
        editPane = this.getCurrentEditTab(); // is now next tab or null
        if (editPane == null) {
            FileStatus.set(FileStatus.NO_FILE);
            this.editor.setTitle("", "", FileStatus.NO_FILE);
            Globals.getGui().setMenuState(FileStatus.NO_FILE);
        } else {
            FileStatus.set(editPane.getFileStatus());
            this.updateTitlesAndMenuState(editPane);
        }
        // When last file is closed, menu is unable to respond to mnemonics
        // and accelerators. Let's have it request focus so it may do so.
        if (tabbedPane.getTabCount() == 0)
            this.mainUI.haveMenuRequestFocus();
    }

    // Handy little utility to update the title on the current tab and the frame
    // title bar
    // and also to update the MARS menu state (controls which actions are enabled).
    private void updateTitlesAndMenuState(final EditPane editPane) {
        this.editor.setTitle(editPane.getPathname(), editPane.getFilename(), editPane.getFileStatus());
        editPane.updateStaticFileStatus(); // for legacy code that depends on the static FileStatus (pre 4.0)
        Globals.getGui().setMenuState(editPane.getFileStatus());
    }

    // Handy little utility to update the title on the current tab and the frame
    // title bar
    // and also to update the MARS menu state (controls which actions are enabled).
    // DPS 9-Aug-2011
    private void updateTitles(final EditPane editPane) {
        this.editor.setTitle(editPane.getPathname(), editPane.getFilename(), editPane.getFileStatus());
        final boolean assembled = FileStatus.isAssembled();
        editPane.updateStaticFileStatus(); // for legacy code that depends on the static FileStatus (pre 4.0)
        FileStatus.setAssembled(assembled);
    }

    /**
     * If there is an EditPane for the given file pathname, return it else return
     * null.
     *
     * @param pathname Pathname for desired file
     * @return the EditPane for this file if it is open in the editor, or null if
     * not.
     */
    public EditPane getEditPaneForFile(final String pathname) {
        EditPane openPane = null;
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            final EditPane pane = (EditPane) tabbedPane.getComponentAt(i);
            if (pane.getPathname().equals(pathname)) {
                openPane = pane;
                break;
            }
        }
        return openPane;
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
            return switch (this.confirm(currentPane.getFilename())) {
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

    private int confirm(final String name) {
        return JOptionPane.showConfirmDialog(this.mainUI,
                "Changes to " + name + " will be lost unless you save.  Do you wish to save all changes now?",
                "Save program changes?",
                JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
    }

    public @NotNull JTabbedPane getTabbedPane() {
        return tabbedPane;
    }

    private class FileOpener {
        private final JFileChooser fileChooser;
        private final ArrayList<FileFilter> fileFilterList;
        private final PropertyChangeListener listenForUserAddedFileFilter;
        private final Editor theEditor;
        private File mostRecentlyOpenedFile;
        private int fileFilterCount;

        public FileOpener(final Editor theEditor) {
            this.mostRecentlyOpenedFile = null;
            this.theEditor = theEditor;
            this.fileChooser = new JFileChooser();
            this.listenForUserAddedFileFilter = new ChoosableFileFilterChangeListener();
            this.fileChooser.addPropertyChangeListener(this.listenForUserAddedFileFilter);

            // Note: add sequence is significant - last one added becomes default.
            this.fileFilterList = new ArrayList<>();
            this.fileFilterList.add(this.fileChooser.getAcceptAllFileFilter());
            this.fileFilterList.add(FilenameFinder.getFileFilter(Globals.fileExtensions, "Assembler Files", true));
            this.fileFilterCount = 0; // this will trigger fileChooser file filter load in next line
            this.setChoosableFileFilters();
        }

        /*
         * Launch a file chooser for name of file to open. Return true if file opened,
         * false otherwise
         */
        private boolean openFile() {
            // The fileChooser's list may be rebuilt from the master ArrayList if a new
            // filter
            // has been added by the user.
            this.setChoosableFileFilters();
            // get name of file to be opened and load contents into text editing area.
            this.fileChooser.setCurrentDirectory(new File(this.theEditor.getCurrentOpenDirectory()));
            // Set default to previous file opened, if any. This is useful in conjunction
            // with option to assemble file automatically upon opening. File likely to have
            // been edited externally (e.g. by Mipster).
            if (Globals.getSettings().getBoolSettings().getSetting(BoolSetting.ASSEMBLE_ON_OPEN)
                    && this.mostRecentlyOpenedFile != null) {
                this.fileChooser.setSelectedFile(this.mostRecentlyOpenedFile);
            }

            if (this.fileChooser.showOpenDialog(EditTabbedPane.this.mainUI) == JFileChooser.APPROVE_OPTION) {
                final File theFile = this.fileChooser.getSelectedFile();
                this.theEditor.setCurrentOpenDirectory(theFile.getParent());
                // theEditor.setCurrentSaveDirectory(theFile.getParent());// 13-July-2011 DPS.
                if (!this.openFile(theFile)) {
                    return false;
                }

                // possibly send this file right through to the assembler by firing
                // Run->Assemble's
                // actionPerformed() method.
                if (theFile.canRead() && Globals.getSettings().getBoolSettings().getSetting(BoolSetting.ASSEMBLE_ON_OPEN)) {
                    EditTabbedPane.this.mainUI.getRunAssembleAction().actionPerformed(null);
                }
            }
            return true;
        }

        /*
         * Open the specified file. Return true if file opened, false otherwise
         */

        private boolean openFile(File theFile) {
            try {
                theFile = theFile.getCanonicalFile();
            } catch (final IOException ioe) {
                // nothing to do, theFile will keep current second
            }
            final String currentFilePath = theFile.getPath();
            // If this file is currently already open, then simply select its tab
            EditPane editPane = EditTabbedPane.this.getEditPaneForFile(currentFilePath);
            if (editPane != null) {
                tabbedPane.setSelectedComponent(editPane);
                // updateTitlesAndMenuState(editPane);
                EditTabbedPane.this.updateTitles(editPane);
                return false;
            } else {
                editPane = new EditPane(EditTabbedPane.this.mainUI);
            }
            editPane.setPathname(currentFilePath);
            // FileStatus.reset();
            FileStatus.setName(currentFilePath);
            FileStatus.setFile(theFile);
            FileStatus.set(FileStatus.OPENING);// DPS 9-Aug-2011
            if (theFile.canRead()) {
                Globals.program = new RISCVprogram();
                try {
                    Globals.program.readSource(currentFilePath);
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
                editPane.setShowLineNumbersEnabled(true);
                editPane.setFileStatus(FileStatus.NOT_EDITED);

                tabbedPane.addTab(editPane.getFilename(), editPane);
                tabbedPane.setToolTipTextAt(tabbedPane.indexOfComponent(editPane), editPane.getPathname());
                tabbedPane.setSelectedComponent(editPane);
                FileStatus.setSaved(true);
                FileStatus.setEdited(false);
                FileStatus.set(FileStatus.NOT_EDITED);

                // If assemble-all, then allow opening of any file w/o invalidating assembly.
                // DPS 9-Aug-2011
                if (Globals.getSettings().getBoolSettings().getSetting(BoolSetting.ASSEMBLE_ALL)) {
                    EditTabbedPane.this.updateTitles(editPane);
                } else {// this was the original code...
                    EditTabbedPane.this.updateTitlesAndMenuState(editPane);
                    EditTabbedPane.this.mainPane.getExecutePane().clearPane();
                }

                EditTabbedPane.this.mainPane.setSelectedComponent(EditTabbedPane.this);
                editPane.tellEditingComponentToRequestFocusInWindow();
                this.mostRecentlyOpenedFile = theFile;
            }
            return true;
        }

        // Private method to generate the file chooser's list of choosable file filters.
        // It is called when the file chooser is created, and called again each time the
        // Open
        // dialog is activated. We do this because the user may have added a new filter
        // during the previous dialog. This can be done by entering e.g. *.txt in the
        // file
        // name text field. Java is funny, however, in that if the user does this then
        // cancels the dialog, the new filter will remain in the list BUT if the user
        // does
        // this then ACCEPTS the dialog, the new filter will NOT remain in the list.
        // However
        // the act of entering it causes a property change event to occur, and we have a
        // handler that will add the new filter to our internal filter list and
        // "restore" it
        // the next time this method is called. Strangely, if the user then similarly
        // adds yet another new filter, the new one becomes simply a description change
        // to the previous one, the previous object is modified AND NO PROPERTY CHANGE
        // EVENT
        // IS FIRED! I could obviously deal with this situation if I wanted to, but
        // enough
        // is enough. The limit will be one alternative filter at a time.
        // DPS... 9 July 2008

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
        }//////////////////////////////////////////////////////////////////////////////////
        // Private inner class for special property change listener. DPS 9 July 2008.
        // If user adds a file filter, e.g. by typing *.txt into the file text field
        ////////////////////////////////////////////////////////////////////////////////// then
        ////////////////////////////////////////////////////////////////////////////////// pressing
        // Enter, then it is automatically added to the array of choosable file filters.
        ////////////////////////////////////////////////////////////////////////////////// BUT,
        ////////////////////////////////////////////////////////////////////////////////// unless
        ////////////////////////////////////////////////////////////////////////////////// you
        // Cancel out of the Open dialog, it is then REMOVED from the list automatically
        ////////////////////////////////////////////////////////////////////////////////// also.
        ////////////////////////////////////////////////////////////////////////////////// Here
        // we will achieve a sort of persistence at least through the current activation

        /// /////////////////////////////////////////////////////////////////////////////// of
        /// /////////////////////////////////////////////////////////////////////////////// MARS.

        private class ChoosableFileFilterChangeListener implements PropertyChangeListener {
            @Override
            public void propertyChange(final java.beans.PropertyChangeEvent e) {
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
