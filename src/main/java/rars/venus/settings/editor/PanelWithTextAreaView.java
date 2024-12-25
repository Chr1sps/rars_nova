package rars.venus.settings.editor;

import org.jetbrains.annotations.NotNull;
import rars.venus.editors.TextEditingArea;
import rars.venus.editors.TextEditingAreaFactory;
import rars.venus.editors.Theme;

import javax.swing.*;
import java.awt.*;

import static rars.settings.Settings.*;

public final class PanelWithTextAreaView extends JPanel {
    public final @NotNull PickerCardView upperComponent;
    private final @NotNull TextEditingArea textArea;

    public PanelWithTextAreaView(final @NotNull PickerCardView pickerCardView) {
        super();
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.upperComponent = pickerCardView;
        this.add(pickerCardView);
        this.add(Box.createVerticalGlue());
        this.textArea = createTextArea(EDITOR_THEME_SETTINGS.getCurrentTheme()); // TODO: default theme
        final var outerComponent = this.textArea.getOuterComponent();
        final var textAreaSize = new Dimension(500, 300);
        outerComponent.setMinimumSize(textAreaSize);
        outerComponent.setPreferredSize(textAreaSize);
        outerComponent.setMaximumSize(textAreaSize);
        this.add(outerComponent);
    }

    private static @NotNull TextEditingArea createTextArea(final @NotNull Theme theme) {
        final var result = TextEditingAreaFactory.createTextEditingArea(theme); // TODO: default theme
        final var exampleText = """
            # Some macro definitions to print strings
            string:
            \t.asciz "Some string"
            
            char:
            \t.byte 'a'
            .macro printStr (%str) # print a string
            \t.data
            myLabel:
            \t.asciz %str
            \t.text
            \tli a7, 4
            \tla a0, myLabel
            \tecall
            .end_macro""";
        result.setText(exampleText);
        final var selectedText = "myLabel:";
        final var selectionStart = exampleText.indexOf(selectedText);
        final var selectionEnd = selectionStart + selectedText.length();
        result.setTabSize(OTHER_SETTINGS.getEditorTabSize()); // TODO: default tab size
        final var blinkRate = OTHER_SETTINGS.getCaretBlinkRate();
        result.setCaretBlinkRate(blinkRate); // TODO: default caret blink rate
        result.setFont(FONT_SETTINGS.getCurrentFont()); // TODO: default font
        result.select(selectionStart, selectionEnd);
        return result;
    }

    public void setTextAreaTheme(final @NotNull Theme theme) {
        this.textArea.setTheme(theme);
    }

    public void setTextAreaFont(final @NotNull Font font) {
        this.textArea.setFont(font);
    }

    public void setCaretBlinkRate(final int rate) {
        this.textArea.setCaretBlinkRate(rate);
    }
}