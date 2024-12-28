package rars.venus.editors;

import org.jetbrains.annotations.NotNull;
import rars.venus.editors.rsyntaxtextarea.RSyntaxTextAreaBasedEditor;

public final class TextEditingAreaFactory {
    private TextEditingAreaFactory() {
    }

    public static @NotNull TextEditingArea createTextEditingArea(final @NotNull EditorTheme theme) {
        return new RSyntaxTextAreaBasedEditor(theme);
    }
}
