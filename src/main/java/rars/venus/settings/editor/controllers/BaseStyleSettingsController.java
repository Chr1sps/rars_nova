package rars.venus.settings.editor.controllers;

import org.jetbrains.annotations.NotNull;
import rars.venus.editors.TextEditingArea;
import rars.venus.settings.editor.views.BaseStyleView;

import java.awt.*;

import static rars.settings.EditorThemeSettings.EDITOR_THEME_SETTINGS;

public final class BaseStyleSettingsController {
    private final @NotNull BaseStyleView view;
    private final @NotNull TextEditingArea textArea;
    private @NotNull Color foregroundColor, backgroundColor, selectionColor, caretColor, lineHighlightColor;


    public BaseStyleSettingsController(final @NotNull BaseStyleView view,
                                       final @NotNull TextEditingArea textArea) {
        this.view = view;
        this.textArea = textArea;
        loadValuesFromSettings();
        initializeButtons();
    }

    private void loadValuesFromSettings() {
        final var currentTheme = EDITOR_THEME_SETTINGS.currentTheme;
        this.foregroundColor = currentTheme.foregroundColor;
        this.backgroundColor = currentTheme.backgroundColor;
        this.selectionColor = currentTheme.selectionColor;
        this.caretColor = currentTheme.caretColor;
        this.lineHighlightColor = currentTheme.lineHighlightColor;
    }

    private void initializeButtons() {
        final var backgroundColorButton = this.view.backgroundColorButton;
        backgroundColorButton.setColor(this.backgroundColor);
        backgroundColorButton.addChangeListener(e -> {
            this.backgroundColor = backgroundColorButton.getColor();
            this.textArea.setBackground(this.backgroundColor);
        });

        final var foregroundColorButton = this.view.foregroundColorButton;
        foregroundColorButton.setColor(this.foregroundColor);
        foregroundColorButton.addChangeListener(e -> {
            this.foregroundColor = foregroundColorButton.getColor();
            this.textArea.setForeground(this.foregroundColor);
        });

        final var selectionColorButton = this.view.textSelectionColorButton;
        selectionColorButton.setColor(this.selectionColor);
        selectionColorButton.addChangeListener(e -> {
            this.selectionColor = selectionColorButton.getColor();
            this.textArea.setSelectionColor(this.selectionColor);
        });

        final var caretColorButton = this.view.caretColorButton;
        caretColorButton.setColor(this.caretColor);
        caretColorButton.addChangeListener(e -> {
            this.caretColor = caretColorButton.getColor();
            this.textArea.setCaretColor(this.caretColor);
        });

        final var lineHighlightColorButton = this.view.lineHighlightColorButton;
        lineHighlightColorButton.setColor(this.lineHighlightColor);
        lineHighlightColorButton.addChangeListener(e -> {
            this.lineHighlightColor = lineHighlightColorButton.getColor();
            this.textArea.setLineHighlightColor(this.lineHighlightColor);
        });
    }

    public void writeSettings() {
        EDITOR_THEME_SETTINGS.currentTheme.foregroundColor = this.foregroundColor;
        EDITOR_THEME_SETTINGS.currentTheme.backgroundColor = this.backgroundColor;
        EDITOR_THEME_SETTINGS.currentTheme.selectionColor = this.selectionColor;
        EDITOR_THEME_SETTINGS.currentTheme.caretColor = this.caretColor;
        EDITOR_THEME_SETTINGS.currentTheme.lineHighlightColor = this.lineHighlightColor;
    }
}
