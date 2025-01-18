package rars.venus.settings.editor.controllers;

import org.jetbrains.annotations.NotNull;
import rars.settings.OtherSettings;
import rars.venus.editors.TextEditingArea;
import rars.venus.settings.editor.views.OtherSettingsView;

public final class OtherSettingsController {
    private final @NotNull OtherSettingsView view;
    private final @NotNull TextEditingArea textArea;
    private final @NotNull OtherSettings settings;
    private int caretBlinkRate, editorTabSize;

    public OtherSettingsController(
        final @NotNull OtherSettingsView view,
        final @NotNull TextEditingArea textArea,
        final @NotNull OtherSettings settings
    ) {
        this.view = view;
        this.textArea = textArea;
        this.settings = settings;
        loadValuesFromSettings();
        initializeBlinkRateSpinner();
        initializeTabSizeSpinner();
    }

    private void loadValuesFromSettings() {
        this.caretBlinkRate = this.settings.getCaretBlinkRate();
        this.editorTabSize = this.settings.getEditorTabSize();
    }

    private void initializeBlinkRateSpinner() {
        this.view.blinkRateSpinner.setValue(this.caretBlinkRate);
        this.view.blinkRateSpinner.addChangeListener(e -> {
            this.caretBlinkRate = (int) this.view.blinkRateSpinner.getValue();
            this.textArea.setCaretBlinkRate(this.caretBlinkRate);
        });
    }

    private void initializeTabSizeSpinner() {
        this.view.tabSizeSpinner.setValue(this.editorTabSize);
        this.view.tabSizeSpinner.addChangeListener(e -> {
            this.editorTabSize = (int) this.view.tabSizeSpinner.getValue();
            this.textArea.setTabSize(this.editorTabSize);
        });
    }

    public void applySettings() {
        this.settings.setCaretBlinkRateAndSave(this.caretBlinkRate);
        this.settings.setEditorTabSizeAndSave(this.editorTabSize);
    }
}
