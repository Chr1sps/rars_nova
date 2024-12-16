package rars.venus.settings.editor;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

public final class BaseStylePickerPanel extends JPanel {
    private static final @NotNull JLabel FOREGROUND_LABEL = new JLabel("Foreground");
    private static final @NotNull JLabel BACKGROUND_LABEL = new JLabel("Background");
    private static final @NotNull JLabel LINE_HIGHLIGHT = new JLabel("Line highlight");
    private static final @NotNull JLabel TEXT_SELECTION = new JLabel("Text selection");
    private static final @NotNull JLabel CARET = new JLabel("Caret");
    private final @NotNull ColorPickerButton foregroundColorButton, backgroundColorButton, lineHighlightColorButton,
            textSelectionColorButton, caretColorButton;

    public BaseStylePickerPanel() {
        this.setLayout(new GridBagLayout());
        final var gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        gbc.gridy = 0;
        gbc.gridx = 0;
        this.add(FOREGROUND_LABEL, gbc);
        this.foregroundColorButton = new ColorPickerButton();
        gbc.gridx = 1;
        this.add(foregroundColorButton, gbc);

        gbc.gridy = 1;
        gbc.gridx = 0;
        this.add(BACKGROUND_LABEL, gbc);
        this.backgroundColorButton = new ColorPickerButton();
        gbc.gridx = 1;
        this.add(backgroundColorButton, gbc);

        gbc.gridy = 2;
        gbc.gridx = 0;
        this.add(LINE_HIGHLIGHT, gbc);
        this.lineHighlightColorButton = new ColorPickerButton();
        gbc.gridx = 1;
        this.add(lineHighlightColorButton, gbc);

        gbc.gridy = 3;
        gbc.gridx = 0;
        this.add(TEXT_SELECTION, gbc);
        this.textSelectionColorButton = new ColorPickerButton();
        gbc.gridx = 1;
        this.add(textSelectionColorButton, gbc);

        gbc.gridy = 4;
        gbc.gridx = 0;
        this.add(CARET, gbc);
        this.caretColorButton = new ColorPickerButton();
        gbc.gridx = 1;
        this.add(caretColorButton, gbc);
    }

    public @NotNull ColorPickerButton getForegroundColorButton() {
        return foregroundColorButton;
    }

    public @NotNull ColorPickerButton getBackgroundColorButton() {
        return backgroundColorButton;
    }

    public @NotNull ColorPickerButton getTextSelectionColorButton() {
        return textSelectionColorButton;
    }

    public @NotNull ColorPickerButton getCaretColorButton() {
        return caretColorButton;
    }

    public @NotNull ColorPickerButton getLineHighlightColorButton() {
        return lineHighlightColorButton;
    }
}
