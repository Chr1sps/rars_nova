package io.github.chr1sps.rars.venus.editors;

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
 * @author chrisps
 * @version $Id: $Id
 */
public interface TextEditingArea {

    // Used by Find/Replace
    /**
     * Constant <code>TEXT_NOT_FOUND=0</code>
     */
    public static final int TEXT_NOT_FOUND = 0;
    /**
     * Constant <code>TEXT_FOUND=1</code>
     */
    public static final int TEXT_FOUND = 1;
    /**
     * Constant <code>TEXT_REPLACED_FOUND_NEXT=2</code>
     */
    public static final int TEXT_REPLACED_FOUND_NEXT = 2;
    /**
     * Constant <code>TEXT_REPLACED_NOT_FOUND_NEXT=3</code>
     */
    public static final int TEXT_REPLACED_NOT_FOUND_NEXT = 3;

    /**
     * <p>copy.</p>
     */
    public void copy();

    /**
     * <p>cut.</p>
     */
    public void cut();

    /**
     * <p>doFindText.</p>
     *
     * @param find          a {@link java.lang.String} object
     * @param caseSensitive a boolean
     * @return a int
     */
    public int doFindText(String find, boolean caseSensitive);

    /**
     * <p>doReplace.</p>
     *
     * @param find          a {@link java.lang.String} object
     * @param replace       a {@link java.lang.String} object
     * @param caseSensitive a boolean
     * @return a int
     */
    public int doReplace(String find, String replace, boolean caseSensitive);

    /**
     * <p>doReplaceAll.</p>
     *
     * @param find          a {@link java.lang.String} object
     * @param replace       a {@link java.lang.String} object
     * @param caseSensitive a boolean
     * @return a int
     */
    public int doReplaceAll(String find, String replace, boolean caseSensitive);

    /**
     * <p>getCaretPosition.</p>
     *
     * @return a int
     */
    public int getCaretPosition();

    /**
     * <p>getDocument.</p>
     *
     * @return a {@link javax.swing.text.Document} object
     */
    public Document getDocument();

    /**
     * <p>getSelectedText.</p>
     *
     * @return a {@link java.lang.String} object
     */
    public String getSelectedText();

    /**
     * <p>getSelectionEnd.</p>
     *
     * @return a int
     */
    public int getSelectionEnd();

    /**
     * <p>getSelectionStart.</p>
     *
     * @return a int
     */
    public int getSelectionStart();

    /**
     * <p>select.</p>
     *
     * @param selectionStart a int
     * @param selectionEnd   a int
     */
    public void select(int selectionStart, int selectionEnd);

    /**
     * <p>selectAll.</p>
     */
    public void selectAll();

    /**
     * <p>getText.</p>
     *
     * @return a {@link java.lang.String} object
     */
    public String getText();

    /**
     * <p>getUndoManager.</p>
     *
     * @return a {@link javax.swing.undo.UndoManager} object
     */
    public UndoManager getUndoManager();

    /**
     * <p>paste.</p>
     */
    public void paste();

    /**
     * <p>replaceSelection.</p>
     *
     * @param str a {@link java.lang.String} object
     */
    public void replaceSelection(String str);

    /**
     * <p>setCaretPosition.</p>
     *
     * @param position a int
     */
    public void setCaretPosition(int position);

    /**
     * <p>setEditable.</p>
     *
     * @param editable a boolean
     */
    public void setEditable(boolean editable);

    /**
     * <p>setSelectionEnd.</p>
     *
     * @param pos a int
     */
    public void setSelectionEnd(int pos);

    /**
     * <p>setSelectionStart.</p>
     *
     * @param pos a int
     */
    public void setSelectionStart(int pos);

    /**
     * <p>setText.</p>
     *
     * @param text a {@link java.lang.String} object
     */
    public void setText(String text);

    /**
     * <p>setFont.</p>
     *
     * @param f a {@link java.awt.Font} object
     */
    public void setFont(Font f);

    /**
     * <p>getFont.</p>
     *
     * @return a {@link java.awt.Font} object
     */
    public Font getFont();

    /**
     * <p>requestFocusInWindow.</p>
     *
     * @return a boolean
     */
    public boolean requestFocusInWindow();

    /**
     * <p>getFontMetrics.</p>
     *
     * @param f a {@link java.awt.Font} object
     * @return a {@link java.awt.FontMetrics} object
     */
    public FontMetrics getFontMetrics(Font f);

    /**
     * <p>setBackground.</p>
     *
     * @param c a {@link java.awt.Color} object
     */
    public void setBackground(Color c);

    /**
     * <p>setEnabled.</p>
     *
     * @param enabled a boolean
     */
    public void setEnabled(boolean enabled);

    /**
     * <p>grabFocus.</p>
     */
    public void grabFocus();

    /**
     * <p>redo.</p>
     */
    public void redo();

    /**
     * <p>revalidate.</p>
     */
    public void revalidate();

    /**
     * <p>setSourceCode.</p>
     *
     * @param code     a {@link java.lang.String} object
     * @param editable a boolean
     */
    public void setSourceCode(String code, boolean editable);

    /**
     * <p>setCaretVisible.</p>
     *
     * @param vis a boolean
     */
    public void setCaretVisible(boolean vis);

    /**
     * <p>setSelectionVisible.</p>
     *
     * @param vis a boolean
     */
    public void setSelectionVisible(boolean vis);

    /**
     * <p>undo.</p>
     */
    public void undo();

    /**
     * <p>discardAllUndoableEdits.</p>
     */
    public void discardAllUndoableEdits();

    /**
     * <p>setLineHighlightEnabled.</p>
     *
     * @param highlight a boolean
     */
    public void setLineHighlightEnabled(boolean highlight);

    /**
     * <p>setCaretBlinkRate.</p>
     *
     * @param rate a int
     */
    public void setCaretBlinkRate(int rate);

    /**
     * <p>setTabSize.</p>
     *
     * @param chars a int
     */
    public void setTabSize(int chars);

    /**
     * <p>updateSyntaxStyles.</p>
     */
    public void updateSyntaxStyles();

    /**
     * <p>getOuterComponent.</p>
     *
     * @return a {@link java.awt.Component} object
     */
    public Component getOuterComponent();
}
