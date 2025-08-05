package rars.venus.settings.editor.views;

import org.jetbrains.annotations.NotNull;
import rars.Globals;
import rars.venus.editors.TextEditingArea;
import rars.venus.editors.TextEditingAreaFactory;

import javax.swing.*;
import java.awt.*;

import static rars.Globals.FONT_SETTINGS;
import static rars.Globals.OTHER_SETTINGS;

public final class PanelWithTextAreaView extends JPanel {
    public final @NotNull PickerCardView pickerCardView;
    public final @NotNull TextEditingArea textArea;

    public PanelWithTextAreaView(final @NotNull PickerCardView pickerCardView) {
        super(new BorderLayout());
        this.pickerCardView = pickerCardView;
        // Top: controls/card view
        this.add(pickerCardView, BorderLayout.NORTH);

        // Center: text area that should expand to fill available space
        this.textArea = createTextArea();
        final var outerComponent = this.textArea.getOuterComponent();
        // Provide only a modest minimum/preferred size; do not cap maximum to allow expansion
        final var minSize = new Dimension(300, 200);
        outerComponent.setMinimumSize(minSize);
        outerComponent.setPreferredSize(new Dimension(500, 300));
        this.add(outerComponent, BorderLayout.CENTER);
    }

    private static @NotNull TextEditingArea createTextArea() {
        final var currentTheme = Globals.EDITOR_THEME_SETTINGS.getCurrentTheme().toEditorTheme();
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
        result.setTabSize(OTHER_SETTINGS.getEditorTabSize());
        result.setCaretBlinkRate(OTHER_SETTINGS.getCaretBlinkRate());
        result.setFont(FONT_SETTINGS.getCurrentFont());
        final var selectionEnd = selectionStart + selectedText.length();
        result.select(selectionStart, selectionEnd);
        return result;
    }
}