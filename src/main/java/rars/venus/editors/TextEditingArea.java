package rars.venus.editors;

import org.jetbrains.annotations.NotNull;

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

    String getSelectedText();

    int getSelectionEnd();

    int getSelectionStart();

    void select(int selectionStart, int selectionEnd);

    void selectAll();

    String getText();

    void setText(String text);

    void paste();

    void replaceSelection(String str);

    void setEditable(boolean editable);
    
    void setFocusable(boolean focusable);

    Font getFont();

    void setFont(Font f);

    boolean requestFocusInWindow();

    FontMetrics getFontMetrics(Font f);

    void setForeground(Color c);
    
    void setBackground(Color c);
    
    void setSelectionColor(Color c);
    
    void setCaretColor(Color c);
    
    void setLineHighlightColor(Color c);
    
    void setEnabled(boolean enabled);
    
    void disableFully();

    void grabFocus();

    void redo();

    void revalidate();

    void setSourceCode(String code, boolean editable);

    void undo();

    void discardAllUndoableEdits();

    void setLineHighlightEnabled(boolean highlight);
    
    void setCaret(final @NotNull Caret caret);
    
    void setCaretBlinkRate(int rate);

    void setTabSize(int chars);

    Component getOuterComponent();

    boolean canUndo();

    boolean canRedo();

    int getCaretPosition();

    void setCaretPosition(int position);

    void requestFocus();

    @NotNull ColorScheme getColorScheme();

    void setColorScheme(final @NotNull ColorScheme colorScheme);

    void setTheme(final @NotNull Theme theme);
    
    @NotNull Theme getTheme();

    // Used by Find/Replace
    enum FindReplaceResult {
        TEXT_NOT_FOUND,
        TEXT_FOUND,
        TEXT_REPLACED_FOUND_NEXT,
        TEXT_REPLACED_NOT_FOUND_NEXT
    }
}
