package rars.venus.settings.editor.controllers;

import org.jetbrains.annotations.NotNull;
import rars.venus.editors.TextEditingArea;
import rars.venus.settings.editor.views.BaseStyleView;

public final class BaseStyleSettingsController {
    private final @NotNull BaseStyleView view;
    private final @NotNull TextEditingArea textArea;
    private final @NotNull EditorSettingsController parentController;

    public BaseStyleSettingsController(
        final @NotNull BaseStyleView view,
        final @NotNull TextEditingArea textArea,
        final @NotNull EditorSettingsController parentController
    ) {
        this.view = view;
        this.textArea = textArea;
        this.parentController = parentController;
        initializeButtons();
    }

    public void resetButtonValues() {
        final var settingsTheme = this.parentController.settingsTheme;
        this.view.foregroundColorButton.setColor(settingsTheme.foregroundColor);
        this.view.backgroundColorButton.setColor(settingsTheme.backgroundColor);
        this.view.textSelectionColorButton.setColor(settingsTheme.selectionColor);
        this.view.caretColorButton.setColor(settingsTheme.caretColor);
        this.view.lineHighlightColorButton.setColor(settingsTheme.lineHighlightColor);
    }

    private void initializeButtons() {
        final var backgroundColorButton = this.view.backgroundColorButton;
        backgroundColorButton.setColor(this.parentController.settingsTheme.backgroundColor);
        backgroundColorButton.addChangeListener(e -> {
            this.parentController.settingsTheme.backgroundColor = backgroundColorButton.getColor();
            this.textArea.setBackground(this.parentController.settingsTheme.backgroundColor);
        });

        final var foregroundColorButton = this.view.foregroundColorButton;
        foregroundColorButton.setColor(this.parentController.settingsTheme.foregroundColor);
        foregroundColorButton.addChangeListener(e -> {
            this.parentController.settingsTheme.foregroundColor = foregroundColorButton.getColor();
            this.textArea.setForeground(this.parentController.settingsTheme.foregroundColor);
        });

        final var selectionColorButton = this.view.textSelectionColorButton;
        selectionColorButton.setColor(this.parentController.settingsTheme.selectionColor);
        selectionColorButton.addChangeListener(e -> {
            this.parentController.settingsTheme.selectionColor = selectionColorButton.getColor();
            this.textArea.setSelectionColor(this.parentController.settingsTheme.selectionColor);
        });

        final var caretColorButton = this.view.caretColorButton;
        caretColorButton.setColor(this.parentController.settingsTheme.caretColor);
        caretColorButton.addChangeListener(e -> {
            this.parentController.settingsTheme.caretColor = caretColorButton.getColor();
            this.textArea.setCaretColor(this.parentController.settingsTheme.caretColor);
        });

        final var lineHighlightColorButton = this.view.lineHighlightColorButton;
        lineHighlightColorButton.setColor(this.parentController.settingsTheme.lineHighlightColor);
        lineHighlightColorButton.addChangeListener(e -> {
            this.parentController.settingsTheme.lineHighlightColor = lineHighlightColorButton.getColor();
            this.textArea.setLineHighlightColor(this.parentController.settingsTheme.lineHighlightColor);
        });
    }
}
