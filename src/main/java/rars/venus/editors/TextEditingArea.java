package rars.venus.editors;

import org.jetbrains.annotations.NotNull;

import javax.swing.text.Document;
import javax.swing.undo.UndoManager;
import java.awt.*;

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
 * Specifies capabilities that any test editor used in MARS must have.
 *
 */
public interface TextEditingArea {

    /**
     * <p>copy.</p>
     */
    void copy();

    /**
     * <p>cut.</p>
     */
    void cut();

    /**
     * <p>doFindText.</p>
     *
     * @param find          a {@link java.lang.String} object
     * @param caseSensitive a boolean
     * @return a int
     */
    @NotNull FindReplaceResult doFindText(String find, boolean caseSensitive);

    /**
     * <p>doReplace.</p>
     *
     * @param find          a {@link java.lang.String} object
     * @param replace       a {@link java.lang.String} object
     * @param caseSensitive a boolean
     * @return a int
     */
    @NotNull FindReplaceResult doReplace(String find, String replace, boolean caseSensitive);

    /**
     * <p>doReplaceAll.</p>
     *
     * @param find          a {@link java.lang.String} object
     * @param replace       a {@link java.lang.String} object
     * @param caseSensitive a boolean
     * @return a int
     */
    int doReplaceAll(String find, String replace, boolean caseSensitive);

    /**
     * <p>getCaretPosition.</p>
     *
     * @return a int
     */
    int getCaretPosition();

    /**
     * <p>setCaretPosition.</p>
     *
     * @param position a int
     */
    void setCaretPosition(int position);

    /**
     * <p>getDocument.</p>
     *
     * @return a {@link javax.swing.text.Document} object
     */
    Document getDocument();

    /**
     * <p>getSelectedText.</p>
     *
     * @return a {@link java.lang.String} object
     */
    String getSelectedText();

    /**
     * <p>getSelectionEnd.</p>
     *
     * @return a int
     */
    int getSelectionEnd();

    /**
     * <p>setSelectionEnd.</p>
     *
     * @param pos a int
     */
    void setSelectionEnd(int pos);

    /**
     * <p>getSelectionStart.</p>
     *
     * @return a int
     */
    int getSelectionStart();

    /**
     * <p>setSelectionStart.</p>
     *
     * @param pos a int
     */
    void setSelectionStart(int pos);

    /**
     * <p>select.</p>
     *
     * @param selectionStart a int
     * @param selectionEnd   a int
     */
    void select(int selectionStart, int selectionEnd);

    /**
     * <p>selectAll.</p>
     */
    void selectAll();

    /**
     * <p>getText.</p>
     *
     * @return a {@link java.lang.String} object
     */
    String getText();

    /**
     * <p>setText.</p>
     *
     * @param text a {@link java.lang.String} object
     */
    void setText(String text);

    /**
     * <p>getUndoManager.</p>
     *
     * @return a {@link javax.swing.undo.UndoManager} object
     */
    UndoManager getUndoManager();

    /**
     * <p>paste.</p>
     */
    void paste();

    /**
     * <p>replaceSelection.</p>
     *
     * @param str a {@link java.lang.String} object
     */
    void replaceSelection(String str);

    /**
     * <p>setEditable.</p>
     *
     * @param editable a boolean
     */
    void setEditable(boolean editable);

    /**
     * <p>getFont.</p>
     *
     * @return a {@link java.awt.Font} object
     */
    Font getFont();

    /**
     * <p>setFont.</p>
     *
     * @param f a {@link java.awt.Font} object
     */
    void setFont(Font f);

    /**
     * <p>requestFocusInWindow.</p>
     *
     * @return a boolean
     */
    boolean requestFocusInWindow();

    /**
     * <p>getFontMetrics.</p>
     *
     * @param f a {@link java.awt.Font} object
     * @return a {@link java.awt.FontMetrics} object
     */
    FontMetrics getFontMetrics(Font f);

    /**
     * <p>setBackground.</p>
     *
     * @param c a {@link java.awt.Color} object
     */
    void setBackground(Color c);

    /**
     * <p>setEnabled.</p>
     *
     * @param enabled a boolean
     */
    void setEnabled(boolean enabled);

    /**
     * <p>grabFocus.</p>
     */
    void grabFocus();

    /**
     * <p>redo.</p>
     */
    void redo();

    /**
     * <p>revalidate.</p>
     */
    void revalidate();

    /**
     * <p>setSourceCode.</p>
     *
     * @param code     a {@link java.lang.String} object
     * @param editable a boolean
     */
    void setSourceCode(String code, boolean editable);

    /**
     * <p>setCaretVisible.</p>
     *
     * @param vis a boolean
     */
    void setCaretVisible(boolean vis);

    /**
     * <p>setSelectionVisible.</p>
     *
     * @param vis a boolean
     */
    void setSelectionVisible(boolean vis);

    /**
     * <p>undo.</p>
     */
    void undo();

    /**
     * <p>discardAllUndoableEdits.</p>
     */
    void discardAllUndoableEdits();

    /**
     * <p>setLineHighlightEnabled.</p>
     *
     * @param highlight a boolean
     */
    void setLineHighlightEnabled(boolean highlight);

    /**
     * <p>setCaretBlinkRate.</p>
     *
     * @param rate a int
     */
    void setCaretBlinkRate(int rate);

    /**
     * <p>setTabSize.</p>
     *
     * @param chars a int
     */
    void setTabSize(int chars);

    /**
     * <p>updateSyntaxStyles.</p>
     */
    void updateSyntaxStyles();

    /**
     * <p>getOuterComponent.</p>
     *
     * @return a {@link java.awt.Component} object
     */
    Component getOuterComponent();

    // Used by Find/Replace
    enum FindReplaceResult {
        TEXT_NOT_FOUND,
        TEXT_FOUND,
        TEXT_REPLACED_FOUND_NEXT,
        TEXT_REPLACED_NOT_FOUND_NEXT
    }
}
