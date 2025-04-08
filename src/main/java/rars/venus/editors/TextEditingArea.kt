package rars.venus.editors;

import kotlin.Pair;
import org.jetbrains.annotations.NotNull;
import rars.riscv.lang.lexing.RVTokenType;

import javax.swing.text.Caret;
import javax.swing.text.Document;
import java.awt.*;

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

    int getCaretBlinkRate();

    void setCaretBlinkRate(int rate);

    int getTabSize();

    void setTabSize(int chars);

    @NotNull Pair<@NotNull Integer, @NotNull Integer> getCaretPosition();

    Component getOuterComponent();

    boolean canUndo();

    boolean canRedo();

    @NotNull EditorTheme getTheme();

    void setTheme(final @NotNull EditorTheme theme);

    void setTokenStyle(final @NotNull RVTokenType type, final @NotNull TokenStyle style);

    // Used by Find/Replace
    enum FindReplaceResult {
        TEXT_NOT_FOUND,
        TEXT_FOUND,
        TEXT_REPLACED_FOUND_NEXT,
        TEXT_REPLACED_NOT_FOUND_NEXT
    }
}
