package rars.venus.settings.editor.controllers;

import org.jetbrains.annotations.NotNull;
import rars.venus.settings.editor.views.OtherSettingsView;

import static rars.settings.Settings.OTHER_SETTINGS;

public final class OtherSettingsController {
    private final @NotNull OtherSettingsView view;
    private final @NotNull EditorSettingsController parentController;
    private int caretBlinkRate, editorTabSize;

    public OtherSettingsController(
        final @NotNull OtherSettingsView view,
        final @NotNull EditorSettingsController parentController
    ) {
        this.view = view;
        this.parentController = parentController;
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
            this.parentController.editorSettingsView.panelWithTextAreaView.setCaretBlinkRate(this.caretBlinkRate);
        });
    }

    private void initializeTabSizeSpinner() {
        this.view.tabSizeSpinner.setValue(this.editorTabSize);
        this.view.tabSizeSpinner.addChangeListener(e -> {
            this.editorTabSize = (int) this.view.tabSizeSpinner.getValue();
            this.parentController.editorSettingsView.panelWithTextAreaView.setTextAreaTabSize(this.editorTabSize);
        });
    }

    public void applySettings() {
        OTHER_SETTINGS.setCaretBlinkRateAndSave(this.caretBlinkRate);
        OTHER_SETTINGS.setEditorTabSizeAndSave(this.editorTabSize);
    }
}
