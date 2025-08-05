package rars.venus.settings.editor.views;

import org.jetbrains.annotations.NotNull;
import rars.venus.settings.editor.ColorPickerButton;
import rars.venus.util.GridBagBuilder;

import javax.swing.*;
import java.awt.*;

import static rars.Globals.EDITOR_THEME_SETTINGS;

public final class BaseStyleView extends JPanel {
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

    public BaseStyleView() {
        super(new GridBagLayout());

        final var theme = EDITOR_THEME_SETTINGS.getCurrentTheme();
        final var builder = new GridBagBuilder(this)
            .withInsets(new Insets(5, 5, 5, 5));

        // Create labels and buttons for each row
        final var fgLabel = new JLabel(FOREGROUND);
        this.foregroundColorButton = new ColorPickerButton(theme.foregroundColor);
        builder.addLabelAndField(0, fgLabel, this.foregroundColorButton);

        final var bgLabel = new JLabel(BACKGROUND);
        this.backgroundColorButton = new ColorPickerButton(theme.backgroundColor);
        builder.addLabelAndField(1, bgLabel, this.backgroundColorButton);

        final var lhLabel = new JLabel(LINE_HIGHLIGHT);
        this.lineHighlightColorButton = new ColorPickerButton(theme.lineHighlightColor);
        builder.addLabelAndField(2, lhLabel, this.lineHighlightColorButton);

        final var tsLabel = new JLabel(TEXT_SELECTION);
        this.textSelectionColorButton = new ColorPickerButton(theme.selectionColor);
        builder.addLabelAndField(3, tsLabel, this.textSelectionColorButton);

        final var caretLabel = new JLabel(CARET);
        this.caretColorButton = new ColorPickerButton(theme.caretColor);
        builder.addLabelAndField(4, caretLabel, this.caretColorButton);

        // Add a filler to push content to the top if the panel grows
        builder.addFiller(5, 2);
    }

}
