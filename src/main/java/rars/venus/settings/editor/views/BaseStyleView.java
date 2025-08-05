package rars.venus.settings.editor.views;

import org.jetbrains.annotations.NotNull;
import rars.venus.settings.editor.ColorPickerButton;

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

        // Create labels and buttons for each row
        final var fgLabel = new JLabel(FOREGROUND);
        this.foregroundColorButton = new ColorPickerButton(theme.foregroundColor);

        final var bgLabel = new JLabel(BACKGROUND);
        this.backgroundColorButton = new ColorPickerButton(theme.backgroundColor);

        final var lhLabel = new JLabel(LINE_HIGHLIGHT);
        this.lineHighlightColorButton = new ColorPickerButton(theme.lineHighlightColor);

        final var tsLabel = new JLabel(TEXT_SELECTION);
        this.textSelectionColorButton = new ColorPickerButton(theme.selectionColor);

        final var caretLabel = new JLabel(CARET);
        this.caretColorButton = new ColorPickerButton(theme.caretColor);

        // Layout as a two-column grid: label | button
        final var gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.NONE;

        int row = 0;
        addRow(this, gbc, row++, fgLabel, this.foregroundColorButton);
        addRow(this, gbc, row++, bgLabel, this.backgroundColorButton);
        addRow(this, gbc, row++, lhLabel, this.lineHighlightColorButton);
        addRow(this, gbc, row++, tsLabel, this.textSelectionColorButton);
        addRow(this, gbc, row, caretLabel, this.caretColorButton);

        // Add a vertical glue/filler to push content to top if the panel grows
        gbc.gridx = 0;
        gbc.gridy = row + 1;
        gbc.gridwidth = 3;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.VERTICAL;
        this.add(Box.createVerticalGlue(), gbc);
    }

    private static void addRow(final JPanel panel, final GridBagConstraints gbc, final int r,
                               final JComponent label, final JComponent control) {
        // Label
        gbc.gridx = 0;
        gbc.gridy = r;
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(label, gbc);

        // Control
        gbc.gridx = 1;
        gbc.gridy = r;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(control, gbc);
    }
}
