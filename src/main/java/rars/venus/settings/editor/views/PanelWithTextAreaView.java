package rars.venus.settings.editor.views;

import org.jetbrains.annotations.NotNull;
import rars.settings.EditorThemeSettings;
import rars.venus.editors.TextEditingArea;
import rars.venus.editors.TextEditingAreaFactory;

import javax.swing.*;
import java.awt.*;

import static rars.settings.FontSettings.FONT_SETTINGS;
import static rars.settings.Settings.OTHER_SETTINGS;

public final class PanelWithTextAreaView extends JPanel {
    public final @NotNull PickerCardView pickerCardView;
    public final @NotNull TextEditingArea textArea;

    public PanelWithTextAreaView(final @NotNull PickerCardView pickerCardView) {
        super();
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.pickerCardView = pickerCardView;
        this.add(pickerCardView);
        this.add(Box.createVerticalGlue());
        this.textArea = createTextArea();
        final var outerComponent = this.textArea.getOuterComponent();
        final var textAreaSize = new Dimension(500, 300);
        outerComponent.setMinimumSize(textAreaSize);
        outerComponent.setPreferredSize(textAreaSize);
        outerComponent.setMaximumSize(textAreaSize);
        this.add(outerComponent);
    }

    private static @NotNull TextEditingArea createTextArea() {
        final var currentTheme = EditorThemeSettings.EDITOR_THEME_SETTINGS.currentTheme.toEditorTheme();
        final var result = TextEditingAreaFactory.createTextEditingArea(currentTheme);
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
        result.setTabSize(OTHER_SETTINGS.getEditorTabSize());
        result.setCaretBlinkRate(OTHER_SETTINGS.getCaretBlinkRate());
        result.setFont(FONT_SETTINGS.getCurrentFont());
        result.select(selectionStart, selectionEnd);
        return result;
    }
}