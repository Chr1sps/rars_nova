package rars.venus.settings.editor.controllers;

import org.jetbrains.annotations.NotNull;
import rars.settings.SettingsTheme;
import rars.venus.editors.TextEditingArea;
import rars.venus.settings.editor.views.BaseStyleView;

public final class BaseStyleSettingsController {
    private final @NotNull BaseStyleView view;
    private final @NotNull TextEditingArea textArea;
    private final @NotNull SettingsTheme settingsTheme;

    public BaseStyleSettingsController(
        final @NotNull BaseStyleView view,
        final @NotNull TextEditingArea textArea,
        final @NotNull SettingsTheme settingsTheme
    ) {
        this.view = view;
        this.textArea = textArea;
        this.settingsTheme = settingsTheme;
        initializeButtons();
    }

    public void resetButtonValues() {
        this.view.foregroundColorButton.setColor(this.settingsTheme.foregroundColor);
        this.view.backgroundColorButton.setColor(this.settingsTheme.backgroundColor);
        this.view.textSelectionColorButton.setColor(this.settingsTheme.selectionColor);
        this.view.caretColorButton.setColor(this.settingsTheme.caretColor);
        this.view.lineHighlightColorButton.setColor(this.settingsTheme.lineHighlightColor);
    }

    private void initializeButtons() {
        final var backgroundColorButton = this.view.backgroundColorButton;
        backgroundColorButton.setColor(this.settingsTheme.backgroundColor);
        backgroundColorButton.addChangeListener(e -> {
            this.settingsTheme.backgroundColor = backgroundColorButton.getColor();
            this.textArea.setBackground(this.settingsTheme.backgroundColor);
        });

        final var foregroundColorButton = this.view.foregroundColorButton;
        foregroundColorButton.setColor(this.settingsTheme.foregroundColor);
        foregroundColorButton.addChangeListener(e -> {
            this.settingsTheme.foregroundColor = foregroundColorButton.getColor();
            this.textArea.setForeground(this.settingsTheme.foregroundColor);
        });

        final var selectionColorButton = this.view.textSelectionColorButton;
        selectionColorButton.setColor(this.settingsTheme.selectionColor);
        selectionColorButton.addChangeListener(e -> {
            this.settingsTheme.selectionColor = selectionColorButton.getColor();
            this.textArea.setSelectionColor(this.settingsTheme.selectionColor);
        });

        final var caretColorButton = this.view.caretColorButton;
        caretColorButton.setColor(this.settingsTheme.caretColor);
        caretColorButton.addChangeListener(e -> {
            this.settingsTheme.caretColor = caretColorButton.getColor();
            this.textArea.setCaretColor(this.settingsTheme.caretColor);
        });

        final var lineHighlightColorButton = this.view.lineHighlightColorButton;
        lineHighlightColorButton.setColor(this.settingsTheme.lineHighlightColor);
        lineHighlightColorButton.addChangeListener(e -> {
            this.settingsTheme.lineHighlightColor = lineHighlightColorButton.getColor();
            this.textArea.setLineHighlightColor(this.settingsTheme.lineHighlightColor);
        });
    }
}
