package rars.venus;

import kotlin.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rars.Globals;
import rars.RISCVProgram;
import rars.logging.Logger;
import rars.logging.LoggingExtKt;
import rars.logging.RARSLogging;
import rars.settings.AllSettings;
import rars.settings.BoolSetting;
import rars.util.AsmFileFilter;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
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
import static java.util.Objects.requireNonNull;
import static rars.util.UtilsKt.unwrap;
import static rars.venus.FileStatusKt.getNameForEditorTab;
import static rars.venus.FileStatusKt.isEdited;

/**
 * Tabbed pane for the editor. Each of its tabs represents an open file.
 *
 * @author Sanderson
 */
public final class EditTabbedPane extends JPanel {
    private static final @NotNull Logger LOGGER
        = RARSLogging.forJavaClass(EditTabbedPane.class);
    private final @NotNull MainPane mainPane;
    private final @NotNull VenusUI mainUI;
    private final @NotNull Editor editor;
    private final @NotNull FileOpener fileOpener;
    private final @NotNull JTabbedPane tabbedPane;
    private final @NotNull AllSettings allSettings;

    public EditTabbedPane(
        final @NotNull VenusUI mainUI,
        final @NotNull Editor editor,
        final @NotNull MainPane mainPane,
        final @NotNull AllSettings allSettings
    ) {
        super();
        this.allSettings = allSettings;
        this.tabbedPane = new JTabbedPane();
        this.mainUI = mainUI;
        this.editor = editor;
        this.fileOpener = new FileOpener(editor);
        this.mainPane = mainPane;
        this.editor.setEditTabbedPane(this);
        this.tabbedPane.addChangeListener(
            e -> {
                final var editorTab = (EditorTabNew) tabbedPane.getSelectedComponent();
                if (editorTab != null) {
                    // New IF statement to permit free traversal of edit panes w/o invalidating
                    // assembly if assemble-all is selected.
                    if (allSettings.boolSettings.getSetting(BoolSetting.ASSEMBLE_ALL)) {
                        updateTitles(editorTab);
                    } else {
                        updateTitlesAndMenuState(editorTab);
                        this.mainPane.executePane.clearPane();
                    }
                    editorTab.getTextArea().requestFocusInWindow();
                }
            });
        this.tabbedPane.putClientProperty(TABBED_PANE_TAB_CLOSABLE, true);
        this.tabbedPane.putClientProperty(
            TABBED_PANE_TAB_CLOSE_TOOLTIPTEXT,
            "Close current file"
        );
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
    public @Nullable EditorTabNew getCurrentEditTab() {
        return (EditorTabNew) tabbedPane.getSelectedComponent();
    }

    /**
     * Select the specified EditPane to be the current tab.
     *
     * @param editorTab
     *     The EditPane tab to become current.
     */
    public void setCurrentEditTab(final @NotNull EditorTabNew editorTab) {
        tabbedPane.setSelectedComponent(editorTab);
    }

    /**
     * Carries out all necessary operations to implement
     * the New operation from the File menu.
     */
    public void newFile() {
        final var name = this.editor.getNextDefaultFilename();
        final var status = new FileStatus.New.NotEdited(name);
        final var editPane = new EditorTabNew(
            mainUI,
            allSettings,
            status
        );
        editPane.getTextArea().setSourceCode("");
        tabbedPane.addTab(name, editPane);

        GlobalFileStatus.set(status);

        Globals.REGISTER_FILE.resetRegisters();
        this.mainUI.isMemoryReset = true;
        this.mainPane.executePane.clearPane();
        this.mainPane.setSelectedComponent(this);
        editPane.displayCaretPosition(new Pair<>(1, 1));
        tabbedPane.setSelectedComponent(editPane);
        this.updateTitlesAndMenuState(editPane);
        editPane.getTextArea().requestFocusInWindow();
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
        final var editorTab = this.getCurrentEditTab();
        if (editorTab != null) {
            if (this.editsSavedOrAbandoned()) {
                this.removePane(editorTab);
                this.mainPane.executePane.clearPane();
                this.mainPane.setSelectedComponent(this);
            } else {
                return false;
            }
        }
        return true;
    }

    private void closeFile(final int index) {
        final var editorTab = (EditorTabNew) tabbedPane.getComponentAt(index);
        if (this.editsSavedOrAbandoned()) {
            this.removePane(editorTab);
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
            final var tabs = new EditorTabNew[tabCount];
            boolean unsavedChanges = false;
            for (int i = 0; i < tabCount; i++) {
                tabs[i] = (EditorTabNew) tabbedPane.getComponentAt(i);
                if (isEdited(tabs[i].getFileStatus())) {
                    unsavedChanges = true;
                }
            }
            if (unsavedChanges) {
                switch (this.confirm("one or more files")) {
                    case JOptionPane.YES_OPTION:
                        boolean removedAll = true;
                        for (int i = 0; i < tabCount; i++) {
                            if (isEdited(tabs[i].getFileStatus())) {
                                tabbedPane.setSelectedComponent(tabs[i]);
                                final var isSaved = saveCurrentFile();
                                if (isSaved) {
                                    this.removePane(tabs[i]);
                                } else {
                                    removedAll = false;
                                }
                            } else {
                                this.removePane(tabs[i]);
                            }
                        }
                        return removedAll;
                    case JOptionPane.NO_OPTION:
                        for (int i = 0; i < tabCount; i++) {
                            this.removePane(tabs[i]);
                        }
                        return true;
                    default:
                        return false;
                }
            } else {
                for (int i = 0; i < tabCount; i++) {
                    this.removePane(tabs[i]);
                }
            }
        }
        return true;
    }

    /**
     * Saves file under existing name. If no name, will invoke Save As.
     *
     * @return true if the file was actually saved.
     */
    public boolean saveCurrentFile() {
        final var editPane = this.getCurrentEditTab();
        if (saveFile(editPane)) {
            GlobalFileStatus.set(editPane.getFileStatus());
            this.updateTitlesAndMenuState(editPane);
            return true;
        } else {
            return false;
        }
    }

    // Save file associatd with specified edit pane.
    // Returns true if save operation worked, else false.
    private boolean saveFile(final EditorTabNew editorTab) {
        if (editorTab != null) {
            final var fileStatus = editorTab.getFileStatus();
            if (fileStatus instanceof FileStatus.New) {
                final var file = saveAsFile(editorTab);
                if (file != null) {
                    editorTab.setFileStatus(
                        new FileStatus.Existing.NotEdited(file)
                    );
                }
                return true;
            } else if (fileStatus instanceof final FileStatus.Existing existing) {
                final var file = existing.getFile();
                try (
                    final var outFileStream = new BufferedWriter(
                        new FileWriter(file)
                    )
                ) {
                    outFileStream.write(editorTab.getTextArea().getText());
                } catch (final IOException c) {
                    JOptionPane.showMessageDialog(
                        null,
                        "Save operation could not be completed due to an error:\n" + c,
                        "Save Operation Failed",
                        JOptionPane.ERROR_MESSAGE
                    );
                    return false;
                }
                editorTab.setFileStatus(
                    new FileStatus.Existing.NotEdited(file)
                );
                return true;
            }
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
        final var currentTab = requireNonNull(this.getCurrentEditTab());
        final var savedFile = this.saveAsFile(currentTab);
        if (savedFile != null) {
            GlobalFileStatus.set(currentTab.getFileStatus());
            this.editor.setCurrentSaveDirectory(savedFile.getParent());
            currentTab.setFileStatus(
                new FileStatus.Existing.NotEdited(savedFile)
            );
            this.updateTitlesAndMenuState(currentTab);
            return true;
        }
        return false;
    }

    // perform Save As for selected edit pane. If the save is performed,
    // return its File object. Otherwise return null.
    private @Nullable File saveAsFile(final @NotNull EditorTabNew editorTab) {
        File selectedFile;
        while (true) {
            // Set Save As dialog directory in a logical way. If file in
            // edit pane had been previously saved, default to its directory.
            // If a new file (mipsN.asm), default to current save directory.
            final JFileChooser saveDialog;
            final File paneFile;
            switch (editorTab.getFileStatus()) {
                case final FileStatus.New ignored -> {
                    paneFile = null;
                    saveDialog = new JFileChooser(this.editor.getCurrentSaveDirectory());
                }
                case final FileStatus.Existing existing -> {
                    final var existingFile = existing.getFile();
                    paneFile = existingFile;
                    saveDialog = new JFileChooser(existingFile.getParent());
                }
                default -> throw new IllegalStateException("Unreachable case.");
            }
            if (paneFile != null) {
                saveDialog.setSelectedFile(paneFile);
            }
            saveDialog.setDialogTitle("Save As");

            final int decision = saveDialog.showSaveDialog(this.mainUI);
            if (decision != JFileChooser.APPROVE_OPTION) {
                return null;
            }
            selectedFile = saveDialog.getSelectedFile();
            if (selectedFile.exists()) {
                final var overwrite = JOptionPane.showConfirmDialog(
                    this.mainUI,
                    "File %s already exists. Do you wish to overwrite it?".formatted(
                        selectedFile.getName()
                    ),
                    "Overwrite existing file?",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE
                );
                if (overwrite == JOptionPane.YES_OPTION) {
                    break;
                }
            } else {
                break;
            }
        }
        // Either file with selected name does not exist or user wants to
        // overwrite it, so go for it!
        try (
            final BufferedWriter outFileStream = new BufferedWriter(new FileWriter(
                selectedFile
            ))
        ) {
            outFileStream.write(editorTab.getTextArea().getText());
            return selectedFile;
        } catch (final IOException c) {
            JOptionPane.showMessageDialog(
                null,
                "Save As operation could not be completed due to an error:\n" + c,
                "Save As Operation Failed",
                JOptionPane.ERROR_MESSAGE
            );
            return null;
        }
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
            for (int i = 0; i < tabCount; i++) {
                final var tab = (EditorTabNew) tabbedPane.getComponentAt(i);
                if (isEdited(tab.getFileStatus())) {
                    result &= saveFile(tab);
                    setTitleForTab(tab, tab.getFileStatus());
                }
            }
            if (result) {
                final var editorTab = requireNonNull(getCurrentEditTab());
                GlobalFileStatus.set(editorTab.getFileStatus());
                // editorTab.setFileStatus(FileStatusOld.State.NOT_EDITED);
                this.updateTitlesAndMenuState(editorTab);
            }
        }
        return result;
    }

    public void setDefaultTitle() {
        mainUI.setTitle(Globals.RARS_TITLE);
    }

    public void setTitleForTab(
        final @NotNull EditorTabNew editorTab,
        final @NotNull FileStatus fileStatus
    ) {
        final var editIndicator = isEdited(fileStatus)
            ? "*"
            : "";
        final var titleName = switch (fileStatus) {
            case final FileStatus.New statusNew -> statusNew.getTmpName();
            case final FileStatus.Existing statusExisting ->
                statusExisting.getFile().getAbsolutePath();
            default -> throw new IllegalStateException("Unreachable case.");
        };
        final var tabName = switch (fileStatus) {
            case final FileStatus.New statusNew -> statusNew.getTmpName();
            case final FileStatus.Existing statusExisting ->
                statusExisting.getFile().getName();
            default -> throw new IllegalStateException("Unreachable case.");
        };
        mainUI.setTitle(titleName + editIndicator + " - " + Globals.RARS_TITLE);
        final var index = tabbedPane.indexOfComponent(editorTab);
        tabbedPane.setTitleAt(index, tabName + editIndicator);
    }

    public @NotNull List<@NotNull File> getOpenFilePaths() {
        final var result = new ArrayList<File>();
        for (final var component : tabbedPane.getComponents()) {
            final var fileStatus = ((EditorTabNew) component).getFileStatus();
            if (fileStatus instanceof final FileStatus.Existing existing) {
                result.add(existing.getFile());
            } else {
                LoggingExtKt.logWarning(LOGGER,
                    () -> "File status not set as existing: " + fileStatus
                );
            }
        }
        return result;
    }

    /**
     * Remove the pane and update menu status
     *
     * @param editorTab
     *     a {@link EditorTabNew} object
     */
    public void removePane(final EditorTabNew editorTab) {
        tabbedPane.remove(editorTab);
        final var currentTab = getCurrentEditTab();
        if (currentTab == null) {
            GlobalFileStatus.reset();
            setDefaultTitle();
            this.mainUI.setMenuState(null);
        } else {
            GlobalFileStatus.set(currentTab.getFileStatus());
            this.updateTitlesAndMenuState(currentTab);
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
    private void updateTitlesAndMenuState(final @NotNull EditorTabNew editorTab) {
        final var fileStatus = editorTab.getFileStatus();
        editor.setTitleFromFileStatus(fileStatus);
        GlobalFileStatus.set(fileStatus);
        // editorTab.updateStaticFileStatus(); // for legacy code that depends on the static FileStatus (pre 4.0)
        this.mainUI.setMenuState(editorTab.getFileStatus());
    }

    // Handy little utility to update the title on the current tab and the frame
    // title bar
    // and also to update the MARS menu state (controls which actions are enabled).
    private void updateTitles(final @NotNull EditorTabNew editorTab) {
        this.editor.setTitleFromFileStatus(
            editorTab.getFileStatus()
        );
        final var isAssembled = FileStatusKt.isAssembled(GlobalFileStatus.get());
        // final boolean assembled = GlobalFileStatus.isAssembled();
        // editorTab.updateStaticFileStatus(); // for legacy code that depends on the static FileStatus (pre 4.0)
        // GlobalFileStatus.setAssembled(assembled);
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
    public @Nullable EditorTabNew getEditPaneForFile(final @NotNull File file) {
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            final var pane = (EditorTabNew) tabbedPane.getComponentAt(i);
            final var paneStatus = pane.getFileStatus();
            if (paneStatus instanceof final FileStatus.Existing statusExisting &&
                statusExisting.getFile().equals(file)) {
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
        final var currentPane = this.getCurrentEditTab();
        if (currentPane != null && isEdited(currentPane.getFileStatus())) {
            final var status = currentPane.getFileStatus();
            return switch (confirm(getNameForEditorTab(status))) {
                case JOptionPane.YES_OPTION -> saveCurrentFile();
                case JOptionPane.NO_OPTION -> true;
                case JOptionPane.CANCEL_OPTION -> false;
                default ->
                    throw new IllegalStateException("Unexpected value: " + status);
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
        final var editorTab = this.getEditPaneForFile(file);
        final EditorTabNew tabForLine;
        if (editorTab != null) {
            if (editorTab != this.getCurrentEditTab()) {
                this.setCurrentEditTab(editorTab);
            }
            tabForLine = editorTab;
        } else {
            // file is not open. Try to open it.
            if (this.openFile(file)) {
                tabForLine = this.getCurrentEditTab();
            } else {
                tabForLine = null;
            }
        }
        // If editPane == null, it means the desired file was not open. Line selection
        // does not properly with the JEditTextArea editor in this situation (it works
        // fine for the original generic editor). So we just won't do it. 
        if (editorTab != null) {
            tabForLine.getTextArea().selectLine(line - 1);
        }
    }

    private final class FileOpener {
        private static final @NotNull Logger LOGGER = RARSLogging.forJavaClass(
            FileOpener.class
        );
        private final @NotNull JFileChooser fileChooser;
        private final @NotNull ArrayList<@NotNull FileFilter> fileFilterList;
        private final @NotNull PropertyChangeListener listenForUserAddedFileFilter;
        private final @NotNull Editor theEditor;
        private @Nullable File mostRecentlyOpenedFile;
        private int fileFilterCount;

        private FileOpener(final @NotNull Editor theEditor) {
            this.mostRecentlyOpenedFile = null;
            this.theEditor = theEditor;
            this.fileChooser = new JFileChooser();
            this.listenForUserAddedFileFilter = e -> {
                if (e.getPropertyName()
                    .equals(JFileChooser.CHOOSABLE_FILE_FILTER_CHANGED_PROPERTY)) {
                    final FileFilter[] newFilters = (FileFilter[]) e.getNewValue();
                    if (newFilters.length > FileOpener.this.fileFilterList.size()) {
                        // new filter added, so add to end of master list.
                        FileOpener.this.fileFilterList.add(newFilters[newFilters.length - 1]);
                    }
                }
            };
            this.fileChooser.addPropertyChangeListener(this.listenForUserAddedFileFilter);

            // Note: add sequence is significant - last one added becomes default.
            this.fileFilterList = new ArrayList<>();
            this.fileFilterList.add(this.fileChooser.getAcceptAllFileFilter());
            this.fileFilterList.add(AsmFileFilter.INSTANCE);
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
            if (allSettings.boolSettings.getSetting(BoolSetting.ASSEMBLE_ON_OPEN)
                && this.mostRecentlyOpenedFile != null) {
                this.fileChooser.setSelectedFile(this.mostRecentlyOpenedFile);
            }

            if (this.fileChooser.showOpenDialog(mainUI) == JFileChooser.APPROVE_OPTION) {
                final var startTime = Instant.now();
                final File theFile = this.fileChooser.getSelectedFile();
                this.theEditor.setCurrentOpenDirectory(theFile.getParent());
                if (!this.openFile(theFile)) {
                    return false;
                }

                // possibly send this file right through to the assembler by firing
                // Run->Assemble's
                // actionPerformed() method.
                if (theFile.canRead()) {
                    if (allSettings.boolSettings.getSetting(BoolSetting.ASSEMBLE_ON_OPEN)) {
                        mainUI.getRunAssembleAction()
                            .actionPerformed(null);
                    }
                }
                final var endTime = Instant.now();
                final var duration = Duration.between(startTime, endTime);
                LoggingExtKt.logInfo(LOGGER, () ->
                    "Opened file in %dms.".formatted(
                        duration.toMillis()
                    )
                );
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
            var editPane = getEditPaneForFile(theFile);
            if (editPane != null) {
                tabbedPane.setSelectedComponent(editPane);
                // updateTitlesAndMenuState(editPane);
                updateTitles(editPane);
                return false;
            } else {
                editPane = new EditorTabNew(
                    mainUI,
                    allSettings,
                    new FileStatus.Existing.NotEdited(theFile));
            }
            // FileStatus.reset();
            GlobalFileStatus.reset();
            // GlobalFileStatus.systemFile = theFile;
            // GlobalFileStatus.setSystemState(GlobalFileStatus.State.OPENING);
            if (theFile.canRead()) {
                Globals.PROGRAM = new RISCVProgram();
                unwrap(Globals.PROGRAM.readSource(theFile));
                // Defined a StringBuffer to receive all file contents,
                // one line at a time, before adding to the Edit pane with one setText.
                // StringBuffer is preallocated to full filelength to eliminate dynamic
                // expansion as lines are added to it. Previously, each line was appended
                // to the Edit pane as it was read, way slower due to dynamic string alloc.
                final var fileContents = new StringBuilder((int) theFile.length());
                int lineNumber = 1;
                String line = Globals.PROGRAM.getSourceLine(lineNumber);
                lineNumber++;
                while (line != null) {
                    fileContents.append(line).append('\n');
                    line = Globals.PROGRAM.getSourceLine(lineNumber);
                    lineNumber++;
                }
                tabbedPane.addTab(theFile.getName(), editPane);
                tabbedPane.setToolTipTextAt(
                    tabbedPane.indexOfComponent(editPane),
                    theFile.getPath()
                );
                tabbedPane.setSelectedComponent(editPane);
                final var textArea = editPane.getTextArea();
                textArea.setSourceCode(fileContents.toString());
                // The above operation generates an undoable edit, setting the initial
                // text area contents, that should not be seen as undoable by the Undo
                // action. Let's get rid of it.
                textArea.discardAllUndoableEdits();
                editPane.setFileStatus(
                    new FileStatus.Existing.NotEdited(theFile)
                );

                GlobalFileStatus.set(new FileStatus.Existing.NotEdited(theFile));

                // If assemble-all, then allow opening of any file w/o invalidating assembly.
                if (allSettings.boolSettings.getSetting(BoolSetting.ASSEMBLE_ALL)) {
                    updateTitles(editPane);
                } else {
                    // this was the original code...
                    updateTitlesAndMenuState(editPane);
                    mainPane.executePane.clearPane();
                }

                mainPane.setSelectedComponent(EditTabbedPane.this);
                editPane.getTextArea().requestFocusInWindow();
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
    }
}
