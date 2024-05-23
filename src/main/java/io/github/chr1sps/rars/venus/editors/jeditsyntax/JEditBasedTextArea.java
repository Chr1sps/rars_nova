package io.github.chr1sps.rars.venus.editors.jeditsyntax;

import javax.swing.*;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.CompoundEdit;
import javax.swing.undo.UndoManager;

import io.github.chr1sps.rars.Globals;
import io.github.chr1sps.rars.Settings;
import io.github.chr1sps.rars.venus.EditPane;
import io.github.chr1sps.rars.venus.editors.TextEditingArea;
import io.github.chr1sps.rars.venus.editors.jeditsyntax.tokenmarker.RISCVTokenMarker;

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

    private final JComponent lineNumbers;
    private final EditPane editPane;
    private final UndoManager undoManager;
    private final UndoableEditListener undoableEditListener;
    private boolean isCompoundEdit = false;
    private CompoundEdit compoundEdit;
    private final JEditBasedTextArea sourceCode;

    /**
     * <p>Constructor for JEditBasedTextArea.</p>
     *
     * @param editPain    a {@link io.github.chr1sps.rars.venus.EditPane} object
     * @param lineNumbers a {@link javax.swing.JComponent} object
     */
    public JEditBasedTextArea(EditPane editPain, JComponent lineNumbers) {
        super(lineNumbers);
        this.lineNumbers = lineNumbers;
        this.editPane = editPain;
        this.undoManager = new UndoManager();
        this.compoundEdit = new CompoundEdit();
        this.sourceCode = this;

        // Needed to support unlimited undo/redo capability
        undoableEditListener = new UndoableEditListener() {
            public void undoableEditHappened(UndoableEditEvent e) {
                // Remember the edit and update the menus.
                if (isCompoundEdit) {
                    compoundEdit.addEdit(e.getEdit());
                } else {
                    undoManager.addEdit(e.getEdit());
                    editPane.updateUndoAndRedoState();
                }
            }
        };
        this.getDocument().addUndoableEditListener(undoableEditListener);
        this.setFont(Globals.getSettings().getEditorFont());
        this.setTokenMarker(new RISCVTokenMarker());

        addCaretListener(this);
    }

    /**
     * {@inheritDoc}
     */
    public void setFont(Font f) {
        getPainter().setFont(f);
    }

    /**
     * <p>getFont.</p>
     *
     * @return a {@link java.awt.Font} object
     */
    public Font getFont() {
        return getPainter().getFont();
    }

    // public void repaint() { getPainter().repaint(); }
    // public Dimension getSize() { return painter.getSize(); }
    // public void setSize(Dimension d) { painter.setSize(d);}

    /**
     * {@inheritDoc}
     * <p>
     * Use for highlighting the line currently being edited.
     */
    public void setLineHighlightEnabled(boolean highlight) {
        getPainter().setLineHighlightEnabled(highlight);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Set the caret blinking rate in milliseconds. If rate is 0
     * will disable blinking. If negative, do nothing.
     */
    public void setCaretBlinkRate(int rate) {
        if (rate == 0) {
            caretBlinks = false;
        }
        if (rate > 0) {
            caretBlinks = true;
            caretBlinkRate = rate;
            caretTimer.setDelay(rate);
            caretTimer.setInitialDelay(rate);
            caretTimer.restart();
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Set the number of characters a tab will expand to.
     */
    public void setTabSize(int chars) {
        painter.setTabSize(chars);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setBackground(Color bg) {
        super.setBackground(bg);
        getPainter().setBackground(bg);
        lineNumbers.setOpaque(true);
        lineNumbers.setBackground(bg);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setForeground(Color fg) {
        super.setForeground(fg);
        getPainter().setForeground(fg);
        lineNumbers.setForeground(fg);
    }

    /**
     * Update editor-colors based on the information
     * from {@link io.github.chr1sps.rars.Globals#getSettings()}
     */
    public void updateEditorColors() {
        boolean editable = this.isEditable();
        Settings settings = Globals.getSettings();
        Color background = settings.getColorSettingByPosition(Settings.EDITOR_BACKGROUND);
        Color foreground = settings.getColorSettingByPosition(Settings.EDITOR_FOREGROUND);
        this.setBackground((editable) ? background : background.darker());
        this.getPainter().setLineHighlightColor(settings.getColorSettingByPosition(Settings.EDITOR_LINE_HIGHLIGHT));
        this.getPainter().setSelectionColor(settings.getColorSettingByPosition(Settings.EDITOR_SELECTION_COLOR));
        this.getPainter().setCaretColor(settings.getColorSettingByPosition(Settings.EDITOR_CARET_COLOR));
        this.setForeground((editable) ? foreground : foreground.darker());
    }

    /**
     * Update editor colors and update the syntax style table,
     * which is obtained from {@link io.github.chr1sps.rars.venus.editors.jeditsyntax.SyntaxUtilities}.
     */
    public void updateSyntaxStyles() {
        updateEditorColors();
        painter.setStyles(SyntaxUtilities.getCurrentSyntaxStyles());
    }

    /**
     * <p>getOuterComponent.</p>
     *
     * @return a {@link java.awt.Component} object
     */
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
    public void discardAllUndoableEdits() {
        this.undoManager.discardAllEdits();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Display caret position on the edit pane.
     */
    public void caretUpdate(CaretEvent e) {
        editPane.displayCaretPosition(e.getDot());
    }

    /**
     * {@inheritDoc}
     * <p>
     * Same as setSelectedText but named for compatibility with
     * JTextComponent method replaceSelection.
     * DPS, 14 Apr 2010
     */
    public void replaceSelection(String replacementText) {
        setSelectedText(replacementText);
    }

    //
    //

    /**
     * {@inheritDoc}
     */
    public void setSelectionVisible(boolean vis) {

    }

    //
    //

    /**
     * {@inheritDoc}
     */
    public void setSourceCode(String s, boolean editable) {
        this.setText(s);
        this.setEditable(editable);
        this.setEnabled(editable);
        // this.getCaret().setVisible(editable);
        this.setCaretPosition(0);
        updateEditorColors();
        if (editable)
            this.requestFocusInWindow();
    }

    /**
     * Returns the undo manager for this editing area
     *
     * @return the undo manager
     */
    public UndoManager getUndoManager() {
        return undoManager;
    }

    /**
     * Undo previous edit
     */
    public void undo() {
        // "unredoing" is mode used by DocumentHandler's insertUpdate() and
        // removeUpdate()
        // to pleasingly mark the text and location of the undo.
        unredoing = true;
        try {
            this.undoManager.undo();
        } catch (CannotUndoException ex) {
            System.out.println("Unable to undo: " + ex);
            ex.printStackTrace();
        }
        unredoing = false;
        this.setCaretVisible(true);
    }

    /**
     * Redo previous edit
     */
    public void redo() {
        // "unredoing" is mode used by DocumentHandler's insertUpdate() and
        // removeUpdate()
        // to pleasingly mark the text and location of the redo.
        unredoing = true;
        try {
            this.undoManager.redo();
        } catch (CannotRedoException ex) {
            System.out.println("Unable to redo: " + ex);
            ex.printStackTrace();
        }
        unredoing = false;
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
     * {@inheritDoc}
     * <p>
     * Finds next occurrence of text in a forward search of a string. Search begins
     * at the current cursor location, and wraps around when the end of the string
     * is reached.
     */
    public int doFindText(String find, boolean caseSensitive) {
        int findPosn = sourceCode.getCaretPosition();
        int nextPosn = 0;
        nextPosn = nextIndex(sourceCode.getText(), find, findPosn, caseSensitive);
        if (nextPosn >= 0) {
            sourceCode.requestFocus(); // guarantees visibility of the blue highlight
            sourceCode.setSelectionStart(nextPosn); // position cursor at word start
            sourceCode.setSelectionEnd(nextPosn + find.length());
            // Need to repeat start due to quirk in JEditTextArea implementation of
            // setSelectionStart.
            sourceCode.setSelectionStart(nextPosn);
            return TEXT_FOUND;
        } else {
            return TEXT_NOT_FOUND;
        }
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
    public int nextIndex(String input, String find, int start, boolean caseSensitive) {
        int textPosn = -1;
        if (input != null && find != null && start < input.length()) {
            if (caseSensitive) { // indexOf() returns -1 if not found
                textPosn = input.indexOf(find, start);
                // If not found from non-starting cursor position, wrap around
                if (start > 0 && textPosn < 0) {
                    textPosn = input.indexOf(find);
                }
            } else {
                String lowerCaseText = input.toLowerCase();
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
     * {@inheritDoc}
     * <p>
     * Finds and replaces next occurrence of text in a string in a forward search.
     * If cursor is initially at end
     * of matching selection, will immediately replace then find and select the
     * next occurrence if any. Otherwise it performs a find operation. The replace
     * can be undone with one undo operation.
     */
    public int doReplace(String find, String replace, boolean caseSensitive) {
        int nextPosn = 0;
        int posn;
        // Will perform a "find" and return, unless positioned at the end of
        // a selected "find" result.
        if (find == null || !find.equals(sourceCode.getSelectedText()) ||
                sourceCode.getSelectionEnd() != sourceCode.getCaretPosition()) {
            return doFindText(find, caseSensitive);
        }
        // We are positioned at end of selected "find". Rreplace and find next.
        nextPosn = sourceCode.getSelectionStart();
        sourceCode.grabFocus();
        sourceCode.setSelectionStart(nextPosn); // posn cursor at word start
        sourceCode.setSelectionEnd(nextPosn + find.length()); // select found text
        // Need to repeat start due to quirk in JEditTextArea implementation of
        // setSelectionStart.
        sourceCode.setSelectionStart(nextPosn);
        isCompoundEdit = true;
        compoundEdit = new CompoundEdit();
        sourceCode.replaceSelection(replace);
        compoundEdit.end();
        undoManager.addEdit(compoundEdit);
        editPane.updateUndoAndRedoState();
        isCompoundEdit = false;
        sourceCode.setCaretPosition(nextPosn + replace.length());
        if (doFindText(find, caseSensitive) == TEXT_NOT_FOUND) {
            return TEXT_REPLACED_NOT_FOUND_NEXT;
        } else {
            return TEXT_REPLACED_FOUND_NEXT;
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
    public int doReplaceAll(String find, String replace, boolean caseSensitive) {
        int nextPosn = 0;
        int findPosn = 0; // *** begin at start of text
        int replaceCount = 0;
        compoundEdit = null; // new one will be created upon first replacement
        isCompoundEdit = true; // undo manager's action listener needs this
        while (nextPosn >= 0) {
            nextPosn = nextIndex(sourceCode.getText(), find, findPosn, caseSensitive);
            if (nextPosn >= 0) {
                // nextIndex() will wrap around, which causes infinite loop if
                // find string is a substring of replacement string. This
                // statement will prevent that.
                if (nextPosn < findPosn) {
                    break;
                }
                sourceCode.grabFocus();
                sourceCode.setSelectionStart(nextPosn); // posn cursor at word start
                sourceCode.setSelectionEnd(nextPosn + find.length()); // select found text
                // Need to repeat start due to quirk in JEditTextArea implementation of
                // setSelectionStart.
                sourceCode.setSelectionStart(nextPosn);
                if (compoundEdit == null) {
                    compoundEdit = new CompoundEdit();
                }
                sourceCode.replaceSelection(replace);
                findPosn = nextPosn + replace.length(); // set for next search
                replaceCount++;
            }
        }
        isCompoundEdit = false;
        // Will be true if any replacements were performed
        if (compoundEdit != null) {
            compoundEdit.end();
            undoManager.addEdit(compoundEdit);
            editPane.updateUndoAndRedoState();
        }
        return replaceCount;
    }
    //
    ///////////////////////////// End Find/Replace methods
    // //////////////////////////

    //
    //////////////////////////////////////////////////////////////////

}
