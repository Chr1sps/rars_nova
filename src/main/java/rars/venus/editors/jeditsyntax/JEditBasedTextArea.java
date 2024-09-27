package rars.venus.editors.jeditsyntax;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import rars.Globals;
import rars.Settings;
import rars.venus.EditPane;
import rars.venus.editors.TextEditingArea;
import rars.venus.editors.jeditsyntax.tokenmarker.RISCVTokenMarker;

import javax.swing.*;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.UndoableEditListener;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.CompoundEdit;
import javax.swing.undo.UndoManager;
import java.awt.*;

/**
 * Adaptor subclass for JEditTextArea
 * <p>
 * Provides those methods required by the TextEditingArea interface
 * that are not defined by JEditTextArea. This permits JEditTextArea
 * to be used within MARS largely without modification. DPS 4-20-2010
 *
 * @author Pete Sanderson
 * @since 4.0
 */
public class JEditBasedTextArea extends JEditTextArea implements TextEditingArea, CaretListener {
    private final static Logger LOGGER = LogManager.getLogger(JEditBasedTextArea.class);

    private final JComponent lineNumbers;
    private final EditPane editPane;
    private final UndoManager undoManager;
    private final JEditBasedTextArea sourceCode;
    private boolean isCompoundEdit = false;
    private CompoundEdit compoundEdit;

    /**
     * <p>Constructor for JEditBasedTextArea.</p>
     *
     * @param editPain    a {@link EditPane} object
     * @param lineNumbers a {@link javax.swing.JComponent} object
     */
    public JEditBasedTextArea(final EditPane editPain, final JComponent lineNumbers) {
        super(lineNumbers);
        this.lineNumbers = lineNumbers;
        this.editPane = editPain;
        this.undoManager = new UndoManager();
        this.compoundEdit = new CompoundEdit();
        this.sourceCode = this;

        // Needed to support unlimited undo/redo capability
        // Remember the edit and update the menus.
        final UndoableEditListener undoableEditListener = e -> {
            // Remember the edit and update the menus.
            if (this.isCompoundEdit) {
                this.compoundEdit.addEdit(e.getEdit());
            } else {
                this.undoManager.addEdit(e.getEdit());
                this.editPane.updateUndoAndRedoState();
            }
        };
        this.getDocument().addUndoableEditListener(undoableEditListener);
        this.setFont(Globals.getSettings().getEditorFont());
        this.setTokenMarker(new RISCVTokenMarker());

        this.addCaretListener(this);
    }

    /**
     * Returns next posn of word in text - forward search. If end of string is
     * reached during the search, will wrap around to the beginning one time.
     *
     * @param input         the string to search
     * @param find          the string to find
     * @param start         the character position to start the search
     * @param caseSensitive true for case sensitive. false to ignore case
     * @return next indexed position of found text or -1 if not found
     */
    public static int nextIndex(final String input, final String find, final int start, final boolean caseSensitive) {
        int textPosn = -1;
        if (input != null && find != null && start < input.length()) {
            if (caseSensitive) { // indexOf() returns -1 if not found
                textPosn = input.indexOf(find, start);
                // If not found from non-starting cursor position, wrap around
                if (start > 0 && textPosn < 0) {
                    textPosn = input.indexOf(find);
                }
            } else {
                final String lowerCaseText = input.toLowerCase();
                textPosn = lowerCaseText.indexOf(find.toLowerCase(), start);
                // If not found from non-starting cursor position, wrap around
                if (start > 0 && textPosn < 0) {
                    textPosn = lowerCaseText.indexOf(find.toLowerCase());
                }
            }
        }
        return textPosn;
    }

    /**
     * <p>getFont.</p>
     *
     * @return a {@link java.awt.Font} object
     */
    @Override
    public Font getFont() {
        return this.getPainter().getFont();
    }

    // public void repaint() { getPainter().repaint(); }
    // public Dimension getSize() { return painter.getSize(); }
    // public void setSize(Dimension d) { painter.setSize(d);}

    /**
     * {@inheritDoc}
     */
    @Override
    public void setFont(final Font f) {
        this.getPainter().setFont(f);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Use for highlighting the line currently being edited.
     */
    @Override
    public void setLineHighlightEnabled(final boolean highlight) {
        this.getPainter().setLineHighlightEnabled(highlight);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Set the caret blinking rate in milliseconds. If rate is 0
     * will disable blinking. If negative, do nothing.
     */
    @Override
    public void setCaretBlinkRate(final int rate) {
        if (rate == 0) {
            this.caretBlinks = false;
        }
        if (rate > 0) {
            this.caretBlinks = true;
            this.caretBlinkRate = rate;
            JEditTextArea.caretTimer.setDelay(rate);
            JEditTextArea.caretTimer.setInitialDelay(rate);
            JEditTextArea.caretTimer.restart();
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Set the number of characters a tab will expand to.
     */
    @Override
    public void setTabSize(final int chars) {
        this.painter.setTabSize(chars);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setBackground(final Color bg) {
        super.setBackground(bg);
        this.getPainter().setBackground(bg);
        this.lineNumbers.setOpaque(true);
        this.lineNumbers.setBackground(bg);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setForeground(final Color fg) {
        super.setForeground(fg);
        this.getPainter().setForeground(fg);
        this.lineNumbers.setForeground(fg);
    }

    /**
     * Update editor-colors based on the information
     * from {@link Globals#getSettings()}
     */
    public void updateEditorColors() {
        final boolean editable = this.isEditable();
        final Settings settings = Globals.getSettings();
        final Color background = settings.getColorSettingByPosition(Settings.EDITOR_BACKGROUND);
        final Color foreground = settings.getColorSettingByPosition(Settings.EDITOR_FOREGROUND);
        this.setBackground((editable) ? background : background.darker());
        this.getPainter().setLineHighlightColor(settings.getColorSettingByPosition(Settings.EDITOR_LINE_HIGHLIGHT));
        this.getPainter().setSelectionColor(settings.getColorSettingByPosition(Settings.EDITOR_SELECTION_COLOR));
        this.getPainter().setCaretColor(settings.getColorSettingByPosition(Settings.EDITOR_CARET_COLOR));
        this.setForeground((editable) ? foreground : foreground.darker());
    }

    /**
     * Update editor colors and update the syntax style table,
     * which is obtained from {@link SyntaxUtilities}.
     */
    @Override
    public void updateSyntaxStyles() {
        this.updateEditorColors();
        this.painter.setStyles(SyntaxUtilities.getCurrentSyntaxStyles());
    }

    /**
     * <p>getOuterComponent.</p>
     *
     * @return a {@link java.awt.Component} object
     */
    @Override
    public Component getOuterComponent() {
        return this;
    }

    /**
     * Get rid of any accumulated undoable edits. It is useful to call
     * this method after opening a file into the text area. The
     * act of setting its text content upon reading the file will generate
     * an undoable edit. Normally you don't want a freshly-opened file
     * to appear with its Undo action enabled. But it will unless you
     * call this after setting the text.
     */
    @Override
    public void discardAllUndoableEdits() {
        this.undoManager.discardAllEdits();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Display caret position on the edit pane.
     */
    @Override
    public void caretUpdate(final CaretEvent e) {
        this.editPane.displayCaretPosition(e.getDot());
    }

    //
    //

    /**
     * {@inheritDoc}
     * <p>
     * Same as setSelectedText but named for compatibility with
     * JTextComponent method replaceSelection.
     * DPS, 14 Apr 2010
     */
    @Override
    public void replaceSelection(final String replacementText) {
        this.setSelectedText(replacementText);
    }

    //
    //

    /**
     * {@inheritDoc}
     */
    @Override
    public void setSelectionVisible(final boolean vis) {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setSourceCode(final String s, final boolean editable) {
        this.setText(s);
        this.setEditable(editable);
        this.setEnabled(editable);
        // this.getCaret().setVisible(editable);
        this.setCaretPosition(0);
        this.updateEditorColors();
        if (editable)
            this.requestFocusInWindow();
    }

    /**
     * Returns the undo manager for this editing area
     *
     * @return the undo manager
     */
    @Override
    public UndoManager getUndoManager() {
        return this.undoManager;
    }

    /**
     * Undo previous edit
     */
    @Override
    public void undo() {
        // "unredoing" is mode used by DocumentHandler's insertUpdate() and
        // removeUpdate()
        // to pleasingly mark the text and location of the undo.
        this.unredoing = true;
        try {
            this.undoManager.undo();
        } catch (final CannotUndoException ex) {
            JEditBasedTextArea.LOGGER.error("Unable to undo: {}", ex.getMessage(), ex);
        }
        this.unredoing = false;
        this.setCaretVisible(true);
    }

    //////////////////////////////////////////////////////////////////////////
    // Methods to support Find/Replace feature
    //
    // Basis for this Find/Replace solution is:
    // http://java.ittoolbox.com/groups/technical-functional/java-l/search-and-replace-using-jtextpane-630964
    // as written by Chris Dickenson in 2005
    //

    /**
     * Redo previous edit
     */
    @Override
    public void redo() {
        // "unredoing" is mode used by DocumentHandler's insertUpdate() and
        // removeUpdate()
        // to pleasingly mark the text and location of the redo.
        this.unredoing = true;
        try {
            this.undoManager.redo();
        } catch (final CannotRedoException ex) {
            JEditBasedTextArea.LOGGER.error("Unable to redo: {}", ex.getMessage(), ex);
        }
        this.unredoing = false;
        this.setCaretVisible(true);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Finds next occurrence of text in a forward search of a string. Search begins
     * at the current cursor location, and wraps around when the end of the string
     * is reached.
     */
    @Override
    public @NotNull FindReplaceResult doFindText(final String find, final boolean caseSensitive) {
        final int findPosn = this.sourceCode.getCaretPosition();
        final int nextPosn;
        nextPosn = JEditBasedTextArea.nextIndex(this.sourceCode.getText(), find, findPosn, caseSensitive);
        if (nextPosn >= 0) {
            this.sourceCode.requestFocus(); // guarantees visibility of the blue highlight
            this.sourceCode.setSelectionStart(nextPosn); // position cursor at word start
            this.sourceCode.setSelectionEnd(nextPosn + find.length());
            // Need to repeat start due to quirk in JEditTextArea implementation of
            // setSelectionStart.
            this.sourceCode.setSelectionStart(nextPosn);
            return FindReplaceResult.TEXT_FOUND;
        } else {
            return FindReplaceResult.TEXT_NOT_FOUND;
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Finds and replaces next occurrence of text in a string in a forward search.
     * If cursor is initially at end
     * of matching selection, will immediately replace then find and select the
     * next occurrence if any. Otherwise it performs a find operation. The replace
     * can be undone with one undo operation.
     */
    @Override
    public @NotNull FindReplaceResult doReplace(final String find, final String replace, final boolean caseSensitive) {
        final int nextPosn;
        int posn;
        // Will perform a "find" and return, unless positioned at the end of
        // a selected "find" result.
        if (find == null || !find.equals(this.sourceCode.getSelectedText()) ||
                this.sourceCode.getSelectionEnd() != this.sourceCode.getCaretPosition()) {
            return this.doFindText(find, caseSensitive);
        }
        // We are positioned at end of selected "find". Rreplace and find next.
        nextPosn = this.sourceCode.getSelectionStart();
        this.sourceCode.grabFocus();
        this.sourceCode.setSelectionStart(nextPosn); // posn cursor at word start
        this.sourceCode.setSelectionEnd(nextPosn + find.length()); // select found text
        // Need to repeat start due to quirk in JEditTextArea implementation of
        // setSelectionStart.
        this.sourceCode.setSelectionStart(nextPosn);
        this.isCompoundEdit = true;
        this.compoundEdit = new CompoundEdit();
        this.sourceCode.replaceSelection(replace);
        this.compoundEdit.end();
        this.undoManager.addEdit(this.compoundEdit);
        this.editPane.updateUndoAndRedoState();
        this.isCompoundEdit = false;
        this.sourceCode.setCaretPosition(nextPosn + replace.length());
        if (this.doFindText(find, caseSensitive) == FindReplaceResult.TEXT_NOT_FOUND) {
            return FindReplaceResult.TEXT_REPLACED_NOT_FOUND_NEXT;
        } else {
            return FindReplaceResult.TEXT_REPLACED_FOUND_NEXT;
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Finds and replaces <B>ALL</B> occurrences of text in a string in a forward
     * search.
     * All replacements are bundled into one CompoundEdit, so one Undo operation
     * will
     * undo all of them.
     */
    @Override
    public int doReplaceAll(final String find, final String replace, final boolean caseSensitive) {
        int nextPosn = 0;
        int findPosn = 0; // *** begin at start of text
        int replaceCount = 0;
        this.compoundEdit = null; // new one will be created upon first replacement
        this.isCompoundEdit = true; // undo manager's action listener needs this
        while (nextPosn >= 0) {
            nextPosn = JEditBasedTextArea.nextIndex(this.sourceCode.getText(), find, findPosn, caseSensitive);
            if (nextPosn >= 0) {
                // nextIndex() will wrap around, which causes infinite loop if
                // find string is a substring of replacement string. This
                // statement will prevent that.
                if (nextPosn < findPosn) {
                    break;
                }
                this.sourceCode.grabFocus();
                this.sourceCode.setSelectionStart(nextPosn); // posn cursor at word start
                this.sourceCode.setSelectionEnd(nextPosn + find.length()); // select found text
                // Need to repeat start due to quirk in JEditTextArea implementation of
                // setSelectionStart.
                this.sourceCode.setSelectionStart(nextPosn);
                if (this.compoundEdit == null) {
                    this.compoundEdit = new CompoundEdit();
                }
                this.sourceCode.replaceSelection(replace);
                findPosn = nextPosn + replace.length(); // set for next search
                replaceCount++;
            }
        }
        this.isCompoundEdit = false;
        // Will be true if any replacements were performed
        if (this.compoundEdit != null) {
            this.compoundEdit.end();
            this.undoManager.addEdit(this.compoundEdit);
            this.editPane.updateUndoAndRedoState();
        }
        return replaceCount;
    }
    //
    ///////////////////////////// End Find/Replace methods
    // //////////////////////////

    //
    //////////////////////////////////////////////////////////////////

}
