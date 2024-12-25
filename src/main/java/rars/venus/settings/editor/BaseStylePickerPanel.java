package rars.venus.settings.editor;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.Objects;

import static rars.settings.Settings.EDITOR_THEME_SETTINGS;
import static rars.venus.settings.editor.SyntaxStylePickerPanel.buildRow;

public final class BaseStylePickerPanel extends JPanel {
    private static final @NotNull String FOREGROUND = "Foreground",
        BACKGROUND = "Background",
        LINE_HIGHLIGHT = "Line highlight",
        CARET = "Caret",
        TEXT_SELECTION = "Text selection";
    public final @NotNull ColorPickerButton
        foregroundColorButton,
        backgroundColorButton,
        lineHighlightColorButton,
        textSelectionColorButton,
        caretColorButton;

    public BaseStylePickerPanel() {
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        final var theme = EDITOR_THEME_SETTINGS.getCurrentTheme();

        // foreground
        final var foregroundSection = new OptionSection(FOREGROUND, null, theme.foregroundColor);
        this.foregroundColorButton = Objects.requireNonNull(foregroundSection.colorPickerButton);

        // background
        final var backgroundSection = new OptionSection(BACKGROUND, null, theme.backgroundColor);
        this.backgroundColorButton = Objects.requireNonNull(backgroundSection.colorPickerButton);

        // line highlight
        final var lineHighlightSection = new OptionSection(LINE_HIGHLIGHT, null, theme.lineHighlightColor);
        this.lineHighlightColorButton = Objects.requireNonNull(lineHighlightSection.colorPickerButton);

        // top row
        final var topRow = buildRow(false, foregroundSection, backgroundSection, lineHighlightSection);
        this.add(topRow);
        this.add(Box.createVerticalStrut(5));

        // text selection
        final var textSelectionSection = new OptionSection(TEXT_SELECTION, null, theme.selectionColor);
        this.textSelectionColorButton = Objects.requireNonNull(textSelectionSection.colorPickerButton);

        // caret
        final var caretSection = new OptionSection(CARET, null, theme.caretColor);
        this.caretColorButton = Objects.requireNonNull(caretSection.colorPickerButton);

        // bottom row
        final var bottomRow = buildRow(true, textSelectionSection, caretSection);
        this.add(bottomRow);
        this.add(Box.createVerticalGlue());
    }
}
