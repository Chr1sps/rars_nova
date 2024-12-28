package rars.venus.settings.editor.controllers;

import org.jetbrains.annotations.NotNull;
import rars.venus.editors.TextEditingArea;
import rars.venus.settings.editor.views.OtherSettingsView;

import static rars.settings.OtherSettings.OTHER_SETTINGS;

public final class OtherSettingsController {
    private final @NotNull OtherSettingsView view;
    private final @NotNull TextEditingArea textArea;
    private int caretBlinkRate, editorTabSize;

    public OtherSettingsController(
        final @NotNull OtherSettingsView view,
        final @NotNull TextEditingArea textArea
    ) {
        this.view = view;
        this.textArea = textArea;
        loadValuesFromSettings();
        initializeBlinkRateSpinner();
        initializeTabSizeSpinner();
    }

    private void loadValuesFromSettings() {
        this.caretBlinkRate = OTHER_SETTINGS.getCaretBlinkRate();
        this.editorTabSize = OTHER_SETTINGS.getEditorTabSize();
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
        OTHER_SETTINGS.setCaretBlinkRateAndSave(this.caretBlinkRate);
        OTHER_SETTINGS.setEditorTabSizeAndSave(this.editorTabSize);
    }
}
