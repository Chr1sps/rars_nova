package rars.venus;

import kotlin.Pair;
import kotlin.Unit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rars.settings.AllSettings;
import rars.settings.BoolSetting;
import rars.venus.editors.TextEditingArea;
import rars.venus.editors.TextEditingArea.FindReplaceResult;
import rars.venus.editors.TextEditingAreaKt;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.io.File;

/**
 * Represents one file opened for editing. Maintains required internal
 * structures.
 * Before Mars 4.0, there was only one editor pane, a tab, and only one file
 * could
 * be open at a time. With 4.0 came the multifile (pane, tab) editor, and
 * existing
 * duties were split between EditPane and the new EditTabbedPane class.
 *
 * @author Sanderson and Bumgarner
 */
public final class EditPane extends JPanel {

    public final @NotNull TextEditingArea sourceCode;
    private final @NotNull VenusUI mainUI;
    private final @NotNull JLabel caretPositionLabel;
    private final @NotNull FileStatus fileStatus;

    public EditPane(
        final @NotNull VenusUI appFrame,
        final @NotNull AllSettings allSettings
    ) {
        super(new BorderLayout());
        this.mainUI = appFrame;
        // user.dir, user's current working directory, is guaranteed to have a value
        // mainUI.editor = new Editor(mainUI);
        // We want to be notified of editor font changes! See update() below.

        this.fileStatus = new FileStatus();

        final var editorThemeSettings = allSettings.editorThemeSettings;
        this.sourceCode = TextEditingAreaKt.createTextEditingArea(
            allSettings
        );

        this.sourceCode.setTheme(editorThemeSettings.getCurrentTheme().toEditorTheme());
        editorThemeSettings.onChangeListenerHook.subscribe(ignored -> {
            this.sourceCode.setTheme(editorThemeSettings.getCurrentTheme().toEditorTheme());
            return Unit.INSTANCE;
        });

        final var fontSettings = allSettings.fontSettings;
        this.sourceCode.setFont(fontSettings.getCurrentFont());
        fontSettings.onChangeListenerHook.subscribe(ignored -> {
            this.sourceCode.setFont(fontSettings.getCurrentFont());
            return Unit.INSTANCE;
        });

        final var boolSettings = allSettings.boolSettings;
        this.sourceCode.setLineHighlightEnabled(boolSettings.getSetting(BoolSetting.EDITOR_CURRENT_LINE_HIGHLIGHTING));
        boolSettings.getOnChangeListenerHook().subscribe(ignored -> {
            this.sourceCode.setLineHighlightEnabled(
                boolSettings.getSetting(BoolSetting.EDITOR_CURRENT_LINE_HIGHLIGHTING)
            );
            return Unit.INSTANCE;
        });

        final var otherSettings = allSettings.otherSettings;
        this.sourceCode.setCaretBlinkRate(otherSettings.getCaretBlinkRate());
        this.sourceCode.setTabSize(otherSettings.getEditorTabSize());
        otherSettings.getOnChangeListenerHook().subscribe(ignore -> {
            this.sourceCode.setCaretBlinkRate(otherSettings.getCaretBlinkRate());
            this.sourceCode.setTabSize(otherSettings.getEditorTabSize());
            return Unit.INSTANCE;
        });

        // sourceCode is responsible for its own scrolling
        this.add(this.sourceCode.getOuterComponent(), BorderLayout.CENTER);

        // If source code is modified, will set flag to trigger/request file save.
        this.sourceCode.getDocument().addDocumentListener(
            new DocumentListener() {
                @Override
                public void insertUpdate(final DocumentEvent evt) {
                    // This method is triggered when file contents added to document
                    // upon opening, even though not edited by user. The IF
                    // statement will sense this situation and immediately return.
                    if (FileStatus.getSystemState() == FileStatus.State.OPENING) {
                        EditPane.this.setFileStatus(FileStatus.State.NOT_EDITED);
                        FileStatus.setSystemState(FileStatus.State.NOT_EDITED);
                        return;
                    }
                    if (EditPane.this.getFileStatus() == FileStatus.State.NEW_NOT_EDITED) {
                        EditPane.this.setFileStatus(FileStatus.State.NEW_EDITED);
                    }
                    if (EditPane.this.getFileStatus() == FileStatus.State.NOT_EDITED) {
                        EditPane.this.setFileStatus(FileStatus.State.EDITED);
                    }
                    if (EditPane.this.getFileStatus() == FileStatus.State.NEW_EDITED) {
                        EditPane.this.mainUI.editor.setTitle(
                            "",
                            EditPane.this.getFile().getName(),
                            EditPane.this.getFileStatus()
                        );
                    } else {
                        EditPane.this.mainUI.editor.setTitleFromFile(
                            EditPane.this.getFile(),
                            EditPane.this.getFileStatus()
                        );
                    }

                    switch (FileStatus.getSystemState()) {
                        case FileStatus.State.NEW_NOT_EDITED:
                            FileStatus.setSystemState(FileStatus.State.NEW_EDITED);
                            break;
                        case FileStatus.State.NEW_EDITED:
                            break;
                        default:
                            FileStatus.setSystemState(FileStatus.State.EDITED);
                    }

                    EditPane.this.mainUI.mainPane.executePane.clearPane();
                }

                @Override
                public void removeUpdate(final DocumentEvent evt) {
                    this.insertUpdate(evt);
                }

                @Override
                public void changedUpdate(final DocumentEvent e) {

                }

                @Override
                public String toString() {
                    return "EditPane DocumentListener";
                }
            });

        this.setSourceCode("", false);
        final JPanel editInfo = new JPanel(new BorderLayout());
        this.caretPositionLabel = new JLabel();
        this.caretPositionLabel.setToolTipText("Tracks the current position of the text editing cursor.");
        this.displayCaretPosition(new Pair<>(0, 0));
        this.sourceCode.getCaret().addChangeListener(e -> {
            final var position = this.sourceCode.getCaretPosition();
            this.displayCaretPosition(new Pair<>(position.getFirst() + 1, position.getSecond() + 1));
        });
        editInfo.add(this.caretPositionLabel, BorderLayout.WEST);
        this.add(editInfo, BorderLayout.SOUTH);
    }

    /**
     * For initalizing the source code when opening an ASM file
     *
     * @param s
     *     String containing text
     * @param editable
     *     set true if code is editable else false
     */
    public void setSourceCode(final String s, final boolean editable) {
        this.sourceCode.setSourceCode(s, editable);
    }

    /**
     * Get rid of any accumulated undoable edits. It is useful to call
     * this method after opening a file into the text area. The
     * act of setting its text content upon reading the file will generate
     * an undoable edit. Normally you don't want a freshly-opened file
     * to appear with its Undo action enabled. But it will unless you
     * call this after setting the text.
     */
    public void discardAllUndoableEdits() {
        this.sourceCode.discardAllUndoableEdits();
    }

    /**
     * Get source code text
     *
     * @return Sting containing source code
     */
    public String getSource() {
        return this.sourceCode.getText();
    }

    /**
     * Get the editing status for this EditPane's associated document.
     * This will be one of the constants from class FileStatus.
     *
     * @return a int
     */
    public @NotNull FileStatus.State getFileStatus() {
        return this.fileStatus.getFileStatus();
    }

    /**
     * Set the editing status for this EditPane's associated document.
     * For the argument, use one of the constants from class FileStatus.
     *
     * @param fileStatus
     *     the status constant from class FileStatus
     */
    public void setFileStatus(final @NotNull FileStatus.State fileStatus) {
        this.fileStatus.setFileStatus(fileStatus);
    }

    /** Returns a file associated with this pane. */
    public @Nullable File getFile() {
        return this.fileStatus.getFile();
    }

    /** Sets the file associated with this pane. */
    public void setFile(final @NotNull File file) {
        this.fileStatus.setFile(file);
    }

    /**
     * Delegates to corresponding FileStatus method
     *
     * @return a boolean
     */
    public boolean hasUnsavedEdits() {
        return this.fileStatus.hasUnsavedEdits();
    }

    /**
     * Delegates to corresponding FileStatus method
     *
     * @return a boolean
     */
    public boolean isNew() {
        return this.fileStatus.isNew();
    }

    /**
     * Delegates to text area's requestFocusInWindow method.
     */
    public void tellEditingComponentToRequestFocusInWindow() {
        this.sourceCode.requestFocusInWindow();
    }

    /*
     * Note: these are invoked only when copy/cut/paste are used from the
     * toolbar or menu or the defined menu Alt codes. When
     * Ctrl-C, Ctrl-X or Ctrl-V are used, this code is NOT invoked
     * but the operation works correctly!
     * The "set visible" operations are used because clicking on the toolbar
     * icon causes both the selection highlighting AND the blinking cursor
     * to disappear! This does not happen when using menu selection or
     * Ctrl-C/X/V
     */

    /**
     * Delegates to corresponding FileStatus method
     */
    public void updateStaticFileStatus() {
        this.fileStatus.updateStaticFileStatus();
    }

    /**
     * copy currently-selected text into clipboard
     */
    public void copyText() {
        this.sourceCode.copy();
    }

    /**
     * cut currently-selected text into clipboard
     */
    public void cutText() {
        this.sourceCode.cut();
    }

    /**
     * paste clipboard contents at cursor position
     */
    public void pasteText() {
        this.sourceCode.paste();
    }

    /**
     * Undo previous edit
     */
    public void undo() {
        this.sourceCode.undo();
    }

    /**
     * Redo previous edit
     */
    public void redo() {
        this.sourceCode.redo();
    }

    /**
     * Display cursor coordinates
     *
     * @param p
     *     Point object with x-y (column, line number) coordinates of cursor
     */
    public void displayCaretPosition(final @NotNull Pair<Integer, Integer> p) {
        this.caretPositionLabel.setText("Line: " + p.getFirst() + " Column: " + p.getSecond());
    }

    /**
     * Select the specified editor text line. Lines are numbered starting with 1,
     * consistent
     * with line numbers displayed by the editor.
     *
     * @param line
     *     The desired line number of this TextPane's text. Numbering
     *     starts at 1, and
     *     nothing will happen if the parameter value is less than 1
     */
    public void selectLine(final int line) {
        this.sourceCode.selectLine(line - 1);
    }

    /**
     * Finds next occurrence of text in a forward search of a string. Search begins
     * at the current cursor location, and wraps around when the end of the string
     * is reached.
     *
     * @param find
     *     the text to locate in the string
     * @param caseSensitive
     *     true if search is to be case-sensitive, false otherwise
     * @return TEXT_FOUND or TEXT_NOT_FOUND, depending on the result.
     */
    public @NotNull FindReplaceResult doFindText(final String find, final boolean caseSensitive) {
        return this.sourceCode.doFindText(find, caseSensitive);
    }

    /**
     * Finds and replaces next occurrence of text in a string in a forward search.
     * If cursor is initially at end
     * of matching selection, will immediately replace then find and select the
     * next occurrence if any. Otherwise it performs a find operation. The replace
     * can be undone with one undo operation.
     *
     * @param find
     *     the text to locate in the string
     * @param replace
     *     the text to replace the find text with - if the find
     *     text exists
     * @param caseSensitive
     *     true for case sensitive. false to ignore case
     * @return Returns TEXT_FOUND if not initially at end of selected match and
     * matching
     * occurrence is found. Returns TEXT_NOT_FOUND if the text is not
     * matched.
     * Returns TEXT_REPLACED_NOT_FOUND_NEXT if replacement is successful but
     * there are
     * no additional matches. Returns TEXT_REPLACED_FOUND_NEXT if
     * reaplacement is
     * successful and there is at least one additional match.
     */
    public @NotNull FindReplaceResult doReplace(final String find, final String replace, final boolean caseSensitive) {
        return this.sourceCode.doReplace(find, replace, caseSensitive);
    }

    public boolean canUndo() {
        return this.sourceCode.canUndo();
    }

    public boolean canRedo() {
        return this.sourceCode.canRedo();
    }

    /**
     * Finds and replaces <B>ALL</B> occurrences of text in a string in a forward
     * search.
     * All replacements are bundled into one CompoundEdit, so one Undo operation
     * will
     * undo all of them.
     *
     * @param find
     *     the text to locate in the string
     * @param replace
     *     the text to replace the find text with - if the find
     *     text exists
     * @param caseSensitive
     *     true for case sensitive. false to ignore case
     * @return the number of occurrences that were matched and replaced.
     */
    public int doReplaceAll(final String find, final String replace, final boolean caseSensitive) {
        return this.sourceCode.doReplaceAll(find, replace, caseSensitive);
    }
}
