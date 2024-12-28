package rars.venus.editors;

import org.jetbrains.annotations.NotNull;
import rars.riscv.lang.lexing.RVTokenType;
import rars.util.Pair;

import javax.swing.text.Caret;
import javax.swing.text.Document;
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
 */
public interface TextEditingArea {

    void copy();

    void cut();

    @NotNull FindReplaceResult doFindText(String find, boolean caseSensitive);

    @NotNull FindReplaceResult doReplace(String find, String replace, boolean caseSensitive);

    int doReplaceAll(String find, String replace, boolean caseSensitive);

    Document getDocument();

    void select(int selectionStart, int selectionEnd);

    void selectLine(int lineNumber);

    String getText();

    void setText(String text);

    void paste();

    void setEditable(boolean editable);

    Font getFont();

    void setFont(Font f);

    void requestFocusInWindow();

    void setForeground(Color c);

    void setBackground(Color c);

    void setSelectionColor(Color c);

    void setCaretColor(Color c);

    void setLineHighlightColor(Color c);

    @NotNull Caret getCaret();

    void setEnabled(boolean enabled);

    void redo();

    void setSourceCode(String code, boolean editable);

    void undo();

    void discardAllUndoableEdits();

    void setLineHighlightEnabled(boolean highlight);

    void setCaretBlinkRate(int rate);

    void setTabSize(int chars);

    @NotNull Pair<Integer, Integer> getCaretPosition();

    Component getOuterComponent();

    boolean canUndo();

    boolean canRedo();

    @NotNull EditorTheme getTheme();

    void setTokenStyle(final @NotNull RVTokenType type, final @NotNull TokenStyle style);
    
    void setTheme(final @NotNull EditorTheme theme);

    // Used by Find/Replace
    enum FindReplaceResult {
        TEXT_NOT_FOUND,
        TEXT_FOUND,
        TEXT_REPLACED_FOUND_NEXT,
        TEXT_REPLACED_NOT_FOUND_NEXT
    }
}
